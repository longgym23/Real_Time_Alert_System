<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
    Hệ thống cảnh báo thời gian thực
</h2>
<div align="center">
    <p align="center">
        <img alt="AIoTLab Logo" width="170" src="https://github.com/user-attachments/assets/711a2cd8-7eb4-4dae-9d90-12c0a0a208a2" />
        <img alt="AIoTLab Logo" width="180" src="https://github.com/user-attachments/assets/dc2ef2b8-9a70-4cfa-9b4b-f6c2f25f1660" />
        <img alt="DaiNam University Logo" width="200" src="https://github.com/user-attachments/assets/77fe0fd1-2e55-4032-be3c-b1a705a1b574" />
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

---
# 🌦️ Hệ Thống Cảnh Báo Thời Gian Thực (UDP)

## 1. Giới thiệu hệ thống

Đây là một hệ thống cảnh báo thời gian thực hoạt động theo mô hình **Server - Client**.  
Server sẽ gửi các thông báo/cảnh báo thời tiết tới nhiều Client thông qua giao thức **UDP**.

### 🎯 Mục tiêu hệ thống
- Đảm bảo truyền tải cảnh báo nhanh chóng (gần như tức thì).
- Hỗ trợ nhiều Client cùng lúc (tối đa **10 client**).
- Dễ dàng triển khai trong các hệ thống giám sát, IoT, hoặc quản lý tập trung.

### 🏗️ Thành phần
- **Server**:  
  - Quản lý danh sách client.  
  - Gửi broadcast thông báo thời tiết (mưa lớn, bão, nắng nóng, v.v.).  
  - Ghi log lại tất cả cảnh báo đã gửi vào GUI.  

- **Client**:  
  - Nhận và hiển thị cảnh báo thời gian thực kèm **timestamp**.  
  - Ghi log vào file `weather_alerts.log`.  
  - Phân loại và hiển thị nhiều loại cảnh báo khác nhau.  

---

## ⚙️ Chức năng chính

### 🔹 Server
- Khởi tạo server UDP trên port **12345** để phát cảnh báo.  
- GUI (Swing) cho phép gửi thông báo cảnh báo đến tất cả client đã đăng ký.  
- Hỗ trợ nhiều loại cảnh báo thời tiết:
  - Mưa lớn 🌧️
  - Bão 🌀
  - Nắng nóng ☀️
  - Sương mù 🌫️
  - Lốc xoáy 🌪️  

### 🔹 Client
- Kết nối tới Server qua UDP socket và tự động đăng ký.  
- Nhận và hiển thị cảnh báo thời gian thực trên console với **timestamp**.  
- Ghi log lịch sử cảnh báo đã nhận vào file `weather_alerts.log`.  

---

## 2. Ngôn ngữ & Công nghệ chính

- **Ngôn ngữ**: Java (JDK 8+)  
- **Công nghệ sử dụng**:
  - `UDP Socket (java.net)` → Giao thức truyền dữ liệu nhanh, không kết nối.  
  - `Swing (javax.swing)` → Xây dựng GUI cho server.  
  - `Threading (java.util.concurrent)` → Xử lý bất đồng bộ cho đăng ký và nhận tin.  
  - `File I/O (java.io)` → Ghi log cảnh báo vào file.  
  - `SimpleDateFormat (java.text)` → Định dạng timestamp cho thông báo.  

---

## 3. Hướng dẫn chạy

### 📌 Yêu cầu
- **JDK**: Phiên bản 8 trở lên  
- **Hệ điều hành**: Windows / Linux / macOS  
- **Công cụ**: IDE (IntelliJ IDEA, Eclipse) hoặc dòng lệnh `javac`  
- **Mạng**: Các thiết bị cùng mạng LAN (hoặc dùng `localhost` để test)  

### 📂 Cấu trúc dự án
```
Alert/
├── AlertClient.java     // Client nhận và log cảnh báo
├── AlertServer.java     // Server quản lý client và broadcast
├── AlertServerGUI.java  // GUI cho server
```

### 🚀 Cách chạy chương trình

1. **Biên dịch code**
   ```bash
   cd Alert
   javac *.java
   ```

2. **Chạy Server với GUI**
   ```bash
   java Alert.AlertServerGUI
   ```
   → GUI xuất hiện để chọn điều kiện thời tiết và gửi cảnh báo.  

3. **Chạy Client**
   ```bash
   java Alert.AlertClient
   ```
   → Client tự động đăng ký và chờ nhận cảnh báo.  
   → Nhấn **Enter** để dừng client.  

4. **Gửi cảnh báo**
   - Trên GUI Server: Chọn điều kiện, nhập vị trí → nhấn **Gửi thông báo**.  
   - Client sẽ hiển thị thông báo kèm timestamp và ghi log.  

5. **Dừng hệ thống**
   - Đóng GUI để dừng server.  
   - Nhấn **Enter** trong terminal client để dừng.  

### ⚠️ Lưu ý
- Luôn chạy **Server trước Client**.  
- File log `weather_alerts.log` được tạo trong thư mục chạy client.  
- Nếu chạy trên nhiều máy, thay `localhost` trong `AlertClient.java` bằng **IP Server**.  
- Kiểm tra **firewall** để mở port `12345`.  

---

## 🚀 Hướng phát triển
- Thêm giao diện người dùng (UI) web thay cho desktop.  
- Cho phép Client phản hồi lại Server (ACK hoặc gửi trạng thái).  
- Mã hóa dữ liệu cảnh báo để tăng tính bảo mật.  
- Mở rộng cho môi trường Internet (không chỉ trong LAN).  
- Hỗ trợ đa nền tảng (Windows, Linux, Android, iOS).  
- Tích hợp dữ liệu thời tiết thực tế từ API.  

---

## 4. Hình ảnh minh họa
> *(Hình ảnh GUI server và console client sẽ được thêm sau khi test.)*  

---

## 📚 5. Các project đã thực hiện dựa trên Platform
- **Khóa 15**  
- **Khóa 16 (Coming soon)**  
