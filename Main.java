
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Library Management System");
            frame.setSize(800, 500);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);

            JLabel title = new JLabel(
                "LIBRARY MANAGEMENT SYSTEM",
                SwingConstants.CENTER
            );
            title.setFont(new Font("Arial", Font.BOLD, 26));

            JLabel message = new JLabel(
                "Welcome to our Library!",
                SwingConstants.CENTER
            );
            message.setFont(new Font("Arial", Font.PLAIN, 18));

            JButton loginButton = new JButton("Admin Login");
            loginButton.addActionListener(e ->
                JOptionPane.showMessageDialog(
                    frame,
                    "Welcome, Admin!"
                )
            );

            JPanel panel = new JPanel(new GridLayout(3, 1, 10, 20));
            panel.setBorder(BorderFactory.createEmptyBorder(60, 50, 60, 50));
            panel.add(title);
            panel.add(message);
            panel.add(loginButton);

            frame.add(panel);
            frame.setVisible(true);
        });
    }
}
