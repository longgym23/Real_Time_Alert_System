package Alert;

import java.util.Arrays;  // Cho copyOfRange trong addToHistory
import java.io.IOException;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.event.*;  // Cho DocumentListener và DocumentEvent
import javax.swing.text.BadLocationException;  // Cho doc.remove() trong log()

// Cho setEmojiFont và UI
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("unused")  // Suppress warning cho field như mongoDBManager nếu không dùng trực tiếp
public class AlertServerGUI extends JFrame {
    private AlertServer server;
    private JTextField cityField;
    private JTextField intervalField;
    private JTextField customMessageField;
    private JComboBox<String> alertTypeCombo;
    private JComboBox<String> severityCombo;
    private JComboBox<Integer> portCombo;
    private JTextPane logPane;
    private JTextPane historyPane;  // Thay đổi từ JTextArea thành JTextPane để hỗ trợ icon HTML
    private JButton startButton, stopButton, sendManualButton, sendToAllButton, clearLogButton;
    private JProgressBar progressBar;
    private Thread serverThread;
    private String lastApiData;
    private MongoDBManager mongoDBManager;
    private int loadedHistorySize = 0;  // Lưu size history để dùng trong done()

    public AlertServerGUI() {
        super("Weather Alert Server (OpenWeather API)");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            // Tùy chỉnh theme
            UIManager.put("nimbusBlueGrey", new Color(104, 153, 199));
            UIManager.put("control", new Color(240, 245, 250));
        } catch (Exception e) {
            System.err.println("Cannot set Nimbus Look and Feel: " + e.getMessage());
        }

