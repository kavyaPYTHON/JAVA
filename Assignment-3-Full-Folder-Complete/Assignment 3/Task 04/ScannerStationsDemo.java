import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Approach 1: Extending Thread
class ScannerThread extends Thread {
    private final int scriptsToScan;
    private final AtomicInteger totalScanned;

    ScannerThread(String name, int scriptsToScan, AtomicInteger totalScanned) {
        super(name);
        this.scriptsToScan = scriptsToScan;
        this.totalScanned = totalScanned;
    }

    @Override
    public void run() {
        for (int i = 1; i <= scriptsToScan; i++) {
            try { Thread.sleep(15); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            totalScanned.incrementAndGet();
        }
        System.out.println("[" + getName() + " | Thread subclass] Finished scanning " + scriptsToScan + " scripts.");
    }
}

// Approach 2: Implementing Runnable (Preferred: decouples task from thread runner)
class ScannerRunnable implements Runnable {
    private final String stationName;
    private final int scriptsToScan;
    private final AtomicInteger totalScanned;
    private final List<String> completionOrder;

    ScannerRunnable(String stationName, int scriptsToScan, AtomicInteger totalScanned, List<String> completionOrder) {
        this.stationName = stationName;
        this.scriptsToScan = scriptsToScan;
        this.totalScanned = totalScanned;
        this.completionOrder = completionOrder;
    }

    @Override
    public void run() {
        for (int i = 1; i <= scriptsToScan; i++) {
            try { Thread.sleep(15); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            totalScanned.incrementAndGet();
        }
        completionOrder.add(stationName);
    }
}

public class ScannerStationsDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Part 1: Extending Thread + Lifecycle Checks (isAlive, join) ===");
        AtomicInteger total1 = new AtomicInteger(0);
        ScannerThread[] stations = new ScannerThread[4];
        for (int i = 0; i < 4; i++) {
            stations[i] = new ScannerThread("Station-" + (i + 1), 10, total1);
            System.out.println(stations[i].getName() + " state before start(): " + stations[i].getState());
            stations[i].start();
        }
        System.out.println("Station-1 isAlive() right after start(): " + stations[0].isAlive());
        for (ScannerThread st : stations) st.join();
        System.out.println("Station-1 state after join(): " + stations[0].getState() + " | isAlive=" + stations[0].isAlive());
        System.out.println("Total scripts scanned (all 4 stations joined): " + total1.get());

        System.out.println("\n=== Part 2: Calling run() directly vs start() ===");
        ScannerThread directRun = new ScannerThread("DirectRunTest", 1, new AtomicInteger());
        System.out.println("Caller thread: " + Thread.currentThread().getName());
        directRun.run(); // Executes synchronously on main thread; no new OS thread spawned

        System.out.println("\n=== Part 3: Runnable across 10 runs showing non-deterministic completion order ===");
        for (int run = 1; run <= 10; run++) {
            AtomicInteger totalRun = new AtomicInteger(0);
            List<String> order = Collections.synchronizedList(new ArrayList<>());
            Thread[] threads = new Thread[4];
            for (int i = 0; i < 4; i++) {
                threads[i] = new Thread(new ScannerRunnable("S" + (i + 1), 5, totalRun, order));
                if (i == 0) threads[i].setPriority(Thread.MAX_PRIORITY);
                if (i == 3) threads[i].setPriority(Thread.MIN_PRIORITY);
                threads[i].start();
            }
            for (Thread t : threads) t.join();
            System.out.println("Run " + String.format("%02d", run) + " finish order: " + order + " | Total=" + totalRun.get());
        }

        System.out.println("\n=== Part 4: Alternative Concept (ExecutorService FixedThreadPool) ===");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        AtomicInteger poolTotal = new AtomicInteger(0);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tasks.add(() -> {
                for (int k = 0; k < 10; k++) poolTotal.incrementAndGet();
                return 10;
            });
        }
        pool.invokeAll(tasks);
        pool.shutdown();
        System.out.println("ExecutorService total scripts scanned: " + poolTotal.get());
    }
}
