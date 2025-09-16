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

public class AlertClientGUI extends JFrame {
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    private AtomicBoolean running;
    private PrintWriter logWriter;
    private JTextPane logPane;
    private JButton stopButton;

    public AlertClientGUI(String groupAddress, int port) {
        super("Weather Alert Client (OpenWeather API)");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            System.err.println("Cannot set Nimbus Look and Feel: " + e.getMessage());
        }

        this.group = null;
        this.port = port;
        this.running = new AtomicBoolean(true);

        // Khởi tạo log file
        try {
            logWriter = new PrintWriter(new FileWriter("weather_alerts.log", true));
            System.out.println("Log file weather_alerts.log opened successfully.");
        } catch (IOException e) {
            System.err.println("Error opening log file: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Không thể mở file log: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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
        logPane.setFont(new Font("Arial", Font.PLAIN, 14));
        logPane.setText("<html><body style='font-family: Arial; font-size: 14px;'></body></html>");
        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Alert Log"));

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(240, 240, 240));
        stopButton = new JButton("Stop Client");
        stopButton.setFont(new Font("Arial", Font.BOLD, 14));
        buttonPanel.add(stopButton);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Event listeners
        stopButton.addActionListener(e -> stopClient());

        // Khởi động thread nhận dữ liệu
        new Thread(() -> {
            try {
                group = InetAddress.getByName(groupAddress);
                socket = new MulticastSocket(port);
                socket.joinGroup(group);
                logToGui("Kết nối đến 🌐 " + groupAddress + ":" + port, "info");

                byte[] buffer = new byte[1024];
                while (running.get()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    JSONObject json = new JSONObject(received);
                    String type = json.getString("type");
                    String desc = json.getString("description");
                    double temp = json.getDouble("temperature");
                    double wind = json.getDouble("wind");
                    double rain = json.getDouble("precipitation");
                    String location = json.getString("location");
                    String message = String.format("%s tại %s 🌡️ %.1f°C, 🌬️ %.1f km/h, 💧 %.1f mm", desc, location, temp, wind, rain);
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
                System.err.println("Error closing socket: " + e.getMessage());
            }
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

    public static void main(String[] args) throws Exception {
        String groupAddress = (args.length > 0) ? args[0] : "239.255.0.1";
        int port = (args.length > 1) ? Integer.parseInt(args[1]) : 4446;
        if (args.length < 2) {
            System.out.println("Warning: Sử dụng giá trị mặc định - group: 239.255.0.1, port: 4446. " +
                             "Cú pháp chính xác: java Alert.AlertClientGUI <groupAddress> <port>");
        }
        SwingUtilities.invokeLater(() -> {
            new AlertClientGUI(groupAddress, port).setVisible(true);
        });
    }
}