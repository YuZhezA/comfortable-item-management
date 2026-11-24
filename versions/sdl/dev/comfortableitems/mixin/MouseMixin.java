package dev.comfortableitems.mixin;

import dev.comfortableitems.ClientActions;
import dev.comfortableitems.ClientPlatform;
import dev.comfortableitems.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHandler.class, priority = 1500)
abstract class MouseMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void press(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (window != minecraft.getWindow().handle() || minecraft.options == null) return;
        MouseButtonEvent event = new MouseButtonEvent(0, 0, info);
        int button = switch (info.button()) {
            case InputConstants.MOUSE_BUTTON_LEFT -> 0;
            case InputConstants.MOUSE_BUTTON_RIGHT -> 1;
            default -> info.button() + 1;
        };
        if (ClientActions.mouse(minecraft, button, minecraft.options.keyDrop.matchesMouse(event),
            Settings.SORT.matchesMouse(event), action, ClientPlatform.modifiers(minecraft))) ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void scroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window == minecraft.getWindow().handle() && ClientActions.scroll(minecraft, vertical)) ci.cancel();
    }

    @Inject(method = "onMove", at = @At("RETURN"))
    private void move(long window, double x, double y, double deltaX, double deltaY, CallbackInfo ci) {
        if (window == minecraft.getWindow().handle()) ClientActions.move(minecraft, x, y);
    }
}
