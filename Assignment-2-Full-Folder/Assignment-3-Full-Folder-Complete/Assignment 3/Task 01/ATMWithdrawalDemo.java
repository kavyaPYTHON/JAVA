import java.io.*;
import java.util.*;

class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) { super(message); }
}

class InvalidAmountException extends RuntimeException {
    public InvalidAmountException(String message) { super(message); }
}

public class ATMWithdrawalDemo {
    private static double balance = 5000.0;

    static void verifyCardReader(String cardFilePath) throws FileNotFoundException {
        File cardFile = new File(cardFilePath);
        if (!cardFile.exists()) {
            throw new FileNotFoundException("Card-reader configuration missing: " + cardFilePath);
        }
    }

    static void processWithdrawal(String cardFilePath, String accInput, String amtInput) {
        System.out.println("\n--- Transaction Request [CardFile=" + cardFilePath + ", Acc=" + accInput + ", Amt=" + amtInput + "] ---");
        try {
            verifyCardReader(cardFilePath);
            int accountNumber = Integer.parseInt(accInput.trim());
            if (accountNumber <= 0) {
                throw new IllegalArgumentException("Account number must be positive: " + accountNumber);
            }
            double amount = Double.parseDouble(amtInput.trim());
            if (amount <= 0) {
                throw new InvalidAmountException("Withdrawal amount must be positive: " + amount);
            }
            if (amount > balance) {
                throw new InsufficientFundsException("Requested Rs." + amount + " exceeds available balance Rs." + balance);
            }
            balance -= amount;
            System.out.printf("SUCCESS: Dispensed Rs.%.2f for Account #%d. Remaining Balance: Rs.%.2f%n", amount, accountNumber, balance);
        } catch (FileNotFoundException e) {
            System.out.println("CAUGHT [Checked - FileNotFoundException]: " + e.getMessage());
        } catch (NumberFormatException | InvalidAmountException e) {
            System.out.println("CAUGHT [Unchecked Multi-Catch - " + e.getClass().getSimpleName() + "]: " + e.getMessage());
        } catch (InsufficientFundsException e) {
            System.out.println("CAUGHT [Checked User-Defined - InsufficientFundsException]: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("CAUGHT [Unchecked - IllegalArgumentException]: " + e.getMessage());
        } finally {
            System.out.println("FINALLY: Session closed, card ejected, and audit state finalized.");
        }
    }

    public static void main(String[] args) throws IOException {
        File validCard = new File("card_reader.cfg");
        try (FileWriter fw = new FileWriter(validCard)) {
            fw.write("READER_OK=true\n");
        }

        // 5 distinct test paths covering all failure modes + valid transaction
        processWithdrawal("card_reader.cfg", "10045", "1500");      // Path 1: Valid withdrawal
        processWithdrawal("missing_reader.cfg", "10045", "500");    // Path 2: Checked FileNotFoundException
        processWithdrawal("card_reader.cfg", "ABC", "500");         // Path 3: Unchecked NumberFormatException (multi-catch)
        processWithdrawal("card_reader.cfg", "10045", "-250");      // Path 4: Unchecked InvalidAmountException (multi-catch)
        processWithdrawal("card_reader.cfg", "10045", "9000");      // Path 5: Checked InsufficientFundsException

        // Alternative concept demonstration: Pre-validation without exceptions using Scanner.hasNextInt/hasNextDouble
        System.out.println("\n--- Alternative Concept: Pre-validation Error-Code Check ---");
        int status = withdrawWithErrorCodes("card_reader.cfg", "10045", "500");
        System.out.println("Pre-validated withdrawal status code: " + status + " (0 = SUCCESS)");

        validCard.delete();
    }

    static int withdrawWithErrorCodes(String cardFilePath, String accStr, String amtStr) {
        if (!new File(cardFilePath).exists()) return -1;
        Scanner accScan = new Scanner(accStr);
        if (!accScan.hasNextInt()) return -2;
        int acc = accScan.nextInt();
        if (acc <= 0) return -2;
        Scanner amtScan = new Scanner(amtStr);
        if (!amtScan.hasNextDouble()) return -3;
        double amt = amtScan.nextDouble();
        if (amt <= 0) return -4;
        if (amt > balance) return -5;
        balance -= amt;
        return 0;
    }
}
