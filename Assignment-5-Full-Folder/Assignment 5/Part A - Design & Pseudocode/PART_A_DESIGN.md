# Assignment 5 (CO5) – Part A: Persistence Design & Pseudocode

> **Standard:** SIMATS Engineering – Standard for Algorithmic Pseudocode  
> **Note:** Java code is intentionally omitted in Part A in accordance with the assignment instructions.

---

## Task A1 – Design a File Format (Transport Office Bus-Pass Records)

### A1.1 – Represent the Record
1. **Delimited Text File (`passes.csv`):**
   - **Field Order:** `passId | regNo | studentName | routeNo | boardingStop | validityDate | amountPaid`
   - **Delimiter:** Pipe (`|`) with line separator `\n` (UTF-8 encoding).
   - **Handling Embedded Delimiters:** Any literal pipe (`|`) or newline inside `studentName` or `boardingStop` is escaped as `\|` (or fields containing delimiters are enclosed in double quotes `"..."` with internal quotes doubled `""` per RFC 4180).

2. **Binary File (`passes.dat` via `DataOutputStream`):**
   - **Record Layout:**
     - `passId`: UTF string (`writeUTF` – 2-byte length prefix + UTF-8 bytes, ~10 bytes)
     - `regNo`: UTF string (`writeUTF`, ~12 bytes)
     - `studentName`: UTF string (`writeUTF`, variable length)
     - `routeNo`: 4-byte signed integer (`writeInt`)
     - `boardingStop`: UTF string (`writeUTF`, variable length)
     - `validityDate`: 8-byte epoch day/millis (`writeLong`)
     - `amountPaid`: 8-byte IEEE-754 double (`writeDouble`)
   - **Handling Embedded Delimiters:** No delimiter is needed because `writeUTF()` prefixes each string with its exact byte length and numeric fields have fixed byte widths.

### A1.2 – Design the Operations (Text-File Design)
- **Stream Choices:**
  - `BufferedWriter` wrapping `FileWriter` (in append mode) for adding records: batches small writes into an 8 KB buffer to minimize OS `write()` system calls.
  - `BufferedReader` wrapping `FileReader` for searching and route listing: reads 8 KB chunks into memory and provides `readLine()` for line-by-line scanning without loading all 8,000+ passes at once.

```text
ALGORITHM AddBusPass(filePath, passRecord)
BEGIN
    OPEN writer AS BufferedWriter(FileWriter(filePath, APPEND))
    line <- Escape(passRecord.passId) + "|" + Escape(passRecord.regNo) + "|" +
            Escape(passRecord.studentName) + "|" + passRecord.routeNo + "|" +
            Escape(passRecord.boardingStop) + "|" + passRecord.validityDate + "|" +
            passRecord.amountPaid
    WRITE line TO writer
    WRITE NEWLINE TO writer
    FLUSH AND CLOSE writer
END

ALGORITHM FindPassById(filePath, targetPassId)
BEGIN
    IF NOT FileExists(filePath) THEN
        RETURN NULL
    END IF
    OPEN reader AS BufferedReader(FileReader(filePath))
    WHILE (line <- ReadLine(reader)) IS NOT NULL DO
        fields <- SplitUnescaped(line, "|")
        IF Length(fields) = 7 AND fields[0] = targetPassId THEN
            CLOSE reader
            RETURN fields
        END IF
    END WHILE
    CLOSE reader
    RETURN NULL
END

ALGORITHM ListPassesByRoute(filePath, targetRoute)
BEGIN
    resultList <- EmptyList()
    IF NOT FileExists(filePath) THEN
        RETURN resultList
    END IF
    OPEN reader AS BufferedReader(FileReader(filePath))
    WHILE (line <- ReadLine(reader)) IS NOT NULL DO
        fields <- SplitUnescaped(line, "|")
        IF Length(fields) = 7 AND ParseInt(fields[3]) = targetRoute THEN
            ADD fields TO resultList
        END IF
    END WHILE
    CLOSE reader
    RETURN resultList
END
```

### A1.3 – Design for Damaged Data
- **Damaged Line:** Validate `Length(fields) == 7`, numeric parsability of `routeNo` and `amountPaid`, and ISO date format of `validityDate`. Malformed lines are logged to an error file and skipped rather than crashing the scan.
- **Missing File:** Check file existence prior to reading; `FindPassById` returns `NULL` and `ListPassesByRoute` returns an empty list, while `AddBusPass` creates the file cleanly.
- **Incomplete Write (Power Failure):** Append a trailing record checksum (CRC32) or write updates atomically to a temporary file (`passes.tmp`), call `flush()` / `fsync`, and rename over `passes.csv`. On startup, any trailing line lacking a newline or failing checksum validation is truncated/quarantined.

---

## Task A2 – Buffered I/O Design (`50 MB` Attendance Log)

