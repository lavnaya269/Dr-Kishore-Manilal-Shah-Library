
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.sql.*;

public class BookManagement extends JFrame {

    private JTextField titleField, authorField, categoryField, quantityField;
    private JTextField searchField;
    private JTable booksTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel totalBooksLabel;

    private final Color NAVY = new Color(24, 35, 65);
    private final Color BLUE = new Color(55, 105, 210);
    private final Color BG = new Color(245, 247, 252);
    private final Color TEXT = new Color(45, 55, 75);
    private final Color MUTED = new Color(115, 125, 145);
    private final Color BORDER = new Color(225, 230, 240);

    public BookManagement() {
        setTitle("Library Pro - Manage Books");
        setSize(1100, 720);
        setMinimumSize(new Dimension(850, 600));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(22, 28, 22, 28));

        JPanel headingText = new JPanel();
        headingText.setOpaque(false);
        headingText.setLayout(new BoxLayout(headingText, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Manage Books");
        title.setFont(new Font("SansSerif", Font.BOLD, 27));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(
                "Add books to your collection and manage library inventory");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(205, 215, 235));

        headingText.add(title);
        headingText.add(Box.createVerticalStrut(6));
        headingText.add(subtitle);

        JButton closeButton = createButton(
                "Close", new Color(65, 78, 110));
        closeButton.addActionListener(e -> dispose());

        header.add(headingText, BorderLayout.WEST);
        header.add(closeButton, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(22, 25, 22, 25));

        // Add Book form
        JPanel formCard = new JPanel(new BorderLayout(0, 16));
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(createCardBorder());

        JPanel formHeading = new JPanel(new BorderLayout());
        formHeading.setOpaque(false);

        JLabel formTitle = new JLabel("Add New Book");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 19));
        formTitle.setForeground(TEXT);

        JLabel formSubtitle = new JLabel("Enter the details below");
        formSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        formSubtitle.setForeground(MUTED);

        JPanel formTitlePanel = new JPanel();
        formTitlePanel.setOpaque(false);
        formTitlePanel.setLayout(
                new BoxLayout(formTitlePanel, BoxLayout.Y_AXIS));

        formTitlePanel.add(formTitle);
        formTitlePanel.add(Box.createVerticalStrut(4));
        formTitlePanel.add(formSubtitle);

        formHeading.add(formTitlePanel, BorderLayout.WEST);

        JPanel fields = new JPanel(new GridLayout(2, 2, 18, 14));
        fields.setOpaque(false);

        titleField = createTextField("Enter book title");
        authorField = createTextField("Enter author's name");
        categoryField = createTextField("e.g. Programming, Fiction");
        quantityField = createTextField("1");
        quantityField.setText("1");

        fields.add(createFieldPanel("BOOK TITLE *", titleField));
        fields.add(createFieldPanel("AUTHOR *", authorField));
        fields.add(createFieldPanel("CATEGORY", categoryField));
        fields.add(createFieldPanel("QUANTITY *", quantityField));

        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        JButton clearButton = createButton(
                "Clear Form", new Color(235, 239, 247));
        clearButton.setForeground(TEXT);

        JButton addButton = createButton("+  Add Book", BLUE);

        clearButton.addActionListener(e -> clearForm());
        addButton.addActionListener(e -> addBook());

        buttons.add(clearButton);
        buttons.add(addButton);

        JPanel formBody = new JPanel(new BorderLayout(0, 16));
        formBody.setOpaque(false);
        formBody.add(fields, BorderLayout.CENTER);
        formBody.add(buttons, BorderLayout.SOUTH);

        formCard.add(formHeading, BorderLayout.NORTH);
        formCard.add(formBody, BorderLayout.CENTER);

        // Book list
        JPanel tableCard = new JPanel(new BorderLayout(0, 15));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(createCardBorder());

        JPanel tableHeading = new JPanel(new BorderLayout(10, 8));
        tableHeading.setOpaque(false);

        JPanel tableTitlePanel = new JPanel();
        tableTitlePanel.setOpaque(false);
        tableTitlePanel.setLayout(
                new BoxLayout(tableTitlePanel, BoxLayout.Y_AXIS));

        JLabel listTitle = new JLabel("Books in Library");
        listTitle.setFont(new Font("SansSerif", Font.BOLD, 19));
        listTitle.setForeground(TEXT);

        totalBooksLabel = new JLabel("Total records: 0");
        totalBooksLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        totalBooksLabel.setForeground(MUTED);

        tableTitlePanel.add(listTitle);
        tableTitlePanel.add(Box.createVerticalStrut(5));
        tableTitlePanel.add(totalBooksLabel);

        JPanel searchPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);

        searchField = createTextField("Search books...");
        searchField.setPreferredSize(new Dimension(210, 36));

        JButton refreshButton = createButton("Refresh", BLUE);
        refreshButton.addActionListener(e -> loadBooks());

        // Live search: title, author, and category only
        searchField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {

                    private void filter() {
                        String text = searchField.getText().trim();

                        if (sorter == null) {
                            return;
                        }

                        if (text.isEmpty()) {
                            sorter.setRowFilter(null);
                        } else {
                            sorter.setRowFilter(
                                    RowFilter.regexFilter(
                                            "(?i)" +
                                            java.util.regex.Pattern.quote(text),
                                            1, 2, 3
                                    )
                            );
                        }
                    }

                    @Override
                    public void insertUpdate(
                            javax.swing.event.DocumentEvent e) {
                        filter();
                    }

                    @Override
                    public void removeUpdate(
                            javax.swing.event.DocumentEvent e) {
                        filter();
                    }

                    @Override
                    public void changedUpdate(
                            javax.swing.event.DocumentEvent e) {
                        filter();
                    }
                });

        searchPanel.add(searchField);
        searchPanel.add(refreshButton);

        tableHeading.add(tableTitlePanel, BorderLayout.WEST);
        tableHeading.add(searchPanel, BorderLayout.EAST);

        // Table
        tableModel = new DefaultTableModel(
                new String[]{
                    "ID", "Book Title", "Author", "Category", "Quantity"
                }, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        booksTable = new JTable(tableModel);
        booksTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        booksTable.setForeground(TEXT);
        booksTable.setRowHeight(34);
        booksTable.setSelectionBackground(new Color(225, 235, 255));
        booksTable.setSelectionForeground(NAVY);
        booksTable.setShowVerticalLines(false);
        booksTable.setGridColor(new Color(238, 241, 247));
        booksTable.setFillsViewportHeight(true);

        booksTable.getTableHeader().setFont(
                new Font("SansSerif", Font.BOLD, 12));
        booksTable.getTableHeader().setBackground(
                new Color(239, 243, 250));
        booksTable.getTableHeader().setForeground(NAVY);
        booksTable.getTableHeader().setPreferredSize(
                new Dimension(0, 38));
        booksTable.getTableHeader().setReorderingAllowed(false);

        sorter = new TableRowSorter<>(tableModel);
        booksTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(booksTable);
        scrollPane.setBorder(
                BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);

        tableCard.add(tableHeading, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        content.add(formCard, BorderLayout.NORTH);
        content.add(tableCard, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        loadBooks();
    }

    private Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(20, 20, 20, 20));
    }

    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setPreferredSize(new Dimension(200, 36));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(5, 10, 5, 10)));
        field.setToolTipText(placeholder);
        return field;
    }

    private JPanel createFieldPanel(
            String labelText, JTextField field) {

        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(MUTED);

        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);

        return panel;
    }

    private JButton createButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 17, 10, 17));
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }

    private void clearForm() {
        titleField.setText("");
        authorField.setText("");
        categoryField.setText("");
        quantityField.setText("1");
        titleField.requestFocusInWindow();
    }

    private void addBook() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String category = categoryField.getText().trim();
        String quantityText = quantityField.getText().trim();

        if (title.isEmpty() || author.isEmpty()
                || quantityText.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter the book title, author, and quantity.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int quantity;

        try {
            quantity = Integer.parseInt(quantityText);

            if (quantity < 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Quantity cannot be negative.",
                        "Invalid Quantity",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid whole number for quantity.",
                    "Invalid Quantity",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO books "
                + "(title, author, category, quantity) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, title);
            ps.setString(2, author);
            ps.setString(3, category.isEmpty() ? null : category);
            ps.setInt(4, quantity);

            ps.executeUpdate();

            JOptionPane.showMessageDialog(
                    this,
                    "Book added successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            loadBooks();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not add book: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBooks() {
        if (tableModel == null) {
            return;
        }

        tableModel.setRowCount(0);

        String sql = "SELECT book_id, title, author, category, quantity "
                + "FROM books ORDER BY book_id DESC";

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("category"),
                    rs.getInt("quantity")
                });
            }

            totalBooksLabel.setText(
                    "Total records: " + tableModel.getRowCount());

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load books: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> new BookManagement().setVisible(true));
    }
}
