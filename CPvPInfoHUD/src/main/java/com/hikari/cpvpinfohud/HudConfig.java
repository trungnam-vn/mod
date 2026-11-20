package com.hikari.cpvpinfohud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cpvp-info-hud.json");

    public static boolean hudEnabled = true;
    public static boolean armorEnabled = true;
    public static boolean effectsEnabled = true;
    public static boolean inventoryEnabled = true;

    private HudConfig() {}

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }

        try {
            StoredConfig stored = GSON.fromJson(Files.readString(FILE), StoredConfig.class);
            if (stored != null) {
                hudEnabled = stored.hudEnabled;
                armorEnabled = stored.armorEnabled;
                effectsEnabled = stored.effectsEnabled;
                inventoryEnabled = stored.inventoryEnabled;
            }
        } catch (Exception ignored) {
            // Keep defaults if the config is damaged or unreadable.
        }
    }

    public static void save() {
        StoredConfig stored = new StoredConfig();
        stored.hudEnabled = hudEnabled;
        stored.armorEnabled = armorEnabled;
        stored.effectsEnabled = effectsEnabled;
        stored.inventoryEnabled = inventoryEnabled;

        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(stored));
        } catch (IOException ignored) {
            // The HUD remains usable for this session if saving fails.
        }
    }

    private static final class StoredConfig {
        boolean hudEnabled = true;
        boolean armorEnabled = true;
        boolean effectsEnabled = true;
        boolean inventoryEnabled = true;
    }
}
