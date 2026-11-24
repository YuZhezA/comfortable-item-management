package dev.comfortableitems.mixin;

import dev.comfortableitems.Settings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(Options.class)
abstract class OptionsMixin {
    @Shadow @Final protected Minecraft minecraft;
    @Shadow @Final @Mutable public KeyMapping[] keyMappings;
    @Unique private boolean comfortableItems$registered;

    @Inject(method = "load", at = @At("HEAD"))
    private void register(CallbackInfo ci) {
        if (comfortableItems$registered) return;
        Settings.load(minecraft);
        keyMappings = Arrays.copyOf(keyMappings, keyMappings.length + 1);
        keyMappings[keyMappings.length - 1] = Settings.SORT;
        comfortableItems$registered = true;
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void loadKey(CallbackInfo ci) {
        Settings.applySortKey();
    }

    @Inject(method = "save", at = @At("HEAD"))
    private void saveKey(CallbackInfo ci) {
        Settings.saveSortKey();
    }
}
