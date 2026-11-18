import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class SeatBookingBenchmark {
    static class UnsynchronizedScreen {
        final boolean[] booked;
        int bookingsMade = 0;
        UnsynchronizedScreen(int n) { booked = new boolean[n]; }
        void book(int seat) {
            if (!booked[seat]) {
                Thread.yield();
                booked[seat] = true;
                bookingsMade++;
            }
        }
    }

    static class SyncBlockScreen {
        final boolean[] booked;
        final Object lock = new Object();
        int bookingsMade = 0;
        SyncBlockScreen(int n) { booked = new boolean[n]; }
        void book(int seat) {
            // Non-critical validation can happen outside lock
            if (seat < 0 || seat >= booked.length) return;
            synchronized (lock) {
                if (!booked[seat]) {
                    booked[seat] = true;
                    bookingsMade++;
                }
            }
        }
    }

    static class AtomicScreen {
        final AtomicBoolean[] booked;
        final AtomicInteger bookingsMade = new AtomicInteger(0);
        AtomicScreen(int n) {
            booked = new AtomicBoolean[n];
            for (int i = 0; i < n; i++) booked[i] = new AtomicBoolean(false);
        }
        void book(int seat) {
            if (booked[seat].compareAndSet(false, true)) {
                bookingsMade.incrementAndGet();
            }
        }
    }

    static class ReentrantLockScreen {
        final boolean[] booked;
        final ReentrantLock lock = new ReentrantLock();
        int bookingsMade = 0;
        ReentrantLockScreen(int n) { booked = new boolean[n]; }
        void book(int seat) {
            lock.lock();
            try {
                if (!booked[seat]) {
                    booked[seat] = true;
                    bookingsMade++;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 1. Reproducing Race Condition (10 runs without synchronization) ===");
        int raceRuns = 0;
        for (int r = 1; r <= 10; r++) {
            UnsynchronizedScreen s = new UnsynchronizedScreen(10);
            Thread[] users = new Thread[100];
            for (int i = 0; i < 100; i++) {
                final int seat = i % 10;
                users[i] = new Thread(() -> s.book(seat));
                users[i].start();
            }
            for (Thread t : users) t.join();
            if (s.bookingsMade > 10) raceRuns++;
            System.out.printf("Run %02d -> Seats sold: %d (Expected: 10)%n", r, s.bookingsMade);
        }
        System.out.println("Runs exhibiting double-booking: " + raceRuns + "/10");

        System.out.println("\n=== 2. Throughput Comparison (10,000 booking attempts across 1,000 seats) ===");
        int seats = 1000, attempts = 10000;

        SyncBlockScreen sb = new SyncBlockScreen(seats);
        long t1 = System.nanoTime();
        runLoad(attempts, seats, sb::book);
        long syncBlockNs = System.nanoTime() - t1;

        AtomicScreen at = new AtomicScreen(seats);
        long t2 = System.nanoTime();
        runLoad(attempts, seats, at::book);
        long atomicNs = System.nanoTime() - t2;

        ReentrantLockScreen rl = new ReentrantLockScreen(seats);
        long t3 = System.nanoTime();
        runLoad(attempts, seats, rl::book);
        long reentrantNs = System.nanoTime() - t3;

        System.out.printf("Synchronized Block : %.3f ms (sold=%d)%n", syncBlockNs / 1e6, sb.bookingsMade);
        System.out.printf("AtomicBoolean CAS  : %.3f ms (sold=%d)%n", atomicNs / 1e6, at.bookingsMade.get());
        System.out.printf("ReentrantLock      : %.3f ms (sold=%d)%n", reentrantNs / 1e6, rl.bookingsMade);
    }

    static void runLoad(int attempts, int seats, java.util.function.IntConsumer action) throws InterruptedException {
        Thread[] threads = new Thread[16];
        int perThread = attempts / threads.length;
        for (int t = 0; t < threads.length; t++) {
            final int offset = t * perThread;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < perThread; i++) action.accept((offset + i) % seats);
            });
            threads[t].start();
        }
        for (Thread t : threads) t.join();
    }
}
