package com.minetogether.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LocalData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("minetogether-data.json");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final List<String> friends = new ArrayList<>();
    private final List<String> groups = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();

    private LocalData() {}

    public static LocalData load() {
        LocalData data = new LocalData();
        if (!Files.exists(FILE)) return data;

        try (Reader reader = Files.newBufferedReader(FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            readStringArray(root, "friends", data.friends);
            readStringArray(root, "groups", data.groups);
            readStringArray(root, "messages", data.messages);
        } catch (Exception exception) {
            System.err.println("[MineTogether] Could not read local data: " + exception.getMessage());
        }
        return data;
    }

    private static void readStringArray(JsonObject root, String key, List<String> target) {
        if (!root.has(key) || !root.get(key).isJsonArray()) return;
        for (JsonElement element : root.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) target.add(element.getAsString());
        }
    }

    public synchronized boolean addFriend(String name) {
        String cleaned = cleanName(name);
        if (cleaned.isEmpty() || existsIgnoreCase(friends, cleaned)) return false;
        friends.add(cleaned);
        save();
        return true;
    }

    public synchronized boolean removeFriend(String name) {
        boolean removed = friends.removeIf(friend -> friend.equalsIgnoreCase(name));
        if (removed) save();
        return removed;
    }

    public synchronized boolean addGroup(String name) {
        String cleaned = cleanName(name);
        if (cleaned.isEmpty() || existsIgnoreCase(groups, cleaned)) return false;
        groups.add(cleaned);
        save();
        return true;
    }

    public synchronized boolean removeGroup(String name) {
        boolean removed = groups.removeIf(group -> group.equalsIgnoreCase(name));
        if (removed) save();
        return removed;
    }

    public synchronized void addMessage(String sender, String recipient, String message) {
        String cleaned = message == null ? "" : message.trim();
        if (cleaned.isEmpty()) return;
        if (cleaned.length() > 180) cleaned = cleaned.substring(0, 180);
        String safeSender = sender == null ? "Người chơi" : sender.trim();
        String safeRecipient = recipient == null ? "Ghi chú" : recipient.trim();
        messages.add("[" + LocalTime.now().format(TIME_FORMAT) + "] " + safeSender + " → " + safeRecipient + ": " + cleaned);
        while (messages.size() > 120) messages.remove(0);
        save();
    }

    public synchronized List<String> getFriends() {
        return Collections.unmodifiableList(new ArrayList<>(friends));
    }

    public synchronized List<String> getGroups() {
        return Collections.unmodifiableList(new ArrayList<>(groups));
    }

    public synchronized List<String> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    private static String cleanName(String value) {
        if (value == null) return "";
        String cleaned = value.trim();
        return cleaned.length() > 32 ? cleaned.substring(0, 32) : cleaned;
    }

    private static boolean existsIgnoreCase(List<String> values, String candidate) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(candidate));
    }

    private synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            root.add("friends", toJsonArray(friends));
            root.add("groups", toJsonArray(groups));
            root.add("messages", toJsonArray(messages));
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            System.err.println("[MineTogether] Could not save local data: " + exception.getMessage());
        }
    }

    private static JsonArray toJsonArray(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }
}
