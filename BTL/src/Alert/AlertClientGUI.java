package Alert;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;
import java.util.List;
import java.io.File;

public class AlertClientGUI extends JFrame {
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    private AtomicBoolean running;
    private PrintWriter logWriter;
    private JTextPane logPane;
    private JButton stopButton;
    private TrayIcon trayIcon;
    private MongoDBManager mongoDBManager; // Thêm MongoDBManager

    public AlertClientGUI(String groupAddress, int port) {
        super("Weather Alert Client");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            System.err.println("Cannot set Nimbus Look and Feel: " + e.getMessage());
        }

        this.group = null;
        this.port = port;
        this.running = new AtomicBoolean(true);
        this.mongoDBManager = new MongoDBManager(); // Khởi tạo MongoDBManager

        // Gradient Background
        getContentPane().setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(240, 248, 255), 0, getHeight(), new Color(230, 240, 245));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Log Area
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setFont(new Font("Arial", Font.PLAIN, 12));
        logPane.setContentType("text/html");
        logPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Nhật ký Cảnh báo", TitledBorder.CENTER, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12), Color.DARK_GRAY));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(new Color(245, 245, 245));
        stopButton = new JButton("Dừng Ứng dụng");
        stopButton.setFont(new Font("Arial", Font.BOLD, 14));
        stopButton.setBackground(new Color(220, 20, 60));
        stopButton.setForeground(Color.WHITE);
        buttonPanel.add(stopButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 450);
        setLocationRelativeTo(null);

        // Log File
        try {
            logWriter = new PrintWriter(new FileWriter("weather_alerts.log", true));
            System.out.println("Log file weather_alerts.log opened successfully.");
        } catch (IOException e) {
            System.err.println("Error opening log file: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Cannot open log file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        // System Tray
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
            trayIcon = new TrayIcon(image, "Weather Alert Client");
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("Error adding system tray icon: " + e.getMessage());
                logToGui("Error adding system tray icon: " + e.getMessage(), "error");
            }
        } else {
            System.err.println("System tray not supported on this platform.");
            logToGui("System tray not supported on this platform", "error");
        }

        // Event Listeners
        stopButton.addActionListener(e -> stopClient());

        // Start Receiving Thread
        new Thread(() -> {
            try {
                group = InetAddress.getByName(groupAddress);
                socket = new MulticastSocket(port);
                socket.joinGroup(group);
                logToGui("Connected to 🌐 " + groupAddress + ":" + port, "info");

                byte[] buffer = new byte[1024];
                while (running.get()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength(), java.nio.charset.StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(received);
                    String type = json.getString("type");
                    String desc = json.getString("description");
                    double temp = json.getDouble("temperature");
                    double wind = json.getDouble("wind");
                    double rain = json.getDouble("precipitation");
                    String location = json.getString("location");
                    String severity = json.optString("severity", "minor");
                    String emojiMessage = String.format("%s tại %s 🌡️ %.1f°C, 🌬️ %.1f km/h, 💧 %.1f mm (Mức độ: %s)", desc, location, temp, wind, rain, severity);
                    String typeEmoji = getEmojiForType(type);

                    if (trayIcon != null) {
                        String notificationTitle = type.equalsIgnoreCase("storm") ? "CẢNH BÁO NGHIÊM TRỌNG" : "Cảnh báo thời tiết";
                        trayIcon.displayMessage(notificationTitle, typeEmoji + " " + emojiMessage, type.equalsIgnoreCase("storm") ? TrayIcon.MessageType.WARNING : TrayIcon.MessageType.INFO);
                    }

                    String htmlMessage = emojiMessage
                        .replace("🌡️ ", "<img src='file:" + new File("icons/thermometer.png").getAbsolutePath() + "' width='16' height='16' alt='🌡️'> ")
                        .replace("🌬️ ", "<img src='file:" + new File("icons/wind.png").getAbsolutePath() + "' width='16' height='16' alt='🌬️'> ")
                        .replace("💧 ", "<img src='file:" + new File("icons/rain.png").getAbsolutePath() + "' width='16' height='16' alt='💧'> ");
                    String typeIconHtml = getIconForType(type);
                    String fullHtmlMessage = typeIconHtml + " " + htmlMessage;

                    SwingUtilities.invokeLater(() -> {
                        showAlertDialog(fullHtmlMessage, severity);
                    });

                    logToGui(emojiMessage, type);
                    if (logWriter != null) {
                        logWriter.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " - " + emojiMessage);
                        logWriter.flush();
                    }
                }
            } catch (Exception e) {
                logToGui("Error receiving data: 🌡️ " + e.getMessage(), "error");
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        }).start();

        // Load history from MongoDB
        loadHistoryFromMongoDB();
    }

    private String getIconForType(String type) {
        String iconPath = "";
        switch (type.toLowerCase()) {
            case "rain": iconPath = "icons/rain.png"; break;
            case "storm": iconPath = "icons/storm.png"; break;
            case "heat": iconPath = "icons/sun.png"; break;
            case "wind": iconPath = "icons/wind.png"; break;
            case "clear": iconPath = "icons/clear.png"; break;
            default: iconPath = "icons/warning.png"; break;
        }
        return "<img src='file:" + new File(iconPath).getAbsolutePath() + "' width='16' height='16' alt='" + getEmojiForType(type) + "'> ";
    }

    // THÊM: Get emoji for type (fallback cho tray notification)
    private String getEmojiForType(String type) {
        switch (type.toLowerCase()) {
            case "rain": return "🌧️";
            case "storm": return "⛈️";
            case "heat": return "☀️";
            case "wind": return "🌬️";
            case "clear": return "🌤️";
            default: return "⚠️";
        }
    }

    private void showAlertDialog(String message, String severity) {
        JDialog dialog = new JDialog(this, "Cảnh báo Thời tiết", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, getBackgroundColorForSeverity(severity), 0, getHeight(), new Color(255, 255, 255));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getColorForSeverity(severity), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel label = new JLabel("<html><b>" + message + "</b></html>", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setForeground(getColorForSeverity(severity));
        panel.add(label, BorderLayout.CENTER);

        JButton closeButton = new JButton("Đóng");
        closeButton.setFont(new Font("Arial", Font.BOLD, 12));
        closeButton.setBackground(new Color(220, 20, 60));
        closeButton.setForeground(Color.WHITE);
        closeButton.addActionListener(e -> dialog.dispose());
        panel.add(closeButton, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private Color getColorForSeverity(String severity) {
        switch (severity.toLowerCase()) {
            case "severe": return Color.RED;
            case "moderate": return Color.ORANGE;
            case "minor": return Color.GREEN;
            default: return Color.BLACK;
        }
    }

    private Color getBackgroundColorForSeverity(String severity) {
        switch (severity.toLowerCase()) {
            case "severe": return new Color(255, 204, 204);
            case "moderate": return new Color(255, 229, 180);
            case "minor": return new Color(204, 255, 204);
            default: return new Color(240, 240, 240);
        }
    }

    private void stopClient() {
        running.set(false);
        if (socket != null) {
            try {
                socket.leaveGroup(group);
                socket.close();
            } catch (Exception e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
        if (logWriter != null) {
            logWriter.close();
        }
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        if (mongoDBManager != null) {
            mongoDBManager.close();
        }
        dispose();
    }

    // Thay thế phương thức loadHistoryFromMongoDB() bằng:
    private void loadHistoryFromMongoDB() {
        if (mongoDBManager != null) {
            List<String> history = mongoDBManager.getAlertHistory(50); // Chỉ tải 50 entries mới nhất
            for (String entry : history) {
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
                        logToGui(message, type);
                    } else {
                        logToGui(entry, "info");
                    }
                } catch (Exception ex) {
                    System.err.println("Lỗi parse history entry: " + entry + " - " + ex.getMessage());
                    logToGui(entry, "info");
                }
            }
            logToGui("Đã tải " + history.size() + " lịch sử mới nhất từ MongoDB.", "info"); // Thông báo để biết
        }
    }

    private void logToGui(String message, String type) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> logToGui(message, type));
            return;
        }

        String displayMessage = message
            .replace("🌡️ ", "<img src='file:" + new File("icons/thermometer.png").getAbsolutePath() + "' width='16' height='16' alt='🌡️'> ")
            .replace("🌬️ ", "<img src='file:" + new File("icons/wind.png").getAbsolutePath() + "' width='16' height='16' alt='🌬️'> ")
            .replace("💧 ", "<img src='file:" + new File("icons/rain.png").getAbsolutePath() + "' width='16' height='16' alt='💧'> ");

        String timestamp = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date());
        String color;
        String iconHtml = getIconForType(type);

        switch (type.toLowerCase()) {
            case "rain": color = "#0000FF"; break;
            case "storm": color = "#800080"; break;
            case "heat": color = "#FF0000"; break;
            case "wind": color = "#00FFFF"; break;
            case "clear": color = "#FFA500"; break;
            case "error": color = "#FF0000"; break;
            default: color = "#000000"; break;
        }

        String htmlMessage = String.format(
            "<p style='color:%s; font-family: Arial; font-size: 12px;'>[%s] %s %s</p>",
            color, timestamp, iconHtml, displayMessage
        );

        HTMLDocument doc = (HTMLDocument) logPane.getDocument();
        try {
            HTMLEditorKit kit = new HTMLEditorKit();
            kit.insertHTML(doc, doc.getLength(), htmlMessage, 0, 0, null);
            logPane.setCaretPosition(doc.getLength());

            // Lưu vào MongoDB
            if (mongoDBManager != null) {
                mongoDBManager.saveAlert(message, type, getSeverityFromMessage(message), timestamp);
            }
        } catch (BadLocationException | IOException e) {
            System.err.println("Error adding to log: " + e.getMessage());
        }
    }

    private String getSeverityFromMessage(String message) {
        if (message.contains("Mức độ: severe")) return "severe";
        if (message.contains("Mức độ: moderate")) return "moderate";
        if (message.contains("Mức độ: minor")) return "minor";
        return "minor";
    }

    public static void main(String[] args) throws Exception {
        String groupAddress = (args.length > 0) ? args[0] : "239.255.0.1";
        int port = (args.length > 1) ? Integer.parseInt(args[1]) : 4446;
        if (args.length < 2) {
            System.out.println("Warning: Using default values - group: 239.255.0.1, port: 4446. Correct syntax: java Alert.AlertClientGUI <groupAddress> <port>");
        }
        SwingUtilities.invokeLater(() -> {
            new AlertClientGUI(groupAddress, port).setVisible(true);
        });
    }
}