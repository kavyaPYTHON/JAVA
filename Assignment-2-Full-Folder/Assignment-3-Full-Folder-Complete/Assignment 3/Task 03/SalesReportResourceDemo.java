import java.io.*;
import java.util.*;

public class SalesReportResourceDemo {

    static void setupSampleStoreFiles(List<File> files) throws IOException {
        for (int i = 0; i < files.size(); i++) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(files.get(i)))) {
                if (i == 2) {
                    pw.println("STORE_" + i + ",CORRUPT_AMOUNT"); // Triggers NumberFormatException mid-run
                } else {
                    pw.println("STORE_" + i + "," + ((i + 1) * 1250.50));
                }
            }
        }
    }

    // Version 1: Naive try-catch-finally that leaks earlier BufferedReaders if a later file throws before close
    static int generateReportManualLeaky(List<File> storeFiles, File outFile) {
        List<BufferedReader> openedReaders = new ArrayList<>();
        BufferedWriter writer = null;
        int leakedCount = 0;
        try {
            writer = new BufferedWriter(new FileWriter(outFile));
            double grandTotal = 0.0;
            for (File f : storeFiles) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                openedReaders.add(br); // Opened and held in list
                String line = br.readLine();
                String[] parts = line.split(",");
                double val = Double.parseDouble(parts[1]); // Throws NumberFormatException on corrupt file
                grandTotal += val;
                writer.write(parts[0] + ": " + val + "\n");
            }
            writer.write("TOTAL: " + grandTotal + "\n");
        } catch (IOException | RuntimeException e) {
            System.out.println("[Manual Version] Failed mid-processing: " + e);
            // Notice: none of the readers inside openedReaders were closed before exception jumped to catch!
            leakedCount = openedReaders.size();
        } finally {
            if (writer != null) {
                try { writer.close(); } catch (IOException ignored) {}
            }
        }
        // Clean up leaked handles in benchmark harness so OS limits aren't exhausted during test
        for (BufferedReader r : openedReaders) {
            try { r.close(); } catch (IOException ignored) {}
        }
        return leakedCount;
    }

    // Version 2: Safe try-with-resources per file + outer writer
    static int generateReportTryWithResources(List<File> storeFiles, File outFile) {
        int leakedCount = 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outFile))) {
            double grandTotal = 0.0;
            for (File f : storeFiles) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String line = br.readLine();
                    String[] parts = line.split(",");
                    double val = Double.parseDouble(parts[1]);
                    grandTotal += val;
                    writer.write(parts[0] + ": " + val + "\n");
                } // br is guaranteed closed here even when parseDouble throws!
            }
            writer.write("TOTAL: " + grandTotal + "\n");
        } catch (IOException | RuntimeException e) {
            System.out.println("[Try-With-Resources] Failed cleanly on bad file: " + e + " | Open handles leaked: " + leakedCount);
        }
        return leakedCount;
    }

    // Suppressed exception demonstration
    static void demonstrateSuppressedException() {
        class FaultyResource implements AutoCloseable {
            public void use() throws IOException { throw new IOException("Primary read/write failure"); }
            @Override public void close() throws IOException { throw new IOException("Secondary failure during close()"); }
        }
        try (FaultyResource r = new FaultyResource()) {
            r.use();
        } catch (Exception e) {
            System.out.println("[Suppressed Demo] Primary: " + e.getMessage() + " | Suppressed count: " + e.getSuppressed().length
                    + " (" + e.getSuppressed()[0].getMessage() + ")");
        }
    }

    public static void main(String[] args) throws IOException {
        List<File> storeFiles = new ArrayList<>();
        for (int i = 0; i < 4; i++) storeFiles.add(new File("store_" + i + ".csv"));
        setupSampleStoreFiles(storeFiles);

        File out1 = new File("summary_manual.txt");
        File out2 = new File("summary_twr.txt");

        int leakedManual = generateReportManualLeaky(storeFiles, out1);
        System.out.println("Files leaked on failure in naive manual version: " + leakedManual);

        int leakedTwr = generateReportTryWithResources(storeFiles, out2);
        System.out.println("Files leaked on failure in try-with-resources version: " + leakedTwr);

        demonstrateSuppressedException();

        for (File f : storeFiles) f.delete();
        out1.delete();
        out2.delete();
    }
}
