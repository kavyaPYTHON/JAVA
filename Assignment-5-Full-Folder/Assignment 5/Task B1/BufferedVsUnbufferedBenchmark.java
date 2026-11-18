import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BufferedVsUnbufferedBenchmark {

    static long countLinesUnbuffered(Path path) throws IOException {
        long lines = 0;
        try (InputStream in = Files.newInputStream(path)) {
            int b;
            while ((b = in.read()) != -1) {
                if (b == '\n') lines++;
            }
        }
        return lines;
    }

    static long countLinesBuffered(Path path) throws IOException {
        long lines = 0;
        try (InputStream in = new BufferedInputStream(Files.newInputStream(path), 8192)) {
            int b;
            while ((b = in.read()) != -1) {
                if (b == '\n') lines++;
            }
        }
        return lines;
    }

    static void createLogFile(Path path, int targetBytes) throws IOException {
        byte[] sampleLine = "2026-11-18T08:30:00Z,REG23CS101,GATE_NORTH,PRESENT\n".getBytes();
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
            int written = 0;
            while (written < targetBytes) {
                out.write(sampleLine);
                written += sampleLine.length;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=== B1.3: Buffering Line-Count Benchmark (1 MB, 10 MB; scale to 50 MB) ===");
        for (int mb : new int[]{1, 10}) {
            Path tmp = Files.createTempFile("attendance_" + mb + "mb_", ".log");
            createLogFile(tmp, mb * 1024 * 1024);

            long t1 = System.nanoTime();
            long c1 = countLinesUnbuffered(tmp);
            long unbufNs = System.nanoTime() - t1;

            long t2 = System.nanoTime();
            long c2 = countLinesBuffered(tmp);
            long bufNs = System.nanoTime() - t2;

            System.out.printf("Size=%2d MB | Unbuffered=%.2f ms (lines=%d) | Buffered=%.2f ms (lines=%d)%n",
                    mb, unbufNs / 1e6, c1, bufNs / 1e6, c2);
            Files.delete(tmp);
        }

        System.out.println("\n=== B1.4: PassStore Linear Search Growth (1,000 vs 10,000 vs 100,000 records) ===");
        for (int n : new int[]{1000, 10000, 100000}) {
            Path passFile = Files.createTempFile("passes_" + n + "_", ".csv");
            try (BufferedWriter w = Files.newBufferedWriter(passFile)) {
                for (int i = 1; i <= n; i++) {
                    w.write("P" + i + ",23CS" + i + ",Student" + i + "," + (i % 25 + 1) + ",4500.0\n");
                }
            }
            PassStore store = new PassStore(passFile);
            String target = "P" + n; // Worst-case tail lookup
            long start = System.nanoTime();
            String[] found = store.find(target);
            long elapsed = System.nanoTime() - start;
            System.out.printf("Records=%6d | Tail lookup (%s) = %.3f ms | Found=%s%n",
                    n, target, elapsed / 1e6, found != null);
            Files.delete(passFile);
        }
    }
}
