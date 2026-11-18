import java.awt.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

// Domain Model
class Registration {
    final String regNo;
    final String studentName;
    final String email;
    final String eventName;

    Registration(String regNo, String studentName, String email, String eventName) {
        this.regNo = regNo;
        this.studentName = studentName;
        this.email = email;
        this.eventName = eventName;
    }
}

// Data-Access Layer (JDBC with clean in-memory fallback when MySQL is offline)
class RegistrationRepository {
    private static final String URL = "jdbc:mysql://localhost:3306/campus_events";
    private static final String USER = "root";
    private static final String PWD = "password";

    private final List<Registration> fallbackStore = Collections.synchronizedList(new ArrayList<>(Arrays.asList(
            new Registration("23CS101", "Aarav Sharma", "aarav@college.edu", "Hackathon 2026"),
            new Registration("23CS102", "Diya Nair", "diya@college.edu", "Robotics Expo")
    )));

    public synchronized void save(Registration r) throws Exception {
        String sql = "INSERT INTO event_registrations (reg_no, student_name, email, event_name) VALUES (?, ?, ?, ?)";
        try (Connection c = DriverManager.getConnection(URL, USER, PWD);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.regNo);
            ps.setString(2, r.studentName);
            ps.setString(3, r.email);
            ps.setString(4, r.eventName);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new IllegalStateException("Duplicate registration: " + r.regNo + " is already registered for " + r.eventName + ".");
        } catch (SQLException offline) {
            // Fallback in-memory duplicate check & persistence
            for (Registration existing : fallbackStore) {
                if (existing.regNo.equalsIgnoreCase(r.regNo) && existing.eventName.equalsIgnoreCase(r.eventName)) {
                    throw new IllegalStateException("Duplicate registration: " + r.regNo + " is already registered for " + r.eventName + ".");
                }
            }
            fallbackStore.add(r);
        }
    }

    public synchronized List<Registration> findByEvent(String eventFilter) {
        boolean all = eventFilter == null || "All Events".equals(eventFilter);
        String sql = all
                ? "SELECT reg_no, student_name, email, event_name FROM event_registrations ORDER BY id DESC"
                : "SELECT reg_no, student_name, email, event_name FROM event_registrations WHERE event_name = ? ORDER BY id DESC";
        try (Connection c = DriverManager.getConnection(URL, USER, PWD);
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (!all) ps.setString(1, eventFilter);
            try (ResultSet rs = ps.executeQuery()) {
                List<Registration> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new Registration(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
                }
                return rows;
            }
        } catch (SQLException offline) {
            List<Registration> rows = new ArrayList<>();
            for (Registration r : fallbackStore) {
                if (all || r.eventName.equalsIgnoreCase(eventFilter)) rows.add(r);
            }
            return rows;
        }
    }
}

// Service Layer: Input validation & business rule enforcement
class RegistrationService {
    private final RegistrationRepository repo = new RegistrationRepository();

    public void register(String regNo, String name, String email, String eventName) throws Exception {
        if (regNo == null || !regNo.trim().matches("[A-Za-z0-9]{5,15}")) {
            throw new IllegalArgumentException("Invalid Register Number (5-15 alphanumeric characters required).");
        }
        if (name == null || name.trim().length() < 2) {
            throw new IllegalArgumentException("Student name must be at least 2 characters.");
        }
        if (email == null || !email.trim().matches("^[^@\s]+@[^@\s]+\.[^@\s]+$")) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("Please select an event.");
        }
        repo.save(new Registration(regNo.trim().toUpperCase(), name.trim(), email.trim(), eventName.trim()));
    }

    public List<Registration> getRegistrations(String eventFilter) {
        return repo.findByEvent(eventFilter);
    }

    public File exportCsv(List<Registration> rows, File target) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(target))) {
            pw.println("RegisterNo,StudentName,Email,EventName");
            for (Registration r : rows) {
                pw.printf("%s,%s,%s,%s%n", r.regNo, r.studentName.replace(",", " "), r.email, r.eventName);
            }
        }
        return target;
    }
}

