package dev.comfortableitems;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;

public final class Settings {
    public enum Match { OFF, TYPE, ID, STRICT }
    public enum Order {
        CATEGORY_NAME, CATEGORY_COUNT, CATEGORY_RARITY, CATEGORY_RAWID,
        ITEM_NAME, ITEM_COUNT, ITEM_RARITY, ITEM_RAWID, NAME, ID, COUNT
    }

    public static final Logger LOG = LoggerFactory.getLogger("comfortable_items");
    public static final KeyMapping SORT = ClientPlatform.sortKey();
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public static Settings current = new Settings();
    private static Path path;
    private static boolean readable = true;

    String sortKey = "key.keyboard.r";
    boolean enabled = true;
    boolean reverseSingleScroll = false;
    boolean reverseStackScroll = false;
    boolean positionAwareScroll = true;
    boolean sortEnabled = true;
    Order sortOrder = Order.CATEGORY_NAME;
    ArrayList<String> sortCategoryOrder = new ArrayList<>();
    boolean sortShulkersAtEnd = true;
    boolean sortShulkersInverted = false;
    boolean sortBundlesAtEnd = true;
    boolean sortBundlesInverted = false;
    ArrayList<String> sortTopPriority = new ArrayList<>();
    ArrayList<String> sortBottomPriority = new ArrayList<>();
    Map<String, String> gestures = defaultGestures();
    boolean refill = true;
    boolean suppressRefillOnDrop = true;
    Match refillItems = Match.ID;
    Match refillEquipment = Match.ID;
    Match replaceEquipment = Match.OFF;
    Match refillTools = Match.ID;
    Match replaceTools = Match.OFF;

    private static Map<String, String> defaultGestures() {
        return new java.util.LinkedHashMap<>(Map.ofEntries(
            Map.entry("moveAll", "ALT+SHIFT+LEFT"), Map.entry("moveMatching", "ALT+SHIFT+RIGHT"),
            Map.entry("moveAllWithHotbar", "ALT+SHIFT+CTRL+LEFT"), Map.entry("moveMatchingWithHotbar", "ALT+SHIFT+CTRL+RIGHT"),
            Map.entry("dragStacks", "SHIFT+LEFT"), Map.entry("dragLeaveOne", "SHIFT+RIGHT"),
            Map.entry("dragOne", "CTRL+LEFT"), Map.entry("dragMatching", "ALT+LEFT"),
            Map.entry("dragDropStacks", "SHIFT+DROP+LEFT"), Map.entry("dragDropLeaveOne", "SHIFT+DROP+RIGHT"),
            Map.entry("dragDropOne", "DROP+LEFT"), Map.entry("scrollOne", "WHEEL"),
            Map.entry("scrollStack", "SHIFT+WHEEL"), Map.entry("scrollMatching", "ALT+WHEEL"),
            Map.entry("scrollAll", "ALT+SHIFT+WHEEL"), Map.entry("dropStack", "SHIFT+DROP"),
            Map.entry("dropMatching", "ALT+DROP"), Map.entry("dropAll", "ALT+SHIFT+DROP"),
            Map.entry("dropAllWithHotbar", "ALT+SHIFT+CTRL+DROP"), Map.entry("outsideMatchingAlt", "ALT+CLICK"),
            Map.entry("outsideMatchingShift", "SHIFT+CLICK"), Map.entry("outsideAll", "ALT+SHIFT+CLICK"),
            Map.entry("outsideAllWithHotbar", "ALT+SHIFT+CTRL+CLICK"),
            Map.entry("upstreamDropMatching", "CTRL+SHIFT+DROP")
        ));
    }

    public static void load(Minecraft minecraft) {
        if (path != null) return;
        path = minecraft.gameDirectory.toPath().resolve("config/comfortable-items.json");
        if (Files.isRegularFile(path)) {
            try {
                Settings loaded = JSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Settings.class);
                if (loaded == null) throw new IllegalArgumentException("Expected an object");
                if (loaded.gestures == null) loaded.gestures = defaultGestures();
                defaultGestures().forEach(loaded.gestures::putIfAbsent);
                if (loaded.sortOrder == null) loaded.sortOrder = Order.CATEGORY_NAME;
                if (loaded.sortCategoryOrder == null) loaded.sortCategoryOrder = new ArrayList<>();
                if (loaded.sortTopPriority == null) loaded.sortTopPriority = new ArrayList<>();
                if (loaded.sortBottomPriority == null) loaded.sortBottomPriority = new ArrayList<>();
                SORT.setKey(InputConstants.getKey(loaded.sortKey));
                current = loaded;
            } catch (IOException | RuntimeException error) {
                readable = false;
                LOG.warn("Could not read {}. Defaults will apply for this session.", path, error);
            }
        } else {
            save();
        }
    }

    public static void applySortKey() {
        SORT.setKey(InputConstants.getKey(current.sortKey));
        KeyMapping.resetMapping();
    }

    public static void saveSortKey() {
        String key = SORT.saveString();
        if (!key.equals(current.sortKey)) {
            current.sortKey = key;
            save();
        }
    }

    private static void save() {
        if (path == null || !readable) return;
        try {
            Files.createDirectories(path.getParent());
            Path temp = Files.createTempFile(path.getParent(), "comfortable-items-", ".json");
            try {
                Files.writeString(temp, JSON.toJson(current) + System.lineSeparator(), StandardCharsets.UTF_8);
                try {
                    Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException error) {
            LOG.warn("Could not save {}", path, error);
        }
    }

    private Settings() {}
}
