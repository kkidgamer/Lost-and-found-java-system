import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.table.DefaultTableModel;

public class LostAndFoundSystem {
    // Database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/usersdb";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "admin"; // Change to your MySQL password

    // Sign-up frame
    static class SignUpFrame extends JFrame {
        private JTextField usernameField;
        private JTextField emailField;
        private JPasswordField passwordField;
        private JButton submitButton;
        private JButton loginButton;
        private JLabel statusLabel;

        public SignUpFrame() {
            setTitle("Sign Up");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(450, 350);
            setLocationRelativeTo(null);

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(10, 10, 10, 10);
            constraints.fill = GridBagConstraints.HORIZONTAL;

            JLabel usernameLabel = new JLabel("Username:");
            constraints.gridx = 0;
            constraints.gridy = 0;
            panel.add(usernameLabel, constraints);

            usernameField = new JTextField(20);
            constraints.gridx = 1;
            constraints.gridy = 0;
            panel.add(usernameField, constraints);

            JLabel emailLabel = new JLabel("Email:");
            constraints.gridx = 0;
            constraints.gridy = 1;
            panel.add(emailLabel, constraints);

            emailField = new JTextField(20);
            constraints.gridx = 1;
            constraints.gridy = 1;
            panel.add(emailField, constraints);

            JLabel passwordLabel = new JLabel("Password:");
            constraints.gridx = 0;
            constraints.gridy = 2;
            panel.add(passwordLabel, constraints);

            passwordField = new JPasswordField(20);
            constraints.gridx = 1;
            constraints.gridy = 2;
            panel.add(passwordField, constraints);

            submitButton = new JButton("Sign Up");
            constraints.gridx = 1;
            constraints.gridy = 3;
            panel.add(submitButton, constraints);

            loginButton = new JButton("Go to Login");
            constraints.gridx = 0;
            constraints.gridy = 3;
            panel.add(loginButton, constraints);

            statusLabel = new JLabel("Ready to sign up...");
            constraints.gridx = 0;
            constraints.gridy = 4;
            constraints.gridwidth = 2;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.CENTER;
            statusLabel.setForeground(Color.BLUE);
            panel.add(statusLabel, constraints);

            submitButton.addActionListener(e -> {
                String username = usernameField.getText().trim();
                String email = emailField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();

                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    showError("Please fill in all fields");
                    statusLabel.setText("Please fill in all fields");
                    statusLabel.setForeground(Color.RED);
                    return;
                }

                if (!email.contains("@") || !email.contains(".")) {
                    showError("Please enter a valid email address");
                    statusLabel.setText("Invalid email format");
                    statusLabel.setForeground(Color.RED);
                    return;
                }

                if (password.length() < 6) {
                    showError("Password must be at least 6 characters long");
                    statusLabel.setText("Password too short");
                    statusLabel.setForeground(Color.RED);
                    return;
                }

                if (userExists(username, email)) {
                    showError("Username or email already exists");
                    statusLabel.setText("Username or email already exists");
                    statusLabel.setForeground(Color.RED);
                    return;
                }

                if (insertUser(username, email, password)) {
                    JOptionPane.showMessageDialog(this, 
                        "Sign Up Successful!\nWelcome, " + username + "!",
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    statusLabel.setText("Sign up successful! Welcome " + username);
                    statusLabel.setForeground(Color.GREEN);
                    clearFields();
                    setVisible(false);
                    new LoginFrame(this).setVisible(true);
                } else {
                    statusLabel.setText("Failed to create account");
                    statusLabel.setForeground(Color.RED);
                }
            });

            loginButton.addActionListener(e -> {
                setVisible(false);
                new LoginFrame(this).setVisible(true);
            });

            add(panel);
            testDatabaseConnection(statusLabel);
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        }

