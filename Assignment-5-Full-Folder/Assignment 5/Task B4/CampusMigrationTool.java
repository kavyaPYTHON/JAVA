import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class CampusMigrationTool {
    private static final String URL = "jdbc:mysql://localhost:3306/transport_db";
    private static final String USER = "root";
    private static final String PWD = "password";

    static void printDatabaseMetadata(Connection con) throws SQLException {
        DatabaseMetaData dbMeta = con.getMetaData();
        System.out.println("=== B4.2: DatabaseMetaData Inspection ===");
        System.out.println("Database Product : " + dbMeta.getDatabaseProductName() + " " + dbMeta.getDatabaseProductVersion());
        System.out.println("JDBC Driver      : " + dbMeta.getDriverName() + " " + dbMeta.getDriverVersion());
        System.out.print("Tables Present   : ");
        try (ResultSet tables = dbMeta.getTables(con.getCatalog(), null, "%", new String[]{"TABLE"})) {
            List<String> names = new ArrayList<>();
            while (tables.next()) names.add(tables.getString("TABLE_NAME"));
            System.out.println(names);
        }
    }

    static void migrateFileToDatabase(Path sourceFile, Connection con) throws IOException, SQLException {
        List<String[]> validRows = new ArrayList<>();
        List<String> errorReport = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(sourceFile)) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                String[] parts = line.split(",");
                if (parts.length != 5) {
                    errorReport.add("Line " + lineNo + ": Expected 5 fields -> " + line);
                    continue;
                }
                try {
                    int route = Integer.parseInt(parts[3].trim());
                    double amt = Double.parseDouble(parts[4].trim());
                    if (route <= 0 || amt <= 0) {
                        errorReport.add("Line " + lineNo + ": Non-positive route/amount -> " + line);
                        continue;
                    }
                    validRows.add(parts);
                } catch (NumberFormatException nfe) {
                    errorReport.add("Line " + lineNo + ": Invalid number format -> " + line);
                }
            }
        }

        System.out.println("=== B4.1: Migration Validation Report ===");
        System.out.println("Valid records queued : " + validRows.size());
        System.out.println("Invalid records skipped: " + errorReport.size());
        for (String err : errorReport) System.out.println("  [REJECTED] " + err);

        if (con != null && !validRows.isEmpty()) {
            int inserted = PassLoader.loadAll(con, validRows);
            System.out.println("Committed " + inserted + " validated rows into MySQL.");
        }
    }

    public static void main(String[] args) throws Exception {
        Path legacyFile = Files.createTempFile("legacy_campus_passes_", ".csv");
        Files.write(legacyFile, Arrays.asList(
                "C01-P101,23CS101,Aarav,12,4500.0",
                "C01-P102,23CS102,CORRUPT_ROW",
                "C02-P201,23EC201,Diya,-5,4500.0",
                "C02-P202,23ME301,Rohan,8,4800.0"
        ));

        try (Connection con = DriverManager.getConnection(URL, USER, PWD)) {
            printDatabaseMetadata(con);
            try (Statement st = con.createStatement()) { st.executeUpdate("DELETE FROM bus_pass"); }
            migrateFileToDatabase(legacyFile, con);
        } catch (SQLException offline) {
            System.out.println("[DB Offline Mode] Running file validation & rejection report without live MySQL:");
            migrateFileToDatabase(legacyFile, null);
        } finally {
            Files.delete(legacyFile);
        }
    }
}
