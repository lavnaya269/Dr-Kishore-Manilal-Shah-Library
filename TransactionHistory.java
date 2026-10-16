
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class TransactionHistory extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> statusFilter;

    public TransactionHistory() {

        setTitle("Library Management System - Transaction History");
        setSize(1050, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Heading
        JLabel heading = new JLabel(
            "Issue & Return Transaction History",
            SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 25));
        heading.setBorder(
            BorderFactory.createEmptyBorder(15, 10, 10, 10)
        );

        add(heading, BorderLayout.NORTH);

        // Top panel with status filter and refresh button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel filterLabel = new JLabel("Filter by status:");

        statusFilter = new JComboBox<>(
            new String[]{"All", "Issued", "Returned"}
        );

        JButton refreshButton = new JButton("Refresh");

        topPanel.add(filterLabel);
        topPanel.add(statusFilter);
        topPanel.add(refreshButton);

        // Transaction table
        model = new DefaultTableModel(
            new String[]{
                "Issue ID",
                "Student Name",
                "Roll Number",
                "Book Title",
                "Issue Date",
                "Return Date",
                "Status"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(25);
        table.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(table);

        // Center panel
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 15, 10, 15)
        );

        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Filter and refresh actions
        statusFilter.addActionListener(e -> loadTransactions());

        refreshButton.addActionListener(e -> loadTransactions());

        // Load transaction records
        loadTransactions();

        setVisible(true);
    }

    // Load transaction history from MySQL
    private void loadTransactions() {

        model.setRowCount(0);

        String selectedStatus =
            (String) statusFilter.getSelectedItem();

        String sql =
            "SELECT ib.issue_id, s.name, s.roll_number, " +
            "b.title, ib.issue_date, ib.return_date, ib.status " +
            "FROM issued_books ib " +
            "JOIN students s ON ib.student_id = s.student_id " +
            "JOIN books b ON ib.book_id = b.book_id ";

        if ("Issued".equals(selectedStatus)
                || "Returned".equals(selectedStatus)) {
            sql += "WHERE ib.status = ? ";
        }

        sql += "ORDER BY ib.issue_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if ("Issued".equals(selectedStatus)
                    || "Returned".equals(selectedStatus)) {
                ps.setString(1, selectedStatus);
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    model.addRow(new Object[]{
                        rs.getInt("issue_id"),
                        rs.getString("name"),
                        rs.getString("roll_number"),
                        rs.getString("title"),
                        rs.getDate("issue_date"),
                        rs.getDate("return_date"),
                        rs.getString("status")
                    });
                }
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                this,
                "Could not load transaction history:\n"
                    + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new TransactionHistory();
        });
    }
}
