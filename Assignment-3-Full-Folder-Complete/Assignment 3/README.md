# Programming in Java – Assignment 3 (CO3)

**Course:** CSA09 – Programming in Java  
**Repository Naming Format:** `CSA09-<RegisterNumber>-Assignment3`  
**Java Version:** Java 17+ (compatible with Java 11+)  

## Student Details
- **Student A Name & Register Number:** `<Enter Student A Name & RegNo>`
- **Student B Name & Register Number:** `<Enter Student B Name & RegNo>`

## Folder Structure
- `Task 01/` – `ATMWithdrawalDemo.java` (Exception hierarchy, `try-catch-finally`, multi-catch, pre-validation comparison)
- `Task 02/` – `StorageDemo.java` (Completed TODO 1) & `ExamPortalPropagationDemo.java` (Checked/unchecked user-defined exceptions, 3-layer propagation, chaining)
- `Task 03/` – `SalesReportResourceDemo.java` (`try-catch-finally` leak diagnosis, `try-with-resources`, suppressed exceptions)
- `Task 04/` – `ScannerStationsDemo.java` (`Thread` vs `Runnable`, thread lifecycle states, `join()`, `isAlive()`, daemon/priority, `ExecutorService`)
- `Task 05/` – `SeatBooking.java` (Completed TODO 1) & `SeatBookingBenchmark.java` (Race condition reproduction, `synchronized`, `AtomicBoolean`, `ReentrantLock`)
- `Task 06/` – `TokenCounter.java` (Completed TODO 1 & TODO 2) & `WalletDeadlockAndPreventionDemo.java` (Lock-ordering deadlock prevention, `tryLock`, `ArrayBlockingQueue`)
- `Task 07/` – `SemesterResultProcessor.java` (`ExecutorService`, `Callable`, `Future`, `ConcurrentHashMap`, fault-tolerant batch processing across pool sizes 1, 2, 4, 8)
- `OUTPUTS.md` – Console output register for recording local execution results

## How to Compile and Run
From inside any task folder (e.g., `Task 02`):
```bash
javac *.java
java StorageDemo
```
