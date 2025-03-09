import org.opencv.core.Mat;

import java.util.concurrent.ConcurrentHashMap;

public class DescriptorCache {
    private static final ConcurrentHashMap<String, Mat> cache = new ConcurrentHashMap<>();

    public static void addDescriptor(String filePath, Mat descriptor) {
        cache.put(filePath, descriptor);
    }

    public static Mat getDescriptor(String filePath) {
        return cache.get(filePath);
    }

    public static boolean contains(String filePath) {
        return cache.containsKey(filePath);
    }
}
