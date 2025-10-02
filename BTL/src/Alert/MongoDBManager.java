package Alert;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

public class MongoDBManager {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> alertsCollection;
    private MongoCollection<Document> usersCollection;

    public MongoDBManager() {
        try {
            // Kết nối đến MongoDB server (địa chỉ mặc định: localhost:27017)
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            database = mongoClient.getDatabase("weather_alerts_db");
            alertsCollection = database.getCollection("alerts");
            usersCollection = database.getCollection("users");
            
            // Tạo index cho timestamp nếu chưa có (tối ưu query)
            alertsCollection.createIndex(Sorts.descending("timestamp"));
            usersCollection.createIndex(new Document("username", 1));
            
            System.out.println("Kết nối MongoDB thành công!");
        } catch (Exception e) {
            System.err.println("Lỗi kết nối MongoDB: " + e.getMessage());
        }
    }

    // Lưu một cảnh báo vào MongoDB (giữ nguyên)
    public void saveAlert(String message, String type, String severity, String timestamp) {
        try {
            Document alert = new Document("message", message)
                    .append("type", type)
                    .append("severity", severity)
                    .append("timestamp", timestamp);
            alertsCollection.insertOne(alert);
            System.out.println("Lưu cảnh báo vào MongoDB: " + message);
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu cảnh báo vào MongoDB: " + e.getMessage());
        }
    }

    // Lấy danh sách lịch sử cảnh báo từ MongoDB - với limit (mới)
    public List<String> getAlertHistory(int limit) {
        List<String> history = new ArrayList<>();
        try {
            FindIterable<Document> documents = alertsCollection.find()
                    .sort(Sorts.descending("timestamp")) // Sắp xếp theo thời gian giảm dần
                    .limit(limit); // Giới hạn số lượng
            for (Document doc : documents) {
                String entry = String.format("[%s] %s (%s, %s)",
                        doc.getString("timestamp"),
                        doc.getString("message"),
                        doc.getString("type"),
                        doc.getString("severity"));
                history.add(entry);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy lịch sử cảnh báo từ MongoDB: " + e.getMessage());
        }
        return history;
    }

    // Phương thức cũ - sử dụng limit mặc định 50 (giữ nguyên để tương thích)
    public List<String> getAlertHistory() {
        return getAlertHistory(50); // Giới hạn 50 entries mới nhất
    }

    // THÊM: Lưu user mới vào MongoDB
    public boolean saveUser(String username, String password) {
        try {
            // Kiểm tra user đã tồn tại chưa
            Document existingUser = usersCollection.find(new Document("username", username)).first();
            if (existingUser != null) {
                return false; // User đã tồn tại
            }
            Document user = new Document("username", username)
                    .append("password", password); // Lưu ý: Trong thực tế, nên hash password
            usersCollection.insertOne(user);
            System.out.println("Lưu user vào MongoDB: " + username);
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi khi lưu user vào MongoDB: " + e.getMessage());
            return false;
        }
    }

    // THÊM: Tìm user theo username
    public Document findUserByUsername(String username) {
        try {
            return usersCollection.find(new Document("username", username)).first();
        } catch (Exception e) {
            System.err.println("Lỗi khi tìm user: " + e.getMessage());
            return null;
        }
    }

    // THÊM: Xác thực user (kiểm tra username và password)
    public boolean validateUser(String username, String password) {
        Document user = findUserByUsername(username);
        if (user != null) {
            return user.getString("password").equals(password); // Lưu ý: Trong thực tế, so sánh hash
        }
        return false;
    }

    // Đóng kết nối MongoDB (giữ nguyên)
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("Đóng kết nối MongoDB.");
        }
    }
}