
    
    🎓 Faculty of Information Technology (DaiNam University)
    


    Hệ thống cảnh báo thời gian thực - Weather Alert System


    
        
        
        
    





1. Giới thiệu hệ thống
Đây là một hệ thống cảnh báo thời gian thực hoạt động theo mô hình Server - Client.Server sẽ gửi các thông báo/cảnh báo thời tiết tới nhiều Client thông qua giao thức UDP.   
Hệ thống được xây dựng nhằm:

Đảm bảo truyền tải cảnh báo nhanh chóng (gần như tức thì).
Hỗ trợ nhiều Client cùng lúc (tối đa 10 client).
Dễ dàng triển khai trong các hệ thống giám sát, IoT, hoặc quản lý tập trung.

Hệ thống gồm:

Server: Quản lý danh sách client, gửi broadcast thông báo thời tiết (mưa lớn, bão, nắng nóng, v.v.).
Client: Nhận và hiển thị cảnh báo thời gian thực, ghi log vào file.


⚙️ Chức năng chính cần có
Server

Khởi tạo server UDP trên port 12345 để phát cảnh báo.
Cho phép gửi thông báo cảnh báo đến tất cả client đang kết nối qua GUI (Swing).
Gửi cảnh báo theo danh sách Client đã đăng ký.
Hỗ trợ nhiều loại cảnh báo thời tiết (Mưa lớn, Bão, Nắng nóng, Sương mù, Lốc xoáy).
Ghi log lại tất cả cảnh báo đã gửi vào giao diện GUI.

Client

Kết nối tới Server qua UDP socket và tự động đăng ký.
Nhận và hiển thị cảnh báo theo thời gian thực trên console với timestamp.
Có khả năng phân loại và hiển thị nhiều loại cảnh báo khác nhau.
Ghi log lại lịch sử cảnh báo đã nhận vào file weather_alerts.log.


2. Ngôn ngữ & Công nghệ chính

    




Ngôn ngữ: Java (JDK 8+)
Công nghệ:
UDP Socket (java.net): Giao thức truyền dữ liệu nhanh, không kết nối.
Swing (javax.swing): Tạo giao diện đồ họa cho server.
Threading (java.util.concurrent): Xử lý bất đồng bộ cho đăng ký và nhận tin.
File I/O (java.io): Ghi log cảnh báo vào file.
SimpleDateFormat (java.text): Định dạng timestamp cho thông báo.




3. Hướng dẫn chạy
Yêu cầu

JDK: Java Development Kit (JDK) phiên bản 8 trở lên.
Hệ điều hành: Windows, Linux, hoặc macOS.
Công cụ biên dịch: IDE (IntelliJ IDEA, Eclipse) hoặc dòng lệnh javac.
Cấu hình mạng: Đảm bảo các thiết bị trong cùng mạng LAN (hoặc localhost để test).

Cấu trúc dự án
Alert/
├── AlertClient.java     // Client nhận và log cảnh báo
├── AlertServer.java     // Server quản lý client và broadcast
├── AlertServerGUI.java  // GUI cho server

Bước chạy chương trình

Biên dịch code:

Mở terminal hoặc IDE, vào thư mục chứa các file .java.
Chạy lệnh:javac Alert/*.java




Chạy Server với GUI:

Trong terminal, chạy:java Alert.AlertServerGUI


Một cửa sổ GUI sẽ xuất hiện để chọn điều kiện thời tiết và gửi thông báo.


Chạy Client:

Mở terminal mới, chạy:java Alert.AlertClient


Client sẽ tự động đăng ký và chờ nhận cảnh báo. Nhấn Enter để dừng.


Gửi cảnh báo:

Trên GUI server: Chọn điều kiện, nhập vị trí, nhấn Gửi thông báo thời tiết.
Client sẽ nhận và hiển thị thông báo với timestamp, ghi vào file log.


Dừng hệ thống:

Đóng cửa sổ GUI để dừng server.
Nhấn Enter trong terminal client để dừng.



Lưu ý

Chạy server trước client.
File log weather_alerts.log được tạo trong thư mục chạy client.
Nếu chạy trên nhiều máy, thay localhost trong AlertClient.java bằng IP server.
Kiểm tra firewall để mở port 12345.


🚀 Hướng phát triển

Thêm giao diện người dùng (UI) web thay cho desktop.
Cho phép Client phản hồi lại Server (ACK hoặc gửi trạng thái).
Mã hóa dữ liệu cảnh báo để tăng tính bảo mật.
Mở rộng cho môi trường Internet (không chỉ trong LAN).
Hỗ trợ đa nền tảng (Windows, Linux, Android, iOS).
Tích hợp dữ liệu thời tiết thực tế từ API.


4. Hình ảnh các chức năng
(Hình ảnh minh họa GUI server và console client sẽ được thêm sau khi test.)
🚀 5. Các project đã thực hiện dựa trên Platform
Một số project sinh viên đã thực hiện:

Khoá 15

Khoá 16 (Coming soon)

