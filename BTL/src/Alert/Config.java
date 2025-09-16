package Alert;

import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Không tìm thấy file config.properties trong resources/");
            }
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi load file config.properties", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
