
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.List;
import java.util.Map;

public class ConfigUtil {
    private static final String CONFIG_PATH = "config.yml";

    private static Map<String, Object> configMap = getConfig();

    public static String getConfigValue(String key){
        return configMap.get(key).toString();
    }
    public static int getConfigValueAsInt(String key){
        return Integer.parseInt(getConfigValue(key));
    }

    private static Map<String, Object> getConfig(){
        Yaml yaml = new Yaml();
        try {
            InputStream inputStream = new FileInputStream(new File(CONFIG_PATH));
            return yaml.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

}
