import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.ImageWindow;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.time.Instant;
import java.util.*;

public class Main {
    private static final int WEBCAM_ID = ConfigUtil.getConfigValueAsInt("WEBCAM_ID");
    private static final String LOG_LEVEL = ConfigUtil.getConfigValue("LOG_LEVEL");

    private static final List<Integer> ZERO = List.of(96, 48);
    private static final List<Integer> ONE = List.of(97, 49);
    private static final List<Integer> TWO = List.of(98, 50);
    private static final List<Integer> THREE = List.of(99, 51);
    private static final List<Integer> FOUR = List.of(100, 52);
    private static final List<Integer> FIVE = List.of(101, 53);
    private static final List<Integer> S = List.of(83);
    private static final List<Integer> D = List.of(68);
    private static final List<Integer> A = List.of(65);
    private static final List<Integer> LEFT = List.of(37);
    private static final List<Integer> UP = List.of(38);
    private static final List<Integer> RIGHT = List.of(39);
    private static final List<Integer> DOWN = List.of(40);
    private static final List<Integer> ENTER = List.of(10);
    private static final List<Integer> ESCAPE = List.of(27);

    private static final String SCANNING_TEXT = "Scanning...";
    private static final String LOADING_TEXT = "Loading...";
    private static final String OPTIONS_TEXT = "? \n1(N)/2(H)/3(F)/4(HF) to add. \n0 to reset. 5 to add bulk";
    private static int setId;
    private static final List<String> SETS = List.of("ALL", "SOR", "SHD", "TWI", "JTL");


    public static final List<String> PLAYABLE_SETS = SETS.stream().filter(s -> !Objects.equals(s, "ALL")).toList();
    private static final List<Card> currentCards = new ArrayList<>();

    private static final CollectionUtil collectionUtil = new CollectionUtil();
    private static final String BULK_TEXT = "Add how many cards?";
    public static final String RESOURCE_PATH = "src/main/resources/images/";
    private static String tempDisplayText = "Added X cards.";

    private static Instant timeToDisplay = Instant.MIN;
    private static boolean debugMode = false;
    private static boolean bulkMode = false;
    private static final Map<Integer, Integer> bulkAmount = new HashMap<>();

    private static int selected = 0;

    static {
        // Load the OpenCV native library
        OpenCV.loadLocally();
        resetBulkAmount();
        var startSet = ConfigUtil.getConfigValue("START_SET");
        var index = SETS.indexOf(startSet);
        setId = index == -1 ? 0: index;
    }

