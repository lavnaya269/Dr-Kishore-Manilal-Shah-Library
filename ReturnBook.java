
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ReturnBook extends JFrame {

    private JTable table;
    private DefaultTableModel model;

    public ReturnBook() {
        setTitle("Library Management System - Return Books");
        setSize(850, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Heading
        JLabel heading = new JLabel(
            "Return Books", SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 26));
        heading.setBorder(
            BorderFactory.createEmptyBorder(15, 10, 10, 10)
        );
        add(heading, BorderLayout.NORTH);

        // Table
        model = new DefaultTableModel(
            new String[]{
                "Issue ID", "Student Name", "Roll Number",
                "Book Title", "Issue Date"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );
        table.setRowHeight(25);

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Return button
        JButton returnButton = new JButton("Return Selected Book");
        returnButton.setFont(new Font("Arial", Font.BOLD, 15));
        returnButton.setBackground(new Color(30, 120, 80));
        returnButton.setForeground(Color.WHITE);
        returnButton.setFocusPainted(false);

        returnButton.addActionListener(e -> returnBook());

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 15, 10)
        );
        bottomPanel.add(returnButton);

        add(bottomPanel, BorderLayout.SOUTH);

        loadIssuedBooks();

        setVisible(true);
    }

    // Display books that have not yet been returned
    private void loadIssuedBooks() {

        model.setRowCount(0);

        String sql =
            "SELECT ib.issue_id, s.name, s.roll_number, " +
            "b.title, ib.issue_date " +
            "FROM issued_books ib " +
            "JOIN students s ON ib.student_id = s.student_id " +
            "JOIN books b ON ib.book_id = b.book_id " +
            "WHERE ib.status = 'Issued' " +
            "ORDER BY ib.issue_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("issue_id"),
                    rs.getString("name"),
                    rs.getString("roll_number"),
                    rs.getString("title"),
                    rs.getDate("issue_date")
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not load issued books:\n" + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Return the selected book safely using a transaction
    private void returnBook() {

        int selectedRow = table.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a book to return."
            );
            return;
        }

        int issueId = (int) model.getValueAt(
            selectedRow, 0
        );

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to return this book?",
            "Confirm Return",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String findSql =
            "SELECT book_id FROM issued_books " +
            "WHERE issue_id = ? AND status = 'Issued' FOR UPDATE";

        String updateIssueSql =
            "UPDATE issued_books " +
            "SET status = 'Returned', return_date = CURRENT_DATE " +
            "WHERE issue_id = ? AND status = 'Issued'";

        String updateBookSql =
            "UPDATE books SET quantity = quantity + 1 " +
            "WHERE book_id = ?";

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {
                int bookId;

                // Check that this issue is still active
                try (PreparedStatement ps =
                         con.prepareStatement(findSql)) {

                    ps.setInt(1, issueId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                "This book has already been returned " +
                                "or the issue record no longer exists."
                            );
                        }

                        bookId = rs.getInt("book_id");
                    }
                }

                // Mark the issue as returned
                try (PreparedStatement ps =
                         con.prepareStatement(updateIssueSql)) {

                    ps.setInt(1, issueId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                            "Could not update the issue record."
                        );
                    }
                }

                // Increase available book quantity
                try (PreparedStatement ps =
                         con.prepareStatement(updateBookSql)) {

                    ps.setInt(1, bookId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                            "Could not update the book quantity."
                        );
                    }
                }

                con.commit();

                JOptionPane.showMessageDialog(
                    this,
                    "Book returned successfully!\n" +
                    "Return date has been recorded."
                );

            } catch (SQLException ex) {

                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }

                throw ex;

            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException resetError) {
                    // Connection cleanup is handled when it closes.
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not return the book:\n" + ex.getMessage(),
                "Return Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Refresh the list after a successful return
        loadIssuedBooks();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ReturnBook::new);
    }
}