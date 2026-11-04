package com.minetogether.screen;

import com.minetogether.auth.MicrosoftXboxAuth;
import com.minetogether.data.LocalData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class MineTogetherScreen extends Screen {
    private enum Page { FRIENDS, MESSAGES, PLAY, WARDROBE, SCREENSHOTS, PROFILE }

    private static final int BACKGROUND = 0xD9080D18;
    private static final int PANEL = 0xFF111A2D;
    private static final int PANEL_RAISED = 0xFF192641;
    private static final int ACCENT = 0xFF8B9BFF;
    private static final int TEXT = 0xFFF4F6FF;
    private static final int MUTED = 0xFF9EABC7;
    private static final int GOOD = 0xFF7ADDB2;
    private static final int WARNING = 0xFFFFCC85;

    private final Screen parent;
    private final LocalData data = LocalData.load();
    private final Page page;
    private TextFieldWidget friendField;
    private TextFieldWidget messageField;
    private String selectedFriend = "";
    private String notice = "";

    public MineTogetherScreen(Screen parent) {
        this(parent, Page.FRIENDS);
    }

    private MineTogetherScreen(Screen parent, Page page) {
        super(Text.literal("MineTogether"));
        this.parent = parent;
        this.page = page;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(780, this.width - 24);
        int panelHeight = Math.min(440, this.height - 24);
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;
        int sideX = x + 14;
        int navY = y + 86;
        int navWidth = 142;

        addNavButton("Bạn bè", Page.FRIENDS, sideX, navY, navWidth);
        addNavButton("Tin nhắn", Page.MESSAGES, sideX, navY + 29, navWidth);
        addNavButton("Chơi cùng", Page.PLAY, sideX, navY + 58, navWidth);
        addNavButton("Tủ đồ", Page.WARDROBE, sideX, navY + 87, navWidth);
        addNavButton("Ảnh chụp", Page.SCREENSHOTS, sideX, navY + 116, navWidth);
        addNavButton("Tài khoản", Page.PROFILE, sideX, navY + 145, navWidth);
        addDrawableChild(ButtonWidget.builder(Text.literal("Đóng"), button -> close())
                .dimensions(sideX, y + panelHeight - 38, navWidth, 20).build());

        int contentX = x + 178;
        int contentWidth = panelWidth - 202;
        if (page == Page.FRIENDS) {
            friendField = new TextFieldWidget(this.textRenderer, contentX, y + 151, contentWidth - 104, 20, Text.literal("Tên người chơi"));
            friendField.setMaxLength(32);
            friendField.setPlaceholder(Text.literal("Nhập tên Minecraft..."));
            addDrawableChild(friendField);
            addDrawableChild(ButtonWidget.builder(Text.literal("Thêm bạn"), button -> addFriend())
                    .dimensions(contentX + contentWidth - 96, y + 150, 96, 22).build());
        } else if (page == Page.MESSAGES) {
            messageField = new TextFieldWidget(this.textRenderer, contentX, y + panelHeight - 76, contentWidth - 104, 20, Text.literal("Tin nhắn"));
            messageField.setMaxLength(180);
            messageField.setPlaceholder(Text.literal("Viết tin nhắn nháp..."));
            addDrawableChild(messageField);
            addDrawableChild(ButtonWidget.builder(Text.literal("Lưu"), button -> addMessage())
                    .dimensions(contentX + contentWidth - 96, y + panelHeight - 77, 96, 22).build());
        } else if (page == Page.PROFILE) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Liên kết Microsoft / Xbox"), button -> MicrosoftXboxAuth.startLogin())
                    .dimensions(contentX, y + 190, Math.min(250, contentWidth), 22).build());
        } else if (page == Page.SCREENSHOTS) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Mở thư mục ảnh chụp"), button -> openScreenshots())
                    .dimensions(contentX, y + 160, Math.min(220, contentWidth), 22).build());
        } else if (page == Page.PLAY) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Kiểm tra trạng thái thế giới"), button -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        notice = client.isInSingleplayer()
                                ? "Đang ở thế giới đơn. Hosting qua Internet chưa được bật."
                                : "Hãy vào thế giới đơn để kiểm tra trạng thái host.";
                    })
                    .dimensions(contentX, y + 207, Math.min(250, contentWidth), 22).build());
        }
    }

    private void addNavButton(String label, Page target, int x, int y, int width) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> open(target))
                .dimensions(x, y, width, 22).build());
    }

    private void open(Page target) {
        if (this.client != null) this.client.setScreen(new MineTogetherScreen(parent, target));
    }

    private void addFriend() {
        if (friendField == null) return;
        String name = friendField.getText().trim();
        if (name.isEmpty()) {
            notice = "Hãy nhập tên người chơi trước.";
        } else if (data.addFriend(name)) {
            selectedFriend = name;
            notice = "Đã lưu " + name + " vào danh sách trên máy này.";
            friendField.setText("");
        } else {
            notice = "Tên không hợp lệ hoặc đã có trong danh sách.";
        }
    }

    private void addMessage() {
        if (messageField == null) return;
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            notice = "Hãy nhập nội dung trước.";
            return;
        }
        String sender = MinecraftClient.getInstance().getSession().getUsername();
        String recipient = selectedFriend.isBlank() ? "Ghi chú cá nhân" : selectedFriend;
        data.addMessage(sender + " → " + recipient, message);
        messageField.setText("");
        notice = "Đã lưu bản nháp trên máy này, chưa gửi qua Internet.";
    }

    private void openScreenshots() {
        try {
            Path screenshots = FabricLoader.getInstance().getGameDir().resolve("screenshots");
            Files.createDirectories(screenshots);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(screenshots.toFile());
                notice = "Đã mở thư mục ảnh chụp.";
            } else {
                notice = "Không thể mở thư mục tự động: " + screenshots;
            }
        } catch (Exception exception) {
            notice = "Không mở được thư mục ảnh chụp: " + exception.getClass().getSimpleName();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, BACKGROUND);

        int panelWidth = Math.min(780, this.width - 24);
        int panelHeight = Math.min(440, this.height - 24);
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;
        int contentX = x + 178;
        int contentWidth = panelWidth - 202;

        context.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        context.fill(x, y, x + panelWidth, y + 3, ACCENT);
        context.fill(x + 164, y + 68, x + 165, y + panelHeight - 14, 0xFF2A3855);
        context.fill(x + 14, y + 52, x + panelWidth - 14, y + 53, 0xFF2A3855);

        context.drawText(this.textRenderer, Text.literal("MineTogether"), x + 22, y + 18, TEXT, false);
        context.drawText(this.textRenderer, Text.literal("Kết nối bạn bè và cùng chơi Minecraft"), x + 22, y + 35, MUTED, false);
        context.drawText(this.textRenderer, Text.literal("BẢN THỬ NGHIỆM 0.2.0"), x + panelWidth - 156, y + 22, ACCENT, false);

        String pageTitle = switch (page) {
            case FRIENDS -> "Bạn bè";
            case MESSAGES -> "Tin nhắn";
            case PLAY -> "Chơi cùng";
            case WARDROBE -> "Tủ đồ";
            case SCREENSHOTS -> "Ảnh chụp";
            case PROFILE -> "Tài khoản";
        };
        context.drawText(this.textRenderer, Text.literal(pageTitle), contentX, y + 82, TEXT, false);

        if (page == Page.FRIENDS) renderFriends(context, contentX, y, contentWidth);
        if (page == Page.MESSAGES) renderMessages(context, contentX, y, contentWidth);
        if (page == Page.PLAY) renderPlay(context, contentX, y, contentWidth);
        if (page == Page.WARDROBE) renderWardrobe(context, contentX, y, contentWidth);
        if (page == Page.SCREENSHOTS) renderScreenshots(context, contentX, y, contentWidth);
        if (page == Page.PROFILE) renderProfile(context, contentX, y, contentWidth);

        if (!notice.isBlank()) {
            context.fill(contentX - 2, y + panelHeight - 48, x + panelWidth - 18, y + panelHeight - 28, 0xFF192641);
            context.drawText(this.textRenderer, Text.literal(notice), contentX + 6, y + panelHeight - 42, WARNING, false);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderFriends(DrawContext context, int x, int y, int width) {
        context.drawText(this.textRenderer, Text.literal("Thêm người chơi bằng tên Minecraft"), x, y + 111, MUTED, false);
        context.drawText(this.textRenderer, Text.literal("ĐÃ LƯU TRÊN THIẾT BỊ"), x, y + 185, ACCENT, false);
        List<String> friends = data.getFriends();
        if (friends.isEmpty()) {
            drawWrapped(context, "Danh sách đang trống. Thêm tên người chơi ở ô phía trên để tạo danh sách riêng.", x, y + 210, width, MUTED);
        } else {
            int count = Math.min(7, friends.size());
            for (int index = 0; index < count; index++) {
                int rowY = y + 207 + index * 27;
                context.fill(x, rowY - 5, x + width, rowY + 17, 0xFF192641);
                context.fill(x + 7, rowY + 1, x + 13, rowY + 7, MUTED);
                context.drawText(this.textRenderer, Text.literal(friends.get(index)), x + 21, rowY, TEXT, false);
                context.drawText(this.textRenderer, Text.literal("Đã lưu"), x + width - 48, rowY, MUTED, false);
            }
            if (friends.size() > count) {
                context.drawText(this.textRenderer, Text.literal("Còn " + (friends.size() - count) + " người chơi khác"), x, y + 207 + count * 27, MUTED, false);
            }
        }
        drawWrapped(context, "Lưu ý: danh sách hiện là dữ liệu cục bộ, chưa gửi lời mời kết bạn và chưa hiển thị trạng thái online.", x, y + 392, width, WARNING);
    }

    private void renderMessages(DrawContext context, int x, int y, int width) {
        context.drawText(this.textRenderer, Text.literal("Trò chuyện"), x, y + 111, MUTED, false);
        List<String> messages = data.getMessages();
        if (messages.isEmpty()) {
            drawWrapped(context, "Chưa có tin nhắn. Bản này mới lưu ghi chú/bản nháp trên máy.", x, y + 151, width, MUTED);
        } else {
            int start = Math.max(0, messages.size() - 10);
            int row = 0;
            for (int index = start; index < messages.size(); index++) {
                String value = messages.get(index);
                context.fill(x, y + 146 + row * 22, x + width, y + 164 + row * 22, 0xFF192641);
                context.drawText(this.textRenderer, Text.literal(value), x + 7, y + 151 + row * 22, TEXT, false);
                row++;
            }
        }
        context.drawText(this.textRenderer, Text.literal("Bản nháp chỉ lưu trên máy, chưa được gửi cho bạn bè."), x, y + 337, WARNING, false);
    }

    private void renderPlay(DrawContext context, int x, int y, int width) {
        context.drawText(this.textRenderer, Text.literal("Mời bạn bè vào thế giới của bạn"), x, y + 116, TEXT, false);
        drawWrapped(context, "Mục tiêu của MineTogether là cho phép host thế giới và mời bạn bè tham gia như Essential.", x, y + 146, width, MUTED);
        context.fill(x, y + 205, x + width, y + 285, 0xFF192641);
        context.drawText(this.textRenderer, Text.literal("TRẠNG THÁI HOSTING"), x + 12, y + 218, ACCENT, false);
        drawWrapped(context, "Chưa bật. Hosting qua Internet cần backend kết nối P2P/ICE và TURN relay; nút host sẽ chỉ hoạt động sau khi có dịch vụ này.", x + 12, y + 240, width - 24, WARNING);
        boolean singleplayer = MinecraftClient.getInstance().isInSingleplayer();
        context.drawText(this.textRenderer, Text.literal("Thế giới hiện tại: " + (singleplayer ? "Đang chơi đơn" : "Chưa ở thế giới đơn")), x, y + 312, GOOD, false);
    }

    private void renderWardrobe(DrawContext context, int x, int y, int width) {
        context.drawText(this.textRenderer, Text.literal("Tùy chỉnh nhân vật"), x, y + 116, TEXT, false);
        drawWrapped(context, "Khu vực này được dành cho skin, trang phục, phụ kiện và emote riêng của MineTogether.", x, y + 146, width, MUTED);
        context.fill(x, y + 205, x + width, y + 282, 0xFF192641);
        context.drawText(this.textRenderer, Text.literal("TỦ ĐỒ ĐANG ĐƯỢC XÂY DỰNG"), x + 12, y + 220, ACCENT, false);
        drawWrapped(context, "Chưa có cửa hàng cosmetic hoặc emote online. Bản thử nghiệm không tự thay đổi skin tài khoản Minecraft.", x + 12, y + 244, width - 24, WARNING);
    }

    private void renderScreenshots(DrawContext context, int x, int y, int width) {
        context.drawText(this.textRenderer, Text.literal("Ảnh chụp màn hình"), x, y + 116, TEXT, false);
        drawWrapped(context, "Mở thư mục screenshots của phiên Minecraft hiện tại để xem và chia sẻ ảnh.", x, y + 146, width, MUTED);
        context.drawText(this.textRenderer, Text.literal("Tính năng xem, chỉnh sửa và gửi ảnh trong chat sẽ được bổ sung sau."), x, y + 205, WARNING, false);
    }

    private void renderProfile(DrawContext context, int x, int y, int width) {
        String playerName = MinecraftClient.getInstance().getSession().getUsername();
        context.drawText(this.textRenderer, Text.literal("Minecraft: " + playerName), x, y + 116, TEXT, false);
        context.drawText(this.textRenderer, Text.literal("Xbox: " + (MicrosoftXboxAuth.getGamertag().isBlank() ? "Chưa liên kết" : MicrosoftXboxAuth.getGamertag())), x, y + 142, TEXT, false);
        context.drawText(this.textRenderer, Text.literal("Trạng thái: " + MicrosoftXboxAuth.getStatus()), x, y + 168, MUTED, false);
        drawWrapped(context, "Đăng nhập Xbox chỉ xác minh tài khoản Microsoft/Xbox; không cấp quyền sở hữu Minecraft Java. Chỉ xác thực qua trình duyệt Microsoft, không nhập mật khẩu vào mod.", x, y + 218, width, WARNING);
        drawWrapped(context, "Để bật xác thực, cần cấu hình Microsoft Client ID công khai hợp lệ trong config/minetogether.json.", x, y + 282, width, MUTED);
    }

    private void drawWrapped(DrawContext context, String message, int x, int y, int width, int color) {
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(message), width);
        int lineY = y;
        for (OrderedText line : lines) {
            context.drawText(this.textRenderer, line, x, lineY, color, false);
            lineY += 12;
        }
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
