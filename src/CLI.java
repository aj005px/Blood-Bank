import java.sql.*;
import java.util.Scanner;

public class CLI {

    private static final Scanner sc = new Scanner(System.in);

    // ─────────────────────────────── ENTRY POINT ───────────────────────────────

    public static void run() {
        printBanner();
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Choose an option: ");
            switch (choice) {
                case 1  -> donorMenu();
                case 2  -> donationMenu();
                case 3  -> componentMenu();
                case 4  -> transfusionMenu();
                case 5  -> reportsMenu();
                case 0  -> { running = false; System.out.println("\n  Goodbye!\n"); }
                default -> System.out.println("  [!] Invalid option. Try again.");
            }
        }
    }

    // ─────────────────────────────── MENUS ─────────────────────────────────────

    private static void printMainMenu() {
        System.out.println("""

  ╔══════════════════════════════╗
  ║        MAIN MENU             ║
  ╠══════════════════════════════╣
  ║  1. Donor Management         ║
  ║  2. Donation Management      ║
  ║  3. Blood Component Inventory║
  ║  4. Transfusion Management   ║
  ║  5. Reports & Charts         ║
  ║  0. Exit                     ║
  ╚══════════════════════════════╝""");
    }

    // ── Donors ──────────────────────────────────────────────────────────────────

    private static void donorMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("""

  [ Donor Management ]
  1. Add donor
  2. View all donors
  3. Search donor by ID
  4. Search donor by name
  0. Back""");
            switch (readInt("  > ")) {
                case 1  -> addDonor();
                case 2  -> viewAllDonors();
                case 3  -> searchDonorById();
                case 4  -> searchDonorByName();
                case 0  -> back = true;
                default -> System.out.println("  [!] Invalid option.");
            }
        }
    }

    private static void addDonor() {
        System.out.println("\n  --- Add New Donor ---");
        System.out.print("  Name: ");
        String name = sc.nextLine().trim();

        String bg = chooseBloodGroup();
        if (bg == null) return;

        String sql = "INSERT INTO Donors (name, blood_group) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, bg);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                System.out.println("  [✓] Donor added! ID: " + keys.getInt(1));
            }
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void viewAllDonors() {
        String sql = "SELECT donor_id, name, blood_group FROM Donors ORDER BY donor_id";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n  ID   Name                           Blood Group");
            System.out.println("  ─────────────────────────────────────────────");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("  %-5d%-35s%s%n",
                        rs.getInt("donor_id"),
                        rs.getString("name"),
                        rs.getString("blood_group"));
            }
            if (!any) System.out.println("  (no donors on record)");
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void searchDonorById() {
        int id = readInt("  Enter donor ID: ");
        String sql = "SELECT * FROM Donors WHERE donor_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                printDonorRow(rs);
            } else {
                System.out.println("  [!] No donor found with ID " + id);
            }
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void searchDonorByName() {
        System.out.print("  Name (or part of name): ");
        String name = sc.nextLine().trim();
        String sql = "SELECT * FROM Donors WHERE name LIKE ? ORDER BY name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            ResultSet rs = ps.executeQuery();
            System.out.println("\n  ID   Name                           Blood Group");
            System.out.println("  ─────────────────────────────────────────────");
            boolean any = false;
            while (rs.next()) {
                any = true;
                printDonorRow(rs);
            }
            if (!any) System.out.println("  (no results for \"" + name + "\")");
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void printDonorRow(ResultSet rs) throws SQLException {
        System.out.printf("  %-5d%-35s%s%n",
                rs.getInt("donor_id"),
                rs.getString("name"),
                rs.getString("blood_group"));
    }

    // ── Donations ────────────────────────────────────────────────────────────────

    private static void donationMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("""

  [ Donation Management ]
  1. Record new donation
  2. View all donations
  3. View donations by donor ID
  0. Back""");
            switch (readInt("  > ")) {
                case 1  -> addDonation();
                case 2  -> viewAllDonations();
                case 3  -> viewDonationsByDonor();
                case 0  -> back = true;
                default -> System.out.println("  [!] Invalid option.");
            }
        }
    }

    private static void addDonation() {
        System.out.println("\n  --- Record Donation ---");
        int donorId = readInt("  Donor ID: ");

        // Verify donor exists
        if (!donorExists(donorId)) {
            System.out.println("  [!] No donor found with ID " + donorId);
            return;
        }

        System.out.print("  Donation date (YYYY-MM-DD) [leave blank for today]: ");
        String dateInput = sc.nextLine().trim();
        String date = dateInput.isEmpty() ? "CURDATE()" : "'" + dateInput + "'";

        String sql = "INSERT INTO Donations (donor_id, donation_date) VALUES (?, " + date + ")";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, donorId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int donId = keys.getInt(1);
                System.out.println("  [✓] Donation recorded! Donation ID: " + donId);
                System.out.println("  Tip: Use Donation ID " + donId + " to add blood components.");
            }
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void viewAllDonations() {
        String sql = """
                SELECT dn.donation_id, d.name, d.blood_group, dn.donation_date
                FROM Donations dn
                JOIN Donors d ON dn.donor_id = d.donor_id
                ORDER BY dn.donation_date DESC
                """;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n  Don.ID  Donor Name                     Blood Grp  Date");
            System.out.println("  ─────────────────────────────────────────────────────────");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("  %-8d%-35s%-11s%s%n",
                        rs.getInt("donation_id"),
                        rs.getString("name"),
                        rs.getString("blood_group"),
                        rs.getString("donation_date"));
            }
            if (!any) System.out.println("  (no donations on record)");
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void viewDonationsByDonor() {
        int donorId = readInt("  Donor ID: ");
        String sql = """
                SELECT dn.donation_id, dn.donation_date
                FROM Donations dn
                WHERE dn.donor_id = ?
                ORDER BY dn.donation_date DESC
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donorId);
            ResultSet rs = ps.executeQuery();
            System.out.println("\n  Donation ID   Date");
            System.out.println("  ─────────────────────");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("  %-14d%s%n",
                        rs.getInt("donation_id"),
                        rs.getString("donation_date"));
            }
            if (!any) System.out.println("  (no donations for donor ID " + donorId + ")");
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    // ── Blood Components ─────────────────────────────────────────────────────────

    private static void componentMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("""

  [ Blood Component Inventory ]
  1. Add component to donation
  2. View available inventory
  3. View all components
  4. View components by donation ID
  5. Mark component as used / expired
  0. Back""");
            switch (readInt("  > ")) {
                case 1  -> addComponent();
                case 2  -> viewAvailableInventory();
                case 3  -> viewAllComponents();
                case 4  -> viewComponentsByDonation();
                case 5  -> updateComponentStatus();
                case 0  -> back = true;
                default -> System.out.println("  [!] Invalid option.");
            }
        }
    }

    private static void addComponent() {
        System.out.println("\n  --- Add Blood Component ---");
        int donId = readInt("  Donation ID: ");

        if (!donationExists(donId)) {
            System.out.println("  [!] No donation found with ID " + donId);
            return;
        }

        System.out.println("""
  Component types:
    1. Whole Blood
    2. Red Blood Cells
    3. Plasma
    4. Platelets""");
        int t = readInt("  Choose type: ");
        String type = switch (t) {
            case 1 -> "Whole Blood";
            case 2 -> "Red Blood Cells";
            case 3 -> "Plasma";
            case 4 -> "Platelets";
            default -> null;
        };
        if (type == null) { System.out.println("  [!] Invalid type."); return; }

        int volume = readInt("  Volume (ml): ");

        // expiry_date is handled by the DB trigger — pass a placeholder
        String sql = "INSERT INTO Blood_Components (donation_id, component_type, volume_ml, expiry_date) VALUES (?, ?, ?, CURDATE())";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, donId);
            ps.setString(2, type);
            ps.setInt(3, volume);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                System.out.println("  [✓] Component added! ID: " + keys.getInt(1)
                        + " (expiry set automatically by trigger)");
            }
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void viewAvailableInventory() {
        String sql = """
                SELECT bc.component_id, bc.component_type, bc.volume_ml,
                       bc.expiry_date, d.blood_group
                FROM Blood_Components bc
                JOIN Donations dn ON bc.donation_id = dn.donation_id
                JOIN Donors d ON dn.donor_id = d.donor_id
                WHERE bc.status = 'Available'
                ORDER BY bc.expiry_date ASC
                """;
        printComponentTable(sql, null);

        // Print summary
        printInventorySummary();
    }

    private static void viewAllComponents() {
        String sql = """
                SELECT bc.component_id, bc.component_type, bc.volume_ml,
                       bc.expiry_date, d.blood_group, bc.status
                FROM Blood_Components bc
                JOIN Donations dn ON bc.donation_id = dn.donation_id
                JOIN Donors d ON dn.donor_id = d.donor_id
                ORDER BY bc.component_id DESC
                """;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n  ID    Type              Vol(ml)  Expiry      Blood Grp  Status");
            System.out.println("  ────────────────────────────────────────────────────────────────");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("  %-6d%-18s%-9d%-12s%-11s%s%n",
                        rs.getInt("component_id"),
                        rs.getString("component_type"),
                        rs.getInt("volume_ml"),
                        rs.getString("expiry_date"),
                        rs.getString("blood_group"),
                        rs.getString("status"));
            }
            if (!any) System.out.println("  (no components on record)");
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void viewComponentsByDonation() {
        int donId = readInt("  Donation ID: ");
        String sql = """
                SELECT bc.component_id, bc.component_type, bc.volume_ml,
                       bc.expiry_date, d.blood_group, bc.status
                FROM Blood_Components bc
                JOIN Donations dn ON bc.donation_id = dn.donation_id
                JOIN Donors d ON dn.donor_id = d.donor_id
                WHERE bc.donation_id = ?
                ORDER BY bc.component_id
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, donId);
            ResultSet rs = ps.executeQuery();
            System.out.println("\n  ID    Type              Vol(ml)  Expiry      Blood Grp  Status");
            System.out.println("  ────────────────────────────────────────────────────────────────");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("  %-6d%-18s%-9d%-12s%-11s%s%n",
                        rs.getInt("component_id"),
                        rs.getString("component_type"),
                        rs.getInt("volume_ml"),
                        rs.getString("expiry_date"),
                        rs.getString("blood_group"),
                        rs.getString("status"));
            }
            if (!any) System.out.println("  (no components for donation ID " + donId + ")");
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void updateComponentStatus() {
        int compId = readInt("  Component ID: ");
        System.out.println("""
  New status:
    1. Available
    2. Used
    3. Expired""");
        int s = readInt("  Choose: ");
        String status = switch (s) {
            case 1 -> "Available";
            case 2 -> "Used";
            case 3 -> "Expired";
            default -> null;
        };
        if (status == null) { System.out.println("  [!] Invalid choice."); return; }

        String sql = "UPDATE Blood_Components SET status = ? WHERE component_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, compId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("  [✓] Status updated to '" + status + "'");
            } else {
                System.out.println("  [!] No component found with ID " + compId);
            }
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void printComponentTable(String sql, PreparedStatement ps) {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n  ID    Type              Vol(ml)  Expiry      Blood Grp");
            System.out.println("  ─────────────────────────────────────────────────────");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("  %-6d%-18s%-9d%-12s%s%n",
                        rs.getInt("component_id"),
                        rs.getString("component_type"),
                        rs.getInt("volume_ml"),
                        rs.getString("expiry_date"),
                        rs.getString("blood_group"));
            }
            if (!any) System.out.println("  (inventory is empty)");
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void printInventorySummary() {
        String sql = """
                SELECT component_type, COUNT(*) AS units, SUM(volume_ml) AS total_ml
                FROM Blood_Components
                WHERE status = 'Available'
                GROUP BY component_type
                """;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n  Summary (Available):");
            System.out.println("  Type              Units  Total (ml)");
            System.out.println("  ─────────────────────────────────");
            while (rs.next()) {
                System.out.printf("  %-18s%-7d%d%n",
                        rs.getString("component_type"),
                        rs.getInt("units"),
                        rs.getInt("total_ml"));
            }
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    // ── Transfusions ─────────────────────────────────────────────────────────────

    private static void transfusionMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("""

  [ Transfusion Management ]
  1. Record new transfusion
  2. View all transfusions
  3. View transfusions by patient name
  0. Back""");
            switch (readInt("  > ")) {
                case 1  -> addTransfusion();
                case 2  -> viewAllTransfusions();
                case 3  -> viewTransfusionsByPatient();
                case 0  -> back = true;
                default -> System.out.println("  [!] Invalid option.");
            }
        }
    }

    private static void addTransfusion() {
        System.out.println("\n  --- Record Transfusion ---");
        int compId = readInt("  Component ID: ");

        // Verify component is available
        if (!componentAvailable(compId)) {
            System.out.println("  [!] Component " + compId + " is not available (or doesn't exist).");
            return;
        }

        System.out.print("  Patient name: ");
        String patient = sc.nextLine().trim();

        System.out.print("  Transfusion date (YYYY-MM-DD) [leave blank for today]: ");
        String dateInput = sc.nextLine().trim();
        String date = dateInput.isEmpty() ? "CURDATE()" : "'" + dateInput + "'";

        String insertSQL = "INSERT INTO Transfusions (component_id, patient_name, transfusion_date) VALUES (?, ?, " + date + ")";
        String updateSQL = "UPDATE Blood_Components SET status = 'Used' WHERE component_id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement ps2 = conn.prepareStatement(updateSQL)) {

                ps1.setInt(1, compId);
                ps1.setString(2, patient);
                ps1.executeUpdate();

                ps2.setInt(1, compId);
                ps2.executeUpdate();

                conn.commit();
                ResultSet keys = ps1.getGeneratedKeys();
                if (keys.next()) {
                    System.out.println("  [✓] Transfusion recorded! ID: " + keys.getInt(1));
                    System.out.println("  [✓] Component " + compId + " marked as 'Used'");
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void viewAllTransfusions() {
        String sql = """
                SELECT t.transfusion_id, t.patient_name, t.transfusion_date,
                       bc.component_type, d.blood_group
                FROM Transfusions t
                JOIN Blood_Components bc ON t.component_id = bc.component_id
                JOIN Donations dn ON bc.donation_id = dn.donation_id
                JOIN Donors d ON dn.donor_id = d.donor_id
                ORDER BY t.transfusion_date DESC
                """;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n  ID    Patient                        Date        Component          Blood Grp");
            System.out.println("  ─────────────────────────────────────────────────────────────────────────────");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("  %-6d%-31s%-12s%-19s%s%n",
                        rs.getInt("transfusion_id"),
                        rs.getString("patient_name"),
                        rs.getString("transfusion_date"),
                        rs.getString("component_type"),
                        rs.getString("blood_group"));
            }
            if (!any) System.out.println("  (no transfusions on record)");
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    private static void viewTransfusionsByPatient() {
        System.out.print("  Patient name (or part): ");
        String name = sc.nextLine().trim();
        String sql = """
                SELECT t.transfusion_id, t.patient_name, t.transfusion_date,
                       bc.component_type, d.blood_group
                FROM Transfusions t
                JOIN Blood_Components bc ON t.component_id = bc.component_id
                JOIN Donations dn ON bc.donation_id = dn.donation_id
                JOIN Donors d ON dn.donor_id = d.donor_id
                WHERE t.patient_name LIKE ?
                ORDER BY t.transfusion_date DESC
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            ResultSet rs = ps.executeQuery();
            System.out.println("\n  ID    Patient                        Date        Component          Blood Grp");
            System.out.println("  ─────────────────────────────────────────────────────────────────────────────");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("  %-6d%-31s%-12s%-19s%s%n",
                        rs.getInt("transfusion_id"),
                        rs.getString("patient_name"),
                        rs.getString("transfusion_date"),
                        rs.getString("component_type"),
                        rs.getString("blood_group"));
            }
            if (!any) System.out.println("  (no results for \"" + name + "\")");
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    // ── Reports ───────────────────────────────────────────────────────────────────

    private static void reportsMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("""

  [ Reports & Charts ]
  1. Inventory chart (bar)
  2. Blood group distribution chart (pie)
  3. Print expiring components (next 7 days)
  4. Print full inventory summary
  0. Back""");
            switch (readInt("  > ")) {
                case 1  -> GraphingTool.showInventoryChart();
                case 2  -> GraphingTool.showDistributionChart();
                case 3  -> printExpiringComponents();
                case 4  -> { viewAvailableInventory(); printInventorySummary(); }
                case 0  -> back = true;
                default -> System.out.println("  [!] Invalid option.");
            }
        }
    }

    private static void printExpiringComponents() {
        String sql = """
                SELECT bc.component_id, bc.component_type, bc.volume_ml,
                       bc.expiry_date, d.blood_group,
                       DATEDIFF(bc.expiry_date, CURDATE()) AS days_left
                FROM Blood_Components bc
                JOIN Donations dn ON bc.donation_id = dn.donation_id
                JOIN Donors d ON dn.donor_id = d.donor_id
                WHERE bc.status = 'Available'
                  AND bc.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)
                ORDER BY bc.expiry_date ASC
                """;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n  ⚠  Components expiring within 7 days:");
            System.out.println("  ID    Type              Vol(ml)  Expiry      Blood Grp  Days Left");
            System.out.println("  ───────────────────────────────────────────────────────────────────");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("  %-6d%-18s%-9d%-12s%-11s%d%n",
                        rs.getInt("component_id"),
                        rs.getString("component_type"),
                        rs.getInt("volume_ml"),
                        rs.getString("expiry_date"),
                        rs.getString("blood_group"),
                        rs.getInt("days_left"));
            }
            if (!any) System.out.println("  (nothing expiring in the next 7 days)");
        } catch (SQLException e) {
            System.out.println("  [✗] Error: " + e.getMessage());
        }
    }

    // ─────────────────────────────── HELPERS ───────────────────────────────────

    private static String chooseBloodGroup() {
        System.out.println("""
  Blood groups:
    1. A+    2. A-
    3. B+    4. B-
    5. AB+   6. AB-
    7. O+    8. O-""");
        int choice = readInt("  Choose: ");
        return switch (choice) {
            case 1 -> "A+";  case 2 -> "A-";
            case 3 -> "B+";  case 4 -> "B-";
            case 5 -> "AB+"; case 6 -> "AB-";
            case 7 -> "O+";  case 8 -> "O-";
            default -> { System.out.println("  [!] Invalid blood group."); yield null; }
        };
    }

    private static boolean donorExists(int donorId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM Donors WHERE donor_id = ?")) {
            ps.setInt(1, donorId);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    private static boolean donationExists(int donId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM Donations WHERE donation_id = ?")) {
            ps.setInt(1, donId);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    private static boolean componentAvailable(int compId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM Blood_Components WHERE component_id = ? AND status = 'Available'")) {
            ps.setInt(1, compId);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    /** Reads an int, consuming the newline. Returns -1 on bad input. */
    private static int readInt(String prompt) {
        System.out.print(prompt);
        try {
            int v = Integer.parseInt(sc.nextLine().trim());
            return v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void printBanner() {
        System.out.println("""
 
  ██████╗ ██╗      ██████╗  ██████╗ ██████╗     ██████╗  █████╗ ███╗   ██╗██╗  ██╗
  ██╔══██╗██║     ██╔═══██╗██╔═══██╗██╔══██╗    ██╔══██╗██╔══██╗████╗  ██║██║ ██╔╝
  ██████╔╝██║     ██║   ██║██║   ██║██║  ██║    ██████╔╝███████║██╔██╗ ██║█████╔╝ 
  ██╔══██╗██║     ██║   ██║██║   ██║██║  ██║    ██╔══██╗██╔══██║██║╚██╗██║██╔═██╗ 
  ██████╔╝███████╗╚██████╔╝╚██████╔╝██████╔╝    ██████╔╝██║  ██║██║ ╚████║██║  ██╗
  ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝     ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝
                                                           Management System v1.0
  """);
    }
}

