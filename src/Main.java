import java.sql.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Running from: " + new java.io.File(".").getAbsolutePath());

        try {
            Connection conn = DatabaseManager.getConnection();
            System.out.println("  Connected to database successfully.");

            Statement stmt = conn.createStatement();

            // Create tables
            stmt.execute(DatabaseConfig.CREATE_DONORS_TABLE);
            stmt.execute(DatabaseConfig.CREATE_DONATION_TABLE);
            stmt.execute(DatabaseConfig.CREATE_COMPONENTS_TABLE);
            stmt.execute(DatabaseConfig.CREATE_TRANSFUSIONS_TABLE);

            // Triggers
            stmt.execute(DatabaseConfig.DROP_EXPIRY_TRIGGER);
            stmt.execute(DatabaseConfig.CREATE_EXPIRY_TRIGGER);

            // Foreign Keys
            DatabaseConfig.applyForeignKey(stmt, DatabaseConfig.FK_CONSTRAINT_DONATIONS,   "Donations -> Donors");
            DatabaseConfig.applyForeignKey(stmt, DatabaseConfig.FK_CONSTRAINT_COMPONENTS,  "Components -> Donations");
            DatabaseConfig.applyForeignKey(stmt, DatabaseConfig.FK_CONSTRAINT_TRANSFUSIONS,"Transfusions -> Components");

            System.out.println("  Database setup complete.\n");

            conn.close();

            // Run dummy data
            //DatabaseManager.runSQLFile("../query.txt"); added data

        } catch (Exception e) {
            System.out.println("  Fatal DB error: " + e.getMessage());
            return;
        }

        CLI.run();
    }
}