### A2.1 – Predict the Cost
- `50 MB = 50 * 1024 * 1024 = 52,428,800 bytes`.
- `FileInputStream.read()` makes **52,428,800 native JNI/OS `read()` system calls** (plus 1 final call returning `-1`). Each syscall incurs user-to-kernel mode context switching overhead, making it orders of magnitude slower than buffered reads.

### A2.2 – Redesign Using Buffering
```text
ALGORITHM CountLinesBuffered(filePath)
BEGIN
    lineCount <- 0
    OPEN inStream AS BufferedInputStream(FileInputStream(filePath), 8192)
    WHILE (byteVal <- ReadByte(inStream)) <> -1 DO
        IF byteVal = ASCII_NEWLINE THEN
            lineCount <- lineCount + 1
        END IF
    END WHILE
    CLOSE inStream
    RETURN lineCount
END
```
- **Syscall Reduction:** With an 8 KB (`8,192` bytes) buffer, OS `read()` calls drop from `52,428,800` to `ceil(52,428,800 / 8,192) = 6,400` calls (~8,192x fewer syscalls).

### A2.3 – When Buffering Does Not Help
- **Random-access small reads at scattered offsets** (e.g., `RandomAccessFile.seek()` jumping to isolated 16-byte records across a multi-gigabyte file), where prefetched buffer bytes are discarded before the next seek.

---

## Task A3 – Generic Data-Access Design Pattern

### A3.1 – Common vs Varying Steps
- **Repeated Steps:** Acquiring a JDBC `Connection`, preparing statements, executing SQL, handling `SQLException`, closing `ResultSet`/`Statement`/`Connection`.
- **Varying Parts:** Table name, SQL query text, binding domain fields to `?` placeholders, and mapping `ResultSet` columns to domain entity objects.

### A3.2 – Generic `Dao<T, K>` Interface
```text
INTERFACE Dao<T, K>
    PROCEDURE insert(entity : T)
    FUNCTION findById(id : K) : T
    FUNCTION findAll() : List<T>
    PROCEDURE update(entity : T)
    PROCEDURE delete(id : K)
END INTERFACE
```

### A3.3 – Object-to-Table Mapping (`Student` -> `student`)
- `Student.regNo` (`String`) -> `reg_no VARCHAR(20) PRIMARY KEY`
- `Student.name` (`String`) -> `name VARCHAR(100) NOT NULL`
- `Student.department` (`String`) -> `department VARCHAR(50)`
- `Student.semester` (`int`) -> `semester INT`

---

## Task A4 – Serialization Design (`Hostel` -> `Room` -> `Resident`)

### A4.1 – Transient Fields & Object Graph
- **Mark `transient`:**
  1. `Connection dbConnection` (live socket/native resource; `NotSerializableException` is thrown if not `transient`).
  2. `String password` (sensitive credential that must never be persisted in cleartext on disk).
- **Object Graph Traversal:** Serializing `Hostel` recursively serializes its `List<Room>` and each room's `List<Resident>` (preserving shared object references via handle tables) while skipping `transient` fields (which deserialize as `null`).

### A4.2 – Schema Evolution & `serialVersionUID`
- Without an explicit `serialVersionUID`, adding `phoneNumber` changes the compiler-generated hash, throwing `InvalidClassException` when reading older files.
- Declaring an explicit `private static final long serialVersionUID = 1L;` allows older files to deserialize cleanly with `phoneNumber` initialized to `null`.

---

## Task A5 – Transactions & SQL Injection Safety

### A5.1 – Wallet Transfer Transaction Pseudocode (`₹500`)
```text
ALGORITHM TransferWalletFunds(con, hostelId, canteenId, amount)
BEGIN
    oldAutoCommit <- GetAutoCommit(con)
    SetAutoCommit(con, FALSE)
    TRY
        rows1 <- ExecuteUpdate(con, "UPDATE hostel_wallet SET balance = balance - ? WHERE id = ? AND balance >= ?", [amount, hostelId, amount])
        IF rows1 <> 1 THEN
            THROW InsufficientBalanceException
        END IF
        rows2 <- ExecuteUpdate(con, "UPDATE canteen_wallet SET balance = balance + ? WHERE id = ?", [amount, canteenId])
        IF rows2 <> 1 THEN
            THROW AccountNotFoundException
        END IF
        Commit(con)
    CATCH ex
        Rollback(con)
        RETHROW ex
    FINALLY
        SetAutoCommit(con, oldAutoCommit)
    END TRY
END
```

### A5.2 – SQL Injection Analysis
- **Crafted Input:** `id = "admin' OR '1'='1' -- "` turns the query into `SELECT * FROM users WHERE id='admin' OR '1'='1' -- ' AND pwd='...'`, bypassing the password predicate completely.
- **Fix:** Use `PreparedStatement` with `?` parameter binding so user input is treated strictly as literal data, never executable SQL syntax.
