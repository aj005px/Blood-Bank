import java.sql.*;
import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseManager.getConnection();
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

            DatabaseConfig.applyForeignKey(stmt, DatabaseConfig.FK_CONSTRAINT_DONATIONS,"Donations -> Donors");
            DatabaseConfig.applyForeignKey(stmt, DatabaseConfig.FK_CONSTRAINT_COMPONENTS, "Components -> Donations");
            DatabaseConfig.applyForeignKey(stmt, DatabaseConfig.FK_CONSTRAINT_TRANSFUSIONS, "Transfusions -> Components");
            System.out.println("Database setup complete!");

            GraphingTool.showInventoryChart();
            GraphingTool.showDistributionChart();

            conn.close();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

}