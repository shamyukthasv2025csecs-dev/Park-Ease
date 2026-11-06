import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/parkease_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found. Add mysql-connector-j.jar to the project lib folder.", e);
        }

        Properties props = loadProperties();
        String url = getValue(props, "db.url", DEFAULT_URL);
        String user = getValue(props, "db.user", "root");
        String password = getValue(props, "db.password", "");

        return DriverManager.getConnection(url, user, password);
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        Path configPath = Paths.get("config", "db.properties");
        if (Files.exists(configPath)) {
            try {
                props.load(Files.newInputStream(configPath));
            } catch (IOException ignored) {
            }
        }

        String envUrl = System.getenv("PARK_EASE_DB_URL");
        String envUser = System.getenv("PARK_EASE_DB_USER");
        String envPassword = System.getenv("PARK_EASE_DB_PASSWORD");

        if (envUrl != null && !envUrl.trim().isEmpty()) {
            props.setProperty("db.url", envUrl);
        }
        if (envUser != null && !envUser.trim().isEmpty()) {
            props.setProperty("db.user", envUser);
        }
        if (envPassword != null) {
            props.setProperty("db.password", envPassword);
        }

        return props;
    }

    private static String getValue(Properties props, String key, String defaultValue) {
        String value = props.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        return defaultValue;
    }
}
