package Alert;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AlertServerGUI extends JFrame {
    private AlertServer server;
    private JTextField cityField;
    private JTextField intervalField;
    private JTextField customMessageField;
    private JComboBox<String> alertTypeCombo;
    private JComboBox<String> severityCombo;
    private JComboBox<Integer> portCombo;
    private JTextPane logPane;
    private JTextArea historyArea;
    private JButton startButton;
    private JButton stopButton;
    private JButton sendManualButton;
    private JButton sendToAllButton; // Nút mới để gửi đến tất cả client
    private Thread serverThread;
    private String lastApiData;

    public AlertServerGUI() {
        super("Weather Alert Server (OpenWeather API)");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            System.err.println("Cannot set Nimbus Look and Feel: " + e.getMessage());
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Input panel
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.setBackground(new Color(240, 240, 240));
        inputPanel.add(new JLabel("Thành phố (e.g., Hanoi,vn):"));
        cityField = new JTextField(20);
        cityField.setFont(new Font("Arial", Font.PLAIN, 14));
        inputPanel.add(cityField);

        inputPanel.add(new JLabel("Khoảng thời gian (ms):"));
        intervalField = new JTextField("300000", 20); // Mặc định 5 phút
        intervalField.setFont(new Font("Arial", Font.PLAIN, 14));
        inputPanel.add(intervalField);

        inputPanel.add(new JLabel("Nội dung tùy chỉnh:"));
        customMessageField = new JTextField(20);
        customMessageField.setFont(new Font("Arial", Font.PLAIN, 14));
        inputPanel.add(customMessageField);

        // Alert type and severity panel
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        optionsPanel.setBackground(new Color(240, 240, 240));
        optionsPanel.add(new JLabel("Loại cảnh báo:"));
        alertTypeCombo = new JComboBox<>(new String[]{"Tự động", "Mưa", "Bão", "Nóng", "Gió", "Trời trong"});
        alertTypeCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        alertTypeCombo.setPreferredSize(new Dimension(120, 25));
        optionsPanel.add(alertTypeCombo);

        optionsPanel.add(new JLabel("Mức độ:"));
        severityCombo = new JComboBox<>(new String[]{"Nhẹ", "Trung bình", "Nghiêm trọng"});
        severityCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        severityCombo.setPreferredSize(new Dimension(120, 25));
        optionsPanel.add(severityCombo);

        optionsPanel.add(new JLabel("Cổng gửi:"));
        portCombo = new JComboBox<>(new Integer[]{4446, 4447, 4448});
        portCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        portCombo.setPreferredSize(new Dimension(80, 25));
        optionsPanel.add(portCombo);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(240, 240, 240));
        startButton = new JButton("Khởi động Server");
        stopButton = new JButton("Dừng Server");
        sendManualButton = new JButton("Gửi Cảnh báo Thủ công");
        sendToAllButton = new JButton("Gửi đến Tất cả Client"); // Nút mới
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        stopButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendManualButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendToAllButton.setFont(new Font("Arial", Font.BOLD, 14));
        stopButton.setEnabled(false);
        sendManualButton.setEnabled(false);
        sendToAllButton.setEnabled(false); // Ban đầu vô hiệu hóa
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(sendManualButton);
        buttonPanel.add(sendToAllButton);

        // Log area
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(new Font("Arial", Font.PLAIN, 14));
        logPane.setContentType("text/html");
        logPane.setText("");
        JScrollPane logScrollPane = new JScrollPane(logPane);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Nhật ký Cảnh báo"));

        // History area
        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane historyScrollPane = new JScrollPane(historyArea);
        historyScrollPane.setBorder(BorderFactory.createTitledBorder("Lịch sử Gửi Cảnh báo"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logScrollPane, historyScrollPane);
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.5);

        add(inputPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(optionsPanel, BorderLayout.EAST);
        add(buttonPanel, BorderLayout.SOUTH);

        // Event listeners
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        sendManualButton.addActionListener(e -> sendManualAlert());
        sendToAllButton.addActionListener(e -> sendToAllClients());
    }

    private void startServer() {
        String city = cityField.getText().trim();
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Thành phố!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        long interval;
        try {
            interval = Long.parseLong(intervalField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Khoảng thời gian phải là số!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String apiKey = Config.get("WEATHER_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "API Key không tồn tại trong config.properties!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String alertTypeEn = translateAlertType((String) alertTypeCombo.getSelectedItem());
        String severityEn = translateSeverity((String) severityCombo.getSelectedItem());

        serverThread = new Thread(() -> {
            try {
                server = new AlertServer(city, this, alertTypeEn, severityEn, customMessageField.getText().trim(), interval, (Integer) portCombo.getSelectedItem());
                server.start();
            } catch (Exception ex) {
                log("Lỗi khởi động server: 🌡️ " + ex.getMessage(), "error");
            }
        });
        serverThread.start();

        log("Server khởi động cho 🌍 " + city, "info");
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        sendManualButton.setEnabled(true);
        sendToAllButton.setEnabled(true); // Kích hoạt nút gửi đến tất cả khi server chạy
    }

    private void stopServer() {
        try {
            if (server != null) {
                server.stop();
            }
            if (serverThread != null) {
                serverThread.interrupt();
            }
            log("Server dừng ⏹️", "info");
        } catch (Exception e) {
            log("Lỗi dừng server: 🌡️ " + e.getMessage(), "error");
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        sendManualButton.setEnabled(false);
        sendToAllButton.setEnabled(false); // Vô hiệu hóa khi dừng
    }

    private void sendManualAlert() {
        if (server != null) {
            String alertTypeEn = translateAlertType((String) alertTypeCombo.getSelectedItem());
            String severityEn = translateSeverity((String) severityCombo.getSelectedItem());
            int selectedPort = (Integer) portCombo.getSelectedItem();
            if (lastApiData != null) {
                server.sendManualAlertWithLastData(lastApiData, alertTypeEn, severityEn, selectedPort);
            } else {
                server.sendManualAlert(customMessageField.getText().trim(), alertTypeEn, severityEn, selectedPort);
            }
        }
    }

    private void sendToAllClients() {
        if (server != null) {
            String alertTypeEn = translateAlertType((String) alertTypeCombo.getSelectedItem());
            String severityEn = translateSeverity((String) severityCombo.getSelectedItem());
            if (lastApiData != null) {
                server.sendManualAlertToAllPorts(lastApiData, alertTypeEn, severityEn);
            } else {
                server.sendManualAlertToAllPorts(customMessageField.getText().trim(), alertTypeEn, severityEn);
            }
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

    public void log(String message, String type) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> log(message, type));
            return;
        }

        String timestamp = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date());
        String color;
        String icon;

        switch (type.toLowerCase()) {
            case "rain":
                color = "#0000FF";
                icon = "🌧️";
                break;
            case "storm":
                color = "#800080";
                icon = "⛈️";
                break;
            case "heat":
                color = "#FF0000";
                icon = "☀️";
                break;
            case "wind":
                color = "#00FFFF";
                icon = "🌬️";
                break;
            case "clear":
                color = "#FFA500";
                icon = "🌤️";
                break;
            case "error":
                color = "#FF0000";
                icon = "❌";
                break;
            default:
                color = "#000000";
                icon = "";
                break;
        }

        String htmlMessage = String.format(
            "<p style='color:%s'>[%s] %s %s</p>",
            color, timestamp, icon, message
        );

        HTMLDocument doc = (HTMLDocument) logPane.getDocument();
        try {
            HTMLEditorKit kit = new HTMLEditorKit();
            kit.insertHTML(doc, doc.getLength(), htmlMessage, 0, 0, null);
            logPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException | IOException e) {
            System.err.println("Error appending to log: " + e.getMessage());
        }
    }

    public void addToHistory(String message) {
        SwingUtilities.invokeLater(() -> {
            historyArea.append(message + "\n");
        });
    }

    public void setLastApiData(String data) {
        this.lastApiData = data;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AlertServerGUI().setVisible(true);
        });
    }
}