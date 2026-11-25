package dev.comfortableitems;

import dev.comfortableitems.mixin.ContainerAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public final class ClientActions {
    private static final Set<String> SHARED_GESTURES = Set.of("ALT+SHIFT+LEFT", "SHIFT+LEFT", "SHIFT+RIGHT",
        "CTRL+LEFT", "ALT+LEFT", "SHIFT+DROP+LEFT", "SHIFT+DROP+RIGHT", "DROP+LEFT", "WHEEL", "SHIFT+WHEEL",
        "ALT+WHEEL", "ALT+SHIFT+WHEEL", "SHIFT+CLICK", "CTRL+SHIFT+DROP");
    static boolean upstream;
    private static Object upstreamSort;
    private static java.lang.reflect.Method upstreamSortKeys;
    private static boolean lookedUpUpstreamSort;
    private static boolean dropHeld;
    private static String dragAction;
    private static int dragButton = -1;
    private static AbstractContainerScreen<?> dragScreen;
    private static boolean dragFromHotbar;
    private static Slot pendingQuickMove;
    private static Slot lastStackDragSlot;
    private static final Set<Integer> visited = new HashSet<>();

    private record Hover(AbstractContainerScreen<?> screen, Slot slot, double x, double y) {
        boolean outside(int button) {
            ContainerAccess access = (ContainerAccess) screen;
            return ClientPlatform.outside(access, x, y, button);
        }
    }

    private static InventoryActions actions(Minecraft client) {
        if (client.player == null || client.gameMode == null || client.player.isSpectator()) return null;
        Screen screen = ClientPlatform.screen(client);
        if (screen instanceof CreativeModeInventoryScreen creative) {
            return creative.isInventoryOpen() ? new InventoryActions(client, client.player.inventoryMenu) : null;
        }
        if (screen == null ? client.player.isCreative() : !(screen instanceof AbstractContainerScreen<?>)) return null;
        return new InventoryActions(client, client.player.containerMenu);
    }

    private static Hover hover(Minecraft client, double rawX, double rawY) {
        if (!(ClientPlatform.screen(client) instanceof AbstractContainerScreen<?> screen)) return null;
        if (screen.getMenu() != client.player.containerMenu) return null;
        double x = rawX * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth();
        double y = rawY * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight();
        Slot slot = ((ContainerAccess) screen).comfortableItems$slotAt(x, y);
        if (screen instanceof CreativeModeInventoryScreen) {
            // The personal tab wraps inventoryMenu slots in the same order, followed by the trash slot.
            int index = screen.getMenu().slots.indexOf(slot);
            slot = index >= 0 && index < client.player.inventoryMenu.slots.size()
                ? client.player.inventoryMenu.getSlot(index) : null;
        }
        return new Hover(screen, slot, x, y);
    }

    private static String chord(int modifiers, boolean drop, String trigger) {
        return ((modifiers & ClientPlatform.ALT) != 0 ? "ALT+" : "")
            + ((modifiers & ClientPlatform.SHIFT) != 0 ? "SHIFT+" : "")
            + ((modifiers & ClientPlatform.CTRL) != 0 ? "CTRL+" : "")
            + ((modifiers & ClientPlatform.SUPER) != 0 ? "SUPER+" : "")
            + (drop ? "DROP+" : "") + trigger;
    }

    private static String action(String chord, String... candidates) {
        if (upstream && SHARED_GESTURES.contains(chord)) return "";
        for (String name : candidates) {
            if (chord.equalsIgnoreCase(Settings.current.gestures.get(name))) return name;
        }
        return "";
    }

    public static boolean key(Minecraft client, boolean drop, boolean sort, boolean swap, int event, int modifiers) {
        if (drop) dropHeld = event != ClientPlatform.RELEASE;
        if (event == ClientPlatform.RELEASE || !Settings.current.enabled) return false;
        InventoryActions actions = actions(client);
        if (actions == null || actions.creativeInventory()) return false;
        Hover hover = hover(client, client.mouseHandler.xpos(), client.mouseHandler.ypos());
        if (drop) return dropKey(actions, hover, event, modifiers);
        if (hover != null && !dropHeld && modifiers == 0 && sort
            && Settings.current.sortEnabled && actions.ordinary(hover.slot)) {
            if (upstreamOwnsSort()) return false;
            if (event == ClientPlatform.PRESS) actions.sort(actions.side(actions.playerSide(hover.slot), false));
            return true;
        }
        if (swap || sort) {
            Refill.forgetUse();
        }
        return false;
    }

    private static boolean upstreamOwnsSort() {
        if (!upstream) return false;
        try {
            if (!lookedUpUpstreamSort) {
                lookedUpUpstreamSort = true;
                Class<?> hotkeys = Class.forName("fi.dy.masa.itemscroller.config.Hotkeys");
                upstreamSort = hotkeys.getField("SORT_INVENTORY").get(null);
                upstreamSortKeys = upstreamSort.getClass().getMethod("getStringValue");
            }
            if (upstreamSortKeys == null) return false;
            String key = (String) upstreamSortKeys.invoke(upstreamSort);
            if (key.isBlank() || key.contains(",")) return false;
            String name = key.toLowerCase(Locale.ROOT);
            if (name.startsWith("key.")) return name.equals(Settings.SORT.saveString());
            if (name.startsWith("mouse_") || name.startsWith("button_")) {
                return Settings.SORT.saveString().equals("key.mouse." + name.substring(name.lastIndexOf('_') + 1));
            }
            return Settings.SORT.saveString().equals("key.keyboard." + name.replace('_', '.'));
        } catch (ReflectiveOperationException | LinkageError error) {
            upstreamSortKeys = null;
            return false;
        }
    }

    private static boolean dropKey(InventoryActions actions, Hover hover, int event, int modifiers) {
        String action = action(chord(modifiers, false, "DROP"), "dropStack", "dropMatching", "dropAll",
            "dropAllWithHotbar", "upstreamDropMatching");
        if (action.isEmpty()) return false;
        if (event != ClientPlatform.PRESS) return true;
        if (action.equals("dropStack")) {
            if (hover == null) ClientPlatform.drop(actions.client);
            else actions.drop(hover.slot, Integer.MAX_VALUE);
            return true;
        }
        ItemStack sample = (hover == null ? actions.client.player.getMainHandItem()
            : hover.slot == null ? ItemStack.EMPTY : hover.slot.getItem()).copy();
        bulkDrop(actions, action, sample);
        return true;
    }

    private static void bulkDrop(InventoryActions actions, String action, ItemStack sample) {
        boolean matching = action.equals("dropMatching") || action.equals("upstreamDropMatching") || action.startsWith("outsideMatching");
        boolean hotbar = matching || action.endsWith("WithHotbar");
        List<Slot> player = actions.side(true, hotbar);
        List<ItemStack> filter = matching ? List.of(sample) : actions.sample(actions.side(false, true));
        boolean unrestricted = !matching && actions.menu instanceof InventoryMenu;
        actions.dropPlayer(player, item -> unrestricted || filter.stream().anyMatch(other -> InventoryActions.exact(item, other)));
    }

    public static boolean mouse(Minecraft client, int button, boolean drop, boolean sort, int event, int modifiers) {
        boolean wasDragging = dragButton == button;
        if (event == ClientPlatform.RELEASE) {
            if (drop) dropHeld = false;
            if (wasDragging) {
                if (pendingQuickMove != null && Settings.current.enabled && client.isWindowActive()
                    && ClientPlatform.screen(client) == dragScreen) {
                    InventoryActions actions = actions(client);
                    Hover hover = actions == null ? null : hover(client, client.mouseHandler.xpos(), client.mouseHandler.ypos());
                    if (hover != null && hover.slot == pendingQuickMove && actions.menu.getCarried().isEmpty()) {
                        actions.click(pendingQuickMove.index, 0, InventoryActions.Click.QUICK_MOVE);
                    }
                }
                clearDrag();
            }
            return wasDragging;
        }
        InventoryActions actions = actions(client);
        if (actions == null || !Settings.current.enabled) return false;
        Hover hover = hover(client, client.mouseHandler.xpos(), client.mouseHandler.ypos());
        if (actions.creativeInventory()) {
            return hover != null && startDrag(actions, hover, button, modifiers);
        }
        if (drop && dropKey(actions, hover, event, modifiers)) return true;
        if (hover == null) return false;
        if (modifiers == 0 && sort && Settings.current.sortEnabled && actions.ordinary(hover.slot)) {
            if (upstreamOwnsSort()) return false;
            actions.sort(actions.side(actions.playerSide(hover.slot), false));
            return true;
        }
        if (button != 0 && button != 1) return false;
        if (!actions.menu.getCarried().isEmpty() && hover.outside(button)) {
            String out = action(chord(modifiers, dropHeld, "CLICK"), "outsideAllWithHotbar", "outsideAll",
                "outsideMatchingAlt", "outsideMatchingShift");
            if (out.isEmpty()) return false;
            ItemStack sample = actions.menu.getCarried().copy();
            if (out.startsWith("outsideMatching")) actions.click(-999, 0, InventoryActions.Click.PICKUP);
            bulkDrop(actions, out, sample);
            holdMouse(out, button, hover.screen);
            return true;
        }
        ItemStack carried = actions.menu.getCarried();
        if (button == 0 && !carried.isEmpty() && actions.ordinary(hover.slot) && !hover.slot.hasItem()
            && !action(chord(modifiers, dropHeld, "LEFT"), "placeMatching").isEmpty()
            && hover.slot.mayPlace(carried) && hover.slot.getMaxStackSize(carried) >= carried.getCount()) {
            boolean player = actions.playerSide(hover.slot);
            ItemStack sample = carried.copy();
            actions.click(hover.slot.index, 0, InventoryActions.Click.PICKUP);
            actions.transfer(actions.side(!player, true), actions.side(player, true),
                item -> InventoryActions.exact(item, sample), Integer.MAX_VALUE);
            holdMouse("placeMatching", button, hover.screen);
            return true;
        }
        if (!(actions.ordinary(hover.slot) && actions.menu.getCarried().isEmpty())) return false;
        String chord = chord(modifiers, dropHeld, button == 0 ? "LEFT" : "RIGHT");
        String operation = action(chord, "moveAllWithHotbar", "moveMatchingWithHotbar", "moveAll", "moveMatching");
        if (operation.isEmpty()) return startDrag(actions, hover, button, modifiers);
        holdMouse(operation, button, hover.screen);
        boolean sourcePlayer = actions.playerSide(hover.slot);
        boolean includeHotbar = operation.endsWith("WithHotbar");
        List<Slot> target = actions.side(!sourcePlayer, includeHotbar);
        List<ItemStack> filter = operation.startsWith("moveMatching") ? actions.sample(target) : List.of();
        actions.transfer(actions.side(sourcePlayer, includeHotbar), target,
            stack -> operation.startsWith("moveAll") || filter.stream().anyMatch(item -> InventoryActions.exact(stack, item)), Integer.MAX_VALUE);
        return true;
    }

    private static boolean startDrag(InventoryActions actions, Hover hover, int button, int modifiers) {
        if ((button != 0 && button != 1) || !actions.ordinary(hover.slot) || !actions.menu.getCarried().isEmpty()) return false;
        String chord = chord(modifiers, dropHeld, button == 0 ? "LEFT" : "RIGHT");
        String operation = action(chord, "dragStacks", "dragLeaveOne", "dragOne", "dragMatching",
            "dragDropStacks", "dragDropLeaveOne", "dragDropOne");
        if (operation.isEmpty()) return false;
        if (operation.startsWith("dragDrop")) {
            if (actions.creativeInventory()) return false;
        } else if (actions.dragSide(hover.slot, true).isEmpty()) {
            return false;
        }
        holdMouse(operation, button, hover.screen);
        dragFromHotbar = actions.hotbar(hover.slot);
        if (operation.equals("dragStacks")) {
            if (chord.equals("SHIFT+LEFT") && actions.hasPersonalDragSides() && !actions.creativeInventory()) {
                // These menus give a vanilla Shift-click different destinations from our drag.
                pendingQuickMove = hover.slot;
            } else {
                dragStackOnEntry(actions, hover.slot);
            }
        } else {
            drag(actions, hover.slot, operation);
        }
        return true;
    }

    private static void holdMouse(String operation, int button, AbstractContainerScreen<?> screen) {
        clearDrag();
        dragAction = operation;
        dragButton = button;
        dragScreen = screen;
    }

    private static void clearDrag() {
        dragAction = null;
        dragScreen = null;
        dragButton = -1;
        pendingQuickMove = null;
        lastStackDragSlot = null;
        visited.clear();
    }

    public static void move(Minecraft client, double x, double y) {
        if (dragAction == null) return;
        if (!Settings.current.enabled || ClientPlatform.screen(client) != dragScreen || !client.isWindowActive()) {
            clearDrag();
            return;
        }
        if (!dragAction.startsWith("drag")) return;
        InventoryActions actions = actions(client);
        Hover hover = actions == null ? null : hover(client, x, y);
        if (hover == null) return;
        Slot start = pendingQuickMove;
        if (start != null) {
            if (hover.slot == start) return;
            pendingQuickMove = null;
        }
        if (!chord(ClientPlatform.modifiers(client), dropHeld, dragButton == 0 ? "LEFT" : "RIGHT")
            .equalsIgnoreCase(Settings.current.gestures.get(dragAction))) return;
        if (dragAction.equals("dragStacks")) {
            if (start != null) dragStackOnEntry(actions, start);
            dragStackOnEntry(actions, hover.slot);
            return;
        }
        if (actions.ordinary(hover.slot)
            && (!actions.hasPersonalDragSides() || dragAction.startsWith("dragDrop") || actions.hotbar(hover.slot) == dragFromHotbar)) {
            drag(actions, hover.slot, dragAction);
        }
    }

    private static void dragStackOnEntry(InventoryActions actions, Slot slot) {
        if (slot == lastStackDragSlot) return;
        // Record empty slots and gaps as well, so re-entering a slot can move its new contents.
        lastStackDragSlot = slot;
        if (actions.ordinary(slot)) drag(actions, slot, "dragStacks");
    }

    private static void drag(InventoryActions actions, Slot slot, String operation) {
        if ((!operation.equals("dragStacks") && !visited.add(slot.index))
            || !slot.hasItem() || !actions.menu.getCarried().isEmpty()) return;
        if (operation.startsWith("dragDrop")) {
            int count = switch (operation) {
                case "dragDropOne" -> 1;
                case "dragDropLeaveOne" -> slot.getItem().getCount() - 1;
                default -> Integer.MAX_VALUE;
            };
            actions.drop(slot, count);
            return;
        }
        List<Slot> opposite = actions.dragSide(slot, true);
        if (operation.equals("dragMatching")) {
            ItemStack sample = slot.getItem().copy();
            actions.transfer(actions.dragSide(slot, false), opposite, item -> InventoryActions.exact(item, sample), Integer.MAX_VALUE);
        } else {
            int count = switch (operation) {
                case "dragOne" -> 1;
                case "dragLeaveOne" -> slot.getItem().getCount() - 1;
                default -> Integer.MAX_VALUE;
            };
            actions.transfer(List.of(slot), opposite, item -> true, count);
        }
    }

    public static boolean scroll(Minecraft client, double delta) {
        if (delta == 0 || !Settings.current.enabled) return false;
        InventoryActions actions = actions(client);
        if (actions == null || actions.creativeInventory() || !actions.menu.getCarried().isEmpty()) return false;
        Hover hover = hover(client, client.mouseHandler.xpos(), client.mouseHandler.ypos());
        if (hover == null || !actions.ordinary(hover.slot)) return false;
        String operation = action(chord(ClientPlatform.modifiers(client), dropHeld, "WHEEL"), "scrollOne", "scrollStack", "scrollMatching", "scrollAll");
        if (operation.isEmpty()) return false;
        Slot hovered = hover.slot;
        boolean player = actions.playerSide(hovered);
        boolean away = delta > 0;
        boolean batch = operation.equals("scrollAll") || operation.equals("scrollMatching");
        Settings settings = Settings.current;
        if (!batch) {
            if (settings.positionAwareScroll && !player) away = !away;
            if (operation.equals("scrollOne") ? settings.reverseSingleScroll : settings.reverseStackScroll) away = !away;
        }
        List<Slot> side = actions.side(player, true);
        List<Slot> opposite = actions.side(!player, true);
        if (opposite.isEmpty()) return false;
        ItemStack sample = hovered.getItem().copy();
        if (sample.isEmpty() && !operation.equals("scrollAll")) return false;
        List<Slot> from = away ? batch ? side : List.of(hovered) : opposite;
        List<Slot> to = away ? opposite : batch ? side : List.of(hovered);
        Predicate<ItemStack> filter = item -> operation.equals("scrollAll") || InventoryActions.exact(item, sample);
        int count = switch (operation) {
            case "scrollOne" -> Math.max(1, (int) Math.abs(delta));
            case "scrollStack" -> away ? sample.getCount() : sample.getMaxStackSize();
            default -> Integer.MAX_VALUE;
        };
        actions.transfer(from, to, filter, count);
        return true;
    }

    private ClientActions() {}
}
