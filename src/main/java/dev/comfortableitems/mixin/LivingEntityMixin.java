package dev.comfortableitems.mixin;

import dev.comfortableitems.Refill;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void entityEvent(byte event, CallbackInfo ci) {
        Refill.itemBroke((LivingEntity) (Object) this, event);
    }
}
