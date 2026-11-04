package com.minetogether.screen;

import com.minetogether.auth.MicrosoftXboxAuth;
import com.minetogether.data.LocalData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.awt.Desktop;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

public final class MineTogetherScreen extends Screen {
    private enum Page { HOME, FRIENDS, MESSAGES, GROUPS, PLAY, WARDROBE, SCREENSHOTS, PROFILE }

    private static final int BACKGROUND = 0xE9060B16;
    private static final int PANEL = 0xFF10192B;
    private static final int PANEL_RAISED = 0xFF192641;
    private static final int PANEL_HOVER = 0xFF233454;
    private static final int ACCENT = 0xFF91A4FF;
    private static final int TEXT = 0xFFF4F6FF;
    private static final int MUTED = 0xFF9EABC7;
    private static final int GOOD = 0xFF7ADDB2;
    private static final int WARNING = 0xFFFFCC85;
    private static final int BORDER = 0xFF2B3B5B;

    private final Screen parent;
    private final LocalData data;
    private final Page page;
    private String selectedFriend;
    private String notice = "";
    private String lanAddress = "";
    private TextFieldWidget friendField;
    private TextFieldWidget messageField;
    private TextFieldWidget groupField;

    public MineTogetherScreen(Screen parent) {
        this(parent, Page.HOME, "");
    }

    private MineTogetherScreen(Screen parent, Page page, String selectedFriend) {
        super(Text.literal("MineTogether"));
        this.parent = parent;
        this.page = page;
        this.selectedFriend = selectedFriend == null ? "" : selectedFriend;
        this.data = LocalData.load();
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(850, this.width - 24);
        int panelHeight = Math.min(500, this.height - 20);
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;
        int sideX = x + 14;
        int navY = y + 84;
        int navWidth = 150;
        int navGap = 27;

        addNavButton("Tổng quan", Page.HOME, sideX, navY, navWidth);
        addNavButton("Bạn bè", Page.FRIENDS, sideX, navY + navGap, navWidth);
        addNavButton("Tin nhắn", Page.MESSAGES, sideX, navY + navGap * 2, navWidth);
        addNavButton("Nhóm", Page.GROUPS, sideX, navY + navGap * 3, navWidth);
        addNavButton("Chơi cùng", Page.PLAY, sideX, navY + navGap * 4, navWidth);
        addNavButton("Tủ đồ", Page.WARDROBE, sideX, navY + navGap * 5, navWidth);
        addNavButton("Ảnh chụp", Page.SCREENSHOTS, sideX, navY + navGap * 6, navWidth);
        addNavButton("Tài khoản", Page.PROFILE, sideX, navY + navGap * 7, navWidth);
        addDrawableChild(ButtonWidget.builder(Text.literal("Đóng"), button -> close())
                .dimensions(sideX, y + panelHeight - 35, navWidth, 20).build());

        int contentX = x + 184;
        int contentWidth = panelWidth - 208;

        switch (page) {
            case HOME -> initHome(contentX, y, contentWidth);
            case FRIENDS -> initFriends(contentX, y, contentWidth);
            case MESSAGES -> initMessages(contentX, y, contentWidth);
            case GROUPS -> initGroups(contentX, y, contentWidth);
            case PLAY -> initPlay(contentX, y, contentWidth);
            case WARDROBE -> initWardrobe(contentX, y, contentWidth);
            case SCREENSHOTS -> initScreenshots(contentX, y, contentWidth);
            case PROFILE -> initProfile(contentX, y, contentWidth);
        }
    }

    private void initHome(int x, int y, int width) {
        // The left navigation already provides the quick links. Keep this
        // dashboard area clear so the statistic cards never overlap buttons.
    }

