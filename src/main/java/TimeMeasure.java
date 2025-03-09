import java.util.HashMap;

public class TimeMeasure {

    private static final HashMap<String, Long> timeMap = new HashMap<>();
    public static void start(String name){
        timeMap.put(name, System.currentTimeMillis());
    }
    public static void end(String name){
        Main.logDebug("Time taken for "+name+": " + (System.currentTimeMillis() - timeMap.get(name)));
        timeMap.clear();
    }
}
