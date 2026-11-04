# MineTogether 0.2.0 — Fabric 1.21.1

MineTogether là một bản thử nghiệm mod social hub cho Minecraft Java Edition, lấy cảm hứng từ nhóm tính năng phổ biến trong các mod social như Essential. Giao diện, tên, màu sắc và tài nguyên của MineTogether là riêng; dự án không sao chép mã nguồn hay tài sản thương hiệu của Essential.

## Có trong bản 0.2.0

- Phím `M` mở giao diện MineTogether trong game.
- Nút MineTogether trên màn hình chính và màn hình tạm dừng.
- Giao diện trung tâm gồm Bạn bè, Tin nhắn, Chơi cùng, Tủ đồ, Ảnh chụp và Tài khoản.
- Lưu tên người chơi và bản nháp tin nhắn ở máy hiện tại.
- Mở thư mục ảnh chụp màn hình từ menu.
- Trang tài khoản có luồng đăng nhập Microsoft/Xbox qua trình duyệt; cần cấu hình Client ID công khai hợp lệ.

## Chưa có trong bản này

Các mục dưới đây là phần dự kiến, không phải tính năng đã hoạt động:

- Kết bạn thật, trạng thái online và tin nhắn thời gian thực.
- Chat nhóm, chia sẻ hình ảnh và lời mời tham gia trò chơi.
- Host thế giới qua Internet kiểu peer-to-peer.
- Tủ đồ, cosmetic và emote đồng bộ online.

Các tính năng online cần backend riêng, cơ sở dữ liệu, dịch vụ xác thực, cùng hệ thống kết nối peer-to-peer có STUN/TURN/ICE. Không thể tạo hosting Internet chỉ bằng một file mod. Hosting trên máy chủ của chủ thế giới phải có chủ thế giới online và cần xử lý kết nối mạng an toàn.

## Yêu cầu

- Minecraft Java `1.21.1`
- Fabric Loader `0.16.14` trở lên
- Fabric API `0.116.17+1.21.1`
- JDK `21` (64-bit)

## Build trên Windows

1. Giải nén dự án vào một thư mục bình thường (không đặt bên trong `.minecraft/mods`).
2. Nhấp đúp `build.bat`.
3. Script sẽ tìm JDK 21; nếu chưa có, nó tải Temurin JDK 21 vào thư mục người dùng. Script cũng tải Gradle 8.10.2 và chạy Fabric Loom để compile + remap JAR.
4. Khi build thành công, JAR được sao chép vào `release\MineTogether-0.2.0.jar`.
5. Xóa các JAR MineTogether cũ khỏi thư mục `mods`, chỉ giữ một bản 0.2.0. Giữ Fabric API tương thích.

Lần build đầu cần kết nối Internet để tải JDK, Gradle và thư viện Minecraft/Fabric. Nếu build lỗi, gửi toàn bộ phần cuối cửa sổ build để kiểm tra.

## Microsoft / Xbox

Đăng nhập Microsoft/Xbox chỉ xác minh tài khoản Xbox và không tự cấp quyền sở hữu Minecraft Java. Không nhập mật khẩu vào mod; chỉ xác thực trên trang Microsoft. Chức năng này cần Application (client) ID công khai hợp lệ được đăng ký theo luồng public-client/device-code của Microsoft. Mod không lưu access token ra file.

## Ghi chú phát triển

MineTogether hiện là prototype giao diện và lưu dữ liệu cục bộ. Không quảng bá đây là bản thay thế Essential hoàn chỉnh cho tới khi backend, chat, invites và hosting được phát triển và kiểm thử độc lập.
