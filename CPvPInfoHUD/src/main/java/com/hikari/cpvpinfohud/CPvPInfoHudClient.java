package com.hikari.cpvpinfohud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class CPvPInfoHudClient implements ClientModInitializer {
    private static KeyBinding toggleHudKey;
    private static KeyBinding settingsKey;

    @Override
    public void onInitializeClient() {
        HudConfig.load();

        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cpvpinfohud.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyBinding.Category.MISC
        ));

        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cpvpinfohud.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KeyBinding.Category.MISC
        ));

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of("cpvpinfohud", "information_overlay"),
                CPvPInfoHudClient::renderHud
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHudKey.wasPressed()) {
                HudConfig.hudEnabled = !HudConfig.hudEnabled;
                HudConfig.save();
                if (client.player != null) {
                    client.inGameHud.setOverlayMessage(
                            Text.translatable(HudConfig.hudEnabled
                                    ? "message.cpvpinfohud.enabled"
                                    : "message.cpvpinfohud.disabled"),
                            false
                    );
                }
            }

            while (settingsKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new HudSettingsScreen());
                }
            }
        });
    }

    private static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!HudConfig.hudEnabled || client.player == null || client.world == null) {
            return;
        }

        int x = 8;
        int y = 8;
        int width = 184;

        if (HudConfig.armorEnabled) {
            int height = 83;
            drawPanel(context, client, x, y, width, height, "ARMOR");
            String[] labels = {"Boots", "Leggings", "Chestplate", "Helmet"};
            EquipmentSlot[] armorSlots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
            for (int i = 0; i < armorSlots.length; i++) {
                ItemStack stack = client.player.getEquippedStack(armorSlots[i]);
                int rowY = y + 20 + i * 15;
                if (stack.isEmpty()) {
                    context.drawTextWithShadow(client.textRenderer, labels[i] + ": --", x + 8, rowY, 0xFF9AA4B2);
                } else {
                    int max = stack.getMaxDamage();
                    if (max <= 0) {
                        context.drawTextWithShadow(client.textRenderer, labels[i] + ": " + shortName(stack), x + 8, rowY, 0xFFE6EDF7);
                    } else {
                        int remaining = Math.max(0, max - stack.getDamage());
                        int percent = Math.round(remaining * 100.0f / max);
                        int color = percent <= 20 ? 0xFFFF6666 : percent <= 50 ? 0xFFFFD166 : 0xFF76E0A5;
                        context.drawTextWithShadow(client.textRenderer, labels[i] + ": " + percent + "%", x + 8, rowY, color);
                        context.fill(x + 110, rowY + 5, x + 170, rowY + 9, 0xFF303846);
                        int barWidth = Math.max(0, Math.min(60, Math.round(60 * percent / 100.0f)));
                        if (barWidth > 0) {
                            context.fill(x + 110, rowY + 5, x + 110 + barWidth, rowY + 9, color);
                        }
                    }
                }
            }
            y += height + 6;
        }

        if (HudConfig.effectsEnabled) {
            List<StatusEffectInstance> effects = new ArrayList<>(client.player.getStatusEffects());
            int visible = Math.min(effects.size(), 5);
            int height = 31 + Math.max(1, visible + (effects.size() > visible ? 1 : 0)) * 14;
            drawPanel(context, client, x, y, width, height, "EFFECTS");
            if (effects.isEmpty()) {
                context.drawTextWithShadow(client.textRenderer, "No active effects", x + 8, y + 20, 0xFF9AA4B2);
            } else {
                for (int i = 0; i < visible; i++) {
                    StatusEffectInstance effect = effects.get(i);
                    String name = effect.getEffectType().value().getName().getString();
                    int seconds = Math.max(0, effect.getDuration()) / 20;
                    String duration = String.format("%d:%02d", seconds / 60, seconds % 60);
                    String level = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
                    context.drawTextWithShadow(client.textRenderer,
                            trim(name, 15) + level + "  " + duration,
                            x + 8, y + 20 + i * 14, 0xFFE6EDF7);
                }
                if (effects.size() > visible) {
                    context.drawTextWithShadow(client.textRenderer,
                            "+" + (effects.size() - visible) + " more",
                            x + 8, y + 20 + visible * 14, 0xFF9AA4B2);
                }
            }
            y += height + 6;
        }

        if (HudConfig.inventoryEnabled) {
            PlayerInventory inventory = client.player.getInventory();
            int empty = 0;
            for (int i = 0; i < 36; i++) {
                if (inventory.getStack(i).isEmpty()) {
                    empty++;
                }
            }
            int height = 72;
            drawPanel(context, client, x, y, width, height, "INVENTORY");
            context.drawTextWithShadow(client.textRenderer, "Empty slots: " + empty + " / 36", x + 8, y + 20, 0xFFE6EDF7);
            context.drawTextWithShadow(client.textRenderer, "Main hand: " + trim(client.player.getMainHandStack().isEmpty()
                    ? "Empty" : client.player.getMainHandStack().getName().getString(), 19), x + 8, y + 35, 0xFFE6EDF7);
            context.drawTextWithShadow(client.textRenderer, "Offhand: " + trim(client.player.getOffHandStack().isEmpty()
                    ? "Empty" : client.player.getOffHandStack().getName().getString(), 21), x + 8, y + 50, 0xFFE6EDF7);
        }
    }

    private static void drawPanel(DrawContext context, MinecraftClient client, int x, int y,
                                  int width, int height, String title) {
        context.fill(x, y, x + width, y + height, 0xB0101724);
        context.fill(x, y, x + width, y + 1, 0xFF627A9D);
        context.drawTextWithShadow(client.textRenderer, title, x + 8, y + 6, 0xFF9FC5FF);
    }

    private static String shortName(ItemStack stack) {
        return trim(stack.getName().getString(), 18);
    }

    private static String trim(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(1, maxLength - 1)) + "…";
    }
}