// Presentation Layer (Swing GUI with SwingWorker off-EDT execution)
public class CampusEventRegistrationApp extends JFrame {
    private final JTextField regNoField = new JTextField(12);
    private final JTextField nameField = new JTextField(15);
    private final JTextField emailField = new JTextField(15);
    private final JComboBox<String> eventBox = new JComboBox<>(new String[]{"Hackathon 2026", "Robotics Expo", "UI/UX Design Sprint", "Code Golf"});
    private final JComboBox<String> filterBox = new JComboBox<>(new String[]{"All Events", "Hackathon 2026", "Robotics Expo", "UI/UX Design Sprint", "Code Golf"});
    private final DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Register No", "Student Name", "Email", "Event"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel("Ready.");
    private final RegistrationService service = new RegistrationService();
    private List<Registration> currentRows = new ArrayList<>();

    public CampusEventRegistrationApp() {
        super("Campus Event Registration Manager");
        setLayout(new BorderLayout(8, 8));

        // Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportItem = new JMenuItem("Export Filtered CSV...");
        JMenuItem refreshItem = new JMenuItem("Refresh Table");
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exportItem);
        fileMenu.add(refreshItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Top Registration Form
        JPanel formPanel = new JPanel(new GridLayout(2, 4, 8, 6));
        formPanel.setBorder(BorderFactory.createTitledBorder("New Event Registration"));
        formPanel.add(new JLabel("Register No:"));
        formPanel.add(regNoField);
        formPanel.add(new JLabel("Student Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(emailField);
        formPanel.add(new JLabel("Event:"));
        formPanel.add(eventBox);

        JButton registerBtn = new JButton("Register Student");
        JPanel topWrapper = new JPanel(new BorderLayout(4, 4));
        topWrapper.add(formPanel, BorderLayout.CENTER);
        topWrapper.add(registerBtn, BorderLayout.EAST);

        // Center Table + Filter Row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton exportBtn = new JButton("Export CSV");
        filterRow.add(new JLabel("Filter by Event:"));
        filterRow.add(filterBox);
        filterRow.add(exportBtn);

        JPanel centerPanel = new JPanel(new BorderLayout(4, 4));
        centerPanel.add(filterRow, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));

        add(topWrapper, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        registerBtn.addActionListener(e -> submitRegistration());
        filterBox.addActionListener(e -> loadTableAsync((String) filterBox.getSelectedItem()));
        refreshItem.addActionListener(e -> loadTableAsync((String) filterBox.getSelectedItem()));
        exportBtn.addActionListener(e -> exportCsv());
        exportItem.addActionListener(e -> exportCsv());
        exitItem.addActionListener(e -> dispose());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(760, 460);
        setLocationRelativeTo(null);
        loadTableAsync("All Events");
    }

    private void submitRegistration() {
        String r = regNoField.getText();
        String n = nameField.getText();
        String em = emailField.getText();
        String ev = (String) eventBox.getSelectedItem();
        statusLabel.setText("Saving registration...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.register(r, n, em, ev);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    regNoField.setText("");
                    nameField.setText("");
                    emailField.setText("");
                    statusLabel.setText("Registered " + r.trim().toUpperCase() + " for " + ev + ".");
                    loadTableAsync((String) filterBox.getSelectedItem());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText("Rejected: " + cause.getMessage());
                    JOptionPane.showMessageDialog(CampusEventRegistrationApp.this, cause.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void loadTableAsync(String filter) {
        statusLabel.setText("Loading registrations...");
        new SwingWorker<List<Registration>, Void>() {
            @Override
            protected List<Registration> doInBackground() {
                return service.getRegistrations(filter);
            }

            @Override
            protected void done() {
                try {
                    currentRows = get();
                    tableModel.setRowCount(0);
                    for (Registration reg : currentRows) {
                        tableModel.addRow(new Object[]{reg.regNo, reg.studentName, reg.email, reg.eventName});
                    }
                    statusLabel.setText("Loaded " + currentRows.size() + " registration(s) [" + filter + "].");
                } catch (Exception ex) {
                    statusLabel.setText("Failed to load records: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void exportCsv() {
        try {
            File out = service.exportCsv(currentRows, new File("registrations_export.csv"));
            statusLabel.setText("Exported " + currentRows.size() + " row(s) to " + out.getName());
            JOptionPane.showMessageDialog(this, "Saved to " + out.getAbsolutePath(), "CSV Exported", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CampusEventRegistrationApp().setVisible(true));
    }
}
