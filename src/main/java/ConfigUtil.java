
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.List;
import java.util.Map;

public class ConfigUtil {
    private static final String CONFIG_PATH = "config.yml";
    private static final String OVERRIDE_PATH = "overrideConfig.yml";

    private static Map<String, Object> overrideConfigMap = getConfig(OVERRIDE_PATH);
    private static Map<String, Object> configMap = getConfig(CONFIG_PATH);

    public static String getConfigValue(String key){
        if(overrideConfigMap == null){
            return configMap.get(key).toString();
        }
        var overrideVar = overrideConfigMap.get(key);
        if (overrideVar == null){
            return configMap.get(key).toString();
        }
        return overrideVar.toString();

    }
    public static int getConfigValueAsInt(String key){
        return Integer.parseInt(getConfigValue(key));
    }

    private static Map<String, Object> getConfig(String config_path){
        Yaml yaml = new Yaml();
        try {
            InputStream inputStream = new FileInputStream(new File(config_path));
            return yaml.load(inputStream);
        } catch (FileNotFoundException e) {
            if(e.toString().contains(OVERRIDE_PATH)){
                System.out.println("No override file at overrideConfig.yml, using default");
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

}
