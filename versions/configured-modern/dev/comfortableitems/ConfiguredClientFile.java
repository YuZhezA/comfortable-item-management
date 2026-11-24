package dev.comfortableitems;

import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.api.util.ConfigScreenHelper;
import net.minecraft.client.gui.screens.Screen;
import com.mrcrayfish.configured.api.ActionResult;
import com.mrcrayfish.configured.api.IConfigEntry;
import net.minecraft.world.entity.player.Player;

final class ConfiguredClientFile extends ConfiguredProvider.ClientFile {
    static Screen openOverride(Screen parent) {
        if (!Config.isForceConfiguredMenu()) return null;
        return ConfigScreenHelper.createSelectionScreen(parent, ConfigOptions.text("title"), new ConfiguredClientFile());
    }

    @Override
    public IConfigEntry createRootEntry() {
        return read();
    }

    @Override
    public ActionResult update(IConfigEntry entry) {
        return commit(entry) ? ActionResult.success() : ActionResult.fail(ConfigOptions.text("write_failed"));
    }

    @Override
    public ActionResult canPlayerEdit(Player player) {
        return Settings.canWrite() ? ActionResult.success() : ActionResult.fail(ConfigOptions.text("write_failed"));
    }
}
