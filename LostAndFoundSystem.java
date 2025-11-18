import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class LostAndFoundSystem {
    // Database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/usersdb";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "admin"; 

    // --- Utility Class for DB Management and Hashing ---
static class DBManager {
        public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }

        public static String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hashedBytes = md.digest(password.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hashedBytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                // Fallback to plain text if hashing algorithm is unavailable (bad practice in production)
                System.err.println("SHA-256 not available. Storing plain password.");
                return password;
            }
        }

        public static void showError(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
        }

        public static void initializeDatabase(JLabel statusLabel) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                statusLabel.setText("MySQL JDBC Driver not found!");
                statusLabel.setForeground(Color.RED);
                showError(null, "MySQL JDBC Driver not found!\nPlease add the MySQL Connector/J library.");
                return;
            }
            
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
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
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
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
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """;
                stmt.execute(usersTableSQL);
                stmt.execute(lostItemsTableSQL);
                stmt.execute(foundItemsTableSQL);
                statusLabel.setText("Database connected and tables checked.");
                statusLabel.setForeground(Color.GREEN);
            } catch (SQLException ex) {
                statusLabel.setText("Database connection failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
                showError(null, "Database connection failed: " + ex.getMessage() +
                         "\nPlease check your MySQL server and configuration.");
            }
        }
    }

    // --- Sign-up frame ---
    static class SignUpFrame extends JFrame {
        private JTextField usernameField;
        private JTextField emailField;
        private JPasswordField passwordField;
        private JLabel statusLabel;

        public SignUpFrame() {
            setTitle("Sign Up");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(450, 350);
            setLocationRelativeTo(null);

            JPanel panel = createFormPanel();

            DBManager.initializeDatabase(statusLabel); // Initialize DB on startup

            add(panel);
        }

        private JPanel createFormPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(10, 10, 10, 10);
            constraints.fill = GridBagConstraints.HORIZONTAL;

            // Username
            constraints.gridx = 0; constraints.gridy = 0; panel.add(new JLabel("Username:"), constraints);
            usernameField = new JTextField(20);
            constraints.gridx = 1; constraints.gridy = 0; panel.add(usernameField, constraints);

            // Email
            constraints.gridx = 0; constraints.gridy = 1; panel.add(new JLabel("Email:"), constraints);
            emailField = new JTextField(20);
            constraints.gridx = 1; constraints.gridy = 1; panel.add(emailField, constraints);

            // Password
            constraints.gridx = 0; constraints.gridy = 2; panel.add(new JLabel("Password:"), constraints);
            passwordField = new JPasswordField(20);
            constraints.gridx = 1; constraints.gridy = 2; panel.add(passwordField, constraints);

            // Buttons
            JButton submitButton = new JButton("Sign Up");
            constraints.gridx = 1; constraints.gridy = 3; panel.add(submitButton, constraints);

            JButton loginButton = new JButton("Go to Login");
            constraints.gridx = 0; constraints.gridy = 3; panel.add(loginButton, constraints);

            // Status Label
            statusLabel = new JLabel("Ready to sign up...");
            constraints.gridx = 0; constraints.gridy = 4;
            constraints.gridwidth = 2;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.CENTER;
            statusLabel.setForeground(Color.BLUE);
            panel.add(statusLabel, constraints);

            submitButton.addActionListener(e -> handleSignUp());
            loginButton.addActionListener(e -> {
                setVisible(false);
                new LoginFrame().setVisible(true);
            });
            return panel;
        }

        private void handleSignUp() {
            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                DBManager.showError(this, "Please fill in all fields");
                statusLabel.setText("Please fill in all fields");
                statusLabel.setForeground(Color.RED);
                return;
            }

            if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                DBManager.showError(this, "Please enter a valid email address");
                statusLabel.setText("Invalid email format");
                statusLabel.setForeground(Color.RED);
                return;
            }

            if (password.length() < 6) {
                DBManager.showError(this, "Password must be at least 6 characters long");
                statusLabel.setText("Password too short");
                statusLabel.setForeground(Color.RED);
                return;
            }

            if (userExists(username, email)) {
                DBManager.showError(this, "Username or email already exists");
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
                new LoginFrame().setVisible(true);
            } else {
                statusLabel.setText("Failed to create account");
                statusLabel.setForeground(Color.RED);
            }
        }

        private boolean userExists(String username, String email) {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
            try (Connection conn = DBManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, email);
                ResultSet rs = pstmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            } catch (SQLException ex) {
                DBManager.showError(this, "Error checking existing user: " + ex.getMessage());
                return false;
            }
        }

        private boolean insertUser(String username, String email, String password) {
            String hashedPassword = DBManager.hashPassword(password);
            String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
            try (Connection conn = DBManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, hashedPassword);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException ex) {
                DBManager.showError(this, "Error creating user: " + ex.getMessage());
                return false;
            }
        }

        private void clearFields() {
            usernameField.setText("");
            emailField.setText("");
            passwordField.setText("");
        }
    }

    // --- Login frame ---
    static class LoginFrame extends JFrame {
        private JTextField usernameField;
        private JPasswordField passwordField;
        private JLabel statusLabel;

        public LoginFrame() {
            setTitle("Login");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(450, 300);
            setLocationRelativeTo(null);

            JPanel panel = createFormPanel();
            add(panel);
        }

        private JPanel createFormPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(10, 10, 10, 10);
            constraints.fill = GridBagConstraints.HORIZONTAL;

            // Username
            constraints.gridx = 0; constraints.gridy = 0; panel.add(new JLabel("Username:"), constraints);
            usernameField = new JTextField(20);
            constraints.gridx = 1; constraints.gridy = 0; panel.add(usernameField, constraints);

            // Password
            constraints.gridx = 0; constraints.gridy = 1; panel.add(new JLabel("Password:"), constraints);
            passwordField = new JPasswordField(20);
            constraints.gridx = 1; constraints.gridy = 1; panel.add(passwordField, constraints);

            // Buttons
            JButton loginButton = new JButton("Login");
            constraints.gridx = 1; constraints.gridy = 2; panel.add(loginButton, constraints);

            JButton backButton = new JButton("Back to Sign Up");
            constraints.gridx = 0; constraints.gridy = 2; panel.add(backButton, constraints);

            // Status Label
            statusLabel = new JLabel("Ready to login...");
            constraints.gridx = 0; constraints.gridy = 3;
            constraints.gridwidth = 2;
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.CENTER;
            statusLabel.setForeground(Color.BLUE);
            panel.add(statusLabel, constraints);

            loginButton.addActionListener(e -> handleLogin());
            backButton.addActionListener(e -> {
                setVisible(false);
                new SignUpFrame().setVisible(true);
            });
            return panel;
        }

        private void handleLogin() {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) {
                DBManager.showError(this, "Please fill in all fields");
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
                DBManager.showError(this, "Invalid username or password");
                statusLabel.setText("Invalid credentials");
                statusLabel.setForeground(Color.RED);
            }
        }

        private int getUserId(String username, String password) {
            String hashedPassword = DBManager.hashPassword(password);
            String sql = "SELECT id, password FROM users WHERE username = ?";
            try (Connection conn = DBManager.getConnection();
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
                DBManager.showError(this, "Error during login: " + ex.getMessage());
                return -1;
            }
        }

        public void clearFields() {
            usernameField.setText("");
            passwordField.setText("");
            statusLabel.setText("Ready to login...");
            statusLabel.setForeground(Color.BLUE);
        }
    }

    // --- Main menu frame ---
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

            reportLostButton.addActionListener(e -> new ReportLostFrame(userId).setVisible(true));
            reportFoundButton.addActionListener(e -> new ReportFoundFrame(userId).setVisible(true));
            viewLostButton.addActionListener(e -> new ViewLostFrame().setVisible(true));
            viewFoundButton.addActionListener(e -> new ViewFoundFrame().setVisible(true));
            logoutButton.addActionListener(e -> {
                setVisible(false);
                loginFrame.setVisible(true);
            });

            add(panel);
        }
    }

    // --- Report lost item frame ---
    static class ReportLostFrame extends JFrame {
        private int userId;

        public ReportLostFrame(int userId) {
            this.userId = userId;
            setTitle("Report Lost Item");
            // Use DISPOSE_ON_CLOSE for secondary windows
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(450, 400);
            setLocationRelativeTo(null);

            JPanel panel = createFormPanel();
            add(panel);
        }

        private JPanel createFormPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(10, 10, 10, 10);
            constraints.fill = GridBagConstraints.HORIZONTAL;

            JTextField nameField = new JTextField(20);
            JTextArea descArea = new JTextArea(5, 20);
            JTextField locationField = new JTextField(20);
            JTextField dateField = new JTextField(LocalDate.now().toString(), 20); // Pre-fill with current date

            // Item Name
            constraints.gridx = 0; constraints.gridy = 0; panel.add(new JLabel("Item Name:"), constraints);
            constraints.gridx = 1; constraints.gridy = 0; panel.add(nameField, constraints);

            // Description
            constraints.gridx = 0; constraints.gridy = 1; panel.add(new JLabel("Description:"), constraints);
            JScrollPane descScroll = new JScrollPane(descArea);
            constraints.gridx = 1; constraints.gridy = 1; panel.add(descScroll, constraints);

            // Location
            constraints.gridx = 0; constraints.gridy = 2; panel.add(new JLabel("Location:"), constraints);
            constraints.gridx = 1; constraints.gridy = 2; panel.add(locationField, constraints);

            // Date Lost
            constraints.gridx = 0; constraints.gridy = 3; panel.add(new JLabel("Date Lost (YYYY-MM-DD):"), constraints);
            constraints.gridx = 1; constraints.gridy = 3; panel.add(dateField, constraints);

            // Submit Button
            JButton submitButton = new JButton("Submit");
            constraints.gridx = 1; constraints.gridy = 4;
            panel.add(submitButton, constraints);

            submitButton.addActionListener(e -> {
                String name = nameField.getText().trim();
                String desc = descArea.getText().trim();
                String location = locationField.getText().trim();
                String dateStr = dateField.getText().trim();

                if (name.isEmpty() || desc.isEmpty() || location.isEmpty() || dateStr.isEmpty()) {
                    DBManager.showError(this, "Please fill in all fields");
                    return;
                }

                try {
                    LocalDate.parse(dateStr); // Validate date format and convert
                } catch (DateTimeParseException ex) {
                    DBManager.showError(this, "Invalid date format. Use YYYY-MM-DD");
                    return;
                }

                if (insertLostItem(userId, name, desc, location, dateStr)) {
                    JOptionPane.showMessageDialog(this, "Lost item reported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    DBManager.showError(this, "Failed to report lost item");
                }
            });
            return panel;
        }

        private boolean insertLostItem(int userId, String name, String desc, String location, String dateStr) {
            String sql = "INSERT INTO lost_items (user_id, item_name, description, location, date_lost) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DBManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, name);
                pstmt.setString(3, desc);
                pstmt.setString(4, location);
                pstmt.setDate(5, java.sql.Date.valueOf(dateStr));
                return pstmt.executeUpdate() > 0;
            } catch (SQLException ex) {
                ex.printStackTrace();
                DBManager.showError(this, "Database error: " + ex.getMessage());
                return false;
            }
        }
    }

    // --- Report found item frame ---
    static class ReportFoundFrame extends JFrame {
        private int userId;

        public ReportFoundFrame(int userId) {
            this.userId = userId;
            setTitle("Report Found Item");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(450, 400);
            setLocationRelativeTo(null);

            JPanel panel = createFormPanel();
            add(panel);
        }

        private JPanel createFormPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(10, 10, 10, 10);
            constraints.fill = GridBagConstraints.HORIZONTAL;

            JTextField nameField = new JTextField(20);
            JTextArea descArea = new JTextArea(5, 20);
            JTextField locationField = new JTextField(20);
            JTextField dateField = new JTextField(LocalDate.now().toString(), 20); // Pre-fill with current date

            // Item Name
            constraints.gridx = 0; constraints.gridy = 0; panel.add(new JLabel("Item Name:"), constraints);
            constraints.gridx = 1; constraints.gridy = 0; panel.add(nameField, constraints);

            // Description
            constraints.gridx = 0; constraints.gridy = 1; panel.add(new JLabel("Description:"), constraints);
            JScrollPane descScroll = new JScrollPane(descArea);
            constraints.gridx = 1; constraints.gridy = 1; panel.add(descScroll, constraints);

            // Location
            constraints.gridx = 0; constraints.gridy = 2; panel.add(new JLabel("Location:"), constraints);
            constraints.gridx = 1; constraints.gridy = 2; panel.add(locationField, constraints);

            // Date Found
            constraints.gridx = 0; constraints.gridy = 3; panel.add(new JLabel("Date Found (YYYY-MM-DD):"), constraints);
            constraints.gridx = 1; constraints.gridy = 3; panel.add(dateField, constraints);

            // Submit Button
            JButton submitButton = new JButton("Submit");
            constraints.gridx = 1; constraints.gridy = 4;
            panel.add(submitButton, constraints);

            submitButton.addActionListener(e -> {
                String name = nameField.getText().trim();
                String desc = descArea.getText().trim();
                String location = locationField.getText().trim();
                String dateStr = dateField.getText().trim();

                if (name.isEmpty() || desc.isEmpty() || location.isEmpty() || dateStr.isEmpty()) {
                    DBManager.showError(this, "Please fill in all fields");
                    return;
                }

                try {
                    LocalDate.parse(dateStr);
                } catch (DateTimeParseException ex) {
                    DBManager.showError(this, "Invalid date format. Use YYYY-MM-DD");
                    return;
                }

                if (insertFoundItem(userId, name, desc, location, dateStr)) {
                    JOptionPane.showMessageDialog(this, "Found item reported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } else {
                    DBManager.showError(this, "Failed to report found item");
                }
            });
            return panel;
        }

        private boolean insertFoundItem(int userId, String name, String desc, String location, String dateStr) {
            String sql = "INSERT INTO found_items (user_id, item_name, description, location, date_found) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DBManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, name);
                pstmt.setString(3, desc);
                pstmt.setString(4, location);
                pstmt.setDate(5, java.sql.Date.valueOf(dateStr));
                return pstmt.executeUpdate() > 0;
            } catch (SQLException ex) {
                ex.printStackTrace();
                DBManager.showError(this, "Database error: " + ex.getMessage());
                return false;
            }
        }
    }

    // VIEW LOST ITEMS AND VIEW FOUND ITEMS FRAMES

static class ViewLostFrame extends JFrame {
    private DefaultTableModel model;
    private JTextField searchField;

    public ViewLostFrame() {
        setTitle("Lost Items");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // === Top Panel: Title + Search Bar ===
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        JLabel titleLabel = new JLabel("Lost Items", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BorderLayout(10, 0));
        searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setFont(new Font("Arial", Font.PLAIN, 16));
        searchField.setToolTipText("Search by item name, description, location, or reporter");
        searchPanel.add(searchField, BorderLayout.CENTER);

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            loadItems(""); // Reload all
        });
        searchPanel.add(clearBtn, BorderLayout.EAST);

        topPanel.add(searchPanel, BorderLayout.SOUTH);

        // === Table Setup ===
        String[] columns = {"ID", "Item Name", "Description", "Location", "Date Lost", "Reported By"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };

        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(70, 130, 180));
        table.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        // === Add real-time search listener ===
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }

            private void filter() {
                String query = searchField.getText().trim();
                loadItems(query);
            }
        });

        // === Layout ===
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Load all items initially
        loadItems("");
    }

    private void loadItems(String searchQuery) {
        String sql = """
            SELECT l.id, l.item_name, l.description, l.location, l.date_lost, u.username
            FROM lost_items l
            JOIN users u ON l.user_id = u.id
            WHERE LOWER(l.item_name) LIKE ?
               OR LOWER(l.description) LIKE ?
               OR LOWER(l.location) LIKE ?
               OR LOWER(u.username) LIKE ?
            ORDER BY l.id DESC
            """;

        String likeQuery = "%" + searchQuery.toLowerCase() + "%";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, likeQuery);
            pstmt.setString(2, likeQuery);
            pstmt.setString(3, likeQuery);
            pstmt.setString(4, likeQuery);

            ResultSet rs = pstmt.executeQuery();
            model.setRowCount(0); // Clear table

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

            // Update title if searching
            String title = searchQuery.isEmpty() ? "Lost Items" : 
                          "Lost Items - Search: \"" + searchQuery + "\" (" + model.getRowCount() + " results)";
            ((JLabel)((JPanel)getContentPane().getComponent(0)).getComponent(0)).setText(title);

        } catch (SQLException ex) {
            ex.printStackTrace();
            DBManager.showError(this, "Error loading lost items: " + ex.getMessage());
        }
    }
}

// === REPLACE ViewFoundFrame WITH THIS UPGRADED VERSION ===

static class ViewFoundFrame extends JFrame {
    private DefaultTableModel model;
    private JTextField searchField;

    public ViewFoundFrame() {
        setTitle("Found Items");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // === Top Panel ===
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        JLabel titleLabel = new JLabel("Found Items", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setFont(new Font("Arial", Font.PLAIN, 16));
        searchField.setToolTipText("Search by item name, description, location, or reporter");

        searchPanel.add(searchField, BorderLayout.CENTER);

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            loadItems("");
        });
        searchPanel.add(clearBtn, BorderLayout.EAST);

        topPanel.add(searchPanel, BorderLayout.SOUTH);

        // === Table ===
        String[] columns = {"ID", "Item Name", "Description", "Location", "Date Found", "Reported By"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(60, 179, 113));
        table.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        // === Real-time Search ===
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }

            private void filter() {
                String query = searchField.getText().trim();
                loadItems(query);
            }
        });

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadItems(""); // Load all initially
    }

    private void loadItems(String searchQuery) {
        String sql = """
            SELECT f.id, f.item_name, f.description, f.location, f.date_found, u.username
            FROM found_items f
            JOIN users u ON f.user_id = u.id
            WHERE LOWER(f.item_name) LIKE ?
               OR LOWER(f.description) LIKE ?
               OR LOWER(f.location) LIKE ?
               OR LOWER(u.username) LIKE ?
            ORDER BY f.id DESC
            """;

        String likeQuery = "%" + searchQuery.toLowerCase() + "%";

        try (Connection conn = DBManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 1; i <= 4; i++) {
                pstmt.setString(i, likeQuery);
            }

            ResultSet rs = pstmt.executeQuery();
            model.setRowCount(0);

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

            String title = searchQuery.isEmpty() ? "Found Items" :
                          "Found Items - Search: \"" + searchQuery + "\" (" + model.getRowCount() + " results)";
            ((JLabel)((JPanel)getContentPane().getComponent(0)).getComponent(0)).setText(title);

        } catch (SQLException ex) {
            ex.printStackTrace();
            DBManager.showError(this, "Error loading found items: " + ex.getMessage());
        }
    }
}

    // --- Main method ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpFrame().setVisible(true));
    }
}