    private void initFriends(int x, int y, int width) {
        friendField = new TextFieldWidget(this.textRenderer, x, y + 130, width - 112, 20, Text.literal("Tên người chơi"));
        friendField.setMaxLength(32);
        friendField.setPlaceholder(Text.literal("Tên Minecraft..."));
        addDrawableChild(friendField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Thêm"), button -> addFriend())
                .dimensions(x + width - 102, y + 129, 102, 22).build());

        List<String> friends = data.getFriends();
        int maxRows = Math.min(8, friends.size());
        for (int index = 0; index < maxRows; index++) {
            String friend = friends.get(index);
            int rowY = y + 181 + index * 31;
            addDrawableChild(ButtonWidget.builder(Text.literal("Nhắn tin"), button -> open(Page.MESSAGES, friend))
                    .dimensions(x + width - 174, rowY, 82, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Xóa"), button -> removeFriend(friend))
                    .dimensions(x + width - 86, rowY, 82, 20).build());
        }
    }

    private void initMessages(int x, int y, int width) {
        List<String> friends = data.getFriends();
        int buttonX = x;
        for (int index = 0; index < Math.min(5, friends.size()); index++) {
            String friend = friends.get(index);
            int currentX = buttonX + index * 108;
            addDrawableChild(ButtonWidget.builder(Text.literal(friend), button -> open(Page.MESSAGES, friend))
                    .dimensions(currentX, y + 126, 102, 20).build());
        }
        messageField = new TextFieldWidget(this.textRenderer, x, y + 376, width - 110, 20, Text.literal("Tin nhắn"));
        messageField.setMaxLength(180);
        messageField.setPlaceholder(Text.literal("Viết tin nhắn..."));
        addDrawableChild(messageField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Lưu"), button -> saveMessage())
                .dimensions(x + width - 100, y + 375, 100, 22).build());
    }

    private void initGroups(int x, int y, int width) {
        groupField = new TextFieldWidget(this.textRenderer, x, y + 130, width - 112, 20, Text.literal("Tên nhóm"));
        groupField.setMaxLength(32);
        groupField.setPlaceholder(Text.literal("Ví dụ: Survival Team"));
        addDrawableChild(groupField);
        addDrawableChild(ButtonWidget.builder(Text.literal("Tạo nhóm"), button -> addGroup())
                .dimensions(x + width - 102, y + 129, 102, 22).build());

        List<String> groups = data.getGroups();
        for (int index = 0; index < Math.min(8, groups.size()); index++) {
            String group = groups.get(index);
            int rowY = y + 181 + index * 31;
            addDrawableChild(ButtonWidget.builder(Text.literal("Mở chat"), button -> open(Page.MESSAGES, "Nhóm: " + group))
                    .dimensions(x + width - 174, rowY, 82, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Xóa"), button -> removeGroup(group))
                    .dimensions(x + width - 86, rowY, 82, 20).build());
        }
    }

    private void initPlay(int x, int y, int width) {
        addDrawableChild(ButtonWidget.builder(Text.literal("Mở thế giới cho mạng LAN"), button -> openLan())
                .dimensions(x, y + 184, Math.min(260, width), 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sao chép địa chỉ LAN"), button -> copyLanAddress())
                .dimensions(x, y + 214, Math.min(260, width), 22).build());
    }

    private void initWardrobe(int x, int y, int width) {
        addDrawableChild(ButtonWidget.builder(Text.literal("Quản lý skin Minecraft"), button -> openWebsite("https://www.minecraft.net/msaprofile/mygames/editskin"))
                .dimensions(x, y + 194, Math.min(260, width), 22).build());
    }

    private void initScreenshots(int x, int y, int width) {
        addDrawableChild(ButtonWidget.builder(Text.literal("Mở thư mục ảnh chụp"), button -> openScreenshots())
                .dimensions(x, y + 124, Math.min(220, width), 22).build());
        List<Path> screenshots = getScreenshots();
        for (int index = 0; index < Math.min(6, screenshots.size()); index++) {
            Path path = screenshots.get(index);
            String label = path.getFileName().toString();
            if (label.length() > 48) label = label.substring(0, 45) + "...";
            addDrawableChild(ButtonWidget.builder(Text.literal("Mở"), button -> openFile(path))
                    .dimensions(x + width - 72, y + 166 + index * 29, 68, 20).build());
        }
    }

    private void initProfile(int x, int y, int width) {
        addDrawableChild(ButtonWidget.builder(Text.literal("Liên kết tài khoản Microsoft / Xbox"), button -> MicrosoftXboxAuth.startLogin())
                .dimensions(x, y + 194, Math.min(300, width), 22).build());
    }

    private void addNavButton(String label, Page target, int x, int y, int width) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> open(target, selectedFriend))
                .dimensions(x, y, width, 22).build());
    }

