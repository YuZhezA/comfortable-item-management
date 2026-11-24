package dev.comfortableitems;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;

@Mod(value = "comfortable_items", dist = Dist.CLIENT)
public final class LoaderEntry {
    public LoaderEntry(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new SettingsScreen(parent));
        ClientActions.upstream = ModList.get().isLoaded("itemscroller") || ModList.get().isLoaded("rocknroller");
    }
}
