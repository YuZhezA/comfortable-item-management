package dev.comfortableitems.mixin;

import dev.comfortableitems.Refill;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @Inject(method = "drop", at = @At("HEAD"))
    private void drop(boolean wholeStack, CallbackInfoReturnable<Boolean> ci) {
        Refill.dropped((LocalPlayer) (Object) this);
    }
}
