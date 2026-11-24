package dev.comfortableitems;

import com.google.gson.JsonObject;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.api.ValueEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;

public final class ConfiguredProvider implements IModConfigProvider {
    private static final String MOD_ID = "comfortable_items";

    @Override
    public Set<IModConfig> getConfigurationsForMod(ModContext context) {
        return MOD_ID.equals(context.modId()) ? Set.of(new ConfiguredClientFile()) : Set.of();
    }

    abstract static class ClientFile implements IModConfig {
        public ConfigType getType() { return ConfigType.CLIENT; }
        public String getFileName() { return "comfortable-items.json"; }
        public String getModId() { return MOD_ID; }
        public boolean isReadOnly() { return !Settings.canWrite(); }

        IConfigEntry read() {
            JsonObject settings = Settings.snapshot();
            List<IConfigEntry> folders = ConfigOptions.GROUPS.stream().map(group -> {
                List<IConfigEntry> entries = group.options().stream().map(option -> leaf(option, settings)).toList();
                return (IConfigEntry) new Folder(group.name(), group.key(), entries, false);
            }).toList();
            return new Folder("comfortable-items.json", null, folders, true);
        }

        boolean commit(IConfigEntry entry) {
            JsonObject draft = Settings.snapshot();
            applyEdits(entry, draft);
            return Settings.apply(draft);
        }

        private void applyEdits(IConfigEntry entry, JsonObject draft) {
            if (entry.getValue() instanceof Value<?> value && value.isChanged()) {
                value.option.write(draft, value.get());
            }
            entry.getChildren().forEach(child -> applyEdits(child, draft));
        }
    }

    private record Folder(String name, String translationKey, List<IConfigEntry> children, boolean root)
            implements IConfigEntry {
        public List<IConfigEntry> getChildren() { return children; }
        public boolean isRoot() { return root; }
        public boolean isLeaf() { return false; }
        public IConfigValue<?> getValue() { return null; }
        public String getEntryName() { return name; }
        public String getTranslationKey() { return translationKey; }
        public Component getTooltip() { return null; }
    }

    private static IConfigEntry leaf(ConfigOptions.Option option, JsonObject settings) {
        Object initial = option.defaultValue();
        Object current = option.read(settings);
        if (option.toggle()) return new ValueEntry(new Value<>(option, (Boolean) initial, (Boolean) current));
        return new ValueEntry(new Value<>(option, (String) initial, (String) current));
    }

    private static final class Value<T> implements IConfigValue<T> {
        private final ConfigOptions.Option option;
        private final T initial;
        private final T original;
        private T value;

        Value(ConfigOptions.Option option, T initial, T original) {
            this.option = option;
            this.initial = initial;
            this.original = original;
            this.value = original;
        }

        public T get() { return value; }
        public T getDefault() { return initial; }
        public void set(T value) { if (isValid(value)) this.value = value; }
        public boolean isValid(T value) { return option.accepts(value); }
        public boolean isDefault() { return value.equals(initial); }
        public boolean isChanged() { return !value.equals(original); }
        public void restore() { value = initial; }
        public Component getComment() { return option.hint(); }
        public Component getValidationHint() { return option.hint(); }
        public String getTranslationKey() { return option.key(); }
        public String getName() { return option.name(); }
        public void cleanCache() {}
        public boolean requiresWorldRestart() { return false; }
        public boolean requiresGameRestart() { return false; }
    }
}
