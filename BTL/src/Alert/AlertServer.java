package Alert;

import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

public class AlertServer {
    private static final String MULTICAST_GROUP = "239.255.0.1"; // địa chỉ multicast
    private static final int PORT = 4446;                        // cổng multicast
    private static final long INTERVAL = 300_000;                // 5 phút (ms)

    private String apiKey;
    private String apiUrl;
    private String city;
    private AlertServerGUI gui; // Thêm tham chiếu đến GUI

    public AlertServer(String city, AlertServerGUI gui) {
        this.apiKey = Config.get("WEATHER_API_KEY");
        this.apiUrl = Config.get("WEATHER_API_URL");
        this.city = city;
        this.gui = gui;
        if (gui != null) {
            gui.log("Server khởi động cho 🌍 " + city, "info");
        }
    }

    public void start() throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    String alertJson = fetchWeatherAlert();
                    if (alertJson != null) {
                        byte[] data = alertJson.getBytes();
                        DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
                        socket.send(packet);
                        if (gui != null) {
                            gui.log("Alert gửi đến 🌐 " + MULTICAST_GROUP + ":" + PORT + " 📡", "info");
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
        }, 0, INTERVAL);

        if (gui != null) {
            gui.log("Server khởi động 🌐 " + MULTICAST_GROUP + ":" + PORT, "info");
        }
        System.in.read();
        timer.cancel();
        socket.close();
        if (gui != null) {
            gui.log("Server dừng ⏹️", "info");
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

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
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
            String desc = weather.getString("description");

            String type = "info";
            String icon = "ℹ️";
            if (mainWeather.toLowerCase().contains("rain")) {
                type = "rain"; icon = "🌧️";
            } else if (mainWeather.toLowerCase().contains("storm") || mainWeather.toLowerCase().contains("thunder")) {
                type = "storm"; icon = "⛈️";
            } else if (temp > 35) {
                type = "heat"; icon = "☀️";
            } else if (wind > 10) {
                type = "wind"; icon = "🌬️";
            } else if (mainWeather.toLowerCase().contains("clear")) {
                type = "clear"; icon = "🌤️";
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

            if (gui != null) {
                gui.log("Alert tạo 🌡️ " + temp + "°C, 🌬️ " + wind + "m/s", type);
            }
            return alert.toString();

        } catch (Exception e) {
            if (gui != null) {
                gui.log("Lỗi lấy thời tiết: 🌡️ " + e.getMessage(), "error");
            }
            return null;
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

            String message = String.format("%s tại %s 🌡️ %.1f°C, 🌬️ %.1f m/s, 💧 %.1f mm", desc, location, temp, wind, rain);
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

        AlertServer server = new AlertServer(city, null);
        server.start();
    }
}