package dev.comfortableitems;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

abstract class ConfigScreenBase extends Screen {
    ConfigScreenBase(Component title) {
        super(title);
    }

    void returnTo(Screen parent) {
        minecraft.setScreen(parent);
    }
}
