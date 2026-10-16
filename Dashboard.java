
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class Dashboard extends JFrame {

    private final Color NAVY = new Color(22, 32, 55);
    private final Color BLUE = new Color(45, 105, 220);
    private final Color BG = new Color(244, 247, 252);
    private final Color TEXT = new Color(35, 45, 65);
    private final Color MUTED = new Color(115, 125, 145);

    public Dashboard() {

        setTitle("Library Pro | Dashboard");
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ================= SIDEBAR =================

        JPanel sidebar = new JPanel();
        sidebar.setBackground(NAVY);
        sidebar.setPreferredSize(new Dimension(235, 700));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(
            new EmptyBorder(25, 16, 20, 16)
        );

        JLabel logo = new JLabel("  LIBRARY PRO");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 23));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(40));

        JLabel menuLabel = new JLabel("  MAIN MENU");
        menuLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        menuLabel.setForeground(new Color(155, 170, 195));
        menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(menuLabel);
        sidebar.add(Box.createVerticalStrut(15));

        addMenuItem(sidebar, "Dashboard", true);
        addMenuItem(sidebar, "Manage Books", false);
        addMenuItem(sidebar, "Manage Students", false);
        addMenuItem(sidebar, "Issue Books", false);
        addMenuItem(sidebar, "Return Books", false);
        addMenuItem(sidebar, "Transaction History", false);

        sidebar.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("Library Management System");
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        footer.setForeground(new Color(155, 170, 195));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(footer);

        // ================= MAIN CONTENT =================

        JPanel main = new JPanel(new BorderLayout(0, 25));
        main.setBackground(BG);
        main.setBorder(new EmptyBorder(30, 32, 30, 32));

        // Top heading
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel welcome = new JLabel("Welcome to Library Pro");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 29));
        welcome.setForeground(TEXT);

        JLabel subtitle = new JLabel(
            "Manage your library activities from one place."
        );
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);

        header.add(welcome);
        header.add(Box.createVerticalStrut(8));
        header.add(subtitle);

        main.add(header, BorderLayout.NORTH);

        // Cards
        JPanel cards = new JPanel(new GridLayout(0, 2, 20, 20));
        cards.setOpaque(false);

        cards.add(createCard(
            "01", "Manage Books",
            "Add, view and organize book records.",
            new Color(45, 105, 220),
            () -> new BookManagement().setVisible(true)
        ));

        cards.add(createCard(
            "02", "Manage Students",
            "Register students and view their records.",
            new Color(16, 150, 115),
            () -> new StudentManagement().setVisible(true)
        ));

        cards.add(createCard(
            "03", "Issue Books",
            "Issue available books to students.",
            new Color(220, 150, 35),
            () -> new IssueBook().setVisible(true)
        ));

        cards.add(createCard(
            "04", "Return Books",
            "Record returns and update availability.",
            new Color(135, 85, 210),
            () -> new ReturnBook().setVisible(true)
        ));

        cards.add(createCard(
            "05", "Transaction History",
            "Review issue and return transactions.",
            new Color(25, 145, 180),
            () -> new TransactionHistory().setVisible(true)
        ));

        // Scroll support for smaller windows
        JScrollPane scrollPane = new JScrollPane(cards);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        main.add(scrollPane, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);

        setVisible(true);
    }

    // ================= SIDEBAR ITEMS =================

    private void addMenuItem(
        JPanel sidebar,
        String text,
        boolean active
    ) {

        JLabel item = new JLabel("   " + text);
        item.setFont(new Font("Segoe UI", Font.BOLD, 13));
        item.setForeground(
            active ? Color.WHITE : new Color(205, 215, 230)
        );

        item.setOpaque(true);
        item.setBackground(active ? BLUE : NAVY);
        item.setBorder(new EmptyBorder(13, 5, 13, 5));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(item);
        sidebar.add(Box.createVerticalStrut(6));
    }

    // ================= DASHBOARD CARDS =================

    private JPanel createCard(
        String number,
        String title,
        String description,
        Color accent,
        Runnable action
    ) {

        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(
            new CompoundBorder(
                new LineBorder(new Color(225, 231, 240), 1, true),
                new EmptyBorder(20, 20, 20, 20)
            )
        );

        // Top row
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel numberLabel = new JLabel(number);
        numberLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        numberLabel.setForeground(accent);

        JLabel arrow = new JLabel("Open  >");
        arrow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        arrow.setForeground(accent);

        top.add(numberLabel, BorderLayout.WEST);
        top.add(arrow, BorderLayout.EAST);

        // Title
        JLabel name = new JLabel(title);
        name.setFont(new Font("Segoe UI", Font.BOLD, 19));
        name.setForeground(TEXT);

        // Description
        JLabel detail = new JLabel(
            "<html><div style='width:240px'>"
                + description
                + "</div></html>"
        );
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detail.setForeground(MUTED);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(
            new BoxLayout(details, BoxLayout.Y_AXIS)
        );

        details.add(name);
        details.add(Box.createVerticalStrut(8));
        details.add(detail);

        card.add(top, BorderLayout.NORTH);
        card.add(details, BorderLayout.CENTER);

        card.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        card.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(248, 250, 255));
                card.setBorder(
                    new CompoundBorder(
                        new LineBorder(accent, 1, true),
                        new EmptyBorder(20, 20, 20, 20)
                    )
                );
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(Color.WHITE);
                card.setBorder(
                    new CompoundBorder(
                        new LineBorder(
                            new Color(225, 231, 240), 1, true
                        ),
                        new EmptyBorder(20, 20, 20, 20)
                    )
                );
            }
        });

        return card;
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new Dashboard();
        });
    }
}
