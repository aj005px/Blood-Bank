import java.sql.*;
import java.io.*;
import java.util.*;

// Code was gonna turn into goy-slop so I pulled some connection bs here
// Just use the DatabaseManager.getConnection()

public class DatabaseManager {

    private static String url;
    private static String user;
    private static String pass;

    static {
        try {
            Properties env = new Properties();
            env.load(new FileInputStream(".env"));

            url  = env.getProperty("DB_URL");
            user = env.getProperty("DB_USER");
            pass = env.getProperty("DB_PASSWORD");

        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to load .env file!");
            e.printStackTrace();
        }
    }

    // 4. Your connection method works perfectly now
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }
}

