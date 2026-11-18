import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class AdversarialPersistenceDemo {
    private static final String URL = "jdbc:mysql://localhost:3306/transport_db";
    private static final String USER = "root";
    private static final String PWD = "password";

    // Unsafe login vulnerable to SQL Injection
    static boolean unsafeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
        System.out.println("[Unsafe SQL] " + sql);
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
    }

    // Safe login using PreparedStatement parameter binding
    static boolean safeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? AND pwd = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    static void testDamagedPassFile() throws IOException {
        Path badFile = Files.createTempFile("damaged_passes_", ".csv");
        List<String> lines = Arrays.asList(
                "P2001,23CS101,Aarav,12,4500.0",       // Valid
                "P2002,MISSING_FIELDS",               // Wrong field count
                "P2003,23CS103,Meera,14,NOT_A_NUMBER",// Non-numeric amount
                "",                                   // Empty line
                "P2004,23CS104,Kiran,9,4200.0"        // Valid
        );
        Files.write(badFile, lines);

        System.out.println("\n=== Testing Damaged Bus-Pass File Resilience ===");
        int validCount = 0, skippedCount = 0;
        try (BufferedReader br = Files.newBufferedReader(badFile)) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                String[] parts = line.split(",");
                if (parts.length != 5) {
                    skippedCount++;
                    System.out.println("Line " + lineNo + " skipped (expected 5 fields, got " + parts.length + "): '" + line + "'");
                    continue;
                }
                try {
                    Integer.parseInt(parts[3].trim());
                    Double.parseDouble(parts[4].trim());
                    validCount++;
                } catch (NumberFormatException nfe) {
                    skippedCount++;
                    System.out.println("Line " + lineNo + " skipped (corrupt numeric field): '" + line + "'");
                }
            }
        }
        System.out.println("Summary -> Valid records accepted: " + validCount + " | Damaged records quarantined: " + skippedCount);
        Files.delete(badFile);
    }

    public static void main(String[] args) throws Exception {
        String injectedId = "admin' OR '1'='1' -- ";
        String arbitraryPwd = "wrong_password";

        System.out.println("=== Testing SQL Injection Adversarial Input ===");
        try (Connection con = DriverManager.getConnection(URL, USER, PWD)) {
            boolean unsafeAccepted = unsafeLogin(con, injectedId, arbitraryPwd);
            boolean safeAccepted = safeLogin(con, injectedId, arbitraryPwd);
            System.out.println("Unsafe login accepted crafted input? " + unsafeAccepted);
            System.out.println("Safe PreparedStatement accepted crafted input? " + safeAccepted);
        } catch (SQLException offline) {
            System.out.println("[DB Offline Simulation] Constructed unsafe query:");
            System.out.println("  SELECT * FROM users WHERE id='" + injectedId + "' AND pwd='" + arbitraryPwd + "'");
            System.out.println("  -> Unsafe Statement evaluates ('1'='1') as TRUE (Bypassed!).");
            System.out.println("  -> Safe PreparedStatement binds literal id="" + injectedId + "" -> 0 rows matched (Rejected!).");
        }

        testDamagedPassFile();
    }
}
