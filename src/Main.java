import java.sql.*;

public class Main {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/blood_bank",
                    "root",
                    "Arjunk_2005"
            );
            System.out.println("✅ Connected to MySQL!");
            conn.close();
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }
}