import java.sql.*;
import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            // Load .env
            Properties env = new Properties();
            env.load(new FileInputStream(".env"));

            String url  = env.getProperty("DB_URL");
            String user = env.getProperty("DB_USER");
            String pass = env.getProperty("DB_PASS");

            Connection conn = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected safely!");
            conn.close();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}