import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class HospitalAppointmentDesk extends JFrame {
    private final JTextField patientIdField = new JTextField(14);
    private final JComboBox<String> departmentBox;
    private final JComboBox<String> doctorBox = new JComboBox<>();
    private final JComboBox<String> slotBox = new JComboBox<>(new String[]{"09:30 AM", "11:00 AM", "02:30 PM", "04:15 PM"});
    private final JButton bookBtn = new JButton("Book Appointment");
    private final JButton resetBtn = new JButton("Reset");
    private final JLabel statusLabel = new JLabel("Ready.");
    private final Map<String, List<String>> departmentDoctors = new LinkedHashMap<>();
    private boolean bookingInProgress = false;

    public HospitalAppointmentDesk() {
        super("Hospital Outpatient Appointment Desk");
        departmentDoctors.put("Cardiology", Arrays.asList("Dr. Meera Nair", "Dr. Arvind Menon"));
        departmentDoctors.put("Orthopedics", Arrays.asList("Dr. Kiran Rao", "Dr. Sneha Das"));
        departmentDoctors.put("Neurology", Arrays.asList("Dr. Vikram Sethi", "Dr. Priya Iyer"));
        departmentDoctors.put("Dermatology", Arrays.asList("Dr. Ananya Roy"));

        departmentBox = new JComboBox<>(departmentDoctors.keySet().toArray(new String[0]));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        formPanel.add(new JLabel("Patient ID:"));
        formPanel.add(patientIdField);
        formPanel.add(new JLabel("Department:"));
        formPanel.add(departmentBox);
        formPanel.add(new JLabel("Doctor:"));
        formPanel.add(doctorBox);
        formPanel.add(new JLabel("Time Slot:"));
        formPanel.add(slotBox);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        actionPanel.add(bookBtn);
        actionPanel.add(resetBtn);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 10, 12));

        add(formPanel, BorderLayout.NORTH);
        add(actionPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Track dirty state while receptionist enters details
        patientIdField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { bookingInProgress = !patientIdField.getText().trim().isEmpty(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { bookingInProgress = !patientIdField.getText().trim().isEmpty(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });

        departmentBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                refreshDoctors((String) e.getItem());
            }
        });

        bookBtn.addActionListener(e -> bookAppointment());
        resetBtn.addActionListener(e -> clearForm("Form reset."));

        // Window closing guard when booking is in progress
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                System.out.println("[Trace] windowOpened invoked");
            }

            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("[Trace] windowClosing invoked");
                if (bookingInProgress) {
                    int choice = JOptionPane.showConfirmDialog(
                            HospitalAppointmentDesk.this,
                            "A booking is currently in progress. Discard and exit?",
                            "Confirm Close",
                            JOptionPane.YES_NO_OPTION);
                    if (choice != JOptionPane.YES_OPTION) return;
                }
                dispose();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                System.out.println("[Trace] windowClosed invoked");
            }
        });

        refreshDoctors((String) departmentBox.getSelectedItem());
        pack();
        setLocationRelativeTo(null);
    }

    private void refreshDoctors(String department) {
        doctorBox.removeAllItems();
        List<String> docs = departmentDoctors.getOrDefault(department, Collections.emptyList());
        for (String d : docs) doctorBox.addItem(d);
        statusLabel.setText("Loaded " + docs.size() + " doctor(s) for " + department + ".");
    }

    private void bookAppointment() {
        String pid = patientIdField.getText().trim();
        if (!pid.matches("P\\d{3,6}")) {
            statusLabel.setText("Error: Patient ID must match P101..P999999.");
            JOptionPane.showMessageDialog(this, "Enter a valid Patient ID (e.g., P1042).", "Invalid Patient ID", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String dept = (String) departmentBox.getSelectedItem();
        String doc = (String) doctorBox.getSelectedItem();
        String slot = (String) slotBox.getSelectedItem();
        String msg = String.format("Confirmed: Patient %s booked with %s (%s) at %s.", pid, doc, dept, slot);
        clearForm(msg);
        JOptionPane.showMessageDialog(this, msg, "Appointment Confirmed", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearForm(String message) {
        patientIdField.setText("");
        bookingInProgress = false;
        statusLabel.setText(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HospitalAppointmentDesk().setVisible(true));
    }
}
