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
## 1. Giới thiệu hệ thống
Đây là một hệ thống **cảnh báo thời gian thực** hoạt động theo mô hình **Server - Client**.  
Server sẽ gửi các thông báo/cảnh báo tới nhiều Client thông qua giao thức **UDP**.   

Hệ thống được xây dựng nhằm:
- Đảm bảo **truyền tải cảnh báo nhanh chóng** (gần như tức thì).
- Hỗ trợ nhiều Client cùng lúc.
- Dễ dàng triển khai trong các hệ thống giám sát, IoT, hoặc quản lý tập trung.

---

## ⚙️ Chức năng chính cần có

### Server
- Khởi tạo server UDP để phát cảnh báo.
- Cho phép gửi **thông báo cảnh báo** đến tất cả client đang kết nối.
- Gửi cảnh báo theo **danh sách Client** hoặc **broadcast toàn mạng LAN**.
- Hỗ trợ nhiều loại cảnh báo (ví dụ: cảnh báo an ninh, hệ thống, môi trường...).
- Ghi log lại tất cả cảnh báo đã gửi.

### Client
- Kết nối tới Server qua UDP socket.
- Nhận và hiển thị cảnh báo theo thời gian thực.
- Có khả năng phân loại và hiển thị nhiều loại cảnh báo khác nhau.
- Ghi log lại lịch sử cảnh báo đã nhận.

---

## 🚀 Hướng phát triển
- Thêm giao diện người dùng (UI) để dễ thao tác (desktop hoặc web).
- Cho phép Client phản hồi lại Server (ACK hoặc gửi trạng thái).
- Mã hóa dữ liệu cảnh báo để tăng tính bảo mật.
- Mở rộng cho môi trường Internet (không chỉ trong LAN).
- Hỗ trợ đa nền tảng (Windows, Linux, Android, iOS).
---

### 2.Ngôn ngữ & Công nghệ chính
<div align="center">
    
[![JAVA](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.comen/)

</div>


## 3. Hình ảnh các chức năng

## 🚀 4. Các project đã thực hiện dựa trên Platform

Một số project sinh viên đã thực hiện:
- #### [Khoá 15](./docs/projects/K15/README.md)
- #### [Khoá 16]() (Coming soon)

