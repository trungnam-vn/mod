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
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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
                if (client.currentScreen == null
                        || client.currentScreen instanceof TitleScreen
                        || client.currentScreen instanceof GameMenuScreen) {
                    client.setScreen(new MineTogetherScreen(client.currentScreen));
                }
            }
        });

        // Keep the title screen tidy: place MineTogether in the central menu
        // stack, directly below the last full-width main-menu button.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                int centerX = scaledWidth / 2;
                int lastMainButtonBottom = -1;
                int mainButtonWidth = 0;

                for (var existingButton : Screens.getButtons(screen)) {
                    int buttonCenterX = existingButton.getX() + existingButton.getWidth() / 2;
                    boolean centered = Math.abs(buttonCenterX - centerX) <= 18;
                    boolean mainMenuArea = existingButton.getY() >= scaledHeight / 4
                            && existingButton.getY() < scaledHeight * 3 / 4;
                    if (centered && mainMenuArea && existingButton.getWidth() >= 120) {
                        lastMainButtonBottom = Math.max(lastMainButtonBottom,
                                existingButton.getY() + existingButton.getHeight());
                        mainButtonWidth = Math.max(mainButtonWidth, existingButton.getWidth());
                    }
                }

                int buttonWidth = mainButtonWidth > 0
                        ? mainButtonWidth
                        : Math.min(180, scaledWidth - 24);
                int buttonY = lastMainButtonBottom >= 0
                        ? lastMainButtonBottom + 2
                        : scaledHeight / 2 + 56;
                buttonY = Math.min(buttonY, scaledHeight - 42);

                Screens.getButtons(screen).add(ButtonWidget.builder(Text.literal("MineTogether"), button ->
                                client.setScreen(new MineTogetherScreen(screen)))
                        .dimensions(centerX - buttonWidth / 2, buttonY, buttonWidth, 20)
                        .build());
            }
        });
    }
}
