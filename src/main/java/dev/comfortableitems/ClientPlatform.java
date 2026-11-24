package dev.comfortableitems;

import com.mojang.blaze3d.platform.InputConstants;
import dev.comfortableitems.mixin.ContainerAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import org.lwjgl.glfw.GLFW;

public final class ClientPlatform {
    public static final int RELEASE = 0;
    public static final int PRESS = 1;
    public static final int ALT = 1;
    public static final int SHIFT = 2;
    public static final int CTRL = 4;
    public static final int SUPER = 8;

    static KeyMapping sortKey() {
        return new KeyMapping("key.comfortable_items.sort", InputConstants.KEY_R, "key.categories.inventory");
    }

    public static Screen screen(Minecraft client) { return client.screen; }
    static int selected(Inventory inventory) { return inventory.selected; }
    static void drop(Minecraft client) { client.player.drop(true); }

    static void click(Minecraft client, int menu, int slot, int button, InventoryActions.Click action) {
        client.gameMode.handleInventoryMouseClick(menu, slot, button, ClickType.valueOf(action.name()), client.player);
    }

    static void creativeClick(CreativeModeInventoryScreen screen, int slot, int button, InventoryActions.Click action) {
        ((ContainerAccess) screen).comfortableItems$click(screen.getMenu().getSlot(slot), slot, button, ClickType.valueOf(action.name()));
    }

    public static int modifiers(Minecraft client) {
        return mask(held(client, InputConstants.KEY_LALT, InputConstants.KEY_RALT),
            held(client, InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT),
            held(client, InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL),
            held(client, GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER));
    }

    public static int mask(boolean alt, boolean shift, boolean ctrl, boolean meta) {
        return (alt ? ALT : 0) | (shift ? SHIFT : 0) | (ctrl ? CTRL : 0) | (meta ? SUPER : 0);
    }

    private static boolean held(Minecraft client, int left, int right) {
        return InputConstants.isKeyDown(client.getWindow().getWindow(), left)
            || InputConstants.isKeyDown(client.getWindow().getWindow(), right);
    }

    static boolean outside(ContainerAccess access, double x, double y, int button) {
        return access.comfortableItems$outside(x, y, access.comfortableItems$left(), access.comfortableItems$top(), button);
    }

    private ClientPlatform() {}
}
