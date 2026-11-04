package com.minetogether.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MineTogetherConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("minetogether.json");

    private String microsoftClientId = "";

    private MineTogetherConfig() {}

    public static MineTogetherConfig load() {
        MineTogetherConfig config = new MineTogetherConfig();
        try {
            if (Files.exists(FILE)) {
                try (Reader reader = Files.newBufferedReader(FILE)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("microsoftClientId") && !json.get("microsoftClientId").isJsonNull()) {
                        config.microsoftClientId = json.get("microsoftClientId").getAsString().trim();
                    }
                }
            } else {
                config.save();
            }
        } catch (Exception exception) {
            System.err.println("[MineTogether] Could not read config: " + exception.getMessage());
        }
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("microsoftClientId", microsoftClientId);
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException exception) {
            System.err.println("[MineTogether] Could not save config: " + exception.getMessage());
        }
    }

    public String getMicrosoftClientId() {
        return microsoftClientId;
    }
}
