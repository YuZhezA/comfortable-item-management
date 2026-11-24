package dev.comfortableitems;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

final class SettingsScreen extends ConfigScreenBase {
    private final Screen parent;
    private final JsonObject original = Settings.snapshot();
    private final Map<ConfigOptions.Option, Object> edits = new LinkedHashMap<>();
    private int group;
    private int page;
    private Component error;

    SettingsScreen(Screen parent) {
        super(ConfigOptions.text("title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int span = Math.min(460, width - 24);
        int left = (width - span) / 2;
        int rows = Math.max(1, (height - 98) / 24);
        var options = ConfigOptions.GROUPS.get(group).options();
        int pages = (options.size() + rows - 1) / rows;
        page = Math.min(page, pages - 1);

        label(title, left, 9, span);
        int tabWidth = span / ConfigOptions.GROUPS.size();
        for (int i = 0; i < ConfigOptions.GROUPS.size(); i++) {
            int tab = i;
            button(Component.translatable(ConfigOptions.GROUPS.get(i).key()), left + i * tabWidth, 29,
                tabWidth - 3, () -> { group = tab; page = 0; rebuildWidgets(); }).active = i != group;
        }
        int labelWidth = span / 2;
        for (int i = page * rows; i < Math.min(options.size(), (page + 1) * rows); i++) {
            addOption(options.get(i), left, 55 + (i % rows) * 24, labelWidth, span - labelWidth);
        }

        int center = width / 2;
        button(Component.literal("<"), center - 69, height - 45, 22,
            () -> { page--; rebuildWidgets(); }).active = page > 0;
        label(Component.literal((page + 1) + " / " + pages), center - 47, height - 45, 94);
        button(Component.literal(">"), center + 47, height - 45, 22,
            () -> { page++; rebuildWidgets(); }).active = page + 1 < pages;
        button(CommonComponents.GUI_CANCEL, left, height - 24, span / 2 - 3, this::onClose);
        button(ConfigOptions.text("save"), center + 3, height - 24, span / 2 - 3, this::save).active = Settings.canWrite();
        if (!Settings.canWrite()) error = ConfigOptions.text("write_failed");
        if (error != null) label(error, left, height - 64, span);
    }

    private void addOption(ConfigOptions.Option option, int x, int y, int labelWidth, int fieldWidth) {
        Component name = Component.translatable(option.key());
        label(name, x, y, labelWidth - 5);
        Object value = edits.getOrDefault(option, option.read(original));
        int fieldX = x + labelWidth;
        if (option.toggle() || !option.choices().isEmpty()) {
            Button control = addRenderableWidget(Button.builder(valueLabel(value), pressed -> {
                Object previous = edits.getOrDefault(option, option.read(original));
                Object next;
                if (option.toggle()) {
                    next = !(Boolean) previous;
                } else {
                    var choices = option.choices();
                    next = choices.get((choices.indexOf(previous) + 1) % choices.size());
                }
                edits.put(option, next);
                pressed.setMessage(valueLabel(next));
            }).bounds(fieldX, y, fieldWidth, 20).build());
            describe(control, option.hint());
        } else {
            EditBox field = new EditBox(font, fieldX, y, fieldWidth, 20, name);
            String text = (String) value;
            field.setMaxLength(Math.max(32767, text.length()));
            field.setValue(text);
            field.setResponder(updated -> edits.put(option, updated));
            describe(field, option.hint());
            addRenderableWidget(field);
        }
    }

    private static void describe(net.minecraft.client.gui.components.AbstractWidget control, Component hint) {
        if (hint != null) control.setTooltip(Tooltip.create(hint));
    }

    private static Component valueLabel(Object value) {
        if (value instanceof Boolean flag) return flag ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
        return Component.literal(value.toString());
    }

    private StringWidget label(Component text, int x, int y, int span) {
        return addRenderableOnly(new StringWidget(x, y, span, 20, text, font));
    }

    private Button button(Component text, int x, int y, int span, Runnable action) {
        return addRenderableWidget(Button.builder(text, ignored -> action.run()).bounds(x, y, span, 20).build());
    }

    private void save() {
        JsonObject draft = Settings.snapshot();
        for (var edit : edits.entrySet()) {
            var option = edit.getKey();
            if (!option.accepts(edit.getValue())) {
                error = Component.translatable("comfortable_items.config.invalid", Component.translatable(option.key()));
                rebuildWidgets();
                return;
            }
            option.write(draft, edit.getValue());
        }
        if (edits.isEmpty() || Settings.apply(draft)) {
            onClose();
        } else {
            error = ConfigOptions.text("write_failed");
            rebuildWidgets();
        }
    }

    @Override
    public void onClose() {
        returnTo(parent);
    }
}
