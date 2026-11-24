package dev.comfortableitems.mixin;

import dev.comfortableitems.ClientActions;
import dev.comfortableitems.ClientPlatform;
import dev.comfortableitems.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHandler.class, priority = 1500)
abstract class MouseMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void press(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (window == minecraft.getWindow().getWindow() && minecraft.options != null
            && ClientActions.mouse(minecraft, button, minecraft.options.keyDrop.matchesMouse(button),
                Settings.SORT.matchesMouse(button), action, ClientPlatform.modifiers(minecraft))) ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void scroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == minecraft.getWindow().getWindow() && ClientActions.scroll(minecraft, vertical)) ci.cancel();
    }

    @Inject(method = "onMove", at = @At("RETURN"))
    private void move(long window, double x, double y, CallbackInfo ci) {
        if (window == minecraft.getWindow().getWindow()) ClientActions.move(minecraft, x, y);
    }
}
