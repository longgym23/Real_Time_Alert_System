package Alert;

import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.*;

public class AlertClient {
    private DatagramSocket socket;
    
    
    private InetAddress serverAddress;
    private int serverPort;
    private AtomicBoolean running;
    private PrintWriter logWriter;

    public AlertClient(String serverHost, int serverPort) throws Exception {
        socket = new DatagramSocket();
        serverAddress = InetAddress.getByName(serverHost);
        this.serverPort = serverPort;
        running = new AtomicBoolean(true);
        
        // Mở file log để ghi thông báo, thêm try-catch để kiểm tra lỗi
        try {
            logWriter = new PrintWriter(new FileWriter("weather_alerts.log", true));
            System.out.println("Log file weather_alerts.log opened successfully.");
        } catch (IOException e) {
            System.err.println("Error opening log file: " + e.getMessage());
            throw e; // Ném lỗi để dừng chương trình nếu không mở được file
        }
        
        // Register with server
        String registerMessage = "REGISTER";
        byte[] buffer = registerMessage.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, 
            serverAddress, serverPort);
        socket.send(packet);
        System.out.println("Registration packet sent to " + serverHost + ":" + serverPort);
        
        System.out.println("Weather Alert Client started, registered with server: " + 
            serverHost + ":" + serverPort);
    }

    public void startReceiving() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (running.get()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    // Thêm thời gian thực khi nhận thông điệp
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a z, EEEE, MMMM dd, yyyy");
                    String timestamp = sdf.format(new Date());
                    String alert = "WEATHER ALERT [" + timestamp + "]: " + message;
                    System.out.println(alert);
                    // Ghi vào file log
                    logWriter.println(alert);
                    logWriter.flush(); // Đảm bảo ghi ngay lập tức
                    System.out.println("Alert written to weather_alerts.log: " + alert);
                } catch (Exception e) {
                    if (running.get()) {
                        System.err.println("Error receiving or writing alert: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            // Đóng file log khi dừng
            if (logWriter != null) {
                logWriter.close();
                System.out.println("Log file closed.");
            }
        }).start();
    }

    public void stop() {
        running.set(false);
        socket.close();
    }

    public static void main(String[] args) throws Exception {
        AlertClient client = new AlertClient("localhost", 12345);
        client.startReceiving();
        
        // Keep client running until manually stopped
        System.in.read();
        client.stop();
    }
}