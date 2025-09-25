package Alert;

import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

public class AlertServer {
    private static final String MULTICAST_GROUP = "239.255.0.1";
    private String apiKey;
    private String apiUrl;
    private String city;
    private AlertServerGUI gui;
    private String selectedType;
    private String selectedSeverity;
    private String customMessage;
    private long interval;
    private Timer timer;
    private int defaultPort;

    public AlertServer(String city, AlertServerGUI gui, String selectedType, String selectedSeverity, String customMessage, long interval, int port) {
        this.apiKey = Config.get("WEATHER_API_KEY");
        this.apiUrl = Config.get("WEATHER_API_URL");
        this.city = city;
        this.gui = gui;
        this.selectedType = selectedType;
        this.selectedSeverity = selectedSeverity;
        this.customMessage = customMessage;
        this.interval = interval;
        this.defaultPort = port;
        if (gui != null) {
            gui.log("Server khởi động cho 🌍 " + city + " (Type: " + selectedType + ", Severity: " + selectedSeverity + ", Interval: " + interval + "ms, Port: " + port + ")", "info");
        }
    }

    public void start() throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    String alertJson = fetchWeatherAlert();
                    if (alertJson != null) {
                        byte[] data = alertJson.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket packet = new DatagramPacket(data, data.length, group, defaultPort);
                        socket.send(packet);
                        if (gui != null) {
                            gui.log("Alert gửi đến 🌐 " + MULTICAST_GROUP + ":" + defaultPort + " 📡", "info");
                            gui.addToHistory("Gửi lúc " + new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date()) + ": " + formatAlertMessage(alertJson));
                            gui.setLastApiData(alertJson);
                        }
                    } else {
                        if (gui != null) {
                            gui.log("Không có dữ liệu alert 🌫️", "error");
                        }
                    }
                } catch (Exception e) {
                    if (gui != null) {
                        gui.log("Lỗi gửi alert: 🌡️ " + e.getMessage(), "error");
                    }
                    e.printStackTrace();
                }
            }
        }, 0, interval);

        if (gui != null) {
            gui.log("Server khởi động 🌐 " + MULTICAST_GROUP + ":" + defaultPort, "info");
        }
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
        if (gui != null) {
            gui.log("Server dừng ⏹️", "info");
        }
    }

    public void sendManualAlert(String manualMessage, String alertType, String severity, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

            JSONObject alert = new JSONObject();
            alert.put("type", alertType.equals("Auto") ? "manual" : alertType.toLowerCase());
            alert.put("description", manualMessage.isEmpty() ? customMessage : manualMessage);
            alert.put("temperature", 0);
            alert.put("wind", 0);
            alert.put("precipitation", 0);
            alert.put("icon", "⚠️");
            alert.put("location", city);
            alert.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            alert.put("severity", severity.toLowerCase());

            byte[] data = alert.toString().getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
            socket.send(packet);
            socket.close();

            if (gui != null) {
                gui.log("Manual alert gửi đến 🌐 " + MULTICAST_GROUP + ":" + port, "info");
                gui.addToHistory("Gửi thủ công lúc " + new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date()) + ": " + formatAlertMessage(alert.toString()));
            }
        } catch (Exception e) {
            if (gui != null) {
                gui.log("Lỗi gửi manual alert: 🌡️ " + e.getMessage(), "error");
            }
            e.printStackTrace();
        }
    }

    public void sendManualAlertWithLastData(String lastApiData, String alertType, String severity, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

            JSONObject alert = new JSONObject(lastApiData);
            alert.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            alert.put("description", customMessage.isEmpty() ? alert.getString("description") : customMessage);
            alert.put("type", alertType.equals("Auto") ? alert.getString("type") : alertType.toLowerCase());
            alert.put("severity", severity.toLowerCase());

            byte[] data = alert.toString().getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
            socket.send(packet);
            socket.close();

            if (gui != null) {
                gui.log("Manual alert with last data gửi đến 🌐 " + MULTICAST_GROUP + ":" + port, "info");
                gui.addToHistory("Gửi thủ công với dữ liệu cũ lúc " + new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date()) + ": " + formatAlertMessage(alert.toString()));
            }
        } catch (Exception e) {
            if (gui != null) {
                gui.log("Lỗi gửi manual alert with last data: 🌡️ " + e.getMessage(), "error");
            }
            e.printStackTrace();
        }
    }

    public void sendManualAlertToAllPorts(String messageSource, String alertType, String severity) {
        int[] ports = {4446, 4447, 4448}; // Danh sách tất cả cổng của client
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

            JSONObject alert;
            if (messageSource.startsWith("{")) { // Kiểm tra nếu là JSON (lastApiData)
                alert = new JSONObject(messageSource);
                alert.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                alert.put("description", customMessage.isEmpty() ? alert.getString("description") : customMessage);
                alert.put("type", alertType.equals("Auto") ? alert.getString("type") : alertType.toLowerCase());
                alert.put("severity", severity.toLowerCase());
            } else { // Trường hợp manualMessage
                alert = new JSONObject();
                alert.put("type", alertType.equals("Auto") ? "manual" : alertType.toLowerCase());
                alert.put("description", messageSource.isEmpty() ? customMessage : messageSource);
                alert.put("temperature", 0);
                alert.put("wind", 0);
                alert.put("precipitation", 0);
                alert.put("icon", "⚠️");
                alert.put("location", city);
                alert.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                alert.put("severity", severity.toLowerCase());
            }

            byte[] data = alert.toString().getBytes(StandardCharsets.UTF_8);
            for (int port : ports) {
                DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
                socket.send(packet);
            }
            socket.close();

            if (gui != null) {
                String logMessage = messageSource.startsWith("{") 
                    ? "Manual alert with last data gửi đến tất cả client (các cổng 4446, 4447, 4448)" 
                    : "Manual alert gửi đến tất cả client (các cổng 4446, 4447, 4448)";
                gui.log(logMessage, "info");
                gui.addToHistory("Gửi thủ công đến tất cả lúc " + new SimpleDateFormat("HH:mm:ss dd/MM/yyyy").format(new Date()) + ": " + formatAlertMessage(alert.toString()));
            }
        } catch (Exception e) {
            if (gui != null) {
                gui.log("Lỗi gửi manual alert đến tất cả: 🌡️ " + e.getMessage(), "error");
            }
            e.printStackTrace();
        }
    }

    private String fetchWeatherAlert() {
        try {
            URL url = new URL(String.format("%s?q=%s&appid=%s&units=metric&lang=vi", apiUrl, city, apiKey));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();

            if (responseCode != 200) {
                return null;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONObject json = new JSONObject(sb.toString());
            JSONArray list = json.getJSONArray("list");
            if (list.length() == 0) {
                return null;
            }

            JSONObject first = list.getJSONObject(0);
            JSONObject main = first.getJSONObject("main");
            JSONArray weatherArr = first.getJSONArray("weather");
            JSONObject weather = weatherArr.getJSONObject(0);

            double temp = main.getDouble("temp");
            double wind = first.getJSONObject("wind").optDouble("speed", 0);
            double precipitation = first.optJSONObject("rain") != null ? first.getJSONObject("rain").optDouble("3h", 0) : 0;

            String mainWeather = weather.getString("main");
            String desc = weather.getString("description") + (customMessage.isEmpty() ? "" : " - " + customMessage);

            String type = selectedType.equals("Auto") ? "info" : selectedType.toLowerCase();
            String icon = "ℹ️";
            String severity = selectedSeverity.toLowerCase();

            if (selectedType.equals("Auto")) {
                if (mainWeather.toLowerCase().contains("rain") || precipitation > 2) {
                    type = "rain";
                    icon = "🌧️";
                    severity = precipitation > 10 ? "severe" : "moderate";
                } else if (mainWeather.toLowerCase().contains("storm") || mainWeather.toLowerCase().contains("thunder")) {
                    type = "storm";
                    icon = "⛈️";
                    severity = "severe";
                } else if (temp > 35) {
                    type = "heat";
                    icon = "☀️";
                    severity = "severe";
                } else if (wind > 10) {
                    type = "wind";
                    icon = "🌬️";
                    severity = "moderate";
                } else if (mainWeather.toLowerCase().contains("clear")) {
                    type = "clear";
                    icon = "🌤️";
                    severity = "minor";
                }
            } else {
                switch (selectedType.toLowerCase()) {
                    case "rain": icon = "🌧️"; break;
                    case "storm": icon = "⛈️"; break;
                    case "heat": icon = "☀️"; break;
                    case "wind": icon = "🌬️"; break;
                    case "clear": icon = "🌤️"; break;
                }
            }

            JSONObject alert = new JSONObject();
            alert.put("type", type);
            alert.put("description", desc);
            alert.put("temperature", temp);
            alert.put("wind", wind);
            alert.put("precipitation", precipitation);
            alert.put("icon", icon);
            alert.put("location", city);
            alert.put("time", first.getString("dt_txt"));
            alert.put("severity", severity);

            if (gui != null) {
                gui.log("Alert tạo 🌡️ " + temp + "°C, 🌬️ " + wind + "m/s (Severity: " + severity + ")", type);
            }
            return alert.toString();

        } catch (Exception e) {
            if (gui != null) {
                gui.log("Lỗi lấy thời tiết: 🌡️ " + e.getMessage(), "error");
            }
            return null;
        }
    }

    private String formatAlertMessage(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            String type = json.getString("type");
            String desc = json.getString("description");
            double temp = json.getDouble("temperature");
            double wind = json.getDouble("wind");
            double rain = json.getDouble("precipitation");
            String location = json.getString("location");
            String severity = json.getString("severity");
            return String.format("%s tại %s, Nhiệt độ: %.1f°C, Gió: %.1f m/s, Mưa: %.1f mm (Mức độ: %s)", desc, location, temp, wind, rain, severity);
        } catch (Exception e) {
            return "Không thể định dạng thông báo.";
        }
    }

    private void logAlert(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            String type = json.optString("type");
            String desc = json.optString("description");
            double temp = json.optDouble("temperature");
            double wind = json.optDouble("wind");
            double rain = json.optDouble("precipitation");
            String location = json.optString("location");
            String severity = json.optString("severity");

            String message = String.format("%s tại %s 🌡️ %.1f°C, 🌬️ %.1f m/s, 💧 %.1f mm (Severity: %s)", desc, location, temp, wind, rain, severity);
            if (gui != null) {
                gui.log(message, type);
            }
        } catch (Exception e) {
            if (gui != null) {
                gui.log("Lỗi log alert: 🌡️ " + e.getMessage(), "error");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String city;
        if (args.length < 1) {
            city = Config.get("DEFAULT_CITY");
            if (city == null || city.isEmpty()) {
                System.out.println("Usage: java Alert.AlertServer <CityName>");
                return;
            }
            System.out.println("No city provided, using default city: " + city);
        } else {
            city = args[0];
        }

        AlertServer server = new AlertServer(city, null, "Auto", "Minor", "", 300000, 4446);
        server.start();
    }
}