import java.sql.*;
import java.io.*;
import java.util.*;

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
            pass = env.getProperty("DB_PASS"); // make sure this matches your .env

            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    // 🔥 PUT YOUR METHOD HERE
    public static void runSQLFile(String filePath) {
        try (
                Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                BufferedReader reader = new BufferedReader(new FileReader(filePath))
        ) {
            StringBuilder sql = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("--")) continue;

                sql.append(line);

                if (line.endsWith(";")) {
                    String query = sql.toString();
                    query = query.substring(0, query.length() - 1);

                    System.out.println("Executing: " + query);
                    stmt.execute(query);

                    sql.setLength(0);
                } else {
                    sql.append(" ");
                }
            }

            System.out.println(" SQL file executed successfully!");

        } catch (Exception e) {
            System.out.println(" Failed to execute SQL file");
            e.printStackTrace();
        }
    }
}