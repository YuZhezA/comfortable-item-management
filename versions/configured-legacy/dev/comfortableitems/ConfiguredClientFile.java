package dev.comfortableitems;

import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.api.util.ConfigScreenHelper;
import net.minecraft.client.gui.screens.Screen;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;

import java.nio.file.Path;
import java.util.function.Consumer;

final class ConfiguredClientFile extends ConfiguredProvider.ClientFile {
    static Screen openOverride(Screen parent) {
        if (!Config.isForceConfiguredMenu()) return null;
        return ConfigScreenHelper.createSelectionScreen(parent, ConfigOptions.text("title"), new ConfiguredClientFile(), new net.minecraft.resources.ResourceLocation("minecraft", "textures/block/stone.png"));
    }

    @Override
    public IConfigEntry getRoot() {
        return read();
    }

    @Override
    public void update(IConfigEntry entry) {
        if (commit(entry)) return;
        Minecraft client = Minecraft.getInstance();
        client.tell(() -> {
            var parent = client.screen;
            client.setScreen(new AlertScreen(() -> client.setScreen(parent), ConfigOptions.text("title"),
                ConfigOptions.text("write_failed")));
        });
    }

    @Override
    public void loadWorldConfig(Path path, Consumer<IModConfig> result) {}
}
