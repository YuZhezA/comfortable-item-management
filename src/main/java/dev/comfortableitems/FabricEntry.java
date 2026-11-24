package dev.comfortableitems;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientActions.upstream = FabricLoader.getInstance().isModLoaded("itemscroller")
            || FabricLoader.getInstance().isModLoaded("rocknroller");
    }
}
