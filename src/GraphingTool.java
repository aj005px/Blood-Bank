import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import java.awt.Color;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GraphingTool {

    public static void showInventoryChart() {
        String query = """
                SELECT component_type, SUM(volume_ml) as total_volume 
                FROM Blood_Components 
                WHERE status = 'Available' 
                GROUP BY component_type;
                """;

        Map<String, Integer> inventoryData = new HashMap<>();

        // Using DatabaseManager to get connection
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                inventoryData.put(rs.getString("component_type"), rs.getInt("total_volume"));
            }

        } catch (SQLException e) {
            System.err.println("Failed to fetch data for graph.");
            e.printStackTrace();
            return;
        }

        CategoryChart chart = new CategoryChartBuilder()
                .width(800).height(600)
                .title("Current Available Blood Inventory")
                .xAxisTitle("Component Type")
                .yAxisTitle("Total Volume (ml)")
                .build();

        // Chart Style Legend Annotations, Plot Grid Lines
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);

        //chart.getStyler().setHasAnnotations(true); // Shows the exact number on top of the bar
        chart.getStyler().setPlotGridLinesVisible(false);

        // Data Series
        // We use a dummy X-axis array ["Volume"] so they all plot next to each other nicely
        String[] xAxisLabel = new String[]{"Inventory"};

        // Red Blood Cells (Standard Bright Red)
        if (inventoryData.containsKey("Red Blood Cells")) {
            CategorySeries series = chart.addSeries("Red Blood Cells", Arrays.asList(xAxisLabel), Arrays.asList(inventoryData.get("Red Blood Cells")));
            series.setFillColor(new Color(204, 0, 0));
        }

        // Whole Blood (Darker, deeper red)
        if (inventoryData.containsKey("Whole Blood")) {
            CategorySeries series = chart.addSeries("Whole Blood", Arrays.asList(xAxisLabel), Arrays.asList(inventoryData.get("Whole Blood")));
            series.setFillColor(new Color(102, 0, 0));
        }

        // Plasma (Amber / Dark Yellow)
        if (inventoryData.containsKey("Plasma")) {
            CategorySeries series = chart.addSeries("Plasma", Arrays.asList(xAxisLabel), Arrays.asList(inventoryData.get("Plasma")));
            series.setFillColor(new Color(255, 191, 0));
        }

        // Platelets (Lighter, pale yellow/gold)
        if (inventoryData.containsKey("Platelets")) {
            CategorySeries series = chart.addSeries("Platelets", Arrays.asList(xAxisLabel), Arrays.asList(inventoryData.get("Platelets")));
            series.setFillColor(new Color(255, 230, 100));
        }

        // Display
        new SwingWrapper<>(chart).displayChart();
    }
    public static void showDistributionChart() {
        String query = """
                SELECT d.blood_group, SUM(bc.volume_ml) as total_volume
                FROM Donors d
                JOIN Donations dn ON d.donor_id = dn.donor_id
                JOIN Blood_Components bc ON dn.donation_id = bc.donation_id
                WHERE bc.status = 'Available'
                GROUP BY d.blood_group;
                """;

        Map<String, Integer> bloodData = new HashMap<>();

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                bloodData.put(rs.getString("blood_group"), rs.getInt("total_volume"));
            }

        } catch (SQLException e) {
            System.err.println("Failed to fetch blood group data.");
            e.printStackTrace();
            return;
        }

        PieChart chart = new PieChartBuilder()
                .width(800).height(600)
                .title("Available Blood Volume by Blood Group")
                .build();

        // Customize the Pie Chart styling
        chart.getStyler().setCircular(true);
        chart.getStyler().setLegendVisible(true);
        //chart.getStyler().setAnnotationDistance(1.15); // Pushes the labels slightly outside the pie
        chart.getStyler().setPlotContentSize(.7);      // Leaves room for the labels
        chart.getStyler().setDonutThickness(.4);

        // Loop through whatever the database gave us and add a slice for each
        for (Map.Entry<String, Integer> entry : bloodData.entrySet()) {
            chart.addSeries(entry.getKey(), entry.getValue());
        }

        new SwingWrapper<>(chart).displayChart();
    }
}