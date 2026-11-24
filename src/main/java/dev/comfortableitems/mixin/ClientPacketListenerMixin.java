package dev.comfortableitems.mixin;

import dev.comfortableitems.Refill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    private static final int DEATH_PROTECTION = 35;

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void comfortableItems$deathProtection(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (packet.getEventId() != DEATH_PROTECTION) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && packet.getEntity(player.level()) == player) Refill.deathProtection(player);
    }
}
