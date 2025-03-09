import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TextMatcher {
    public static Card findCard(String scannedText, List<Card> collection) {

        List<Card> longCollection = collection.stream().filter(c -> c.getCardName().length() >= 8).sorted((Comparator.comparingInt(o -> -1 * o.getCardName().length()))).collect(Collectors.toList());
        List<Card> shortCollection = collection.stream().filter(c -> c.getCardName().length() < 8).sorted((Comparator.comparingInt(o -> -1 * o.getCardName().length()))).collect(Collectors.toList());

        Card exactMatchLong = findExactMatch(scannedText, longCollection);
        if (exactMatchLong != null) {
            return exactMatchLong;
        }

        Card closestMatchLong = findClosestMatch(scannedText, longCollection, 3);
        if (closestMatchLong != null) {
            return closestMatchLong;
        }

        Card exactMatchShort = findExactMatch(scannedText, shortCollection);
        if (exactMatchShort != null) {
            return exactMatchShort;
        }
        return findClosestMatch(scannedText, shortCollection, 0);
    }

    private static String performOCR(Mat frame, ITesseract tesseract) {
        try {
            // Convert Mat to byte array
            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".jpg", frame, matOfByte);
            byte[] byteArray = matOfByte.toArray();

            // Convert byte array to BufferedImage
            InputStream in = new ByteArrayInputStream(byteArray);
            BufferedImage bufferedImage = ImageIO.read(in);

            // Perform OCR on the BufferedImage
            return tesseract.doOCR(bufferedImage);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static Card findExactMatch(String scannedText, List<Card> collection) {
        for (Card card : collection) {

            if (scannedText.strip().toLowerCase().contains(card.getCardName().strip().toLowerCase())) {
                return card;

            }
        }
        return null;
    }

    public static Card findClosestMatch(String longString, List<Card> cards, int threshold) {
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
        Card closestMatch = null;
        int smallestDistance = Integer.MAX_VALUE;
//        int threshold = 2;  // Define a threshold for acceptable match


        // Create substrings using a sliding window approach
//        int windowSize = cards.stream().map(Card::getCardName).mapToInt(String::length).max().orElse(0);
        for (int i = 0; i <= longString.length(); i++) {

            for (Card card : cards) {
                String substring = longString.substring(i, Math.min(i + card.getCardName().length(), longString.length()));
                int distance = levenshteinDistance.apply(substring.strip().toUpperCase()
                        .replace('Q', 'O'), card.getCardName().strip().toUpperCase()
                        .replace('Q', 'O'));
                if (distance < smallestDistance) {
                    smallestDistance = distance;
                    closestMatch = card;
                }
            }
        }

        // Check if the closest match is within the acceptable threshold
        if (smallestDistance <= threshold) {
            return closestMatch;
        } else {
            return null;  // No match found within the threshold
        }
    }

}
