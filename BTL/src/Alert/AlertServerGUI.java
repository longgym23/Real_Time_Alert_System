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
    private JTextPane logPane;
    private JButton startButton;
    private JButton stopButton;
    private Thread serverThread;

    public AlertServerGUI() {
        super("Weather Alert Server (OpenWeather API)");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            System.err.println("Cannot set Nimbus Look and Feel: " + e.getMessage());
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Input panel
        JPanel inputPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.setBackground(new Color(240, 240, 240));
        inputPanel.add(new JLabel("City (e.g., Hanoi,vn):"));
        cityField = new JTextField(20);
        cityField.setFont(new Font("Arial", Font.PLAIN, 14));
        inputPanel.add(cityField);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(240, 240, 240));
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        stopButton.setFont(new Font("Arial", Font.BOLD, 14));
        stopButton.setEnabled(false);
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        // Log area
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(new Font("Arial", Font.PLAIN, 14));
        logPane.setContentType("text/html");
        logPane.setText("<html><body style='font-family: Arial; font-size: 14px;'></body></html>");
        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Alert Log"));

        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Event listeners
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
    }

    private void startServer() {
        String city = cityField.getText().trim();
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập City!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String apiKey = Config.get("WEATHER_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "API Key không tồn tại trong config.properties!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        serverThread = new Thread(() -> {
            try {
                server = new AlertServer(city, this);
                server.start();
            } catch (Exception ex) {
                log("Lỗi khởi động server: 🌡️ " + ex.getMessage(), "error");
            }
        });
        serverThread.start();

        log("Server khởi động cho 🌍 " + city, "info");
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
    }

    private void stopServer() {
        try {
            if (serverThread != null) {
                serverThread.interrupt();
            }
            log("Server dừng ⏹️", "info");
        } catch (Exception e) {
            log("Lỗi dừng server: 🌡️ " + e.getMessage(), "error");
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
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
                color = "#0000FF"; // Xanh dương
                icon = "🌧️";
                break;
            case "storm":
                color = "#800080"; // Tím
                icon = "⛈️";
                break;
            case "heat":
                color = "#FF0000"; // Đỏ
                icon = "☀️";
                break;
            case "wind":
                color = "#00FFFF"; // Cyan
                icon = "🌬️";
                break;
            case "clear":
                color = "#FFA500"; // Vàng cam
                icon = "🌤️";
                break;
            case "error":
                color = "#FF0000"; // Đỏ cho lỗi
                icon = "❌";
                break;
            default:
                color = "#000000"; // Đen cho info
                icon = "";
                break;
        }

        String htmlMessage = String.format(
            "<div style='color:%s'>[%s] %s %s</div>",
            color, timestamp, icon, message
        );

        HTMLDocument doc = (HTMLDocument) logPane.getDocument();
        try {
            HTMLEditorKit kit = new HTMLEditorKit();
            kit.insertHTML(doc, doc.getLength(), htmlMessage, 0, 0, null);
            logPane.setCaretPosition(doc.getLength()); // Cuộn xuống cuối
        } catch (BadLocationException | IOException e) {
            System.err.println("Error appending to log: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AlertServerGUI().setVisible(true);
        });
    }
}