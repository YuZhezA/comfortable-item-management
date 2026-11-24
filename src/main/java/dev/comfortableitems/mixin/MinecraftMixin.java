package dev.comfortableitems.mixin;

import dev.comfortableitems.Refill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Shadow public LocalPlayer player;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        Refill.tick((Minecraft) (Object) this);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void use(CallbackInfo ci) {
        if (player == null) return;
        Refill.use(player, InteractionHand.MAIN_HAND);
        Refill.use(player, InteractionHand.OFF_HAND);
    }

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void attack(CallbackInfoReturnable<Boolean> ci) {
        if (player != null) Refill.use(player, InteractionHand.MAIN_HAND);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"))
    private void attack(boolean continuing, CallbackInfo ci) {
        if (continuing && player != null) Refill.use(player, InteractionHand.MAIN_HAND);
    }
}
