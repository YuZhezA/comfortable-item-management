package dev.comfortableitems;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class ConfigOptions {
    private static final JsonObject DEFAULTS = Settings.defaults();
    private static final List<String> MATCH = names(Settings.Match.values());
    static final List<Group> GROUPS = List.of(
        group("general", "enabled", "reverseSingleScroll", "reverseStackScroll", "positionAwareScroll"),
        group("sort", "sortEnabled", "sortKey", "sortOrder", "sortCategoryOrder", "sortTopPriority",
            "sortBottomPriority", "sortShulkersAtEnd", "sortShulkersInverted", "sortBundlesAtEnd", "sortBundlesInverted"),
        group("refill", "refill", "suppressRefillOnDrop", "refillItems", "refillEquipment", "replaceEquipment",
            "refillTools", "replaceTools"),
        gestures());

    record Group(String name, List<Option> options) {
        String key() { return "comfortable_items.config.group." + name; }
    }

    record Option(String name, boolean gesture, JsonElement initial, List<String> choices) {
        String key() { return "comfortable_items.config.option." + name; }

        boolean toggle() { return initial.isJsonPrimitive() && initial.getAsJsonPrimitive().isBoolean(); }

        Object defaultValue() { return display(initial); }

        Object read(JsonObject json) {
            JsonElement entry = section(json).get(name);
            return display(entry == null || entry.isJsonNull() ? initial : entry);
        }

        private Object display(JsonElement entry) {
            if (toggle()) return entry.getAsBoolean();
            return entry.isJsonArray() ? entry.toString() : entry.getAsString();
        }

        boolean accepts(Object value) {
            try {
                decode(value);
                return true;
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        Component hint() {
            if (name.equals("sortKey")) return text("key_hint");
            if (gesture) return text("gesture_hint");
            if (initial.isJsonArray()) return text("list_hint");
            if (!choices.isEmpty()) return Component.literal(String.join(" / ", choices));
            return null;
        }

        void write(JsonObject json, Object value) {
            section(json).add(name, decode(value));
        }

        private JsonObject section(JsonObject json) {
            return gesture ? json.getAsJsonObject("gestures") : json;
        }

        private JsonElement decode(Object value) {
            if (toggle()) return new JsonPrimitive((Boolean) value);
            String text = ((String) value).strip();
            if (initial.isJsonArray()) {
                JsonElement list = JsonParser.parseString(text);
                for (JsonElement element : list.getAsJsonArray()) {
                    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                        throw new IllegalArgumentException("Expected a list of strings");
                    }
                }
                return list;
            }
            if (name.equals("sortKey")) {
                InputConstants.getKey(text);
            } else if (gesture) {
                String trigger = initial.getAsString().replaceFirst("^(?:(?:ALT|SHIFT|CTRL|DROP)\\+)+", "");
                if (!text.isEmpty() && !text.toUpperCase(Locale.ROOT).matches("(?:ALT\\+)?(?:SHIFT\\+)?(?:CTRL\\+)?(?:SUPER\\+)?" +
                        (trigger.equals("DROP") ? "" : "(?:DROP\\+)?") + trigger)) {
                    throw new IllegalArgumentException("Invalid input chord");
                }
            } else if (!choices.contains(text)) {
                throw new IllegalArgumentException("Unknown option");
            }
            return new JsonPrimitive(text);
        }
    }

    static Component text(String key) {
        return Component.translatable("comfortable_items.config." + key);
    }

    private static Group group(String name, String... fields) {
        return new Group(name, Arrays.stream(fields).map(field -> {
            List<String> choices = switch (field) {
                case "sortOrder" -> names(Settings.Order.values());
                case "refillItems" -> List.of("OFF", "ID", "STRICT");
                case "refillEquipment", "replaceEquipment", "refillTools", "replaceTools" -> MATCH;
                default -> List.of();
            };
            return new Option(field, false, DEFAULTS.get(field), choices);
        }).toList());
    }

    private static Group gestures() {
        List<Option> options = new ArrayList<>();
        DEFAULTS.getAsJsonObject("gestures").entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
            .forEach(entry -> options.add(new Option(entry.getKey(), true, entry.getValue(), List.of())));
        return new Group("gestures", List.copyOf(options));
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private ConfigOptions() {}
}
