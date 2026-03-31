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
            String pass = env.getProperty("DB_PASSWORD");

            Connection conn = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected safely!");

            //Creating DB for now(you can change how this happens)
            //Right now these commands always run
            Statement stmt = conn.createStatement();

            stmt.execute(DatabaseConfig.CREATE_DONORS_TABLE);
            stmt.execute(DatabaseConfig.CREATE_DONATION_TABLE);
            stmt.execute(DatabaseConfig.CREATE_COMPONENTS_TABLE);
            stmt.execute(DatabaseConfig.CREATE_TRANSFUSIONS_TABLE);
            System.out.println("Tables has been verified!");

            stmt.execute(DatabaseConfig.DROP_EXPIRY_TRIGGER); // Clean slate
            stmt.execute(DatabaseConfig.CREATE_EXPIRY_TRIGGER); // Apply logic
            System.out.println("Expiry Date trigger applied successfully.");

            applyForeignKey(stmt, DatabaseConfig.FK_CONSTRAINT_DONATIONS,"Donations -> Donors");
            applyForeignKey(stmt, DatabaseConfig.FK_CONSTRAINT_COMPONENTS, "Components -> Donations");
            applyForeignKey(stmt, DatabaseConfig.FK_CONSTRAINT_TRANSFUSIONS, "Transfusions -> Components");
            System.out.println("Database setup complete!");


            conn.close();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    private static void applyForeignKey(Statement stmt, String sql, String relationshipName) {
        try {
            stmt.execute(sql);
            System.out.println("Added foreign key: " + relationshipName);
        } catch (SQLException e) {
            // 1061 is the MySQL/MariaDB code for "Duplicate key name"
            if (e.getErrorCode() == 1061 || e.getMessage().contains("Duplicate")) {
                System.out.println("Foreign key already exists (Skipping): " + relationshipName);
            } else {
                System.err.println("Error applying foreign key for " + relationshipName + ": " + e.getMessage());
            }
        }
    }
}