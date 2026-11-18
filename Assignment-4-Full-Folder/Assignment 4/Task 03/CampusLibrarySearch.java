import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class CampusLibrarySearch extends Frame {
    private final TextField queryField = new TextField(22);
    private final Choice categoryChoice = new Choice();
    private final Button searchBtn = new Button("Search");
    private final TextArea resultsArea = new TextArea("", 12, 55, TextArea.SCROLLBARS_VERTICAL_ONLY);
    private final Button prevBtn = new Button("Prev");
    private final Button nextBtn = new Button("Next");
    private final Button reserveBtn = new Button("Reserve");
    private final Label statusLabel = new Label("Ready. Enter keyword and click Search.");

    private final List<String[]> catalog = Arrays.asList(
            new String[]{"CS101", "Java Concurrency in Practice", "Computer Science", "Available"},
            new String[]{"CS102", "Effective Java (3rd Edition)", "Computer Science", "Available"},
            new String[]{"EC201", "Signals and Systems", "Electronics", "Checked Out"},
            new String[]{"MA301", "Linear Algebra and Its Applications", "Mathematics", "Available"},
            new String[]{"CS103", "Operating System Concepts", "Computer Science", "Available"},
            new String[]{"PH105", "Concepts of Modern Physics", "Physics", "Available"}
    );
    private List<String[]> filtered = new ArrayList<>();
    private int page = 0;
    private static final int PAGE_SIZE = 3;

    public CampusLibrarySearch() {
        super("Campus Library Search Screen");
        setLayout(new BorderLayout(8, 8));

        // NORTH container: Title bar + Search row stacked vertically so both keep preferred height
        Panel northContainer = new Panel(new BorderLayout(4, 4));
        Label titleBar = new Label("CAMPUS CENTRAL LIBRARY - CATALOG SEARCH", Label.CENTER);
        titleBar.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleBar.setBackground(new Color(225, 235, 245));

        Panel searchRow = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        categoryChoice.add("All Categories");
        categoryChoice.add("Computer Science");
        categoryChoice.add("Electronics");
        categoryChoice.add("Mathematics");
        categoryChoice.add("Physics");
        searchRow.add(new Label("Keyword:"));
        searchRow.add(queryField);
        searchRow.add(new Label("Category:"));
        searchRow.add(categoryChoice);
        searchRow.add(searchBtn);

        northContainer.add(titleBar, BorderLayout.NORTH);
        northContainer.add(searchRow, BorderLayout.SOUTH);

        // CENTER container: Expands in both width and height when window resizes
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // SOUTH container: Navigation/action buttons + Status bar keeping fixed height
        Panel southContainer = new Panel(new BorderLayout(4, 4));
        Panel buttonRow = new Panel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        buttonRow.add(prevBtn);
        buttonRow.add(nextBtn);
        buttonRow.add(reserveBtn);
        statusLabel.setBackground(new Color(240, 240, 240));
        southContainer.add(buttonRow, BorderLayout.NORTH);
        southContainer.add(statusLabel, BorderLayout.SOUTH);

        add(northContainer, BorderLayout.NORTH);
        add(resultsArea, BorderLayout.CENTER);
        add(southContainer, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> performSearch());
        queryField.addActionListener(e -> performSearch());
        prevBtn.addActionListener(e -> { if (page > 0) { page--; renderPage(); } });
        nextBtn.addActionListener(e -> { if ((page + 1) * PAGE_SIZE < filtered.size()) { page++; renderPage(); } });
        reserveBtn.addActionListener(e -> reserveFirstAvailable());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { dispose(); }
        });

        performSearch();
        setSize(680, 420);
        setLocationRelativeTo(null);
    }

    private void performSearch() {
        String q = queryField.getText().trim().toLowerCase();
        String cat = categoryChoice.getSelectedItem();
        filtered.clear();
        for (String[] book : catalog) {
            boolean matchText = q.isEmpty() || book[0].toLowerCase().contains(q) || book[1].toLowerCase().contains(q);
            boolean matchCat = cat.equals("All Categories") || book[2].equals(cat);
            if (matchText && matchCat) filtered.add(book);
        }
        page = 0;
        renderPage();
    }

    private void renderPage() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s | %-36s | %-18s | %-12s%n", "CODE", "TITLE", "CATEGORY", "STATUS"));
        sb.append("--------------------------------------------------------------------------------\n");
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());
        for (int i = start; i < end; i++) {
            String[] b = filtered.get(i);
            sb.append(String.format("%-8s | %-36s | %-18s | %-12s%n", b[0], b[1], b[2], b[3]));
        }
        if (filtered.isEmpty()) sb.append("No matching titles found.\n");
        resultsArea.setText(sb.toString());
        int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) PAGE_SIZE));
        statusLabel.setText("Showing " + filtered.size() + " result(s) | Page " + (page + 1) + " of " + totalPages);
    }

    private void reserveFirstAvailable() {
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filtered.size());
        for (int i = start; i < end; i++) {
            if ("Available".equals(filtered.get(i)[3])) {
                filtered.get(i)[3] = "Reserved";
                renderPage();
                statusLabel.setText("Reserved book: " + filtered.get(i)[0] + " - " + filtered.get(i)[1]);
                return;
            }
        }
        statusLabel.setText("No available copy on current page to reserve.");
    }

    public static void main(String[] args) {
        new CampusLibrarySearch().setVisible(true);
    }
}
