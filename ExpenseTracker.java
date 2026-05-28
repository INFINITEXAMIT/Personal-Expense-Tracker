import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;

public class ExpenseTracker extends JFrame {

    private final java.util.List<Expense> expenses = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"Date", "Category", "Amount", "Note"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final String[] CATS = {"Food","Transport","Shopping","Bills","Health","Education","Other"};
    private final String FILE = "expenses.dat";

    private JTextField amtField, dateField, noteField;
    private JComboBox<String> catBox;
    private JLabel totalLabel, topCatLabel;

    public ExpenseTracker() {
        setTitle("Personal Expense Tracker");
        setSize(780, 580);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Header
        JLabel header = new JLabel("💰 Personal Expense Tracker", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.setOpaque(true);
        header.setBackground(new Color(34, 139, 87));
        header.setForeground(Color.WHITE);
        header.setBorder(new EmptyBorder(12, 0, 12, 0));
        add(header, BorderLayout.NORTH);

        // Left: form
        JPanel form = new JPanel(new GridLayout(9, 1, 4, 4));
        form.setBorder(new EmptyBorder(14, 14, 14, 8));
        form.setBackground(new Color(245, 250, 245));

        form.add(bold("Amount (₹)")); amtField = new JTextField(); form.add(amtField);
        form.add(bold("Category")); catBox = new JComboBox<>(CATS); form.add(catBox);
        form.add(bold("Date (DD/MM/YYYY)")); dateField = new JTextField(today()); form.add(dateField);
        form.add(bold("Note")); noteField = new JTextField(); form.add(noteField);

        JButton addBtn = btn("➕ Add Expense", new Color(34, 139, 87));
        addBtn.addActionListener(e -> addExpense());
        form.add(addBtn);

        JButton reportBtn = btn("📊 Monthly Report", new Color(70, 130, 180));
        reportBtn.addActionListener(e -> showReport());
        form.add(reportBtn);

        JButton deleteBtn = btn("🗑 Delete Selected", new Color(180, 60, 60));
        deleteBtn.addActionListener(e -> deleteSelected());
        form.add(deleteBtn);

        add(form, BorderLayout.WEST);

        // Center: table
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom: stats
        JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 0));
        bottom.setBorder(new EmptyBorder(8, 14, 10, 14));
        bottom.setBackground(new Color(230, 245, 235));
        totalLabel  = new JLabel("Total: ₹0.00"); totalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        topCatLabel = new JLabel("Top Category: —"); topCatLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        bottom.add(totalLabel); bottom.add(topCatLabel);
        add(bottom, BorderLayout.SOUTH);

        loadData();
        setVisible(true);
    }

    private void addExpense() {
        String amt = amtField.getText().trim();
        String date = dateField.getText().trim();
        if (amt.isEmpty() || date.isEmpty()) { alert("Amount and Date are required."); return; }
        try { Double.parseDouble(amt); } catch (NumberFormatException e) { alert("Invalid amount."); return; }

        Expense ex = new Expense(amt, (String) catBox.getSelectedItem(), date, noteField.getText().trim());
        expenses.add(ex);
        model.addRow(ex.toRow());
        amtField.setText(""); noteField.setText("");
        updateStats(); saveData();
    }

    private void deleteSelected() {
        // find selected row in table
        JTable tbl = (JTable)((JScrollPane)((BorderLayout)getContentPane().getLayout())
            .getLayoutComponent(BorderLayout.CENTER)).getViewport().getView();
        int row = tbl.getSelectedRow();
        if (row < 0) { alert("Select a row to delete."); return; }
        expenses.remove(row); model.removeRow(row);
        updateStats(); saveData();
    }

    private void showReport() {
        if (expenses.isEmpty()) { alert("No expenses recorded."); return; }

        // Group by month
        Map<String, Double> monthly = new LinkedHashMap<>();
        Map<String, Double> catMap  = new LinkedHashMap<>();
        for (Expense e : expenses) {
            String month = e.date.length() >= 7 ? e.date.substring(3) : e.date;
            double amt = Double.parseDouble(e.amount);
            monthly.merge(month, amt, Double::sum);
            catMap.merge(e.category, amt, Double::sum);
        }

        StringBuilder sb = new StringBuilder("=== Monthly Expense Report ===\n\n");
        monthly.forEach((m, v) -> sb.append(String.format("%-12s ₹%.2f%n", m, v)));
        sb.append("\n=== Category Breakdown ===\n\n");
        catMap.forEach((c, v) -> sb.append(String.format("%-12s ₹%.2f%n", c, v)));

        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font("Monospaced", Font.PLAIN, 13));
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Expense Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateStats() {
        double total = expenses.stream().mapToDouble(e -> Double.parseDouble(e.amount)).sum();
        totalLabel.setText(String.format("Total: ₹%.2f", total));

        Map<String, Double> catMap = new HashMap<>();
        for (Expense e : expenses) catMap.merge(e.category, Double.parseDouble(e.amount), Double::sum);
        catMap.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .ifPresent(en -> topCatLabel.setText("Top Category: " + en.getKey() +
                String.format(" (₹%.2f)", en.getValue())));
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE))) {
            java.util.List<Expense> loaded = (java.util.List<Expense>) in.readObject();
            expenses.addAll(loaded);
            expenses.forEach(e -> model.addRow(e.toRow()));
            updateStats();
        } catch (Exception ignored) {}
    }

    private void saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            out.writeObject(expenses);
        } catch (IOException e) { alert("Save failed: " + e.getMessage()); }
    }

    private String today() {
        Calendar c = Calendar.getInstance();
        return String.format("%02d/%02d/%04d", c.get(Calendar.DATE),
            c.get(Calendar.MONTH)+1, c.get(Calendar.YEAR));
    }

    private JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        return l;
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void alert(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExpenseTracker::new);
    }
}