    public static void main(String[] args) throws InterruptedException {
        // Open a connection to the webcam (usually device 0)
        VideoCapture capture = new VideoCapture(WEBCAM_ID);

        if (!capture.isOpened()) {
            logInfo("Error: Could not open video capture.");
            return;
        }


        Mat frame = new Mat();
        String displayText = LOADING_TEXT;
        Card foundCard = null;

        boolean shouldRun = true;

        logInfo("Started SWU scan");
        while (shouldRun) {
            // Read a frame from the webcam
            if (capture.read(frame)) {
                // Perform OCR every second
                // Add the display text to the frame with a black box behind it

                addTextToFrame(frame, displayText);
                var key = HighGui.pressedKey;

                if (debugMode) {
                    var debugText = "KeyId: " + key;
                    logDebug("KeyId: " + key);
                    addTextToFrame(frame, debugText, 200, 90, 0.75, 1, false);
                }

                if(bulkMode){
                    String bulkModeText  = "Normal";
                    String bulkModeText2 = "Hyper";
                    String bulkModeText3 = "Foil";
                    String bulkModeText4 = "Hyperfoil";

                    int margin = 60;
                    int y = 90;
                    int x = 150;
                    int x2 = 300;

                    addTextToFrame(frame, bulkModeText,  x+19, y, 0.75, 1, false);
                    addTextToFrame(frame, bulkModeText2, x+36, y+margin, 0.75, 1, false);
                    addTextToFrame(frame, bulkModeText3, x+61, y+2*margin, 0.75, 1, false);
                    addTextToFrame(frame, bulkModeText4, x, y+3*margin, 0.75, 1, false);

                    addTextToFrame(frame, "< " + bulkAmount.get(0) + " >",  x2, y, 0.75, 1, selected == 0);
                    addTextToFrame(frame, "< " + bulkAmount.get(1) + " >", x2, y+margin, 0.75, 1, selected == 1);
                    addTextToFrame(frame, "< " + bulkAmount.get(2) + " >", x2, y+2*margin, 0.75, 1, selected == 2);
                    addTextToFrame(frame, "< " + bulkAmount.get(3) + " >", x2, y+3*margin, 0.75, 1, selected == 3);
                }

                if (timeToDisplay.isAfter(Instant.now())) {
                    addTextToFrame(frame, tempDisplayText, 10, 60, 0.75, 1, false);
                } else {
                    addTextToFrame(frame, "Set: " + SETS.get(setId), 10, 85, 0.5, 1, false);
                }

                // Display the frame using HighGui
                HighGui.imshow("Webcam", frame);


                if (HighGui.waitKey(250) == 'q') {
                    break;
                } else {
                    if (displayText.equals(SCANNING_TEXT) || displayText.equals(LOADING_TEXT)) {
                        TimeMeasure.start("Search "+SETS.get(setId)+" Cards");
                        foundCard = ImageMatcher.findBestMatchParallel(frame, SETS.get(setId));
//                        foundCard = new Card("SHD","testcard","000");
                        TimeMeasure.end("Search "+SETS.get(setId)+" Cards");
                        if (foundCard != null) {
                            displayText = foundCard.getUniqueDisplayName() + OPTIONS_TEXT;
                        } else {
                            displayText = SCANNING_TEXT;
                        }
                    }
                }

                if (ONE.contains(key) && foundCard != null) {
                    logTempText("Added regular " + foundCard.getUniqueDisplayName() + " to csv");
                    saveCard(foundCard, false, 1);
                    foundCard = null;
                    displayText = SCANNING_TEXT;
                }
                if (TWO.contains(key) && foundCard != null) {
                    if (displayText.equals(BULK_TEXT)) {
                        logTempText("Added 2 regular " + foundCard.getUniqueDisplayName() + " to csv");
                        saveCard(foundCard, false, 2);
                        foundCard = null;
                        displayText = SCANNING_TEXT;
                    } else if (saveHyperspaceCard(foundCard, false)) {
                        logTempText("Added hyperspace " + foundCard.getUniqueDisplayName() + " to csv");
                        foundCard = null;
                        displayText = SCANNING_TEXT;
                    }
                }
                if (THREE.contains(key) && foundCard != null) {
                    if (displayText.equals(BULK_TEXT)) {
                        logTempText("Added 3 regular " + foundCard.getUniqueDisplayName() + " to csv");
                        saveCard(foundCard, false, 3);
                    } else {
                        logTempText("Added foil " + foundCard.getUniqueDisplayName() + " to csv");
                        saveCard(foundCard, true, 1);
                    }
                    foundCard = null;
                    displayText = SCANNING_TEXT;
                }
                if (FOUR.contains(key) && foundCard != null) {
                    if (displayText.equals(BULK_TEXT)) {
                        logTempText("Added 4 regular " + foundCard.getUniqueDisplayName() + " to csv");
                        saveCard(foundCard, false, 4);
                        foundCard = null;
                        displayText = SCANNING_TEXT;
                    } else if (saveHyperspaceCard(foundCard, true)) {
                        logTempText("Added hyperspace foil " + foundCard.getUniqueDisplayName() + " to csv");
                        saveCard(foundCard, false, 1);
                        foundCard = null;
                        displayText = SCANNING_TEXT;
                    }
                }
                if (FIVE.contains(key) && foundCard != null) {
                    displayText = "Add multiple "+foundCard.getUniqueDisplayName();
                    bulkMode = true;
                }
                if (ZERO.contains(key)) {
                    logInfo("Reset");
                    foundCard = null;
                    displayText = SCANNING_TEXT;
                }
                if (S.contains(key)) {
                    setId = (setId + 1) % (SETS.size());
                }
                if (A.contains(key)) {
                    setId = SETS.indexOf("ALL");
                }
                if (D.contains(key)) {
                    debugMode = !debugMode;
                    logInfo("Toggle debug");
                }
                if (LEFT.contains(key)) {
                    if(bulkAmount.get(selected) > 0){
                        bulkAmount.put(selected, bulkAmount.get(selected) - 1);
                    }

                }
                if (UP.contains(key)) {
                    selected = Math.max(0, selected - 1);
                }
                if (RIGHT.contains(key)) {
                    bulkAmount.put(selected, bulkAmount.get(selected) + 1);
                }
                if (DOWN.contains(key)) {
                    selected = Math.min(3, selected + 1);
                }
                if (ESCAPE.contains(key)) {
                    bulkMode = false;
                    resetBulkAmount();
                    bulkMode = false;
                    selected = 0;
                    displayText = foundCard.getUniqueDisplayName() + OPTIONS_TEXT;
                }
                if (ENTER.contains(key) && foundCard != null) {
                    if(bulkMode){
                        if(!saveHyperspaceCard(foundCard, false, bulkAmount.get(2))){
                            logTempText("No hyperspace available for card");
                        } else if (!saveHyperspaceCard(foundCard, true, bulkAmount.get(3))){
                            logTempText("No hyperspace available for card");
                        } else {
                            saveCard(foundCard, false, bulkAmount.get(0));
                            saveCard(foundCard, true, bulkAmount.get(1));
                            logTempText("Added bulk of "+foundCard.getUniqueDisplayName());
                            resetBulkAmount();
                            bulkMode = false;
                            displayText = SCANNING_TEXT;
                            selected = 0;
                        }
                    }
                }

            } else {
                logInfo("Error: Could not read frame.");
                break;
            }
            shouldRun = ((ImageWindow) HighGui.windows.values().toArray()[0]).frame.isVisible();

        }

        // Release the capture and close all HighGui windows
        capture.release();
        HighGui.destroyAllWindows();
        System.exit(0);
    }

