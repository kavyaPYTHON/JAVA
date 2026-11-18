import java.sql.*;
import java.util.*;

public class JdbcBatchBenchmark {
    private static final String URL = "jdbc:mysql://localhost:3306/transport_db";
    private static final String USER = "root";
    private static final String PWD = "password";

    static List<String[]> generatePasses(int count) {
        List<String[]> list = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            list.add(new String[]{"BP" + i, "23CS" + i, "Student_" + i, String.valueOf((i % 30) + 1), "4500.0"});
        }
        return list;
    }

    static void inspectMetadata(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT pass_id, reg_no, name, route_no, amount FROM bus_pass LIMIT 1")) {
            ResultSetMetaData meta = rs.getMetaData();
            System.out.println("=== ResultSetMetaData Verification ===");
            for (int c = 1; c <= meta.getColumnCount(); c++) {
                System.out.printf("Col %d: %s (%s)%n", c, meta.getColumnName(c), meta.getColumnTypeName(c));
            }
        }
    }

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PWD)) {
            // 1. Small data correctness + metadata
            try (Statement st = con.createStatement()) { st.executeUpdate("DELETE FROM bus_pass"); }
            PassLoader.loadAll(con, generatePasses(10));
            inspectMetadata(con);

            // 2. Scaling benchmark: 1,000, 10,000, 100,000 records
            for (int size : new int[]{1000, 10000, 100000}) {
                List<String[]> data = generatePasses(size);
                try (Statement st = con.createStatement()) { st.executeUpdate("DELETE FROM bus_pass"); }
                long t0 = System.nanoTime();
                int loaded = PassLoader.loadAll(con, data);
                long elapsed = System.nanoTime() - t0;
                System.out.printf("Batch+Tx Size=%6d | Loaded=%6d | Time=%.2f ms%n", size, loaded, elapsed / 1e6);
            }

            // 3. Transaction rollback test on mid-list invalid amount
            try (Statement st = con.createStatement()) { st.executeUpdate("DELETE FROM bus_pass"); }
            List<String[]> corruptBatch = generatePasses(2500);
            corruptBatch.get(1500)[4] = "INVALID_AMOUNT";
            try {
                PassLoader.loadAll(con, corruptBatch);
            } catch (Exception expected) {
                System.out.println("Caught mid-batch failure: " + expected);
            }
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
                rs.next();
                System.out.println("Rows remaining in bus_pass after rollback: " + rs.getInt(1) + " (expected 0)");
            }
        } catch (SQLException e) {
            System.out.println("MySQL connection unavailable (" + e.getMessage() + "). Run schema.sql on local MySQL to execute live JDBC timings.");
        }
    }
}
