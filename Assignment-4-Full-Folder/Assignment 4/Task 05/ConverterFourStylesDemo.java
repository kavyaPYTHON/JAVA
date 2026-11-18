import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// Demonstrates all 4 event-handling styles from Task 5.1 & Task 5.2 (Enter trigger + JOptionPane dialog)
public class ConverterFourStylesDemo {

    // Style (a): Frame implements ActionListener
    static class StyleAFrame extends JFrame implements ActionListener {
        private final JTextField amount = new JTextField(8);
        private final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
        private final JLabel result = new JLabel(" ");

        StyleAFrame() {
            super("Style A - Frame implements ActionListener");
            setLayout(new FlowLayout());
            JButton btn = new JButton("Convert");
            add(new JLabel("INR:")); add(amount); add(target); add(btn); add(result);
            btn.addActionListener(this);
            amount.addActionListener(this);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { dispose(); }
            });
            pack();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String out = compute(amount.getText(), (String) target.getSelectedItem());
            result.setText(out);
            JOptionPane.showMessageDialog(this, out, "Style A Result", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Style (b): Named Inner Class
    static class StyleBFrame extends JFrame {
        private final JTextField amount = new JTextField(8);
        private final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
        private final JLabel result = new JLabel(" ");

        private class ConvertHandler implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                String out = compute(amount.getText(), (String) target.getSelectedItem());
                result.setText(out);
                JOptionPane.showMessageDialog(StyleBFrame.this, out, "Style B Result", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        StyleBFrame() {
            super("Style B - Named Inner Class");
            setLayout(new FlowLayout());
            JButton btn = new JButton("Convert");
            add(new JLabel("INR:")); add(amount); add(target); add(btn); add(result);
            ActionListener handler = new ConvertHandler();
            btn.addActionListener(handler);
            amount.addActionListener(handler);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            pack();
        }
    }

    // Style (c): Anonymous Inner Class
    static class StyleCFrame extends JFrame {
        private final JTextField amount = new JTextField(8);
        private final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
        private final JLabel result = new JLabel(" ");

        StyleCFrame() {
            super("Style C - Anonymous Inner Class");
            setLayout(new FlowLayout());
            JButton btn = new JButton("Convert");
            add(new JLabel("INR:")); add(amount); add(target); add(btn); add(result);
            ActionListener anon = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String out = compute(amount.getText(), (String) target.getSelectedItem());
                    result.setText(out);
                    JOptionPane.showMessageDialog(StyleCFrame.this, out, "Style C Result", JOptionPane.INFORMATION_MESSAGE);
                }
            };
            btn.addActionListener(anon);
            amount.addActionListener(anon);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            pack();
        }
    }

    // Style (d): Lambda Expression
    static class StyleDFrame extends JFrame {
        private final JTextField amount = new JTextField(8);
        private final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
        private final JLabel result = new JLabel(" ");

        StyleDFrame() {
            super("Style D - Lambda Expression");
            setLayout(new FlowLayout());
            JButton btn = new JButton("Convert");
            add(new JLabel("INR:")); add(amount); add(target); add(btn); add(result);
            ActionListener lambda = e -> {
                String out = compute(amount.getText(), (String) target.getSelectedItem());
                result.setText(out);
                JOptionPane.showMessageDialog(this, out, "Style D Result", JOptionPane.INFORMATION_MESSAGE);
            };
            btn.addActionListener(lambda);
            amount.addActionListener(lambda);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            pack();
        }
    }

    static String compute(String raw, String currency) {
        try {
            double v = Double.parseDouble(raw.trim());
            if (v < 0) return "Invalid amount";
            double r = "USD".equals(currency) ? 0.012 : 0.011;
            return String.format("%.2f %s", v * r, currency);
        } catch (NumberFormatException e) {
            return "Invalid amount";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame a = new StyleAFrame(); a.setLocation(60, 80); a.setVisible(true);
            JFrame b = new StyleBFrame(); b.setLocation(60, 210); b.setVisible(true);
            JFrame c = new StyleCFrame(); c.setLocation(60, 340); c.setVisible(true);
            JFrame d = new StyleDFrame(); d.setLocation(60, 470); d.setVisible(true);
        });
    }
}
