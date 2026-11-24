package dev.comfortableitems;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("comfortable_items")
public final class LoaderEntry {
    public LoaderEntry() {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        ClientActions.upstream = ModList.get().isLoaded("itemscroller") || ModList.get().isLoaded("rocknroller");
    }
}
