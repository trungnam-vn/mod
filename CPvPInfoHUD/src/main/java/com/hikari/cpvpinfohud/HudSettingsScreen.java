package com.hikari.cpvpinfohud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class HudSettingsScreen extends Screen {
    public HudSettingsScreen() {
        super(Text.literal("CPvP Info HUD"));
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int buttonWidth = 210;
        int left = center - buttonWidth / 2;
        int y = Math.max(62, this.height / 2 - 64);

        addToggleButton(left, y, buttonWidth, "HUD tổng: ", HudConfig.hudEnabled,
                () -> HudConfig.hudEnabled = !HudConfig.hudEnabled);
        y += 28;
        addToggleButton(left, y, buttonWidth, "Armor HUD: ", HudConfig.armorEnabled,
                () -> HudConfig.armorEnabled = !HudConfig.armorEnabled);
        y += 28;
        addToggleButton(left, y, buttonWidth, "Potion HUD: ", HudConfig.effectsEnabled,
                () -> HudConfig.effectsEnabled = !HudConfig.effectsEnabled);
        y += 28;
        addToggleButton(left, y, buttonWidth, "Inventory HUD: ", HudConfig.inventoryEnabled,
                () -> HudConfig.inventoryEnabled = !HudConfig.inventoryEnabled);
        y += 38;

        addDrawableChild(ButtonWidget.builder(Text.literal("Lưu và đóng"), button -> {
            HudConfig.save();
            close();
        }).dimensions(left, y, buttonWidth, 20).build());
    }

    private void addToggleButton(int x, int y, int width, String label, boolean enabled, Runnable toggle) {
        addDrawableChild(ButtonWidget.builder(
                Text.literal(label + (enabled ? "BẬT" : "TẮT")),
                button -> {
                    toggle.run();
                    HudConfig.save();
                    client.setScreen(new HudSettingsScreen());
                }
        ).dimensions(x, y, width, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 28, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("G: bật/tắt HUD   |   H: mở cài đặt"),
                width / 2, 45, 0xFFB9C8DD);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(null);
        }
    }
}
