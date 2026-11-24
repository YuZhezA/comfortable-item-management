package dev.comfortableitems;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

final class InventoryActions {
    enum Click { PICKUP, SWAP, THROW }

    final Minecraft client;
    final AbstractContainerMenu menu;

    InventoryActions(Minecraft client, AbstractContainerMenu menu) {
        this.client = client;
        this.menu = menu;
    }

    static boolean exact(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemTraits.exact(a, b);
    }

    static String id(ItemStack item) {
        return BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
    }

    boolean playerSide(Slot slot) {
        return slot.container == client.player.getInventory();
    }

    boolean ordinary(Slot slot) {
        // Specialized slots can have side effects or only accept a subset of items.
        return slot != null && slot.isActive() && slot.getClass() == Slot.class
            && (!playerSide(slot) || slot.getContainerSlot() < Inventory.INVENTORY_SIZE);
    }

    List<Slot> side(boolean player, boolean hotbar) {
        List<Slot> result = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (ordinary(slot) && playerSide(slot) == player
                && (!player || hotbar || slot.getContainerSlot() >= 9)) result.add(slot);
        }
        return result;
    }

    void click(int index, int button, Click action) {
        ClientPlatform.click(client, menu.containerId, index, button, action);
    }

    void transfer(List<Slot> source, List<Slot> target, Predicate<ItemStack> match, int maximum) {
        if (!menu.getCarried().isEmpty()) return;
        int remaining = maximum;
        for (Slot from : source) {
            if (remaining == 0 || !menu.getCarried().isEmpty()) return;
            ItemStack item = from.getItem();
            if (item.isEmpty() || !match.test(item) || !from.mayPickup(client.player) || !from.mayPlace(item)) continue;
            for (boolean empty : new boolean[] {false, true}) {
                for (Slot to : target) {
                    if (remaining == 0 || from.getItem().isEmpty()) break;
                    if (from == to || to.getItem().isEmpty() != empty) continue;
                    remaining -= move(from, to, remaining);
                    if (!menu.getCarried().isEmpty()) return;
                }
            }
        }
    }

    private int move(Slot from, Slot to, int limit) {
        ItemStack item = from.getItem();
        ItemStack old = to.getItem();
        if (!to.mayPlace(item) || (!old.isEmpty() && !exact(item, old))) return 0;
        int capacity = Math.min(item.getMaxStackSize(), to.getMaxStackSize(item)) - old.getCount();
        int count = Math.min(Math.min(capacity, limit), item.getCount());
        if (count <= 0) return 0;
        int previous = old.getCount();
        click(from.index, 0, Click.PICKUP);
        if (count >= Math.min(capacity, menu.getCarried().getCount())) {
            click(to.index, 0, Click.PICKUP);
        } else {
            for (int i = 0; i < count && !menu.getCarried().isEmpty(); i++) click(to.index, 1, Click.PICKUP);
        }
        if (!menu.getCarried().isEmpty()) click(from.index, 0, Click.PICKUP);
        return Math.max(0, to.getItem().getCount() - previous);
    }

    void drop(Slot slot, int count) {
        if (count <= 0 || slot == null || slot.getItem().isEmpty() || !slot.mayPickup(client.player)) return;
        if (count >= slot.getItem().getCount()) {
            click(slot.index, 1, Click.THROW);
        } else {
            for (int i = 0; i < count && slot.hasItem(); i++) click(slot.index, 0, Click.THROW);
        }
    }

    void dropPlayer(List<Slot> slots, Predicate<ItemStack> match) {
        List<Slot> selected = slots.stream().filter(slot -> slot.hasItem() && match.test(slot.getItem())).toList();
        if (selected.isEmpty()) return;
        if (menu.getCarried().isEmpty()) {
            for (Slot slot : selected) drop(slot, Integer.MAX_VALUE);
            return;
        }
        ItemStack held = menu.getCarried();
        Slot parking = menu.slots.stream().filter(slot -> ordinary(slot) && !slot.hasItem()
            && slot.mayPickup(client.player) && fits(slot, held)).findFirst().orElse(null);
        if (parking == null) return;
        click(parking.index, 0, Click.PICKUP);
        if (!menu.getCarried().isEmpty()) return;
        for (Slot slot : selected) drop(slot, Integer.MAX_VALUE);
        click(parking.index, 0, Click.PICKUP);
    }

    List<ItemStack> sample(List<Slot> slots) {
        return slots.stream().filter(Slot::hasItem).map(slot -> slot.getItem().copy()).toList();
    }

    void sort(List<Slot> region) {
        if (!menu.getCarried().isEmpty()) return;
        List<Slot> slots = region.stream().filter(slot -> slot.mayPickup(client.player)).toList();
        for (int i = 0; i < slots.size(); i++) {
            Slot target = slots.get(i);
            if (!target.hasItem()) continue;
            for (int j = i + 1; j < slots.size(); j++) {
                Slot source = slots.get(j);
                if (exact(target.getItem(), source.getItem()) && source.mayPlace(source.getItem())) {
                    move(source, target, Integer.MAX_VALUE);
                    if (!menu.getCarried().isEmpty()) return;
                }
            }
        }
        List<Slot> desired = new ArrayList<>(slots);
        desired.sort((a, b) -> compare(a.getItem(), b.getItem()));
        for (int i = 0; i < slots.size(); i++) {
            Slot target = slots.get(i);
            Slot source = desired.get(i);
            if (source == target) continue;
            ItemStack incoming = source.getItem();
            ItemStack outgoing = target.getItem();
            if (incoming.isEmpty() || !fits(target, incoming) || !source.mayPlace(incoming)
                || (!outgoing.isEmpty() && (!fits(source, outgoing) || exact(incoming, outgoing)))) continue;
            click(source.index, 0, Click.PICKUP);
            click(target.index, 0, Click.PICKUP);
            if (!menu.getCarried().isEmpty()) click(source.index, 0, Click.PICKUP);
            if (!menu.getCarried().isEmpty()) return;
            int movedFrom = desired.indexOf(target);
            desired.set(i, target);
            if (movedFrom >= 0) desired.set(movedFrom, source);
        }
    }

    private static boolean fits(Slot slot, ItemStack stack) {
        return slot.mayPlace(stack) && slot.getMaxStackSize(stack) >= stack.getCount();
    }

    private static int compare(ItemStack a, ItemStack b) {
        int empty = Boolean.compare(a.isEmpty(), b.isEmpty());
        if (empty != 0 || a.isEmpty()) return empty;
        Settings settings = Settings.current;
        Comparator<ItemStack> priority = Comparator.comparingInt(InventoryActions::sortPriority);
        Comparator<ItemStack> name = Comparator.comparing(stack -> stack.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
        Comparator<ItemStack> rawId = Comparator.comparingInt(stack -> BuiltInRegistries.ITEM.getId(stack.getItem()));
        Comparator<ItemStack> ordering = switch (settings.sortOrder) {
            case CATEGORY_NAME, ITEM_NAME, NAME -> name;
            case CATEGORY_COUNT, ITEM_COUNT, COUNT -> Comparator.comparingInt(ItemStack::getCount).reversed().thenComparing(name);
            case CATEGORY_RARITY, ITEM_RARITY -> Comparator.comparing(ItemStack::getRarity).reversed().thenComparing(rawId);
            case CATEGORY_RAWID, ITEM_RAWID -> rawId;
            case ID -> Comparator.comparing(InventoryActions::id);
        };
        if (settings.sortOrder.name().startsWith("CATEGORY_")) {
            Comparator<ItemStack> categories = Comparator.comparingInt(InventoryActions::categoryPriority)
                .thenComparing(ItemTraits::category);
            ordering = categories.thenComparing(ordering);
        }
        return priority.thenComparing(ordering).thenComparing(InventoryActions::id).compare(a, b);
    }

    private static int categoryPriority(ItemStack stack) {
        int index = Settings.current.sortCategoryOrder.indexOf(ItemTraits.category(stack));
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private static int sortPriority(ItemStack stack) {
        Settings settings = Settings.current;
        String id = id(stack);
        int first = settings.sortTopPriority.indexOf(id);
        if (first >= 0) return first - settings.sortTopPriority.size();
        int last = settings.sortBottomPriority.indexOf(id);
        if (last >= 0) return 3 + last;
        if (id.endsWith("shulker_box") && settings.sortShulkersAtEnd) return settings.sortShulkersInverted ? -1 : 1;
        if ((id.endsWith(":bundle") || id.endsWith("_bundle")) && settings.sortBundlesAtEnd) return settings.sortBundlesInverted ? -1 : 2;
        return 0;
    }
}
