package dev.comfortableitems.mixin;

import dev.comfortableitems.ClientActions;
import dev.comfortableitems.ClientPlatform;
import dev.comfortableitems.Settings;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyboardHandler.class, priority = 1500)
abstract class KeyboardMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void press(long window, int key, int scan, int action, int modifiers, CallbackInfo ci) {
        if (window == minecraft.getWindow().getWindow() && minecraft.options != null
            && ClientActions.key(minecraft, minecraft.options.keyDrop.matches(key, scan), Settings.SORT.matches(key, scan),
                minecraft.options.keySwapOffhand.matches(key, scan), action, ClientPlatform.modifiers(minecraft))) ci.cancel();
    }
}
