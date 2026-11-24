package dev.comfortableitems;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

public final class ModMenuEntry implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuEntry::open;
    }

    private static Screen open(Screen parent) {
        // The official Configured mod declares this metadata. An unrelated Fabric mod uses the same ID.
        boolean hasConfigured = FabricLoader.getInstance().getModContainer("configured")
            .map(container -> container.getMetadata().containsCustomValue("configured"))
            .orElse(false);
        if (hasConfigured) {
            Screen configured = ConfiguredClientFile.openOverride(parent);
            if (configured != null) return configured;
        }
        return new SettingsScreen(parent);
    }
}
