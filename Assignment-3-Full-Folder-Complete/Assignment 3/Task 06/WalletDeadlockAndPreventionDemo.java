import java.util.concurrent.*;
import java.util.concurrent.locks.*;

class WalletAccount {
    final int id;
    double balance;
    final ReentrantLock lock = new ReentrantLock();

    WalletAccount(int id, double balance) {
        this.id = id;
        this.balance = balance;
    }
}

public class WalletDeadlockAndPreventionDemo {

    // Deadlock-free transfer using consistent global lock ordering by account ID
    static void safeTransferOrdered(WalletAccount from, WalletAccount to, double amount) {
        WalletAccount first = from.id < to.id ? from : to;
        WalletAccount second = from.id < to.id ? to : from;
        synchronized (first) {
            synchronized (second) {
                if (from.balance >= amount) {
                    from.balance -= amount;
                    to.balance += amount;
                }
            }
        }
    }

    // Alternative concept: tryLock with timeout
    static boolean tryLockTransfer(WalletAccount from, WalletAccount to, double amount, long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < deadline) {
            if (from.lock.tryLock()) {
                try {
                    if (to.lock.tryLock()) {
                        try {
                            if (from.balance >= amount) {
                                from.balance -= amount;
                                to.balance += amount;
                            }
                            return true;
                        } finally {
                            to.lock.unlock();
                        }
                    }
                } finally {
                    from.lock.unlock();
                }
            }
            Thread.sleep(1);
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        WalletAccount accA = new WalletAccount(101, 1000.0);
        WalletAccount accB = new WalletAccount(102, 1000.0);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 500; i++) safeTransferOrdered(accA, accB, 1.0);
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 500; i++) safeTransferOrdered(accB, accA, 1.0);
        });

        t1.start(); t2.start();
        t1.join(); t2.join();
        System.out.printf("Ordered Lock Transfers Complete -> AccA=%.2f, AccB=%.2f (Total=%.2f)%n",
                accA.balance, accB.balance, accA.balance + accB.balance);

        // Alternative ArrayBlockingQueue(5) demo
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);
        int sum = 0;
        for (int t = 1; t <= 20; t++) {
            queue.put(t);
            sum += queue.take();
        }
        System.out.println("ArrayBlockingQueue(5) sequential check sum: " + sum + " (expected 210)");
    }
}
