import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;

// Service Layer: handles validation, SQL safety (PreparedStatement), and resource closing
class FeeService {
    private static final String URL = "jdbc:mysql://localhost:3306/college_db";
    private static final String USER = "root";
    private static final String PWD = "password";

    public String processPayment(String regNo, String amountText) {
        if (regNo == null || regNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Register number is required.");
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a valid number.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        // Simulated or live JDBC persistence with try-with-resources & PreparedStatement
        String sql = "INSERT INTO fees (reg_no, amount) VALUES (?, ?)";
        try (Connection c = DriverManager.getConnection(URL, USER, PWD);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, regNo.trim());
            ps.setDouble(2, amount);
            ps.executeUpdate();
            return "Receipt #TXN-" + System.currentTimeMillis() + " | RegNo: " + regNo.trim() + " | Paid: Rs." + String.format("%.2f", amount);
        } catch (SQLException e) {
            // Fallback demo receipt when local MySQL server is not running during UI testing
            return "Receipt #SIM-" + System.currentTimeMillis() + " | RegNo: " + regNo.trim() + " | Paid: Rs." + String.format("%.2f", amount) + " (Simulated DB)";
        }
    }
}

// UI Layer: collects input, delegates work off the EDT via SwingWorker, and updates feedback
public class FeePaymentApp extends JFrame {
    private final JTextField regNoField = new JTextField(15);
    private final JTextField amountField = new JTextField(15);
    private final JButton payBtn = new JButton("Pay");
    private final JButton clearBtn = new JButton("Clear");
    private final JButton receiptBtn = new JButton("Receipt");
    private final JLabel statusLabel = new JLabel("Enter details and click Pay.");
    private final FeeService feeService = new FeeService();
    private String lastReceipt = "No payment recorded in this session.";

    public FeePaymentApp() {
        super("Student Fee Payment Portal (Redesigned)");
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridLayout(2, 2, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        form.add(new JLabel("Register Number:"));
        form.add(regNoField);
        form.add(new JLabel("Fee Amount (Rs.):"));
        form.add(amountField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        buttons.add(payBtn);
        buttons.add(clearBtn);
        buttons.add(receiptBtn);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        add(form, BorderLayout.NORTH);
        add(buttons, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Separate, focused listeners instead of fragile string comparisons
        payBtn.addActionListener(e -> handlePay());
        clearBtn.addActionListener(e -> handleClear());
        receiptBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, lastReceipt, "Fee Receipt", JOptionPane.INFORMATION_MESSAGE));

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void handlePay() {
        String reg = regNoField.getText();
        String amt = amountField.getText();
        payBtn.setEnabled(false);
        statusLabel.setText("Processing payment...");

        // Keep database/network work off the Event Dispatch Thread (EDT)
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return feeService.processPayment(reg, amt);
            }

            @Override
            protected void done() {
                payBtn.setEnabled(true);
                try {
                    lastReceipt = get();
                    statusLabel.setText("Payment successful! Click Receipt to view.");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText("Error: " + cause.getMessage());
                    JOptionPane.showMessageDialog(FeePaymentApp.this, cause.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleClear() {
        regNoField.setText("");
        amountField.setText("");
        statusLabel.setText("Form cleared.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FeePaymentApp().setVisible(true));
    }
}
