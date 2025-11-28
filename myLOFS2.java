import javax.swing.*;

import LostAndFoundSystem.DBManager;

import java.awt.*;
import java.sql.*;



public class myLOFS2 {
    
    // Database configurations
    static String db_URL = "jdbc:mysql://localhost:3306/usersdb";
    static String db_USER = "root";
    static String db_PASSWORD = "admin";

    //  --- Utility Class for Database Operations ---
    public static class DatabaseUtil {
        
        // Method to establish a database connection
        public static Connection getConnection() throws SQLException {
            return java.sql.DriverManager.getConnection(db_URL,db_USER,db_PASSWORD);
        }
        
        // Method to close database resources
        public static void closeResources(java.sql.Connection conn, java.sql.Statement stmt, java.sql.ResultSet rs) {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        }
    }
        public static void showError(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
            
        }

        public static void initializeDatabase(JLabel statusLabel) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");

            } catch (ClassNotFoundException e) {
                // TODO: handle exception
                statusLabel.setText("Database Driver not found.");
                statusLabel.setForeground(Color.RED);
                showError(null, "MySQL JDBC Driver not found!\nPlease add the MySQL Connector/J library.");
                return;
            }

            try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()){
                String usersTableSQL = """
                        CREATE TABLE IF NOT EXISTS users (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(50) NOT NULL UNIQUE,
                            email VARCHAR(100) NOT NULL UNIQUE,
                            password VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        );
                        """;

                String lostItemsTableSQL = """
                        CREATE TABLE IF NOT EXISTS lost_items (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            item_name VARCHAR(100) NOT NULL,
                            description TEXT,
                            date_lost DATE,
                            location VARCHAR(255),
                            contact_info VARCHAR(255),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        );
                        """;

                String foundItemsTableSQL = """
                        CREATE TABLE IF NOT EXISTS found_items (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            item_name VARCHAR(100) NOT NULL,
                            description TEXT,
                            date_found DATE,
                            location VARCHAR(255),
                            contact_info VARCHAR(255),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        );
                        """;

                stmt.executeUpdate(usersTableSQL);
                stmt.executeUpdate(lostItemsTableSQL);
                stmt.executeUpdate(foundItemsTableSQL);
                statusLabel.setText("Database initialized successfully.");
                statusLabel.setForeground(new Color(0, 128, 0)); 
            } catch (Exception e) {
                // TODO: handle exception
                statusLabel.setText("Database initialization failed." + e.getMessage());
                statusLabel.setForeground(Color.RED);
                showError(null, "Database initialization failed!\n" + e.getMessage()+ "nPlease check your database settings and ensure the MySQL server is running.");
            }
}
    // Sign up Frame
    static class SignUpFrame extends JFrame {
        private JLabel statusLabel
        public SignUpFrame() {
            setTitle("Sign Up");
            setSize(400, 300);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLocationRelativeTo(null);
            // Add sign-up form components here

            JPanel panel = createFormPanel();

            DBManager.initializeDatabase(statusLabel);

            add(panel);
        }
        private JPanel createFormPanel() {
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(5, 2, 10, 10));

            JLabel usernameLabel = new JLabel("Username:");
            JTextField usernameField = new JTextField();

            JLabel emailLabel = new JLabel("Email:");
            JTextField emailField = new JTextField();

            JLabel passwordLabel = new JLabel("Password:");
            JPasswordField passwordField = new JPasswordField();

            JButton signUpButton = new JButton("Sign Up");
            statusLabel = new JLabel("");

            panel.add(usernameLabel);
            panel.add(usernameField);
            panel.add(emailLabel);
            panel.add(emailField);
            panel.add(passwordLabel);
            panel.add(passwordField);
            panel.add(signUpButton);
            panel.add(statusLabel);

            // Add action listener for sign-up button here
            signUpButton.addActionListener(e -> {
                String username = usernameField.getText();
                String email = emailField.getText();
                String password = new String(passwordField.getPassword());

                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    DBManager.showError(this, "All fields are required!");
                } else {
                   
                    statusLabel.setText("Sign-up successful!");
                    statusLabel.setForeground(new Color(0, 128, 0)); 
                }
            });
            return panel;

    } 
}      
}
