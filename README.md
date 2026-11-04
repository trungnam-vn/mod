# MineTogether 0.3.0 — Fabric 1.21.1

MineTogether là mod social hub cho Minecraft Java Edition, lấy cảm hứng từ nhóm tính năng của các mod social. MineTogether có giao diện, tên và tài nguyên riêng; dự án không sao chép mã nguồn hay tài sản thương hiệu Essential.

## Tính năng hoạt động trong 0.3.0

- Phím `M` mở menu MineTogether.
- Nút MineTogether ở menu chính, đặt cùng cột nút trung tâm.
- Giao diện riêng có Tổng quan, Bạn bè, Tin nhắn, Nhóm, Chơi cùng, Tủ đồ, Ảnh chụp và Tài khoản.
- Lưu danh sách bạn bè, nhóm và ghi chú trò chuyện ở máy hiện tại.
- Mở thế giới chơi đơn cho mạng LAN và sao chép địa chỉ LAN.
- Liệt kê ảnh chụp gần đây, mở thư mục hoặc mở từng ảnh.
- Mở trang chính thức để quản lý skin Minecraft.
- Luồng liên kết Microsoft/Xbox qua trình duyệt; cần cấu hình Client ID hợp lệ.

## Chưa có trong 0.3.0

- Kết bạn thật, trạng thái online, tin nhắn trực tuyến, chat nhóm và gửi ảnh.
- Mời bạn bè vào phiên chơi qua Internet.
- Hosting P2P qua Internet kiểu Essential.
- Cosmetic, áo choàng và emote đồng bộ online.

Các tính năng social online cần backend, cơ sở dữ liệu, xác thực và bảo vệ tài khoản. Hosting P2P cần ICE/STUN/TURN relay và hạ tầng vận hành. Mở LAN trong 0.3.0 chỉ dành cho thiết bị cùng mạng; không phải hosting Internet.

## Yêu cầu

- Minecraft Java `1.21.1`
- Fabric Loader `0.16.14` trở lên
- Fabric API `0.116.17+1.21.1`
- Java `21`

## Build trên Windows

1. Giải nén dự án vào thư mục bình thường.
2. Chạy `build.bat`.
3. Script tìm JDK 21; nếu chưa có thì tải Temurin JDK 21 vào thư mục người dùng. Script tải Gradle 8.10.2 và build/remap bằng Fabric Loom.
4. JAR đầu ra: `release\MineTogether-0.3.0.jar`.
5. Xóa MineTogether phiên bản cũ khỏi `mods`, chỉ giữ một JAR mới.

Lần build đầu cần Internet để tải JDK, Gradle và các thư viện Minecraft/Fabric.

## Tài khoản Microsoft/Xbox

Đăng nhập Microsoft/Xbox chỉ xác minh tài khoản Xbox và không tự cấp quyền sở hữu Minecraft Java. Không nhập mật khẩu trong mod; chỉ xác thực trên trang Microsoft. Tính năng này cần Application Client ID công khai hợp lệ.
