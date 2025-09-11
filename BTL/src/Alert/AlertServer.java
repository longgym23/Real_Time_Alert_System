package Alert;

import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlertServer {
    private DatagramSocket socket;
    private InetAddress[] clientAddresses;
    private int[] clientPorts;
    private int clientCount;
    private AtomicBoolean running;

    public AlertServer(int port, int maxClients) throws SocketException {
        socket = new DatagramSocket(port);
        clientAddresses = new InetAddress[maxClients];
        clientPorts = new int[maxClients];
        clientCount = 0;
        running = new AtomicBoolean(true);
        System.out.println("Weather Alert Server started on port " + port);
    }

    public void registerClient(InetAddress address, int port) {
        if (clientCount < clientAddresses.length) {
            clientAddresses[clientCount] = address;
            clientPorts[clientCount] = port;
            clientCount++;
            System.out.println("Client registered: " + address + ":" + port);
        }
    }

    public void broadcastAlert(String message) throws Exception {
        byte[] buffer = message.getBytes();
        for (int i = 0; i < clientCount; i++) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, 
                clientAddresses[i], clientPorts[i]);
            socket.send(packet);
        }
        System.out.println("Broadcasted weather alert: " + message);
    }

    public void startRegistration() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (running.get()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (message.equals("REGISTER")) {
                        registerClient(packet.getAddress(), packet.getPort());
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stop() {
        running.set(false);
        socket.close();
    }
}