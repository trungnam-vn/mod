package com.minetogether.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minetogether.config.MineTogetherConfig;
import net.minecraft.client.MinecraftClient;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class MicrosoftXboxAuth {
    private static final String DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String XBL_USER_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String XBOX_PROFILE_URL = "https://profile.xboxlive.com/users/me/profile/settings?settings=GameDisplayName";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final AtomicReference<String> STATUS = new AtomicReference<>("Chưa đăng nhập Xbox");
    private static final AtomicReference<String> GAMERTAG = new AtomicReference<>("");
    private static volatile CompletableFuture<Void> currentLogin;

    private MicrosoftXboxAuth() {}

    public static void startLogin() {
        if (!RUNNING.compareAndSet(false, true)) {
            STATUS.set("Đang chờ xác thực trong trình duyệt...");
            return;
        }
        String clientId = MineTogetherConfig.load().getMicrosoftClientId();
        if (clientId.isBlank()) {
            RUNNING.set(false);
            STATUS.set("Thiếu Microsoft Client ID trong config/minetogether.json");
            return;
        }

        STATUS.set("Đang tạo mã đăng nhập Microsoft...");
        currentLogin = CompletableFuture.runAsync(() -> {
            try {
                authenticate(clientId);
            } catch (Exception exception) {
                STATUS.set(friendlyError(exception));
            } finally {
                RUNNING.set(false);
            }
        });
    }

    public static String getStatus() {
        return STATUS.get();
    }

    public static String getGamertag() {
        return GAMERTAG.get();
    }

    private static void authenticate(String clientId) throws Exception {
        JsonObject device = postForm(DEVICE_CODE_URL,
                "client_id=" + form(clientId) + "&scope=" + form("XboxLive.signin offline_access"));

        String deviceCode = requiredString(device, "device_code");
        String userCode = requiredString(device, "user_code");
        String verifyUri = device.has("verification_uri")
                ? device.get("verification_uri").getAsString()
                : "https://www.microsoft.com/link";
        int interval = device.has("interval") ? device.get("interval").getAsInt() : 5;
        int expiresIn = device.has("expires_in") ? device.get("expires_in").getAsInt() : 900;

        STATUS.set("Mở " + verifyUri + " và nhập mã: " + userCode);
        openBrowser(verifyUri);

        String accessToken = pollForMicrosoftToken(clientId, deviceCode, interval, expiresIn);
        STATUS.set("Đã xác thực Microsoft. Đang kiểm tra tài khoản Xbox...");

        JsonObject userTokenResponse = postJson(XBL_USER_AUTH_URL, xboxUserAuthBody(accessToken));
        String userToken = requiredString(userTokenResponse, "Token");
        JsonObject xstsResponse = postJson(XSTS_URL, xstsBody(userToken));
        String xstsToken = requiredString(xstsResponse, "Token");
        String userHash = firstXuiValue(xstsResponse, "uhs");

        String gamertag = requestGamertag(userHash, xstsToken);
        if (gamertag == null || gamertag.isBlank()) gamertag = "Xbox account linked";
        GAMERTAG.set(gamertag);
        STATUS.set("Đã liên kết Xbox: " + gamertag);

        accessToken = null;
        userToken = null;
        xstsToken = null;
    }

    private static String pollForMicrosoftToken(String clientId, String deviceCode, int interval, int expiresIn) throws Exception {
        long deadline = System.currentTimeMillis() + Math.max(60, expiresIn) * 1000L;
        int pollSeconds = Math.max(2, interval);
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(pollSeconds * 1000L);
            String body = "grant_type=" + form("urn:ietf:params:oauth:grant-type:device_code")
                    + "&client_id=" + form(clientId)
                    + "&device_code=" + form(deviceCode);
            HttpResponse<String> response = sendForm(TOKEN_URL, body);
            JsonObject json = parseObject(response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300 && json.has("access_token")) {
                return json.get("access_token").getAsString();
            }
            String error = json.has("error") ? json.get("error").getAsString() : "";
            if ("authorization_pending".equals(error)) continue;
            if ("slow_down".equals(error)) {
                pollSeconds += 5;
                continue;
            }
            if ("authorization_declined".equals(error)) throw new IllegalStateException("Bạn đã từ chối đăng nhập Microsoft.");
            if ("expired_token".equals(error) || "device_code_expired".equals(error)) {
                throw new IllegalStateException("Mã đăng nhập đã hết hạn. Hãy bấm đăng nhập lại.");
            }
            throw new IllegalStateException("Microsoft OAuth trả về: " + safeError(json, response.statusCode()));
        }
        throw new IllegalStateException("Mã đăng nhập đã hết hạn. Hãy bấm đăng nhập lại.");
    }

    private static JsonObject xboxUserAuthBody(String accessToken) {
        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "d=" + accessToken);
        JsonObject body = new JsonObject();
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "http://auth.xboxlive.com");
        body.addProperty("TokenType", "JWT");
        return body;
    }

    private static JsonObject xstsBody(String userToken) {
        JsonObject properties = new JsonObject();
        JsonArray tokens = new JsonArray();
        tokens.add(userToken);
        properties.add("UserTokens", tokens);
        properties.addProperty("SandboxId", "RETAIL");
        JsonObject body = new JsonObject();
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "http://xboxlive.com");
        body.addProperty("TokenType", "JWT");
        return body;
    }

    private static String requestGamertag(String userHash, String xstsToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(XBOX_PROFILE_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "XBL3.0 x=" + userHash + ";" + xstsToken)
                    .header("x-xbl-contract-version", "2")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return "";
            JsonObject root = parseObject(response.body());
            JsonArray users = root.getAsJsonArray("profileUsers");
            if (users == null || users.isEmpty()) return "";
            JsonArray settings = users.get(0).getAsJsonObject().getAsJsonArray("settings");
            if (settings == null || settings.isEmpty()) return "";
            for (int index = 0; index < settings.size(); index++) {
                JsonObject setting = settings.get(index).getAsJsonObject();
                if (setting.has("id") && "GameDisplayName".equals(setting.get("id").getAsString())) {
                    return setting.has("value") ? setting.get("value").getAsString() : "";
                }
            }
        } catch (Exception ignored) {
            // Profile lookup is optional; successful XSTS auth still confirms the account.
        }
        return "";
    }

    private static String firstXuiValue(JsonObject response, String key) {
        JsonObject claims = response.getAsJsonObject("DisplayClaims");
        if (claims == null) throw new IllegalStateException("Xbox không trả về thông tin tài khoản.");
        JsonArray xui = claims.getAsJsonArray("xui");
        if (xui == null || xui.isEmpty()) throw new IllegalStateException("Xbox không trả về mã người dùng.");
        JsonObject user = xui.get(0).getAsJsonObject();
        if (!user.has(key)) throw new IllegalStateException("Xbox không trả về thông tin xác thực cần thiết.");
        return user.get(key).getAsString();
    }

    private static JsonObject postForm(String url, String body) throws Exception {
        HttpResponse<String> response = sendForm(url, body);
        JsonObject json = parseObject(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Không thể bắt đầu đăng nhập Microsoft: " + safeError(json, response.statusCode()));
        }
        return json;
    }

    private static HttpResponse<String> sendForm(String url, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject postJson(String url, JsonObject body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("x-xbl-contract-version", "1")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = parseObject(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String xerr = json.has("XErr") ? json.get("XErr").getAsString() : "";
            if (response.statusCode() == 401 || !xerr.isBlank()) {
                throw new IllegalStateException("Xbox không chấp nhận phiên đăng nhập. Tài khoản có thể cần quyền gia đình/độ tuổi hoặc quyền Xbox phù hợp; hãy kiểm tra cài đặt chính thức.");
            }
            throw new IllegalStateException("Xbox trả về lỗi xác thực (HTTP " + response.statusCode() + ").");
        }
        return json;
    }

    private static JsonObject parseObject(String body) {
        try {
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception exception) {
            return new JsonObject();
        }
    }

    private static String requiredString(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) return json.get(key).getAsString();
        throw new IllegalStateException("Phản hồi đăng nhập thiếu trường " + key + ".");
    }

    private static String safeError(JsonObject json, int statusCode) {
        if (json.has("error_description")) return json.get("error_description").getAsString();
        if (json.has("error")) return json.get("error").getAsString();
        return "HTTP " + statusCode;
    }

    private static String form(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception exception) {
            MinecraftClient.getInstance().execute(() -> STATUS.set("Hãy mở trình duyệt và truy cập " + url));
        }
    }

    private static String friendlyError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "Đăng nhập thất bại. Hãy thử lại sau.";
        return message.length() > 180 ? message.substring(0, 180) : message;
    }
}
