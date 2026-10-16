
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class IssueBook extends JFrame {

    private JComboBox<StudentItem> studentBox;
    private JComboBox<BookItem> bookBox;
    private DefaultTableModel tableModel;

    public IssueBook() {
        setTitle("Library Management System - Issue Books");
        setSize(850, 550);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
                "ISSUE BOOKS", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setBorder(
                BorderFactory.createEmptyBorder(15, 5, 15, 5));
        add(heading, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder(
                "Issue a Book"));

        studentBox = new JComboBox<>();
        bookBox = new JComboBox<>();

        form.add(new JLabel("Select Student:"));
        form.add(studentBox);

        form.add(new JLabel("Select Book:"));
        form.add(bookBox);

        JButton issueButton = new JButton("Issue Book");
        JButton refreshButton = new JButton("Refresh Lists");

        form.add(issueButton);
        form.add(refreshButton);

        tableModel = new DefaultTableModel(
                new String[]{
                    "Issue ID", "Student", "Book",
                    "Issue Date", "Status"
                }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable issueTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(issueTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                "Issued Books"));

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBorder(
                BorderFactory.createEmptyBorder(0, 10, 10, 10));
        center.add(form, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        issueButton.addActionListener(e -> issueBook());
        refreshButton.addActionListener(e -> refreshData());

        refreshData();
    }

    private void refreshData() {
        loadStudents();
        loadBooks();
        loadIssuedBooks();
    }

    private void loadStudents() {
        studentBox.removeAllItems();

        String sql = "SELECT student_id, name, roll_number "
                   + "FROM students ORDER BY name";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                studentBox.addItem(new StudentItem(
                        rs.getInt("student_id"),
                        rs.getString("name"),
                        rs.getString("roll_number")));
            }

        } catch (SQLException ex) {
            showError("Could not load students", ex);
        }
    }

    private void loadBooks() {
        bookBox.removeAllItems();

        String sql = "SELECT book_id, title, quantity "
                   + "FROM books WHERE quantity > 0 "
                   + "ORDER BY title";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                bookBox.addItem(new BookItem(
                        rs.getInt("book_id"),
                        rs.getString("title"),
                        rs.getInt("quantity")));
            }

        } catch (SQLException ex) {
            showError("Could not load books", ex);
        }
    }

    private void issueBook() {
        StudentItem student =
                (StudentItem) studentBox.getSelectedItem();
        BookItem book =
                (BookItem) bookBox.getSelectedItem();

        if (student == null || book == null) {
            JOptionPane.showMessageDialog(this,
                    "Please register a student and add a book "
                    + "with available quantity first.");
            return;
        }

        String insertSql =
                "INSERT INTO issued_books "
                + "(student_id, book_id, issue_date, status) "
                + "VALUES (?, ?, ?, 'Issued')";

        String updateSql =
                "UPDATE books SET quantity = quantity - 1 "
                + "WHERE book_id = ? AND quantity > 0";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                // Reduce stock only if a copy is available.
                try (PreparedStatement update =
                        con.prepareStatement(updateSql)) {
                    update.setInt(1, book.id);

                    if (update.executeUpdate() != 1) {
                        throw new SQLException(
                                "No copies of this book are available.");
                    }
                }

                // Record the issue.
                try (PreparedStatement insert =
                        con.prepareStatement(insertSql)) {
                    insert.setInt(1, student.id);
                    insert.setInt(2, book.id);
                    insert.setDate(3,
                            Date.valueOf(LocalDate.now()));
                    insert.executeUpdate();
                }

                con.commit();

                JOptionPane.showMessageDialog(this,
                        "Book issued successfully to "
                        + student.name + "!");

                refreshData();

            } catch (SQLException ex) {
                con.rollback();
                showError("Could not issue book", ex);
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException ex) {
            showError("Database error", ex);
        }
    }

    private void loadIssuedBooks() {
        tableModel.setRowCount(0);

        String sql =
                "SELECT i.issue_id, s.name AS student_name, "
                + "b.title AS book_title, i.issue_date, i.status "
                + "FROM issued_books i "
                + "JOIN students s ON i.student_id = s.student_id "
                + "JOIN books b ON i.book_id = b.book_id "
                + "ORDER BY i.issue_id DESC";

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("issue_id"),
                    rs.getString("student_name"),
                    rs.getString("book_title"),
                    rs.getDate("issue_date"),
                    rs.getString("status")
                });
            }

        } catch (SQLException ex) {
            showError("Could not load issued books", ex);
        }
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(this,
                message + ":\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class StudentItem {
        int id;
        String name;
        String roll;

        StudentItem(int id, String name, String roll) {
            this.id = id;
            this.name = name;
            this.roll = roll;
        }

        @Override
        public String toString() {
            return name + " (" + roll + ")";
        }
    }

    private static class BookItem {
        int id;
        String title;
        int quantity;

        BookItem(int id, String title, int quantity) {
            this.id = id;
            this.title = title;
            this.quantity = quantity;
        }

        @Override
        public String toString() {
            return title + " (Available: " + quantity + ")";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> new IssueBook().setVisible(true));
    }
}
