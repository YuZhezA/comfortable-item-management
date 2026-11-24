package dev.comfortableitems;

import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;

@Mod(value = "comfortable_items", dist = Dist.CLIENT)
public final class LoaderEntry {
    public LoaderEntry() {
        ClientActions.upstream = ModList.get().isLoaded("itemscroller") || ModList.get().isLoaded("rocknroller");
    }
}
