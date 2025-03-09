import lombok.Getter;
import org.opencv.core.*;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.util.*;

public class ImageMatcher {

    private static final HashMap<String, List<File>> images = new HashMap<>();

    static {
        Main.PLAYABLE_SETS.forEach(set -> {
            File folder = new File(Main.RESOURCE_PATH + set);
            File[] all = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg"));
            if (all == null) {
                throw new RuntimeException("Cannot find files");
            }
            images.put(set, Arrays.stream(all).toList());
        });
        precomputeDescriptors();

    }

    private static void precomputeDescriptors() {
        ORB orb = ORB.create(1000);
        for (String set : Main.PLAYABLE_SETS) {
            for (File imgFile : images.get(set)) {
                if (!DescriptorCache.contains(imgFile.getAbsolutePath())) {
                    Mat img = Imgcodecs.imread(imgFile.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
                    if (!img.empty()) {
                        MatOfKeyPoint keypointsImg = new MatOfKeyPoint();
                        Mat descriptorsImg = new Mat();
                        orb.detectAndCompute(img, new Mat(), keypointsImg, descriptorsImg);
                        DescriptorCache.addDescriptor(imgFile.getAbsolutePath(), descriptorsImg);
                    }
                }
            }
        }
    }

    static class MatchResult {
        Card card;
        @Getter
        int matches;

        MatchResult(Card card, int matches) {
            this.card = card;
            this.matches = matches;
        }

        MatchResult() {
            this.card = null;
            matches = -1;
        }

    }

    public static Card findBestMatchParallel(Mat frame, String set) {
        String folderPath = Main.RESOURCE_PATH + set;
        List<File> imageFiles;
        if (Objects.equals(set, "ALL")) {
            imageFiles = Main.PLAYABLE_SETS.parallelStream()
                    .map(images::get)
                    .flatMap(Collection::parallelStream).toList();

        } else {
            imageFiles = images.get(set);
        }

        if (imageFiles.isEmpty()) {
            Main.logInfo("No images found in directory.");
            return null;
        }

        ORB orb = ORB.create(1000);
        MatOfKeyPoint keypointsFrame = new MatOfKeyPoint();
        Mat descriptorsFrame = new Mat();
        orb.detectAndCompute(frame, new Mat(), keypointsFrame, descriptorsFrame);

        var results = imageFiles.stream().parallel().map(img -> getMatchResult(img, orb, descriptorsFrame, folderPath));

        var matchResult = results.max(Comparator.comparingInt(MatchResult::getMatches)).orElse(new MatchResult());
        Main.logDebug("Score: " + matchResult.matches);
        if (matchResult.matches < 20){
            return null;
        }
        return matchResult.card;
    }

    private static MatchResult getMatchResult(File imageFile, ORB orb, Mat descriptorsFrame, String folderPath) {
        String filePath = imageFile.getAbsolutePath();
        int lastIndexOfSlash = filePath.lastIndexOf("\\");
        if(lastIndexOfSlash == -1){
            lastIndexOfSlash = filePath.lastIndexOf("/");
        }
        String set = filePath.substring(lastIndexOfSlash-3, lastIndexOfSlash);
        Mat descriptorsImg;
        if (DescriptorCache.contains(filePath)) {
            descriptorsImg = DescriptorCache.getDescriptor(filePath);
        } else {
            Mat img = Imgcodecs.imread(filePath, Imgcodecs.IMREAD_GRAYSCALE);
            if (img.empty()) {
                return new MatchResult(new Card(), -1);
            }

            MatOfKeyPoint keypointsImg = new MatOfKeyPoint();
            descriptorsImg = new Mat();
            orb.detectAndCompute(img, new Mat(), keypointsImg, descriptorsImg);

            // Store descriptors in cache
            DescriptorCache.addDescriptor(filePath, descriptorsImg);
        }

        int matches = matchFeatures(descriptorsFrame, descriptorsImg);

        String name = imageFile.getName().substring(0, imageFile.getName().lastIndexOf("_")).replace("_", " ");
        String cardNumber = imageFile.getName().substring(imageFile.getName().lastIndexOf("_") + 1, imageFile.getName().lastIndexOf("."));
        return new MatchResult(new Card(set, name, cardNumber), matches);
    }


    private static int matchFeatures(Mat descriptors1, Mat descriptors2) {
        if (descriptors1.empty() || descriptors2.empty()) {
            Main.logInfo("Error: One or both descriptor matrices are empty!");
            return 0;
        }

        try {
            BFMatcher matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING);
            List<MatOfDMatch> knnMatches = new ArrayList<>();
            matcher.knnMatch(descriptors1, descriptors2, knnMatches, 2); // Get 2 best matches

            // Apply Lowe’s Ratio Test
            double ratioThreshold = 0.75;
            int goodMatches = 0;
            for (MatOfDMatch matOfDMatch : knnMatches) {
                if (matOfDMatch.rows() > 1) {
                    DMatch[] matches = matOfDMatch.toArray();
                    if (matches[0].distance < ratioThreshold * matches[1].distance) {
                        goodMatches++;
                    }
                }
            }
            return goodMatches;
        } catch (Exception e) {
            Main.logInfo("Error during feature matching: " + e.getMessage());
            return 0;
        }
    }
}
