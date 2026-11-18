# Programming in Java – Assignment 5 (CO5)

**Course:** CSA09 – Programming in Java  
**Repository Naming Format:** `CSA09-<RegisterNumber>-Assignment5`  
**Java Version:** Java 17+ (compatible with Java 11+)  

## Student Details
- **Name & Register Number:** `<Enter Name & Register Number>`

## Folder Structure
- `Part A - Design & Pseudocode/PART_A_DESIGN.md` – Complete SIMATS-standard pseudocode, delimited vs binary record layouts, buffered I/O analysis, generic `Dao<T, K>`, serialization/versioning design, and transaction/SQL-injection analysis (`Tasks A1–A5`)
- `Task B1/` – `PassStore.java` (Completed `TODO 1` — line-by-line `BufferedReader` search skipping damaged records) & `BufferedVsUnbufferedBenchmark.java` (Buffering and linear-growth instrumentation)
- `Task B2/` – `AdversarialPersistenceDemo.java` (SQL injection exploit vs `PreparedStatement` defence, and damaged bus-pass file quarantine)
- `Task B3/` – `PassLoader.java` (Completed `TODO 1` to `TODO 3` — transactional 1,000-row batch loader with rollback), `JdbcBatchBenchmark.java`, and `schema.sql`
- `Task B4/` – `CampusMigrationTool.java` (Multi-campus file-to-MySQL migration with validation error report and `DatabaseMetaData` inspection)
- `OUTPUTS.md` – Execution and measurement register for recording local console outputs and benchmark timings

## How to Compile and Run
1. Load `Task B3/schema.sql` into your local MySQL server (and ensure MySQL Connector/J is on your classpath for JDBC tasks).
2. From inside any task folder (e.g., `Task B1`):
```bash
javac *.java
java PassStore
```
