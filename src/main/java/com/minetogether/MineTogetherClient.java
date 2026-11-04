package com.minetogether;

import com.minetogether.config.MineTogetherConfig;
import com.minetogether.data.LocalData;
import com.minetogether.screen.MineTogetherScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.lwjgl.glfw.GLFW;

public final class MineTogetherClient implements ClientModInitializer {
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        MineTogetherConfig.load();
        LocalData.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.minetogether.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.minetogether.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MineTogetherScreen(null));
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen || screen instanceof GameMenuScreen) {
                int buttonWidth = 116;
                int x = scaledWidth - buttonWidth - 8;
                int y = 8;
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("MineTogether"), button ->
                                client.setScreen(new MineTogetherScreen(screen)))
                        .dimensions(x, y, buttonWidth, 20)
                        .build());
            }
        });
    }
}
