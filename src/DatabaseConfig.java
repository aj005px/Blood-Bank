import java.sql.SQLException;
import java.sql.Statement;
// Has all the queries functions we need to create a DB
public class DatabaseConfig {
    //Contains all the SQL queries we require
    public static final String CREATE_DONORS_TABLE = """
            CREATE TABLE IF NOT EXISTS `Donors` (
                `donor_id` INT NOT NULL AUTO_INCREMENT,
                `name` VARCHAR(255) NOT NULL,
                `blood_group` ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL,
                PRIMARY KEY (`donor_id`)
            );
            """;
    public static final String CREATE_DONATION_TABLE = """
            CREATE TABLE IF NOT EXISTS `Donations` (
                `donation_id` INT NOT NULL AUTO_INCREMENT,
                `donor_id` INT NOT NULL,
                `donation_date` DATE NOT NULL,
                PRIMARY KEY (`donation_id`)
            );
            """;
    public static final String CREATE_COMPONENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS `Blood_Components` (
                `component_id` INT NOT NULL AUTO_INCREMENT,
                `donation_id` INT NOT NULL,
                `component_type` ENUM('Whole Blood', 'Red Blood Cells', 'Plasma', 'Platelets') NOT NULL,
                `volume_ml` INT NOT NULL,
                `expiry_date` DATE NOT NULL,
                `status` VARCHAR(50) NOT NULL DEFAULT 'Available',
                PRIMARY KEY (`component_id`)
            );
            """;
    public static final String CREATE_TRANSFUSIONS_TABLE = """
            CREATE TABLE IF NOT EXISTS `Transfusions` (
                `transfusion_id` INT NOT NULL AUTO_INCREMENT,
                `component_id` INT NOT NULL,
                `patient_name` VARCHAR(255) NOT NULL,
                `transfusion_date` DATE NOT NULL,
                PRIMARY KEY (`transfusion_id`)
            );
            """;
    public static final String FK_CONSTRAINT_COMPONENTS = """
            ALTER TABLE `Blood_Components` ADD CONSTRAINT `fk_Blood_Components_donation_id`
            FOREIGN KEY(`donation_id`) REFERENCES `Donations`(`donation_id`);
            """;
    public static final String FK_CONSTRAINT_DONATIONS = """
            ALTER TABLE `Donations` ADD CONSTRAINT `fk_Donations_donor_id`
            FOREIGN KEY(`donor_id`) REFERENCES `Donors`(`donor_id`);
            """;
    public static final String FK_CONSTRAINT_TRANSFUSIONS = """
            ALTER TABLE `Transfusions` ADD CONSTRAINT `fk_Transfusions_component_id` FOREIGN KEY(`component_id`)
            REFERENCES `Blood_Components`(`component_id`);
            """;
    public static final String DROP_EXPIRY_TRIGGER = """
            DROP TRIGGER IF EXISTS `trg_Calculate_Expiry`;
            """;
    public static final String CREATE_EXPIRY_TRIGGER = """
            CREATE TRIGGER `trg_Calculate_Expiry`
                        BEFORE INSERT ON `Blood_Components`
                        FOR EACH ROW
                        BEGIN
                            IF NEW.component_type = 'Platelets' THEN
                                SET NEW.expiry_date = DATE_ADD(CURDATE(), INTERVAL 5 DAY);
                            ELSEIF NEW.component_type = 'Whole Blood' THEN
                                SET NEW.expiry_date = DATE_ADD(CURDATE(), INTERVAL 35 DAY);
                            ELSEIF NEW.component_type = 'Red Blood Cells' THEN
                                SET NEW.expiry_date = DATE_ADD(CURDATE(), INTERVAL 42 DAY);
                            ELSEIF NEW.component_type = 'Plasma' THEN
                                SET NEW.expiry_date = DATE_ADD(CURDATE(), INTERVAL 1 YEAR);
                            END IF;
                        END;
            """;
    public static void applyForeignKey(Statement stmt, String sql, String relationshipName) {
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