        private void testDatabaseConnection(JLabel statusLabel) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String usersTableSQL = """
                    CREATE TABLE IF NOT EXISTS users (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        username VARCHAR(50) UNIQUE NOT NULL,
                        email VARCHAR(100) UNIQUE NOT NULL,
                        password VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;
                String lostItemsTableSQL = """
                    CREATE TABLE IF NOT EXISTS lost_items (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        item_name VARCHAR(100) NOT NULL,
                        description TEXT,
                        location VARCHAR(255),
                        date_lost DATE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id)
                    )
                    """;
                String foundItemsTableSQL = """
                    CREATE TABLE IF NOT EXISTS found_items (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        item_name VARCHAR(100) NOT NULL,
                        description TEXT,
                        location VARCHAR(255),
                        date_found DATE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id)
                    )
                    """;
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(usersTableSQL);
                    stmt.execute(lostItemsTableSQL);
                    stmt.execute(foundItemsTableSQL);
                    statusLabel.setText("Database connected successfully");
                    statusLabel.setForeground(Color.GREEN);
                }
            } catch (SQLException ex) {
                statusLabel.setText("Database connection failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                showError("Database connection failed: " + ex.getMessage() + 
                         "\nPlease check your MySQL server and configuration.");
            }
        }

        private boolean userExists(String username, String email) {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, email);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            } catch (SQLException ex) {
                showError("Error checking existing user: " + ex.getMessage());
            }
            return false;
        }

        private boolean insertUser(String username, String email, String password) {
            String hashedPassword = hashPassword(password);
            String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, hashedPassword);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException ex) {
                showError("Error creating user: " + ex.getMessage());
                return false;
            }
        }

        private String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hashedBytes = md.digest(password.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hashedBytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                return password;
            }
        }

        private void clearFields() {
            usernameField.setText("");
            emailField.setText("");
            passwordField.setText("");
        }
    }

    // Login frame
    static class LoginFrame extends JFrame {
        private JTextField usernameField;
        private JPasswordField passwordField;
        private JButton loginButton;
        private JButton backButton;
        private JLabel statusLabel;
        private SignUpFrame signUpFrame;

        public LoginFrame(SignUpFrame signUpFrame) {
            this.signUpFrame = signUpFrame;
            setTitle("Login");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(450, 300);
            setLocationRelativeTo(null);

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(10, 10, 10, 10);
            constraints.fill = GridBagConstraints.HORIZONTAL;

            JLabel usernameLabel = new JLabel("Username:");
            constraints.gridx = 0;
            constraints.gridy = 0;
            panel.add(usernameLabel, constraints);

            usernameField = new JTextField(20);
            constraints.gridx = 1;
            constraints.gridy = 0;
            panel.add(usernameField, constraints);

            JLabel passwordLabel = new JLabel("Password:");
            constraints.gridx = 0;
            constraints.gridy = 1;
            panel.add(passwordLabel, constraints);

            passwordField = new JPasswordField(20);
            constraints.gridx = 1;
            constraints.gridy = 1;
            panel.add(passwordField, constraints);

            loginButton = new JButton("Login");
            constraints.gridx = 1;
            constraints.gridy = 2;
            panel.add(loginButton, constraints);

            backButton = new JButton("Back to Sign Up");
            constraints.gridx = 0;
            constraints.gridy = 2;
            panel.add(backButton, constraints);

            statusLabel = new JLabel("Ready to login...");
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.gridwidth = 2;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.CENTER;
            statusLabel.setForeground(Color.BLUE);
            panel.add(statusLabel, constraints);

            loginButton.addActionListener(e -> {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();

                if (username.isEmpty() || password.isEmpty()) {
                    showError("Please fill in all fields");
                    statusLabel.setText("Please fill in all fields");
                    statusLabel.setForeground(Color.RED);
                    return;
                }

                int userId = getUserId(username, password);
                if (userId > 0) {
                    JOptionPane.showMessageDialog(this, 
                        "Login Successful!\nWelcome back, " + username + "!",
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    statusLabel.setText("Login successful! Welcome " + username);
                    statusLabel.setForeground(Color.GREEN);
                    clearFields();
                    setVisible(false);
                    new MainMenuFrame(userId, this).setVisible(true);
                } else {
                    showError("Invalid username or password");
                    statusLabel.setText("Invalid credentials");
                    statusLabel.setForeground(Color.RED);
                }
            });

            backButton.addActionListener(e -> {
                setVisible(false);
                signUpFrame.setVisible(true);
            });

            add(panel);
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        }

        private int getUserId(String username, String password) {
            String hashedPassword = hashPassword(password);
            String sql = "SELECT id, password FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (hashedPassword.equals(storedPassword)) {
                        return rs.getInt("id");
                    }
                }
                return -1;
            } catch (SQLException ex) {
                showError("Error during login: " + ex.getMessage());
                return -1;
            }
        }

        private String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hashedBytes = md.digest(password.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hashedBytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                return password;
            }
        }

        public void clearFields() {
            usernameField.setText("");
            passwordField.setText("");
            statusLabel.setText("Ready to login...");
            statusLabel.setForeground(Color.BLUE);
        }
    }

    // Main menu frame
    static class MainMenuFrame extends JFrame {
        private int userId;
        private LoginFrame loginFrame;

        public MainMenuFrame(int userId, LoginFrame loginFrame) {
            this.userId = userId;
            this.loginFrame = loginFrame;
            setTitle("Lost and Found System");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(400, 300);
            setLocationRelativeTo(null);

            JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JButton reportLostButton = new JButton("Report Lost Item");
            JButton reportFoundButton = new JButton("Report Found Item");
            JButton viewLostButton = new JButton("View Lost Items");
            JButton viewFoundButton = new JButton("View Found Items");
            JButton logoutButton = new JButton("Logout");

            panel.add(reportLostButton);
            panel.add(reportFoundButton);
            panel.add(viewLostButton);
            panel.add(viewFoundButton);
            panel.add(logoutButton);

            reportLostButton.addActionListener(e -> new ReportLostFrame(userId, this).setVisible(true));

            reportFoundButton.addActionListener(e -> new ReportFoundFrame(userId, this).setVisible(true));

            viewLostButton.addActionListener(e -> new ViewLostFrame().setVisible(true));

            viewFoundButton.addActionListener(e -> new ViewFoundFrame().setVisible(true));

            logoutButton.addActionListener(e -> {
                setVisible(false);
                loginFrame.setVisible(true);
            });

            add(panel);
        }
    }

    // Report lost item frame
    static class ReportLostFrame extends JFrame {
        private int userId;
        private MainMenuFrame mainMenuFrame;

        public ReportLostFrame(int userId, MainMenuFrame mainMenuFrame) {
            this.userId = userId;
            this.mainMenuFrame = mainMenuFrame;
            setTitle("Report Lost Item");
            setSize(450, 400);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(10, 10, 10, 10);
            constraints.fill = GridBagConstraints.HORIZONTAL;

            JLabel nameLabel = new JLabel("Item Name:");
            constraints.gridx = 0;
            constraints.gridy = 0;
            panel.add(nameLabel, constraints);

            JTextField nameField = new JTextField(20);
            constraints.gridx = 1;
            constraints.gridy = 0;
            panel.add(nameField, constraints);

            JLabel descLabel = new JLabel("Description:");
            constraints.gridx = 0;
            constraints.gridy = 1;
            panel.add(descLabel, constraints);

            JTextArea descArea = new JTextArea(5, 20);
            JScrollPane descScroll = new JScrollPane(descArea);
            constraints.gridx = 1;
            constraints.gridy = 1;
            panel.add(descScroll, constraints);

            JLabel locationLabel = new JLabel("Location:");
            constraints.gridx = 0;
            constraints.gridy = 2;
            panel.add(locationLabel, constraints);

            JTextField locationField = new JTextField(20);
            constraints.gridx = 1;
            constraints.gridy = 2;
            panel.add(locationField, constraints);

            JLabel dateLabel = new JLabel("Date Lost (YYYY-MM-DD):");
            constraints.gridx = 0;
            constraints.gridy = 3;
            panel.add(dateLabel, constraints);

            JTextField dateField = new JTextField(20);
            constraints.gridx = 1;
            constraints.gridy = 3;
            panel.add(dateField, constraints);

            JButton submitButton = new JButton("Submit");
            constraints.gridx = 1;
            constraints.gridy = 4;
            panel.add(submitButton, constraints);

            submitButton.addActionListener(e -> {
                String name = nameField.getText().trim();
                String desc = descArea.getText().trim();
                String location = locationField.getText().trim();
                String dateStr = dateField.getText().trim();

                if (name.isEmpty() || desc.isEmpty() || location.isEmpty() || dateStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill in all fields", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    java.sql.Date.valueOf(dateStr); // Validate date format
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (insertLostItem(userId, name, desc, location, dateStr)) {
                    JOptionPane.showMessageDialog(this, "Lost item reported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to report lost item", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            add(panel);
        }

        private boolean insertLostItem(int userId, String name, String desc, String location, String dateStr) {
            String sql = "INSERT INTO lost_items (user_id, item_name, description, location, date_lost) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, name);
                pstmt.setString(3, desc);
                pstmt.setString(4, location);
                pstmt.setDate(5, java.sql.Date.valueOf(dateStr));
                return pstmt.executeUpdate() > 0;
            } catch (SQLException ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }

    // Report found item frame
    static class ReportFoundFrame extends JFrame {
        private int userId;
        private MainMenuFrame mainMenuFrame;

        public ReportFoundFrame(int userId, MainMenuFrame mainMenuFrame) {
            this.userId = userId;
            this.mainMenuFrame = mainMenuFrame;
            setTitle("Report Found Item");
            setSize(450, 400);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(10, 10, 10, 10);
            constraints.fill = GridBagConstraints.HORIZONTAL;

            JLabel nameLabel = new JLabel("Item Name:");
            constraints.gridx = 0;
            constraints.gridy = 0;
            panel.add(nameLabel, constraints);

            JTextField nameField = new JTextField(20);
            constraints.gridx = 1;
            constraints.gridy = 0;
            panel.add(nameField, constraints);

            JLabel descLabel = new JLabel("Description:");
            constraints.gridx = 0;
            constraints.gridy = 1;
            panel.add(descLabel, constraints);

            JTextArea descArea = new JTextArea(5, 20);
            JScrollPane descScroll = new JScrollPane(descArea);
            constraints.gridx = 1;
            constraints.gridy = 1;
            panel.add(descScroll, constraints);

            JLabel locationLabel = new JLabel("Location:");
            constraints.gridx = 0;
            constraints.gridy = 2;
            panel.add(locationLabel, constraints);

            JTextField locationField = new JTextField(20);
            constraints.gridx = 1;
            constraints.gridy = 2;
            panel.add(locationField, constraints);

            JLabel dateLabel = new JLabel("Date Found (YYYY-MM-DD):");
            constraints.gridx = 0;
            constraints.gridy = 3;
            panel.add(dateLabel, constraints);

            JTextField dateField = new JTextField(20);
            constraints.gridx = 1;
            constraints.gridy = 3;
            panel.add(dateField, constraints);

            JButton submitButton = new JButton("Submit");
            constraints.gridx = 1;
            constraints.gridy = 4;
            panel.add(submitButton, constraints);

            submitButton.addActionListener(e -> {
                String name = nameField.getText().trim();
                String desc = descArea.getText().trim();
                String location = locationField.getText().trim();
                String dateStr = dateField.getText().trim();

                if (name.isEmpty() || desc.isEmpty() || location.isEmpty() || dateStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill in all fields", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    java.sql.Date.valueOf(dateStr);
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (insertFoundItem(userId, name, desc, location, dateStr)) {
                    JOptionPane.showMessageDialog(this, "Found item reported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to report found item", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            add(panel);
        }

        private boolean insertFoundItem(int userId, String name, String desc, String location, String dateStr) {
            String sql = "INSERT INTO found_items (user_id, item_name, description, location, date_found) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, name);
                pstmt.setString(3, desc);
                pstmt.setString(4, location);
                pstmt.setDate(5, java.sql.Date.valueOf(dateStr));
                return pstmt.executeUpdate() > 0;
            } catch (SQLException ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }

    // View lost items frame
    static class ViewLostFrame extends JFrame {
        public ViewLostFrame() {
            setTitle("Lost Items");
            setSize(800, 600);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            String[] columns = {"ID", "Item Name", "Description", "Location", "Date Lost", "Reported By"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);
            JTable table = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(table);
            add(scrollPane);

            loadLostItems(model);
        }

        private void loadLostItems(DefaultTableModel model) {
            String sql = """
                SELECT l.id, l.item_name, l.description, l.location, l.date_lost, u.username
                FROM lost_items l
                JOIN users u ON l.user_id = u.id
                """;
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("id"),
                        rs.getString("item_name"),
                        rs.getString("description"),
                        rs.getString("location"),
                        rs.getDate("date_lost"),
                        rs.getString("username")
                    };
                    model.addRow(row);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading lost items: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // View found items frame
    static class ViewFoundFrame extends JFrame {
        public ViewFoundFrame() {
            setTitle("Found Items");
            setSize(800, 600);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            String[] columns = {"ID", "Item Name", "Description", "Location", "Date Found", "Reported By"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);
            JTable table = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(table);
            add(scrollPane);

            loadFoundItems(model);
        }

        private void loadFoundItems(DefaultTableModel model) {
            String sql = """
                SELECT f.id, f.item_name, f.description, f.location, f.date_found, u.username
                FROM found_items f
                JOIN users u ON f.user_id = u.id
                """;
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Object[] row = {
                        rs.getInt("id"),
                        rs.getString("item_name"),
                        rs.getString("description"),
                        rs.getString("location"),
                        rs.getDate("date_found"),
                        rs.getString("username")
                    };
                    model.addRow(row);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading found items: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
            e.printStackTrace();
            return;
        }

        SwingUtilities.invokeLater(() -> new SignUpFrame().setVisible(true));
    }
}