    private static void resetBulkAmount() {
        bulkAmount.put(0,0);
        bulkAmount.put(1,0);
        bulkAmount.put(2,0);
        bulkAmount.put(3,0);
    }

    private static void logTempText(String text) {
        timeToDisplay = Instant.now().plusSeconds(3);
        tempDisplayText = text;
    }

    private static void saveCard(Card card, boolean isFoil, int count) {
        if(count == 0){
            return;
        }
        currentCards.add(new Card(card.getSet(), card.getCardName(), card.getCardNumber(), count, isFoil));
        collectionUtil.saveToCsv(currentCards);
    }
    private static boolean saveHyperspaceCard(Card card, boolean isFoil) {
        return saveHyperspaceCard(card,isFoil,1);
    }

    private static boolean saveHyperspaceCard(Card card, boolean isFoil, int count) {
        if(count == 0){
            return true;
        }
        Card hyperspaceCard = collectionUtil.getHyperspaceCardFromName(card.getCardName(), SETS.get(setId));
        if (hyperspaceCard == null) {
            logInfo("No hyperspace available for card");
            return false;
        }
        currentCards.add(new Card(hyperspaceCard.getSet(), hyperspaceCard.getCardName(), hyperspaceCard.getCardNumber(), count, isFoil));
        collectionUtil.saveToCsv(currentCards);
        return true;

    }


    private static void addTextToFrame(Mat frame, String text) {
        addTextToFrame(frame, text, 10, frame.rows() - 50, 1.0, 2, false);
    }
    private static void addTextToFrame(Mat frame, String text, int x, int y, double fontScale, int thickness, boolean isHighlighted) {
        Scalar backgroundColor;
        if (isHighlighted){
            backgroundColor = new Scalar(100, 100, 100);
        } else {
            backgroundColor = new Scalar(0, 0, 0);
        }

        int fontFace = 0;
        int[] baseLine = new int[1];
        Point textOrg = new Point(x, y);

        // Split the text into lines
        String[] lines = text.split("\n");

        // Calculate the text size for each line and find the maximum width
        double maxWidth = 0;
        int totalHeight = 0;
        int lineSpacing = 30; // Additional space between lines
        int hSpace = 8;

        for (String line : lines) {
            Size textSize = Imgproc.getTextSize(line, fontFace, fontScale, thickness, baseLine);
            maxWidth = Math.max(maxWidth, textSize.width) + hSpace;
            totalHeight += (int) (textSize.height + lineSpacing); // Adding space between lines
        }

        // Draw the black rectangle for all lines
        Imgproc.rectangle(frame, new Point(textOrg.x - hSpace, textOrg.y - totalHeight),
                new Point(textOrg.x + maxWidth, textOrg.y + baseLine[0]),
                backgroundColor, -1);

        // Draw each line of text with additional spacing
        for (int i = 0; i < lines.length; i++) {
            Point lineOrg = new Point(textOrg.x, textOrg.y - totalHeight + (i + 1) * (baseLine[0] + lineSpacing));
            Imgproc.putText(frame, lines[i], lineOrg, fontFace, fontScale, new Scalar(255, 255, 255), thickness);
        }
    }

    public static void logInfo(String message){
        System.out.println(message);
    }
    public static void logDebug(String message){
        if("DEBUG".equals(LOG_LEVEL)){
            System.out.println(message);
        }
    }

//    private static void addTextToFrame(Mat frame, String text, int x, int y, double fontScale, int thickness) {
//        int fontFace = 0;
//        int[] baseLine = new int[1];
//        Point textOrg = new Point(x, y);
//
//        // Split the text into lines
//        String[] lines = text.split("\n");
//
//        // Calculate the text size for each line and find the maximum width
//        double maxWidth = 0;
//        int totalHeight = 0;
//        int lineSpacing = 30; // Additional space between lines
//        int hSpace = 8;
//
//        for (String line : lines) {
//            Size textSize = Imgproc.getTextSize(line, fontFace, fontScale, thickness, baseLine);
//            maxWidth = Math.max(maxWidth, textSize.width) + hSpace;
//            totalHeight += textSize.height + lineSpacing; // Adding space between lines
//        }
//
//        // Draw the black rectangle for all lines
//        Imgproc.rectangle(frame, new Point(textOrg.x - hSpace, textOrg.y - totalHeight),
//                new Point(textOrg.x + maxWidth, textOrg.y + baseLine[0]),
//                new Scalar(0, 0, 0), -1);
//
//        // Draw each line of text with additional spacing
//        for (int i = 0; i < lines.length; i++) {
//            Point lineOrg = new Point(textOrg.x, textOrg.y - totalHeight + (i + 1) * (baseLine[0] + lineSpacing));
//            Imgproc.putText(frame, lines[i], lineOrg, fontFace, fontScale, new Scalar(255, 255, 255), thickness);
//        }
//    }
}
