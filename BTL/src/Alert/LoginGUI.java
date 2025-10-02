package Alert;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.JTextComponent;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.function.Supplier;

public class LoginGUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private MongoDBManager mongoDBManager;

    public LoginGUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("nimbusBlueGrey", new Color(104, 153, 199));
            UIManager.put("control", new Color(240, 245, 250));
        } catch (Exception e) {
            System.err.println("Cannot set Nimbus Look and Feel: " + e.getMessage());
        }
        mongoDBManager = new MongoDBManager();
        initUI();
    }

    private void initUI() {
        setTitle("Weather Alert");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(400, 500)); // Minimum size to prevent too small window
        pack(); // Use pack to size based on content
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Background Panel with image
        JPanel backgroundPanel = new BackgroundPanel();
        setContentPane(backgroundPanel);

        // Centered form panel on background
        JPanel formPanel = createFormPanel();
        formPanel.setOpaque(false);
        backgroundPanel.add(formPanel, BorderLayout.CENTER);

        // Revalidate and repaint
        revalidate();
        repaint();
    }

    private JPanel createFormPanel() {
        // Initialize fields first to avoid NPE in supplier
        usernameField = new JTextField();
        passwordField = new JPasswordField();

        JPanel mainForm = new JPanel(new GridBagLayout());
        mainForm.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1.0; // Allow horizontal expansion
        gbc.weighty = 1.0; // Allow vertical expansion

        // Outer card panel for the entire form (rounded, shadow-like)
        JPanel cardPanel = new RoundedPanel(20); // Rounded corners
        cardPanel.setBackground(new Color(0, 0, 0, 200)); // Dark semi-transparent
        cardPanel.setLayout(new GridBagLayout());
        GridBagConstraints cardGbc = new GridBagConstraints();
        cardGbc.insets = new Insets(20, 20, 20, 20);
        cardGbc.fill = GridBagConstraints.HORIZONTAL;
        cardGbc.weightx = 1.0;

        // Title
        JLabel titleLabel = new JLabel("Weather Alert", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        cardGbc.gridx = 0; cardGbc.gridy = 0;
        cardPanel.add(titleLabel, cardGbc);

        // Username input (floating label style)
        JPanel userPanel = createModernInputPanel("Tên đăng nhập:", () -> usernameField);
        cardGbc.gridy = 1;
        cardPanel.add(userPanel, cardGbc);

        // Password input
        JPanel passPanel = createModernInputPanel("Mật khẩu:", () -> passwordField);
        cardGbc.gridy = 2;
        cardPanel.add(passPanel, cardGbc);

        // Buttons panel with styled login and register buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);

        // Login button (blue, rounded)
        JButton loginButton = new RoundedButton("Đăng Nhập");
        loginButton.setFont(new Font("Arial", Font.BOLD, 16));
        loginButton.setBackground(new Color(0, 123, 255)); // Blue
        loginButton.setForeground(Color.WHITE);
        loginButton.setPreferredSize(new Dimension(120, 50));
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.addActionListener(e -> handleLogin());
        loginButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(new Color(0, 123, 255).brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(new Color(0, 123, 255));
            }
        });
        buttonPanel.add(loginButton);

        // Register button (green, rounded)
        JButton registerButton = new RoundedButton("Đăng Ký");
        registerButton.setFont(new Font("Arial", Font.BOLD, 16));
        registerButton.setBackground(new Color(40, 167, 69)); // Green
        registerButton.setForeground(Color.WHITE);
        registerButton.setPreferredSize(new Dimension(120, 50));
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        registerButton.addActionListener(e -> handleRegister());
        registerButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                registerButton.setBackground(new Color(40, 167, 69).brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                registerButton.setBackground(new Color(40, 167, 69));
            }
        });
        buttonPanel.add(registerButton);

        cardGbc.gridy = 3;
        cardPanel.add(buttonPanel, cardGbc);

        // Add card to main form
        gbc.gridx = 0; gbc.gridy = 0;
        mainForm.add(cardPanel, gbc);

        return mainForm;
    }

    private JPanel createModernInputPanel(String labelText, Supplier<JComponent> fieldSupplier) {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setOpaque(false);
        inputPanel.setPreferredSize(new Dimension(300, 70));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(new Color(255, 255, 255, 200)); // Semi-transparent white
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        JComponent field = fieldSupplier.get();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        field.setPreferredSize(new Dimension(250, 40));
        field.setForeground(Color.WHITE);
        field.setBackground(new Color(0, 0, 0, 150)); // Semi-transparent dark

        // Cast to JTextComponent for setCaretColor
        if (field instanceof JTextComponent textComp) {
            textComp.setCaretColor(Color.CYAN); // Cyan caret for focus
        }

        // Focus listener for floating label effect (simple color change)
        field.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                label.setForeground(Color.CYAN);
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.CYAN, 2),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                label.setForeground(new Color(255, 255, 255, 200));
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 1),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
        });

        inputPanel.add(label, BorderLayout.NORTH);
        inputPanel.add(field, BorderLayout.CENTER);

        return inputPanel;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (mongoDBManager.validateUser(username, password)) {
            JOptionPane.showMessageDialog(this, "Đăng nhập thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new AlertServerGUI().setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Tên đăng nhập hoặc mật khẩu không đúng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (mongoDBManager.saveUser(username, password)) {
            JOptionPane.showMessageDialog(this, "Đăng ký thành công! Vui lòng đăng nhập.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            usernameField.setText(username);
            passwordField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Tên đăng nhập đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Rounded Button for login and register buttons
    private class RoundedButton extends JButton {
        private static final int ARC_WIDTH = 25;
        private static final int ARC_HEIGHT = 25;

        public RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ButtonModel model = getModel();
            if (model.isArmed()) {
                g2.setColor(getBackground().darker());
            } else {
                g2.setColor(getBackground());
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC_WIDTH, ARC_HEIGHT);
            super.paintComponent(g);
            g2.dispose();
        }
    }

    // Rounded Panel for card effect
    private class RoundedPanel extends JPanel {
        private int radius;

        public RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
        }
    }

    // Background Panel
    private class BackgroundPanel extends JPanel {
        private Image backgroundImage;

        public BackgroundPanel() {
            setLayout(new BorderLayout());
            loadBackgroundImage();
        }

        private void loadBackgroundImage() {
            ImageIcon icon = loadIcon("icons/background.jpg");
            if (icon != null) {
                backgroundImage = icon.getImage();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                // Fallback gradient
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(230, 245, 255), 0, getHeight(), new Color(210, 230, 245));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }

    // Load Icon method
    private ImageIcon loadIcon(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url == null) {
                url = new File(path).toURI().toURL();
            }
            ImageIcon icon = new ImageIcon(url);
            return icon;
        } catch (Exception e) {
            System.err.println("Lỗi load icon " + path + ": " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginGUI().setVisible(true);
        });
    }
}