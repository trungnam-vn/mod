package com.hikari.autototemplus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class AutoTotemPlusClient implements ClientModInitializer {
    private static final int MAX_RECOVERY_TICKS = 30; // 30 ticks = 1.5 seconds at 20 TPS

    private static KeyBinding toggleKey;
    private static boolean enabled = true;

    // -1 means idle. 0..MAX_RECOVERY_TICKS is the active recovery timer.
    private static int recoveryTick = -1;
    private static boolean inventoryOpenedByMod = false;
    private static boolean swapSent = false;
    private static boolean hadOffhandTotemLastTick = false;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autototemplus.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(AutoTotemPlusClient::onEndTick);
    }

    /**
     * Called by the packet mixin for the local player's totem activation.
     * Requiring a Totem in the offhand now or on the previous client tick
     * avoids refilling when a Totem was used from the main hand.
     */
    public static void onTotemUsePacket() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!enabled || client.player == null) {
            return;
        }

        boolean offhandHadTotem = client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                || hadOffhandTotemLastTick;

        if (offhandHadTotem && recoveryTick < 0) {
            recoveryTick = 0;
            inventoryOpenedByMod = false;
            swapSent = false;
        }
    }

    private static void onEndTick(MinecraftClient client) {
        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            client.inGameHud.setOverlayMessage(
                    Text.translatable(enabled
                            ? "message.autototemplus.enabled"
                            : "message.autototemplus.disabled"),
                    false
            );

            if (!enabled) {
                finishRecovery(client);
            }
        }

        if (client.player == null || client.world == null || client.interactionManager == null) {
            hadOffhandTotemLastTick = false;
            return;
        }

        hadOffhandTotemLastTick = client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);

        if (!enabled || recoveryTick < 0) {
            return;
        }

        recoveryTick++;

        // Open the normal player inventory on the first tick after activation.
        if (recoveryTick == 1) {
            if (client.currentScreen != null && !(client.currentScreen instanceof InventoryScreen)) {
                // Do not close or interrupt a chest, crafting table, or another menu.
                finishRecovery(client);
                return;
            }

            if (!(client.currentScreen instanceof InventoryScreen)) {
                client.setScreen(new InventoryScreen(client.player));
                inventoryOpenedByMod = true;
            }
        }

        // Wait for the server's offhand-empty update, then swap in a replacement.
        if (!swapSent && recoveryTick >= 2) {
            if (client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                if (recoveryTick >= MAX_RECOVERY_TICKS - 2) {
                    showNoTotemMessage(client);
                    finishRecovery(client);
                }
                return;
            }

            int inventoryIndex = findTotem(client);
            if (inventoryIndex < 0) {
                showNoTotemMessage(client);
                finishRecovery(client);
                return;
            }

            int screenSlot = inventoryIndex < 9 ? PlayerScreenHandler.HOTBAR_START + inventoryIndex : inventoryIndex;
            client.interactionManager.clickSlot(
                    client.player.currentScreenHandler.syncId,
                    screenSlot,
                    40, // SWAP button 40 targets the offhand
                    SlotActionType.SWAP,
                    client.player
            );
            swapSent = true;
        }

        // Close once the offhand is visibly refilled, or when the 1.5s budget expires.
        if (swapSent && client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            finishRecovery(client);
            return;
        }

        if (recoveryTick >= MAX_RECOVERY_TICKS) {
            finishRecovery(client);
        }
    }

    private static int findTotem(MinecraftClient client) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        return -1;
    }

    private static void showNoTotemMessage(MinecraftClient client) {
        client.inGameHud.setOverlayMessage(Text.translatable("message.autototemplus.no_totem"), false);
    }

    private static void finishRecovery(MinecraftClient client) {
        if (inventoryOpenedByMod && client.currentScreen instanceof InventoryScreen) {
            client.setScreen(null);
        }
        recoveryTick = -1;
        inventoryOpenedByMod = false;
        swapSent = false;
    }
}
