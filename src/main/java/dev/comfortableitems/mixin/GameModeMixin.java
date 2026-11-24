package dev.comfortableitems.mixin;

import dev.comfortableitems.Refill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
abstract class GameModeMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"))
    private void click(int menu, int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        if (type == ClickType.THROW) Refill.containerDrop(minecraft, menu, slot);
    }
}
