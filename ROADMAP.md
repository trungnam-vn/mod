# MineTogether — kế hoạch phát triển

## Mốc 0.2.0 — Social hub prototype
- [x] Mở menu bằng phím M.
- [x] Có nút truy cập ở title screen và pause menu.
- [x] Các trang Bạn bè, Tin nhắn, Chơi cùng, Tủ đồ, Ảnh chụp, Tài khoản.
- [x] Lưu danh sách tên và bản nháp cục bộ.
- [x] Mở thư mục ảnh chụp.
- [x] Giao diện hồ sơ và luồng xác thực Microsoft/Xbox (cần cấu hình client ID).

## Mốc 0.3 — Tài khoản và xã hội online
- [ ] Dịch vụ backend HTTPS và cơ sở dữ liệu.
- [ ] Đăng ký/đăng nhập MineTogether, liên kết danh tính Xbox theo cách được Microsoft cho phép.
- [ ] Lời mời kết bạn, chấp nhận/từ chối, chặn và báo cáo.
- [ ] Trạng thái online/offline và thời gian hoạt động gần nhất có tùy chọn riêng tư.
- [ ] Tin nhắn riêng, chat nhóm, thông báo và đồng bộ nhiều thiết bị.
- [ ] Giới hạn tốc độ, lọc nội dung, báo cáo và bảo vệ tài khoản.

## Mốc 0.4 — Chơi cùng
- [ ] Gửi/nhận lời mời thế giới và server.
- [ ] Hiển thị tên thế giới/server, số người chơi và trạng thái phiên.
- [ ] Lưu lịch sử lời mời và nút tham gia lại.

## Mốc 0.5 — Hosting P2P
- [ ] Thiết kế host session và cơ chế phân quyền.
- [ ] ICE/STUN/TURN, relay fallback và kiểm tra NAT.
- [ ] Chỉ cho người được mời kết nối; mã phiên hết hạn và chống lạm dụng.
- [ ] Kiểm thử với modpack, Sodium/Iris và giới hạn băng thông.

## Mốc 0.6 — Tùy chỉnh và media
- [ ] Thư viện skin/cosmetic theo quyền sử dụng rõ ràng.
- [ ] Emote, tùy chọn ẩn/hiện và đồng bộ an toàn.
- [ ] Xem, cắt, quản lý và chia sẻ screenshots trong chat.

## Lưu ý

Các tính năng online cần hạ tầng máy chủ và kiểm thử mạng riêng. Một mod jar đơn lẻ không thể tự cung cấp relay P2P hoặc dịch vụ chat cho mọi người chơi. Tài khoản Xbox miễn phí không thay thế giấy phép Minecraft Java. Mọi dịch vụ tài khoản phải tuân thủ quy định Microsoft/Xbox và quyền riêng tư của người chơi.