    private void addContentButton(String label, Page target, int x, int y, int width) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), button -> open(target, selectedFriend))
                .dimensions(x, y, Math.min(280, width), 24).build());
    }

    private void open(Page target, String friend) {
        if (this.client != null) this.client.setScreen(new MineTogetherScreen(parent, target, friend));
    }

    private void addFriend() {
        if (friendField == null) return;
        String name = friendField.getText().trim();
        if (data.addFriend(name)) {
            notice = "Đã thêm " + name + " vào danh sách trên thiết bị.";
            friendField.setText("");
            if (this.client != null) this.client.setScreen(new MineTogetherScreen(parent, Page.FRIENDS, name));
        } else {
            notice = "Tên không hợp lệ hoặc đã có trong danh sách.";
        }
    }

    private void removeFriend(String friend) {
        data.removeFriend(friend);
        if (friend.equalsIgnoreCase(selectedFriend)) selectedFriend = "";
        if (this.client != null) this.client.setScreen(new MineTogetherScreen(parent, Page.FRIENDS, selectedFriend));
    }

    private void addGroup() {
        if (groupField == null) return;
        String group = groupField.getText().trim();
        if (data.addGroup(group)) {
            if (this.client != null) this.client.setScreen(new MineTogetherScreen(parent, Page.GROUPS, "Nhóm: " + group));
        } else {
            notice = "Tên nhóm không hợp lệ hoặc đã tồn tại.";
        }
    }

    private void removeGroup(String group) {
        data.removeGroup(group);
        if (this.client != null) this.client.setScreen(new MineTogetherScreen(parent, Page.GROUPS, selectedFriend));
    }

    private void saveMessage() {
        if (messageField == null) return;
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            notice = "Hãy nhập nội dung trước.";
            return;
        }
        String sender = MinecraftClient.getInstance().getSession().getUsername();
        String recipient = selectedFriend.isBlank() ? "Ghi chú cá nhân" : selectedFriend;
        data.addMessage(sender, recipient, message);
        messageField.setText("");
        notice = "Đã lưu bản nháp cục bộ. Chưa gửi qua Internet.";
    }

    private void openLan() {
        MinecraftClient client = MinecraftClient.getInstance();
        IntegratedServer server = client.getServer();
        if (server == null) {
            notice = "Hãy vào một thế giới đơn trước khi mở LAN.";
            return;
        }
        try {
            boolean opened = server.openToLan(GameMode.SURVIVAL, false, 0);
            if (opened) {
                lanAddress = findLanAddress() + ":" + server.getServerPort();
                notice = "Đã mở LAN tại " + lanAddress + ". Chỉ thiết bị cùng mạng có thể tham gia.";
            } else {
                notice = "Minecraft không thể mở thế giới cho mạng LAN.";
            }
        } catch (Exception exception) {
            notice = "Không mở được LAN: " + exception.getClass().getSimpleName();
        }
    }

    private void copyLanAddress() {
        if (lanAddress.isBlank()) {
            notice = "Hãy mở LAN trước.";
            return;
        }
        MinecraftClient.getInstance().keyboard.setClipboard(lanAddress);
        notice = "Đã sao chép địa chỉ LAN: " + lanAddress;
    }

    private static String findLanAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback()) continue;
                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && address.isSiteLocalAddress() && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    private List<Path> getScreenshots() {
        Path folder = FabricLoader.getInstance().getGameDir().resolve("screenshots");
        try {
            Files.createDirectories(folder);
            try (Stream<Path> stream = Files.list(folder)) {
                return stream.filter(Files::isRegularFile)
                        .filter(path -> {
                            String name = path.getFileName().toString().toLowerCase();
                            return name.endsWith(".png") || name.endsWith(".jpg")
                                    || name.endsWith(".jpeg") || name.endsWith(".webp");
                        })
                        .sorted(Comparator.comparingLong(this::lastModified).reversed())
                        .toList();
            }
        } catch (Exception exception) {
            return new ArrayList<>();
        }
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception exception) {
            return 0L;
        }
    }

    private void openScreenshots() {
        Path folder = FabricLoader.getInstance().getGameDir().resolve("screenshots");
        try {
            Files.createDirectories(folder);
            openFile(folder);
        } catch (Exception exception) {
            notice = "Không mở được thư mục ảnh chụp.";
        }
    }

    private void openFile(Path path) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(path.toFile());
            else notice = "Thiết bị không hỗ trợ mở tệp tự động.";
        } catch (Exception exception) {
            notice = "Không thể mở tệp này.";
        }
    }

    private void openWebsite(String url) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception exception) {
            notice = "Không mở được trình duyệt.";
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, BACKGROUND);

        int panelWidth = Math.min(850, this.width - 24);
        int panelHeight = Math.min(500, this.height - 20);
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;
        int contentX = x + 184;
        int contentWidth = panelWidth - 208;

        context.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        context.fill(x, y, x + panelWidth, y + 3, ACCENT);
        context.fill(x + 170, y + 65, x + 171, y + panelHeight - 14, BORDER);
        context.fill(x + 14, y + 52, x + panelWidth - 14, y + 53, BORDER);

        context.drawText(this.textRenderer, Text.literal("MineTogether"), x + 22, y + 17, TEXT, false);
        context.drawText(this.textRenderer, Text.literal("Bạn bè • Tin nhắn • Chơi cùng"), x + 22, y + 35, MUTED, false);
        context.drawText(this.textRenderer, Text.literal("0.3.0 PREVIEW"), x + panelWidth - 132, y + 22, ACCENT, false);

        String pageTitle = switch (page) {
            case HOME -> "Chào mừng trở lại";
            case FRIENDS -> "Bạn bè";
            case MESSAGES -> "Tin nhắn";
            case GROUPS -> "Nhóm";
            case PLAY -> "Chơi cùng";
            case WARDROBE -> "Tủ đồ";
            case SCREENSHOTS -> "Ảnh chụp";
            case PROFILE -> "Tài khoản";
        };
        context.drawText(this.textRenderer, Text.literal(pageTitle), contentX, y + 82, TEXT, false);

        switch (page) {
            case HOME -> renderHome(context, contentX, y, contentWidth);
            case FRIENDS -> renderFriends(context, contentX, y, contentWidth);
            case MESSAGES -> renderMessages(context, contentX, y, contentWidth);
            case GROUPS -> renderGroups(context, contentX, y, contentWidth);
            case PLAY -> renderPlay(context, contentX, y, contentWidth);
            case WARDROBE -> renderWardrobe(context, contentX, y, contentWidth);
            case SCREENSHOTS -> renderScreenshots(context, contentX, y, contentWidth);
            case PROFILE -> renderProfile(context, contentX, y, contentWidth);
        }

        if (!notice.isBlank()) {
            context.fill(contentX - 3, y + panelHeight - 48, x + panelWidth - 18, y + panelHeight - 27, PANEL_RAISED);
            context.drawText(this.textRenderer, Text.literal(notice), contentX + 6, y + panelHeight - 41, WARNING, false);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHome(DrawContext context, int x, int y, int width) {
        String username = MinecraftClient.getInstance().getSession().getUsername();
        context.drawText(this.textRenderer, Text.literal("Xin chào, " + username), x, y + 118, TEXT, false);
        context.drawText(this.textRenderer, Text.literal("Không gian kết nối Minecraft của bạn"), x, y + 140, MUTED, false);
        drawCard(context, x, y + 180, width / 3 - 10, 76, "BẠN BÈ", String.valueOf(data.getFriends().size()), ACCENT);
        drawCard(context, x + width / 3 + 2, y + 180, width / 3 - 10, 76, "NHÓM", String.valueOf(data.getGroups().size()), GOOD);
        drawCard(context, x + width * 2 / 3 + 4, y + 180, width / 3 - 10, 76, "GHI CHÚ CHAT", String.valueOf(data.getMessages().size()), WARNING);
        context.drawText(this.textRenderer, Text.literal("Truy cập nhanh"), x, y + 284, TEXT, false);
        drawWrapped(context, "Thêm bạn, lưu ghi chú trò chuyện, mở thế giới cho mạng LAN và quản lý ảnh chụp từ các mục bên trái.", x, y + 310, width, MUTED);
        drawWrapped(context, "Chat online, lời mời kết bạn và hosting P2P qua Internet cần dịch vụ MineTogether riêng; chưa được bật trong bản này.", x, y + 370, width, WARNING);
    }

    private void drawCard(DrawContext context, int x, int y, int width, int height, String label, String value, int accent) {
        context.fill(x, y, x + width, y + height, PANEL_RAISED);
        context.fill(x, y, x + 3, y + height, accent);
        context.drawText(this.textRenderer, Text.literal(label), x + 11, y + 12, MUTED, false);
        context.drawText(this.textRenderer, Text.literal(value), x + 11, y + 37, TEXT, false);
    }

    private void renderFriends(DrawContext context, int x, int y, int width) {
        context.drawText(this.textRenderer, Text.literal("Thêm tên người chơi để lưu vào danh sách của bạn"), x, y + 112, MUTED, false);
        context.drawText(this.textRenderer, Text.literal("DANH SÁCH CỦA BẠN"), x, y + 165, ACCENT, false);
        List<String> friends = data.getFriends();
        if (friends.isEmpty()) {
            drawWrapped(context, "Chưa có bạn bè. Nhập tên ở ô phía trên để bắt đầu danh sách.", x, y + 194, width, MUTED);
        } else {
            for (int index = 0; index < Math.min(8, friends.size()); index++) {
                String friend = friends.get(index);
                int rowY = y + 193 + index * 31;
                context.fill(x, rowY - 5, x + width, rowY + 20, friend.equalsIgnoreCase(selectedFriend) ? PANEL_HOVER : PANEL_RAISED);
                context.fill(x + 8, rowY + 2, x + 14, rowY + 8, MUTED);
                context.drawText(this.textRenderer, Text.literal(friend), x + 22, rowY, TEXT, false);
            }
        }
        drawWrapped(context, "Lưu ý: đây là danh sách cục bộ. Lời mời và trạng thái online sẽ hoạt động khi có máy chủ social.", x, y + 430, width, WARNING);
    }

    private void renderMessages(DrawContext context, int x, int y, int width) {
        String target = selectedFriend.isBlank() ? "Ghi chú cá nhân" : selectedFriend;
        context.drawText(this.textRenderer, Text.literal("Cuộc trò chuyện: " + target), x, y + 110, ACCENT, false);
        context.drawText(this.textRenderer, Text.literal("Chọn người nhận ở hàng nút phía trên hoặc từ trang Bạn bè."), x, y + 151, MUTED, false);
        List<String> messages = data.getMessages();
        if (messages.isEmpty()) {
            drawWrapped(context, "Chưa có nội dung. Bạn có thể lưu ghi chú tại ô phía dưới.", x, y + 190, width, MUTED);
        } else {
            int start = Math.max(0, messages.size() - 10);
            int row = 0;
            for (int index = start; index < messages.size(); index++) {
                int rowY = y + 166 + row * 23;
                context.fill(x, rowY, x + width, rowY + 19, PANEL_RAISED);
                drawWrapped(context, messages.get(index), x + 7, rowY + 4, width - 14, TEXT);
                row++;
            }
        }
        context.drawText(this.textRenderer, Text.literal("Ghi chú được lưu trên máy, chưa gửi cho người khác."), x, y + 339, WARNING, false);
    }

    private void renderGroups(DrawContext context, int x, int y, int width) {
        context.drawText(this.textRenderer, Text.literal("Tạo nhóm để sắp xếp bạn bè"), x, y + 112, MUTED, false);
        context.drawText(this.textRenderer, Text.literal("NHÓM ĐÃ LƯU"), x, y + 165, ACCENT, false);
        List<String> groups = data.getGroups();
        if (groups.isEmpty()) {
            drawWrapped(context, "Chưa có nhóm nào. Nhập tên nhóm ở ô phía trên.", x, y + 194, width, MUTED);
        } else {
            for (int index = 0; index < Math.min(8, groups.size()); index++) {
                int rowY = y + 193 + index * 31;
                context.fill(x, rowY - 5, x + width, rowY + 20, PANEL_RAISED);
                context.drawText(this.textRenderer, Text.literal(groups.get(index)), x + 10, rowY, TEXT, false);
            }
        }
        drawWrapped(context, "Nhóm hiện được lưu cục bộ. Chat nhóm đồng bộ online sẽ cần backend.", x, y + 430, width, WARNING);
    }

    private void renderPlay(DrawContext context, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean inSingleplayer = client.getServer() != null;
        context.drawText(this.textRenderer, Text.literal("Chia sẻ thế giới hiện tại"), x, y + 116, TEXT, false);
        drawWrapped(context, "Mở LAN cho những người chơi khác đang ở cùng mạng Wi-Fi/LAN. Chức năng này không tự mở kết nối qua Internet.", x, y + 145, width, MUTED);
        context.fill(x, y + 250, x + width, y + 328, PANEL_RAISED);
        context.drawText(this.textRenderer, Text.literal("TRẠNG THÁI"), x + 12, y + 263, ACCENT, false);
        context.drawText(this.textRenderer, Text.literal(inSingleplayer ? "Đang ở thế giới đơn" : "Chưa mở thế giới đơn"), x + 12, y + 286, inSingleplayer ? GOOD : WARNING, false);
        if (!lanAddress.isBlank()) {
            context.drawText(this.textRenderer, Text.literal("Địa chỉ LAN: " + lanAddress), x, y + 350, GOOD, false);
        }
        drawWrapped(context, "Hosting P2P như Essential cần thêm máy chủ điều phối ICE/TURN và relay; chưa có trong bản 0.3.0.", x, y + 405, width, WARNING);
    }

    private void renderWardrobe(DrawContext context, int x, int y, int width) {
        context.drawText(this.textRenderer, Text.literal("Skin Minecraft"), x, y + 116, TEXT, false);
        drawWrapped(context, "Quản lý skin tài khoản Minecraft bằng trang chính thức. Việc thay skin cần tài khoản có quyền sở hữu Minecraft Java.", x, y + 146, width, MUTED);
        context.fill(x, y + 219, x + width, y + 302, PANEL_RAISED);
        context.drawText(this.textRenderer, Text.literal("COSMETIC & EMOTE"), x + 12, y + 233, ACCENT, false);
        drawWrapped(context, "Tủ đồ cosmetic, áo choàng, phụ kiện và emote đồng bộ online cần hệ thống tài khoản và tài nguyên riêng. Chưa bật trong bản này.", x + 12, y + 258, width - 24, WARNING);
    }

    private void renderScreenshots(DrawContext context, int x, int y, int width) {
        context.drawText(this.textRenderer, Text.literal("Ảnh chụp gần đây"), x, y + 116, TEXT, false);
        List<Path> screenshots = getScreenshots();
        if (screenshots.isEmpty()) {
            drawWrapped(context, "Chưa tìm thấy ảnh chụp. Ảnh Minecraft tạo ra sẽ xuất hiện ở đây.", x, y + 164, width, MUTED);
        } else {
            for (int index = 0; index < Math.min(6, screenshots.size()); index++) {
                String filename = screenshots.get(index).getFileName().toString();
                int rowY = y + 165 + index * 29;
                context.fill(x, rowY - 4, x + width, rowY + 18, PANEL_RAISED);
                if (filename.length() > 62) filename = filename.substring(0, 59) + "...";
                context.drawText(this.textRenderer, Text.literal(filename), x + 8, rowY + 2, TEXT, false);
            }
        }
        drawWrapped(context, "Bạn có thể mở ảnh bằng nút bên cạnh tên tệp. Chỉnh sửa và chia sẻ trực tiếp trong chat sẽ được bổ sung cùng backend.", x, y + 390, width, WARNING);
    }

    private void renderProfile(DrawContext context, int x, int y, int width) {
        MinecraftClient client = MinecraftClient.getInstance();
        String playerName = client.getSession().getUsername();
        context.drawText(this.textRenderer, Text.literal("Minecraft: " + playerName), x, y + 116, TEXT, false);
        context.drawText(this.textRenderer, Text.literal("Xbox: " + (MicrosoftXboxAuth.getGamertag().isBlank() ? "Chưa liên kết" : MicrosoftXboxAuth.getGamertag())), x, y + 145, TEXT, false);
        context.drawText(this.textRenderer, Text.literal("Trạng thái: " + MicrosoftXboxAuth.getStatus()), x, y + 174, MUTED, false);
        drawWrapped(context, "Liên kết Xbox xác minh tài khoản Microsoft/Xbox, không tự cấp quyền sở hữu Minecraft Java. Chỉ đăng nhập trên trình duyệt Microsoft.", x, y + 224, width, WARNING);
        drawWrapped(context, "Để bật liên kết Xbox, cần cấu hình Microsoft Client ID hợp lệ trong config/minetogether.json.", x, y + 300, width, MUTED);
    }

    private void drawWrapped(DrawContext context, String message, int x, int y, int width, int color) {
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(message), Math.max(50, width));
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
