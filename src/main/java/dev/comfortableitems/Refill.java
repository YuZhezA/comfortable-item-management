package dev.comfortableitems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public final class Refill {
    private static final int SLOT_COUNT = 41;
    private static final int OFFHAND = 40;
    private static final int BACKPACK_START = 9;
    private static final int ARMOR_START = 36;
    private static final int MINIMUM_DURABILITY = 10;
    private static final ItemStack[] snapshots = new ItemStack[SLOT_COUNT];
    private static final int[] actions = new int[SLOT_COUNT];
    private static final ItemStack[] broken = new ItemStack[SLOT_COUNT];
    private static LocalPlayer owner;
    private static int tick;
    private static int suppressFor;

    static {
        Arrays.fill(snapshots, ItemStack.EMPTY);
        Arrays.fill(broken, ItemStack.EMPTY);
    }

    public static void use(LocalPlayer player, InteractionHand hand) {
        if (player != owner || player.isSpectator()) return;
        int index = hand == InteractionHand.MAIN_HAND ? ClientPlatform.selected(player.getInventory()) : OFFHAND;
        ItemStack stack = player.getInventory().getItem(index);
        if (!stack.isEmpty()) {
            snapshots[index] = stack.copy();
            actions[index] = tick + 20;
        }
    }

    public static void forgetUse() {
        Arrays.fill(actions, 0);
        Arrays.fill(broken, ItemStack.EMPTY);
    }

    public static void dropped(LocalPlayer player) {
        if (player != owner) return;
        int selected = ClientPlatform.selected(player.getInventory());
        if (Settings.current.suppressRefillOnDrop) {
            actions[selected] = 0;
            snapshots[selected] = ItemStack.EMPTY;
            broken[selected] = ItemStack.EMPTY;
        } else {
            use(player, InteractionHand.MAIN_HAND);
        }
    }

    public static void containerDrop(Minecraft minecraft, int menuIndex, int slotIndex) {
        LocalPlayer player = minecraft.player;
        if (owner == null || player != owner || player.containerMenu.containerId != menuIndex || slotIndex < 0
            || slotIndex >= player.containerMenu.slots.size()) return;
        Slot slot = player.containerMenu.getSlot(slotIndex);
        if (slot.container != player.getInventory()) return;
        int index = slot.getContainerSlot();
        if (!tracked(index) || !slot.hasItem()) return;
        if (Settings.current.suppressRefillOnDrop) {
            actions[index] = 0;
            snapshots[index] = ItemStack.EMPTY;
            broken[index] = ItemStack.EMPTY;
        } else {
            snapshots[index] = slot.getItem().copy();
            broken[index] = snapshots[index];
        }
    }

    public static void itemBroke(LivingEntity entity, byte event) {
        if (owner == null || entity != owner || event < 47 || event > 52) return;
        int index = switch (event) {
            case 47 -> ClientPlatform.selected(owner.getInventory());
            case 48 -> OFFHAND;
            case 49 -> 39;
            case 50 -> 38;
            case 51 -> 37;
            default -> 36;
        };
        ItemStack previous = snapshots[index];
        if (!previous.isEmpty() && ItemTraits.kind(previous) != ItemTraits.Kind.ITEM) broken[index] = previous.copy();
    }

    public static void tick(Minecraft client) {
        tick++;
        LocalPlayer player = client.player;
        if (player != owner) {
            owner = player;
            forgetUse();
            Arrays.fill(snapshots, ItemStack.EMPTY);
            suppressFor = tick + 1;
        }
        if (player == null) return;
        Inventory inventory = player.getInventory();
        Settings settings = Settings.current;
        if (!settings.enabled || !settings.refill || player.isCreative() || player.isSpectator() || !player.isAlive()) {
            forgetUse();
            update(inventory);
            return;
        }
        if (ClientPlatform.screen(client) != null || player.containerMenu != player.inventoryMenu || tick <= suppressFor) {
            update(inventory);
            return;
        }
        if (player.isUsingItem()) use(player, player.getUsedItemHand());
        for (int index = 0; index < SLOT_COUNT; index++) {
            if (!tracked(index)) continue;
            ItemStack current = inventory.getItem(index);
            ItemStack previous = snapshots[index];
            if (current.isEmpty()) {
                ItemStack anchor = broken[index];
                if (anchor.isEmpty() && actions[index] >= tick && !previous.isEmpty()) anchor = previous;
                if (!anchor.isEmpty()) equip(client, index, anchor, false);
                actions[index] = 0;
                broken[index] = ItemStack.EMPTY;
            } else {
                if (current.isDamageableItem() && durability(current) < MINIMUM_DURABILITY) equip(client, index, current, true);
                if (actions[index] < tick || !previous.isEmpty() && !ItemStack.isSameItem(previous, current)) actions[index] = 0;
                // A break event may arrive before the server's slot change.
                if (!broken[index].isEmpty() && !ItemStack.isSameItem(broken[index], current)) broken[index] = ItemStack.EMPTY;
            }
        }
        update(inventory);
    }

    private static void update(Inventory inventory) {
        for (int index = 0; index < SLOT_COUNT; index++) {
            if (tracked(index)) snapshots[index] = inventory.getItem(index).copy();
        }
    }

    private static boolean tracked(int index) {
        return index >= 0 && index < SLOT_COUNT && (index < BACKPACK_START || index >= ARMOR_START);
    }

    private static int durability(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    private static void equip(Minecraft client, int index, ItemStack anchor, boolean beforeBreak) {
        ItemTraits.Kind kind = ItemTraits.kind(anchor);
        Settings settings = Settings.current;
        Settings.Match match = switch (kind) {
            case ITEM -> beforeBreak ? Settings.Match.OFF : settings.refillItems;
            case EQUIPMENT -> beforeBreak ? settings.replaceEquipment : settings.refillEquipment;
            case TOOL -> beforeBreak ? settings.replaceTools : settings.refillTools;
        };
        if (match == Settings.Match.OFF || match == null) return;
        var player = client.player;
        var inventory = player.getInventory();
        if (client.gameMode == null || !player.inventoryMenu.getCarried().isEmpty()) return;
        Slot armor = index >= ARMOR_START && index < OFFHAND ? player.inventoryMenu.getSlot(44 - index) : null;
        if (armor != null && !armor.mayPickup(player)) return;
        int candidate = -1;
        int bestDurability = -1;
        for (int slot = BACKPACK_START; slot < ARMOR_START; slot++) {
            ItemStack spare = inventory.getItem(slot);
            if (!ItemTraits.matches(anchor, spare, match, kind) || (armor != null && !armor.mayPlace(spare))) continue;
            int remaining = spare.isDamageableItem() ? durability(spare) : spare.getMaxDamage();
            if (beforeBreak && remaining < MINIMUM_DURABILITY) continue;
            if (candidate < 0 || kind != ItemTraits.Kind.ITEM && remaining > bestDurability) {
                candidate = slot;
                bestDurability = remaining;
            }
        }
        if (candidate < 0) return;
        InventoryActions operation = new InventoryActions(client, player.inventoryMenu);
        if (armor == null) {
            operation.click(candidate, index, InventoryActions.Click.SWAP);
        } else {
            operation.click(candidate, 0, InventoryActions.Click.PICKUP);
            operation.click(armor.index, 0, InventoryActions.Click.PICKUP);
            if (!player.inventoryMenu.getCarried().isEmpty()) operation.click(candidate, 0, InventoryActions.Click.PICKUP);
        }
    }

    private Refill() {}
}
