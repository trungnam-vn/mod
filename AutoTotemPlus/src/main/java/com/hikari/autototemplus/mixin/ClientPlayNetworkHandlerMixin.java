package com.hikari.autototemplus.mixin;

import com.hikari.autototemplus.AutoTotemPlusClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void autototemplus$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        if (packet.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING
                && packet.getEntity(client.world) == client.player) {
            AutoTotemPlusClient.onTotemUsePacket();
        }
    }
}
