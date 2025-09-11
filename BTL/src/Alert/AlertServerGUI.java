package Alert;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.text.SimpleDateFormat;

public class AlertServerGUI extends JFrame {
    private AlertServer server;
    private JComboBox<String> weatherConditionCombo;
    private JTextField locationField;
    private JTextArea logArea;
    private JButton sendButton;
    private final int SERVER_PORT = 12345;
    private final int MAX_CLIENTS = 10;
    private final String[] weatherConditions = {
        "Mưa lớn", "Bão", "Nắng nóng", "Sương mù", "Lốc xoáy"
    };

    public AlertServerGUI() {
        super("Weather Alert Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 400);
        setLocationRelativeTo(null);

        try {
            server = new AlertServer(SERVER_PORT, MAX_CLIENTS);
            server.startRegistration();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khởi động máy chủ: " + e.getMessage());
            System.exit(1);
        }

        // Initialize components
        weatherConditionCombo = new JComboBox<>(weatherConditions);
        locationField = new JTextField(15);
        sendButton = new JButton("Gửi thông báo thời tiết");
        logArea = new JTextArea(15, 30);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // Layout
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel("Điều kiện thời tiết:"));
        inputPanel.add(weatherConditionCombo);
        inputPanel.add(new JLabel("Vị trí:"));
        inputPanel.add(locationField);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(sendButton);

        setLayout(new BorderLayout());
        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Event listeners
        sendButton.addActionListener(e -> sendWeatherAlert());
        locationField.addActionListener(e -> sendWeatherAlert());

        // Window close listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                server.stop();
            }
        });

        log("Máy chủ cảnh báo thời tiết đã khởi động trên cổng " + SERVER_PORT);
    }

    private void sendWeatherAlert() {
        String condition = (String) weatherConditionCombo.getSelectedItem();
        String location = locationField.getText().trim();
        if (location.isEmpty()) {
            location = "Unknown Location";
        }

        // Thêm thời gian thực vào thông điệp
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a z, EEEE, MMMM dd, yyyy");
        String timestamp = sdf.format(new Date());
        String message = String.format("%s at %s [%s]! Hãy thực hiện các biện pháp phòng ngừa cần thiết.", 
            condition, location, timestamp);
        
        try {
            server.broadcastAlert(message);
            log("Broadcasted weather alert: " + message);
            locationField.setText("");
        } catch (Exception ex) {
            log("Error sending weather alert: " + ex.getMessage());
        }
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AlertServerGUI().setVisible(true);
        });
    }
}