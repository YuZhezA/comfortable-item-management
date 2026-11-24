package dev.comfortableitems;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

final class ItemTraits {
    enum Kind { ITEM, EQUIPMENT, TOOL }

    static boolean exact(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    static String type(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof ArmorItem armor) return "armor/" + armor.getEquipmentSlot().getName();
        if (item instanceof ShieldItem) return "shield";
        if (item instanceof Equipable gear && stack.isDamageableItem()) return "armor/" + gear.getEquipmentSlot().getName();
        if (item instanceof PickaxeItem) return "pickaxe";
        if (item instanceof AxeItem) return "axe";
        if (item instanceof ShovelItem) return "shovel";
        if (item instanceof HoeItem) return "hoe";
        if (item instanceof SwordItem) return "sword";
        if (item instanceof DiggerItem) return InventoryActions.id(stack);
        String id = InventoryActions.id(stack);
        return switch (id) {
            case "minecraft:bow", "minecraft:crossbow", "minecraft:trident", "minecraft:shears",
                 "minecraft:fishing_rod", "minecraft:flint_and_steel", "minecraft:brush",
                 "minecraft:carrot_on_a_stick", "minecraft:warped_fungus_on_a_stick" -> id;
            default -> "";
        };
    }

    static Kind kind(ItemStack stack) {
        String type = type(stack);
        if (type.isEmpty()) return Kind.ITEM;
        return type.startsWith("armor/") || type.equals("shield") ? Kind.EQUIPMENT : Kind.TOOL;
    }

    static String category(ItemStack stack) {
        if (kind(stack) != Kind.ITEM) return "equipment/" + type(stack);
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem) return "block";
        if (stack.has(net.minecraft.core.component.DataComponents.FOOD)) return "food";
        return "item";
    }

    static boolean matches(ItemStack previous, ItemStack spare, Settings.Match match, Kind kind) {
        if (spare.isEmpty() || match == null) return false;
        return switch (match) {
            case OFF -> false;
            case ID -> ItemStack.isSameItem(previous, spare);
            case TYPE -> kind != Kind.ITEM && type(previous).equals(type(spare));
            case STRICT -> {
                if (kind == Kind.ITEM) yield InventoryActions.exact(previous, spare);
                if (!ItemStack.isSameItem(previous, spare)) yield false;
                ItemStack a = previous.copy();
                ItemStack b = spare.copy();
                a.setDamageValue(0);
                b.setDamageValue(0);
                yield exact(a, b);
            }
        };
    }

    private ItemTraits() {}
}
