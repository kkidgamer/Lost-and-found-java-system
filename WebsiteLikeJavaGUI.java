import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Main class for the Lost & Found System Desktop Application
 * Uses Java Swing to create a beautiful, modern, website-like interface
 * Features: Responsive navbar, page switching, gradient background, hover effects
 */
public class WebsiteLikeJavaGUI extends JFrame {

    // Panel that holds all the different pages (Home, About, etc.)
    private JPanel cards;

    // Unique identifiers for each page (used by CardLayout)
    private final String HOME = "HOME";
    private final String ABOUT = "ABOUT";
    private final String SERVICES = "SERVICES";
    private final String PORTFOLIO = "PORTFOLIO";
    private final String CONTACT = "CONTACT";

    // We keep a reference to the menu panel so we can highlight the active item
    private JPanel menuPanel;

    /**
     * Constructor - sets up the entire window and layout
     */
    public WebsiteLikeJavaGUI() {
        // Window title and size
        setTitle("Lost & Found System - Campus Edition");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Close app when window closes
        setLocationRelativeTo(null);  // Center window on screen

        // Main container with gradient background
        JPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout());  // Top, bottom, left, right, center layout

        // Create the top navigation bar
        JPanel navbar = createNavbar();
        mainPanel.add(navbar, BorderLayout.NORTH);  // Add navbar to the top

        // Create the area where pages will switch
        cards = new JPanel(new CardLayout());
        cards.setOpaque(false);  // Allow gradient background to show through

        // Add all pages to the card layout
        cards.add(createHomePanel(), HOME);
        cards.add(createAboutPanel(), ABOUT);
        cards.add(createServicesPanel(), SERVICES);
        cards.add(createPortfolioPanel(), PORTFOLIO);
        cards.add(createContactPanel(), CONTACT);

        mainPanel.add(cards, BorderLayout.CENTER);  // Add pages area in the center
        add(mainPanel);  // Add everything to the main window

