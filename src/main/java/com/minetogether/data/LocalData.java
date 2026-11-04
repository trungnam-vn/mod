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
    private final List<String> messages = new ArrayList<>();

    private LocalData() {}

    public static LocalData load() {
        LocalData data = new LocalData();
        if (!Files.exists(FILE)) return data;

        try (Reader reader = Files.newBufferedReader(FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("friends") && root.get("friends").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("friends")) {
                    if (element.isJsonPrimitive()) data.friends.add(element.getAsString());
                }
            }
            if (root.has("messages") && root.get("messages").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("messages")) {
                    if (element.isJsonPrimitive()) data.messages.add(element.getAsString());
                }
            }
        } catch (Exception exception) {
            System.err.println("[MineTogether] Could not read local data: " + exception.getMessage());
        }
        return data;
    }

    public synchronized boolean addFriend(String name) {
        String cleaned = name == null ? "" : name.trim();
        if (cleaned.isEmpty() || cleaned.length() > 32) return false;
        boolean exists = friends.stream().anyMatch(friend -> friend.equalsIgnoreCase(cleaned));
        if (exists) return false;
        friends.add(cleaned);
        save();
        return true;
    }

    public synchronized void addMessage(String sender, String message) {
        String cleaned = message == null ? "" : message.trim();
        if (cleaned.isEmpty()) return;
        if (cleaned.length() > 180) cleaned = cleaned.substring(0, 180);
        messages.add("[" + LocalTime.now().format(TIME_FORMAT) + "] " + sender + ": " + cleaned);
        while (messages.size() > 80) messages.remove(0);
        save();
    }

    public synchronized List<String> getFriends() {
        return Collections.unmodifiableList(new ArrayList<>(friends));
    }

    public synchronized List<String> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    private synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            JsonArray friendArray = new JsonArray();
            friends.forEach(friendArray::add);
            JsonArray messageArray = new JsonArray();
            messages.forEach(messageArray::add);
            root.add("friends", friendArray);
            root.add("messages", messageArray);
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            System.err.println("[MineTogether] Could not save local data: " + exception.getMessage());
        }
    }
}