        mongoDBManager = new MongoDBManager();
        initUI();
        loadHistoryFromMongoDB(); // Tải history với progress
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 850);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);

        // Background gradient panel
        JPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Title
        JLabel titleLabel = new JLabel("Weather Alert Server", SwingConstants.CENTER);  // Bỏ emoji để tránh lỗi
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(50, 70, 100));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        // Input Panel
        JPanel inputPanel = createStyledPanel("Thông Tin Đầu Vào", new Color(230, 245, 255));
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints inputGbc = new GridBagConstraints();
        inputGbc.insets = new Insets(5, 5, 5, 5);
        inputGbc.fill = GridBagConstraints.HORIZONTAL;

        // City
        inputGbc.gridx = 0; inputGbc.gridy = 0; inputGbc.gridwidth = 1;
        inputPanel.add(createLabelWithIcon("Thành phố (e.g., Hanoi,vn):", "icons/location.png"), inputGbc);
        inputGbc.gridx = 1; inputGbc.gridy = 0;
        cityField = createStyledTextField("Hanoi,vn");
        cityField.setToolTipText("Nhập tên thành phố theo định dạng OpenWeather API");
        inputPanel.add(cityField, inputGbc);

        // Interval
        inputGbc.gridx = 0; inputGbc.gridy = 1;
        inputPanel.add(createLabelWithIcon("Khoảng thời gian (ms):", "icons/clock.png"), inputGbc);
        inputGbc.gridx = 1; inputGbc.gridy = 1;
        intervalField = createStyledTextField("300000");
        intervalField.setName("interval");
        intervalField.setToolTipText("Thời gian giữa các lần fetch API (mặc định 5 phút)");
        inputPanel.add(intervalField, inputGbc);

        // Custom Message
        inputGbc.gridx = 0; inputGbc.gridy = 2;
        inputPanel.add(createLabelWithIcon("Nội dung tùy chỉnh:", "icons/chat.png"), inputGbc);
        inputGbc.gridx = 1; inputGbc.gridy = 2;
        customMessageField = createStyledTextField("");
        customMessageField.setToolTipText("Tin nhắn tùy chỉnh cho alert (tùy chọn)");
        inputPanel.add(customMessageField, inputGbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        mainPanel.add(inputPanel, gbc);

        // Options Panel
        JPanel optionsPanel = createStyledPanel("Cài Đặt Cảnh Báo", new Color(230, 250, 230));
        optionsPanel.setLayout(new GridBagLayout());
        GridBagConstraints optGbc = new GridBagConstraints();
        optGbc.insets = new Insets(5, 5, 5, 5);
        optGbc.fill = GridBagConstraints.HORIZONTAL;

        // Alert Type
        optGbc.gridx = 0; optGbc.gridy = 0;
        optionsPanel.add(createLabelWithIcon("Loại cảnh báo:", "icons/gear.png"), optGbc);
        optGbc.gridx = 1; optGbc.gridy = 0;
        alertTypeCombo = new JComboBox<>(new String[]{"Tự động", "Mưa", "Bão", "Nóng", "Gió", "Trời trong"});
        styleComboBox(alertTypeCombo);
        alertTypeCombo.setToolTipText("Chọn loại alert tự động hoặc thủ công");
        optionsPanel.add(alertTypeCombo, optGbc);

        // Severity
        optGbc.gridx = 0; optGbc.gridy = 1;
        optionsPanel.add(createLabelWithIcon("Mức độ:", "icons/level.png"), optGbc);
        optGbc.gridx = 1; optGbc.gridy = 1;
        severityCombo = new JComboBox<>(new String[]{"Nhẹ", "Trung bình", "Nghiêm trọng"});
        styleComboBox(severityCombo);
        severityCombo.setToolTipText("Mức độ nghiêm trọng của cảnh báo");
        optionsPanel.add(severityCombo, optGbc);

        // Port
        optGbc.gridx = 0; optGbc.gridy = 2;
        optionsPanel.add(createLabelWithIcon("Cổng gửi:", "icons/port.png"), optGbc);
        optGbc.gridx = 1; optGbc.gridy = 2;
        portCombo = new JComboBox<>(new Integer[]{4446, 4447, 4448});
        styleComboBox(portCombo);
        portCombo.setToolTipText("Chọn cổng để gửi đến client tương ứng");
        optionsPanel.add(portCombo, optGbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        mainPanel.add(optionsPanel, gbc);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(245, 250, 255));

        startButton = createStyledButton("Khởi Động Server", new Color(46, 125, 50), Color.WHITE, "icons/play.png");
        stopButton = createStyledButton("Dừng Server", new Color(183, 28, 28), Color.WHITE, "icons/stop.png");
        stopButton.setEnabled(false);
        sendManualButton = createStyledButton("Gửi Thủ Công", new Color(33, 150, 243), Color.WHITE, "icons/send.png");
        sendToAllButton = createStyledButton("Gửi Tất Cả Client", new Color(255, 193, 7), Color.WHITE, "icons/broadcast.png");
        clearLogButton = createStyledButton("Xóa Log", new Color(117, 117, 117), Color.WHITE, "icons/trash.png");

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(sendManualButton);
        buttonPanel.add(sendToAllButton);
        buttonPanel.add(clearLogButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        mainPanel.add(buttonPanel, gbc);

        // Log & History
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        mainPanel.add(progressBar, gbc);

        logPane = createStyledTextPane();
        JScrollPane logScrollPane = new JScrollPane(logPane);
        logScrollPane.setBorder(createTitledBorder("Nhật Ký Cảnh Báo", new Color(100, 150, 200)));

        historyPane = createStyledTextPane();  // Thay đổi từ createStyledTextArea thành createStyledTextPane
        JScrollPane historyScrollPane = new JScrollPane(historyPane);
        historyScrollPane.setBorder(createTitledBorder("Lịch Sử Gửi Cảnh Báo", new Color(100, 200, 100)));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logScrollPane, historyScrollPane);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(splitPane, gbc);

        add(mainPanel);

        // Event Listeners
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        sendManualButton.addActionListener(e -> sendManualAlert());
        sendToAllButton.addActionListener(e -> sendToAllClients());
        clearLogButton.addActionListener(e -> clearLogAndHistory());

        // Validation listeners
        cityField.getDocument().addDocumentListener(new ValidationListener(cityField, "Thành phố không được rỗng!"));
        intervalField.getDocument().addDocumentListener(new ValidationListener(intervalField, "Phải là số dương!"));

        // Auto-focus
        SwingUtilities.invokeLater(() -> cityField.requestFocus());
    }

    // ValidationListener (giữ nguyên)
    private static class ValidationListener implements DocumentListener {
        private final JTextField field;
        private final String errorMsg;

        public ValidationListener(JTextField field, String errorMsg) {
            this.field = field;
            this.errorMsg = errorMsg;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            validateField();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            validateField();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            validateField();
        }

        private void validateField() {
            String text = field.getText().trim();
            if (field.getName() != null && field.getName().equals("interval")) {
                try {
                    if (!text.isEmpty() && Integer.parseInt(text) <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (Exception ex) {
                    showError(field, errorMsg);
                    return;
                }
            } else if (text.isEmpty()) {
                showError(field, errorMsg);
                return;
            }
            // Valid: Green border
            field.setBorder(BorderFactory.createLineBorder(new Color(100, 200, 100), 1));
        }

        private void showError(JComponent comp, String msg) {
            comp.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
            comp.setToolTipText(msg);
        }
    }

    // Helper methods
    private JPanel createStyledPanel(String title, Color bgColor) {
        JPanel panel = new JPanel();
        panel.setBackground(bgColor);
        panel.setBorder(createTitledBorder(title, Color.DARK_GRAY));
        return panel;
    }

    private Border createTitledBorder(String title, Color fgColor) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150), 1),
            title, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 16), fgColor
        );
    }

    // createLabelWithIcon với iconPath
    private JLabel createLabelWithIcon(String text, String iconPath) {
        JLabel label = new JLabel(text);
        if (iconPath != null) {
            ImageIcon icon = loadIcon(iconPath);
            if (icon != null) {
                label.setIcon(icon);
            }
        }
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(new Color(60, 60, 60));
        return label;
    }

    private JTextField createStyledTextField(String text) {
        JTextField field = new JTextField(text, 15);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        return field;
    }

    private JComboBox<?> styleComboBox(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        return combo;
    }

    // createStyledButton với iconPath
    private JButton createStyledButton(String text, Color bgColor, Color fgColor, String iconPath) {
        JButton button = new JButton(text);
        if (iconPath != null) {
            ImageIcon icon = loadIcon(iconPath);
            if (icon != null) {
                button.setIcon(icon);
            }
        }
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                button.setBackground(bgColor.brighter()); 
            }
            public void mouseExited(MouseEvent e) { 
                button.setBackground(bgColor); 
            }
        });
        return button;
    }

    private JTextPane createStyledTextPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pane.setContentType("text/html");
        pane.setBackground(new Color(248, 250, 252));
        return pane;
    }

    // Bỏ createStyledTextArea vì giờ dùng JTextPane cho history

    private static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            GradientPaint gp = new GradientPaint(0, 0, new Color(200, 220, 255), 0, getHeight(), new Color(180, 200, 255));
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // loadIcon
    private ImageIcon loadIcon(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url == null) {
                url = new java.io.File(path).toURI().toURL();
            }
            ImageIcon icon = new ImageIcon(url);
            Image img = icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            System.err.println("Lỗi load icon " + path + ": " + e.getMessage());
            return null;
        }
    }

    // Các method khác (startServer, etc.)
    private void startServer() {
        String city = cityField.getText().trim();
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập thành phố!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            long interval = Long.parseLong(intervalField.getText());
            if (interval <= 0) throw new NumberFormatException();
            String type = (String) alertTypeCombo.getSelectedItem();
            String severity = (String) severityCombo.getSelectedItem();
            int port = (Integer) portCombo.getSelectedItem();
            server = new AlertServer(city, this, type, severity, customMessageField.getText(), interval, port);
            serverThread = new Thread(() -> {
                try { server.start(); } catch (Exception e) { log("Lỗi khởi động server: " + e.getMessage(), "error"); }
            });
            serverThread.start();
            log("Server khởi động cho " + city + " (Interval: " + interval + "ms)", "info");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            sendManualButton.setEnabled(true);
            sendToAllButton.setEnabled(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (server != null) server.stop();
        if (serverThread != null) serverThread.interrupt();
        log("Server đã dừng", "info");
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        sendManualButton.setEnabled(false);
        sendToAllButton.setEnabled(false);
    }

    private void sendManualAlert() {
        if (server != null) {
            String alertTypeEn = translateAlertType((String) alertTypeCombo.getSelectedItem());
            String severityEn = translateSeverity((String) severityCombo.getSelectedItem());
            int selectedPort = (Integer) portCombo.getSelectedItem();
            String customMessage = customMessageField.getText().trim();
            if (customMessage.isEmpty() && lastApiData == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập nội dung hoặc có dữ liệu API!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!customMessage.isEmpty()) {
                server.sendManualAlert(customMessage, alertTypeEn, severityEn, selectedPort);
                addToHistory("Gửi thủ công: " + customMessage, alertTypeEn, severityEn);
            } else {
                server.sendManualAlertWithLastData(lastApiData, alertTypeEn, severityEn, selectedPort);
                addToHistory("Gửi thủ công với API: " + lastApiData, alertTypeEn, severityEn);
            }
            log("Đã gửi alert thủ công đến cổng " + selectedPort, alertTypeEn.toLowerCase());
        }
    }

    private void sendToAllClients() {
        if (server != null) {
            String alertTypeEn = translateAlertType((String) alertTypeCombo.getSelectedItem());
            String severityEn = translateSeverity((String) severityCombo.getSelectedItem());
            String customMessage = customMessageField.getText().trim();
            if (customMessage.isEmpty() && lastApiData == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập nội dung hoặc có dữ liệu API!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!customMessage.isEmpty()) {
                server.sendManualAlertToAllPorts(customMessage, alertTypeEn, severityEn);
            } else {
                server.sendManualAlertToAllPorts(lastApiData, alertTypeEn, severityEn);
            }
            log("Đã gửi alert đến tất cả client (4446-4448)", alertTypeEn.toLowerCase());
            addToHistory("Gửi đến tất cả: " + (customMessage.isEmpty() ? "API data" : customMessage), alertTypeEn, severityEn);
        }
    }

    private void clearLogAndHistory() {
        logPane.setText("");
        historyPane.setText("");  // Cập nhật cho historyPane
        log("Đã xóa log và lịch sử", "info");
    }

    private void loadHistoryFromMongoDB() {
        progressBar.setVisible(true);
        progressBar.setValue(0);
        progressBar.setString("Đang tải lịch sử...");
        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                List<String> history = mongoDBManager.getAlertHistory(50);
                loadedHistorySize = history.size();
                for (int i = 0; i < loadedHistorySize; i++) {
                    publish(i * 100 / loadedHistorySize);
                    String entry = history.get(i);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            String[] parts = entry.split(" ", 2);
                            if (parts.length >= 2) {
                                String timestamp = parts[0].substring(1, parts[0].length() - 1);
                                String rest = parts[1];
                                int openParen = rest.indexOf("(");
                                String message = (openParen > 0) ? rest.substring(0, openParen).trim() : rest;
                                String typeAndSeverity = (openParen > 0) ? rest.substring(openParen + 1, rest.length() - 1) : "";
                                String type = "";
                                String severity = "";
                                if (!typeAndSeverity.isEmpty()) {
                                    String[] tsParts = typeAndSeverity.split(",");
                                    if (tsParts.length >= 2) {
                                        type = tsParts[0].trim();
                                        severity = tsParts[1].trim();
                                    }
                                }
                                addToHistory(message, type, severity);
                            } else {
                                // Fallback: append plain text nếu parse lỗi
                                HTMLDocument doc = (HTMLDocument) historyPane.getDocument();
                                try {
                                    HTMLEditorKit kit = new HTMLEditorKit();
                                    kit.insertHTML(doc, doc.getLength(), entry + "<br>", 0, 0, null);
                                } catch (Exception ex) {
                                    historyPane.setText(historyPane.getText() + entry + "\n");
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("Lỗi parse history entry: " + entry + " - " + ex.getMessage());
                            // Fallback: append plain text
                            HTMLDocument doc = (HTMLDocument) historyPane.getDocument();
                            try {
                                HTMLEditorKit kit = new HTMLEditorKit();
                                kit.insertHTML(doc, doc.getLength(), entry + "<br>", 0, 0, null);
                            } catch (Exception ex2) {
                                historyPane.setText(historyPane.getText() + entry + "\n");
                            }
                        }
                    });
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    progressBar.setValue(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                log("Đã tải " + loadedHistorySize + " lịch sử mới nhất từ MongoDB", "info");
            }
        };
        worker.execute();
    }

    public void log(String message, String type) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> log(message, type));
            return;
        }

        String timestamp = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date());
        String color = "#424242"; // Default
        String iconHtml = "";  // Icon HTML cho log (sử dụng emoji fallback, hoặc img nếu PNG)

     switch (type.toLowerCase()) {
    case "rain": color = "#0288D1"; iconHtml = "<img src='file:" + new java.io.File("icons/rain.png").getAbsolutePath() + "' width='16' height='16' alt='🌧️'> "; break;
    case "storm": color = "#7B1FA2"; iconHtml = "<img src='file:" + new java.io.File("icons/storm.png").getAbsolutePath() + "' width='16' height='16' alt='⛈️'> "; break;
    case "heat": color = "#D32F2F"; iconHtml = "<img src='file:" + new java.io.File("icons/sun.png").getAbsolutePath() + "' width='16' height='16' alt='☀️'> "; break;
    case "wind": color = "#00ACC1"; iconHtml = "<img src='file:" + new java.io.File("icons/wind.png").getAbsolutePath() + "' width='16' height='16' alt='🌬️'> "; break;
    case "clear": color = "#F57C00"; iconHtml = "<img src='file:" + new java.io.File("icons/clear.png").getAbsolutePath() + "' width='16' height='16' alt='🌤️'> "; break;
    case "error": color = "#C62828"; iconHtml = "<img src='file:" + new java.io.File("icons/error.png").getAbsolutePath() + "' width='16' height='16' alt='❌'> "; break;
    default: iconHtml = "<img src='file:" + new java.io.File("icons/info.png").getAbsolutePath() + "' width='16' height='16' alt='ℹ️'> "; break;
}

        String htmlMessage = String.format(
            "<p style='color:%s; font-family: Segoe UI, Arial; font-size: 13px;'>[%s] %s %s</p>",
            color, timestamp, iconHtml, message
        );

        HTMLDocument doc = (HTMLDocument) logPane.getDocument();
        try {
            HTMLEditorKit kit = new HTMLEditorKit();
            kit.insertHTML(doc, doc.getLength(), htmlMessage, 0, 0, null);
            logPane.setCaretPosition(doc.getLength());
            if (doc.getLength() > 50000) {
                try {
                    doc.remove(0, 20000);
                } catch (BadLocationException ex) {
                    // Ignore
                }
            }
        } catch (Exception e) {
            System.err.println("Error appending to log: " + e.getMessage());
        }
    }

    public void addToHistory(String message, String type, String severity) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date());
            String iconHtml = getIconHtmlForType(type);  // Lấy icon HTML từ file icons
            String color = getColorForType(type);  // Lấy màu cho type
            String htmlMessage = String.format(
                "<p style='color:%s; font-family: Segoe UI, Arial; font-size: 13px;'>%s - %s %s (Type: %s, Severity: %s)</p>",
                color, timestamp, iconHtml, message, type, severity
            );
            
            HTMLDocument doc = (HTMLDocument) historyPane.getDocument();
            try {
                HTMLEditorKit kit = new HTMLEditorKit();
                kit.insertHTML(doc, doc.getLength(), htmlMessage, 0, 0, null);
                historyPane.setCaretPosition(doc.getLength());
                
                // Giới hạn độ dài (tương tự log, dựa trên length)
                if (doc.getLength() > 50000) {
                    try {
                        doc.remove(0, 20000);
                    } catch (BadLocationException ex) {
                        // Ignore
                    }
                }
            } catch (Exception e) {
                System.err.println("Error appending to history: " + e.getMessage());
            }
            
            if (mongoDBManager != null) {
                mongoDBManager.saveAlert(message, type, severity, timestamp);
            }
        });
    }

    // THÊM: Get icon HTML for history (từ file icons với alt emoji fallback)
    private String getIconHtmlForType(String type) {
        String iconPath = "";
        switch (type.toLowerCase()) {
            case "rain": iconPath = "icons/rain.png"; break;
            case "storm": iconPath = "icons/storm.png"; break;
            case "heat": iconPath = "icons/sun.png"; break;
            case "wind": iconPath = "icons/wind.png"; break;
            case "clear": iconPath = "icons/clear.png"; break;
            default: iconPath = "icons/info.png"; break;
        }
        return "<img src='file:" + new java.io.File(iconPath).getAbsolutePath() + "' width='16' height='16' alt='" + getAltEmojiForType(type) + "'> ";
    }

    // THÊM: Get alt emoji for type (fallback)
    private String getAltEmojiForType(String type) {
        switch (type.toLowerCase()) {
            case "rain": return "🌧️";
            case "storm": return "⛈️";
            case "heat": return "☀️";
            case "wind": return "🌬️";
            case "clear": return "🌤️";
            default: return "ℹ️";
        }
    }

    // THÊM: Get color for type (tương tự log)
    private String getColorForType(String type) {
        switch (type.toLowerCase()) {
            case "rain": return "#0288D1";
            case "storm": return "#7B1FA2";
            case "heat": return "#D32F2F";
            case "wind": return "#00ACC1";
            case "clear": return "#F57C00";
            default: return "#424242";
        }
    }

    private String translateAlertType(String vn) {
        switch (vn) {
            case "Tự động": return "Auto";
            case "Mưa": return "Rain";
            case "Bão": return "Storm";
            case "Nóng": return "Heat";
            case "Gió": return "Wind";
            case "Trời trong": return "Clear";
            default: return "Auto";
        }
    }

    private String translateSeverity(String vn) {
        switch (vn) {
            case "Nhẹ": return "Minor";
            case "Trung bình": return "Moderate";
            case "Nghiêm trọng": return "Severe";
            default: return "Minor";
        }
    }

    public void setLastApiData(String data) {
        this.lastApiData = data;
    }

    public static void main(String[] args) {
        // Không gọi setVisible(true) ở đây nữa, sẽ được gọi từ LoginGUI
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        // Main của AlertServerGUI không còn là entry point chính
    }
}