        // Show the Home page first when app starts
        showPage(HOME);
    }

    /**
     * Creates the top navigation bar with logo and menu items
     */
    private JPanel createNavbar() {
        JPanel navbar = new JPanel();
        navbar.setBackground(new Color(255, 255, 255, 240));  // Semi-transparent white
        navbar.setPreferredSize(new Dimension(1100, 80));
        navbar.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));  // Padding
        navbar.setLayout(new BorderLayout());

        // Logo on the left
        JLabel logo = new JLabel("Lost & Found");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 34));
        logo.setForeground(new Color(102, 126, 234));  // Purple-blue color

        // Panel to hold all menu items (aligned to the right)
        menuPanel = new JPanel();
        menuPanel.setOpaque(false);  // Transparent background
        menuPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 40, 25));  // Spacing between items

        // Define menu items: {Display Text, Card Key}
        String[][] menuData = {
            {"Home",      HOME},
            {"About",     ABOUT},
            {"Services",  SERVICES},
            {"Portfolio", PORTFOLIO},
            {"Contact",   CONTACT}
        };

        // Create each menu item
        for (String[] item : menuData) {
            String displayText = item[0];   // What user sees
            String cardKey = item[1];       // Internal key for page switching

            JLabel menuItem = new JLabel(displayText);
            menuItem.setFont(new Font("Segoe UI", Font.PLAIN, 19));
            menuItem.setForeground(Color.DARK_GRAY);
            menuItem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));  // Hand cursor on hover

            // Mouse hover effects
            menuItem.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    // Only change color if not already active
                    if (!menuItem.getForeground().equals(new Color(102, 126, 234))) {
                        menuItem.setForeground(new Color(102, 126, 234));
                        menuItem.setFont(new Font("Segoe UI", Font.BOLD, 19));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    // Only revert if not active
                    if (!menuItem.getForeground().equals(new Color(102, 126, 234))) {
                        menuItem.setForeground(Color.DARK_GRAY);
                        menuItem.setFont(new Font("Segoe UI", Font.PLAIN, 19));
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    showPage(cardKey);  // Switch to the correct page
                }
            });

            menuPanel.add(menuItem);  // Add this menu item to the navbar
        }

        // Add logo and menu to navbar
        navbar.add(logo, BorderLayout.WEST);
        navbar.add(menuPanel, BorderLayout.CENTER);

        return navbar;
    }

    /**
     * Switches to a specific page and highlights the active menu item
     */
    private void showPage(String cardName) {
        CardLayout cardLayout = (CardLayout) cards.getLayout();
        cardLayout.show(cards, cardName);  // This changes the visible page

        // Update menu highlighting
        for (Component comp : menuPanel.getComponents()) {
            if (comp instanceof JLabel label) {
                String labelText = label.getText();
                if (getCardKey(labelText).equals(cardName)) {
                    // This is the active page → make it bold and colored
                    label.setForeground(new Color(102, 126, 234));
                    label.setFont(new Font("Segoe UI", Font.BOLD, 19));
                } else {
                    // Inactive pages → normal style
                    label.setForeground(Color.DARK_GRAY);
                    label.setFont(new Font("Segoe UI", Font.PLAIN, 19));
                }
            }
        }
    }

    /**
     * Converts menu label text ("Home") to card key ("HOME")
     */
    private String getCardKey(String labelText) {
        return switch (labelText) {
            case "Home"      -> HOME;
            case "About"     -> ABOUT;
            case "Services"  -> SERVICES;
            case "Portfolio" -> PORTFOLIO;
            case "Contact"   -> CONTACT;
            default          -> HOME;
        };
    }

    // ==================== PAGE CREATION METHODS ====================

    private JPanel createHomePanel() {
        return createPage("Welcome to Lost & Found System",
            "Report lost items • Browse found items • Get reunited fast!<br><br>" +
            "<b style='font-size:28px'>Helping students recover lost items since 2025</b>");
    }

    private JPanel createAboutPanel() {
        return createPage("About Us",
            "A student-built system designed to help you recover lost belongings quickly.<br>" +
            "From wallets to laptops — we've got your back!");
    }

    private JPanel createServicesPanel() {
        return createPage("Our Services",
            "• Report Lost Items with photos & details<br>" +
            "• Browse Found Items by category & location<br>" +
            "• Secure messaging between finder and owner<br>" +
            "• Location-based search (dorm, library, cafe)<br>" +
            "• Admin verification for high-value items");
    }

    private JPanel createPortfolioPanel() {
        return createPage("Success Stories",
            "500+ items returned this year!<br>" +
            "• Lost AirPods found in 2 hours<br>" +
            "• Wallet returned with all cash intact<br>" +
            "• Laptop recovered from lecture hall<br>" +
            "98% satisfaction rate • Active on 10+ campuses");
    }

    private JPanel createContactPanel() {
        return createPage("Contact Us",
            "Need help? We're here 24/7<br><br>" +
            "Email: support@lostfound.com<br>" +
            "Phone: +1 (555) 123-4567<br>" +
            "Instagram: @campuslostfound<br>" +
            "Report issues or give feedback anytime!");
    }

    /**
     * Helper method to create a consistent page layout
     */
    private JPanel createPage(String title, String content) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);  // Transparent so gradient shows

        // HTML allows multi-line text and styling
        JLabel label = new JLabel("<html><div style='text-align:center;color:white;max-width:900px'>" +
            "<h1 style='font-size:50px;margin:20px 0'>" + title + "</h1>" +
            "<p style='font-size:26px;line-height:1.8;opacity:0.95'>" + content + "</p>" +
            "</div></html>");

        panel.add(label);
        return panel;
    }

    /**
     * Custom JPanel with beautiful gradient background
     */
    class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // Create smooth gradient from top-left to bottom-right
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(102, 126, 234),      // Start color (purple-blue)
                getWidth(), getHeight(), new Color(118, 75, 162)  // End color (darker purple)
            );

            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());  // Fill entire panel
        }
    }

    /**
     * MAIN METHOD - Entry point of the application
     */
    public static void main(String[] args) {
        // Always use this for Swing apps to avoid threading issues
        SwingUtilities.invokeLater(() -> {
            try {
                // Make it look like Windows/macOS app (not old Java look)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();  // Print error if theme fails
            }

            // Create and show the window
            new WebsiteLikeJavaGUI().setVisible(true);
        });
    }
}