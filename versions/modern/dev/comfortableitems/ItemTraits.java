package dev.comfortableitems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

final class ItemTraits {
    enum Kind { ITEM, EQUIPMENT, TOOL }

    static boolean exact(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    static String type(ItemStack stack) {
        var gear = stack.get(DataComponents.EQUIPPABLE);
        if (gear != null && gear.slot().isArmor() && stack.isDamageableItem()) return "armor/" + gear.slot().getName();
        if (stack.has(DataComponents.BLOCKS_ATTACKS)) return "shield";
        if (stack.is(ItemTags.PICKAXES)) return "pickaxe";
        if (stack.is(ItemTags.AXES)) return "axe";
        if (stack.is(ItemTags.SHOVELS)) return "shovel";
        if (stack.is(ItemTags.HOES)) return "hoe";
        if (stack.is(ItemTags.SWORDS)) return "sword";
        if (stack.is(ItemTags.SPEARS)) return "spear";
        String id = InventoryActions.id(stack);
        return switch (id) {
            case "minecraft:mace", "minecraft:bow", "minecraft:crossbow", "minecraft:trident", "minecraft:shears",
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
