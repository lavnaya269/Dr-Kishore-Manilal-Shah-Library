
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class StudentManagement extends JFrame {

    private JTextField nameField;
    private JTextField rollField;
    private JTextField departmentField;
    private JTextField phoneField;
    private DefaultTableModel tableModel;

    public StudentManagement() {
        setTitle("Library Management System - Manage Students");
        setSize(850, 550);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
                "STUDENT MANAGEMENT", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setBorder(
                BorderFactory.createEmptyBorder(15, 5, 15, 5));
        add(heading, BorderLayout.NORTH);

        // Student registration form
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBorder(
                BorderFactory.createTitledBorder("Student Details"));

        nameField = new JTextField();
        rollField = new JTextField();
        departmentField = new JTextField();
        phoneField = new JTextField();

        formPanel.add(new JLabel("Student Name:"));
        formPanel.add(nameField);

        formPanel.add(new JLabel("Roll Number:"));
        formPanel.add(rollField);

        formPanel.add(new JLabel("Department:"));
        formPanel.add(departmentField);

        formPanel.add(new JLabel("Phone Number:"));
        formPanel.add(phoneField);

        JButton addButton = new JButton("Register Student");
        JButton refreshButton = new JButton("Refresh Students");

        formPanel.add(addButton);
        formPanel.add(refreshButton);

        // Students table
        tableModel = new DefaultTableModel(
                new String[]{
                    "ID", "Name", "Roll Number", "Department", "Phone"
                }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable studentsTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(studentsTable);
        scrollPane.setBorder(
                BorderFactory.createTitledBorder("Registered Students"));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(
                BorderFactory.createEmptyBorder(0, 10, 10, 10));
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        addButton.addActionListener(e -> addStudent());
        refreshButton.addActionListener(e -> loadStudents());

        loadStudents();
    }

    private void addStudent() {
        String name = nameField.getText().trim();
        String roll = rollField.getText().trim();
        String department = departmentField.getText().trim();
        String phone = phoneField.getText().trim();

        if (name.isEmpty() || roll.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "Please enter the student name and roll number.");
            return;
        }

        String sql = "INSERT INTO students "
                   + "(name, roll_number, department, phone) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, roll);
            ps.setString(3, department.isEmpty() ? null : department);
            ps.setString(4, phone.isEmpty() ? null : phone);

            ps.executeUpdate();

            JOptionPane.showMessageDialog(
                    this, "Student registered successfully!");

            nameField.setText("");
            rollField.setText("");
            departmentField.setText("");
            phoneField.setText("");

            loadStudents();

        } catch (SQLIntegrityConstraintViolationException ex) {
            JOptionPane.showMessageDialog(
                    this, "This roll number is already registered.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    this, "Could not register student: " + ex.getMessage());
        }
    }

    private void loadStudents() {
        tableModel.setRowCount(0);

        String sql = "SELECT student_id, name, roll_number, "
                   + "department, phone FROM students "
                   + "ORDER BY student_id DESC";

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("student_id"),
                    rs.getString("name"),
                    rs.getString("roll_number"),
                    rs.getString("department"),
                    rs.getString("phone")
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    this, "Could not load students: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> new StudentManagement().setVisible(true));
    }
}