import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class SemesterResultProcessor {

    static class StudentResult {
        final String studentId;
        final double percentage;
        final String grade;

        StudentResult(String studentId, double percentage, String grade) {
            this.studentId = studentId;
            this.percentage = percentage;
            this.grade = grade;
        }
    }

    static class BatchSummary {
        final int poolSize;
        final int totalFiles;
        final int successCount;
        final int failureCount;
        final long elapsedNanos;

        BatchSummary(int poolSize, int totalFiles, int successCount, int failureCount, long elapsedNanos) {
            this.poolSize = poolSize;
            this.totalFiles = totalFiles;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.elapsedNanos = elapsedNanos;
        }
    }

    static StudentResult processSingleFile(Path file) throws Exception {
        if (!Files.exists(file)) {
            throw new FileNotFoundException("Missing file: " + file.getFileName());
        }
        List<String> lines = Files.readAllLines(file);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Empty/corrupt file: " + file.getFileName());
        }
        // Format: STUDENT_ID,M1,M2,M3,M4,M5
        String[] parts = lines.get(0).trim().split(",");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Malformed record in " + file.getFileName());
        }
        String studentId = parts[0].trim();
        double sum = 0.0;
        for (int i = 1; i <= 5; i++) {
            double mark = Double.parseDouble(parts[i].trim());
            if (mark < 0 || mark > 100) {
                throw new IllegalArgumentException("Out-of-range mark " + mark + " in " + file.getFileName());
            }
            sum += mark;
        }
        double pct = sum / 5.0;
        String grade = pct >= 90 ? "O" : pct >= 80 ? "A+" : pct >= 70 ? "A" : pct >= 60 ? "B" : pct >= 50 ? "C" : "F";
        return new StudentResult(studentId, pct, grade);
    }

    static BatchSummary runBatch(List<Path> files, int poolSize) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        ConcurrentHashMap<String, StudentResult> results = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> errors = new ConcurrentHashMap<>();
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        long start = System.nanoTime();
        List<Future<StudentResult>> futures = new ArrayList<>(files.size());
        for (Path p : files) {
            futures.add(executor.submit(() -> processSingleFile(p)));
        }

        for (int i = 0; i < files.size(); i++) {
            Path p = files.get(i);
            try {
                StudentResult res = futures.get(i).get();
                results.put(res.studentId, res);
                success.incrementAndGet();
            } catch (ExecutionException ee) {
                failed.incrementAndGet();
                errors.put(p.getFileName().toString(), ee.getCause().getClass().getSimpleName() + ": " + ee.getCause().getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        long elapsed = System.nanoTime() - start;
        return new BatchSummary(poolSize, files.size(), success.get(), failed.get(), elapsed);
    }

    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("sem_scripts_");
        int totalScripts = 1000; // Scale to 10000 for full benchmark run
        List<Path> files = new ArrayList<>(totalScripts);

        for (int i = 1; i <= totalScripts; i++) {
            Path f = tempDir.resolve(String.format("script_%05d.txt", i));
            files.add(f);
            if (i % 50 == 0) {
                // Deliberately leave missing (do not create file) -> 20 missing files
                continue;
            } else if (i % 33 == 0) {
                // Deliberately write corrupt data -> 30 corrupt files
                Files.writeString(f, "REG" + i + ",88,CORRUPT,75,90,81\n");
            } else {
                Files.writeString(f, "REG" + i + ",78,84,91,69,88\n");
            }
        }

        System.out.println("PoolSize | Total | Success | Failed | Time (ms)");
        for (int poolSize : new int[]{1, 2, 4, 8}) {
            BatchSummary summary = runBatch(files, poolSize);
            System.out.printf("%-8d | %-5d | %-7d | %-6d | %.3f ms%n",
                    summary.poolSize, summary.totalFiles, summary.successCount, summary.failureCount, summary.elapsedNanos / 1e6);
        }

        // Cleanup temporary files
        try (var walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
