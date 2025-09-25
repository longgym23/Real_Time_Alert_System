package Alert;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;

public class AlertClientGUI2 extends JFrame {
    private MulticastSocket socket;
    private InetAddress group;
    private int port = 4447; // Cổng khác cho client 2
    private AtomicBoolean running;
    private PrintWriter logWriter;
    private JTextPane logPane;
    private JButton stopButton;
    private TrayIcon trayIcon;

    public AlertClientGUI2() {
        super("Ứng dụng Khách Cảnh báo Thời tiết 2 (OpenWeather API)");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            System.err.println("Không thể thiết lập giao diện Nimbus: " + e.getMessage());
        }

        this.group = null;
        this.running = new AtomicBoolean(true);

        // Khởi tạo log file
        try {
            logWriter = new PrintWriter(new FileWriter("weather_alerts2.log", true));
            System.out.println("Tệp log weather_alerts2.log đã mở thành công.");
        } catch (IOException e) {
            System.err.println("Lỗi khi mở tệp log: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Không thể mở tệp log: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        // Khởi tạo SystemTray cho thông báo nội dung
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
            trayIcon = new TrayIcon(image, "Ứng dụng Khách Cảnh báo Thời tiết 2");
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("Lỗi thêm biểu tượng khay hệ thống: " + e.getMessage());
                logToGui("Lỗi thêm biểu tượng khay hệ thống: " + e.getMessage(), "error");
            }
        } else {
            System.err.println("Khay hệ thống không được hỗ trợ trên nền tảng này.");
            logToGui("Khay hệ thống không được hỗ trợ trên nền tảng này", "error");
        }

        // Thiết lập giao diện
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Log area
        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setContentType("text/html");
        logPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
        logPane.setText("");
        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Nhật ký Cảnh báo"));

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(240, 240, 240));
        stopButton = new JButton("Dừng Ứng dụng");
        stopButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        buttonPanel.add(stopButton);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Event listeners
        stopButton.addActionListener(e -> stopClient());

        // Khởi động thread nhận dữ liệu
        new Thread(() -> {
            try {
                group = InetAddress.getByName("239.255.0.1");
                socket = new MulticastSocket(port);
                socket.joinGroup(group);
                logToGui("Kết nối đến 🌐 239.255.0.1:" + port, "info");

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
                    String message = String.format("%s tại %s 🌡️ %.1f°C, 🌬️ %.1f km/h, 💧 %.1f mm (Mức độ: %s)", desc, location, temp, wind, rain, severity);

                    if (trayIcon != null) {
                        String notificationTitle = type.equalsIgnoreCase("storm") ? "CẢNH BÁO NGHIÊM TRỌNG" : "Cảnh báo thời tiết";
                        trayIcon.displayMessage(notificationTitle, message, type.equalsIgnoreCase("storm") ? TrayIcon.MessageType.WARNING : TrayIcon.MessageType.INFO);
                    }

                    logToGui(message, type);
                    if (logWriter != null) {
                        logWriter.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " - " + message);
                        logWriter.flush();
                    }
                }
            } catch (Exception e) {
                logToGui("Lỗi nhận dữ liệu: 🌡️ " + e.getMessage(), "error");
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        }).start();
    }

    private void stopClient() {
        running.set(false);
        if (socket != null) {
            try {
                socket.leaveGroup(group);
                socket.close();
            } catch (Exception e) {
                System.err.println("Lỗi đóng socket: " + e.getMessage());
            }
        }
        if (logWriter != null) {
            logWriter.close();
        }
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        dispose();
    }

    private void logToGui(String message, String type) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> logToGui(message, type));
            return;
        }

        String timestamp = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date());
        String color;
        String icon;

        switch (type.toLowerCase()) {
            case "rain": color = "#0000FF"; icon = "🌧️"; break;
            case "storm": color = "#800080"; icon = "⛈️"; break;
            case "heat": color = "#FF0000"; icon = "☀️"; break;
            case "wind": color = "#00FFFF"; icon = "🌬️"; break;
            case "clear": color = "#FFA500"; icon = "🌤️"; break;
            case "error": color = "#FF0000"; icon = "❌"; break;
            default: color = "#000000"; icon = ""; break;
        }

        String htmlMessage = String.format(
            "<p style='color:%s; font-family: SansSerif; font-size: 14px;'>[%s] %s %s</p>",
            color, timestamp, icon, message
        );

        HTMLDocument doc = (HTMLDocument) logPane.getDocument();
        try {
            HTMLEditorKit kit = new HTMLEditorKit();
            kit.insertHTML(doc, doc.getLength(), htmlMessage, 0, 0, null);
            logPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException | IOException e) {
            System.err.println("Lỗi thêm vào log: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AlertClientGUI2().setVisible(true);
        });
    }
}