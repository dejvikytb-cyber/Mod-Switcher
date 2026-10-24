package dev.modswitch;

import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public final class ModSwitchScreen extends Screen {
    private final Screen parent;
    private List<ModContainer> mods;
    private int scroll = 0;
    private static final int ROW_HEIGHT = 24;
    private static final int VISIBLE_ROWS = 10;

    public ModSwitchScreen() { this(null); }

    public ModSwitchScreen(Screen parent) {
        super(Text.literal("ModSwitch"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        mods = ModSwitchStore.getMods();
        int left = this.width / 2 - 155;
        int top = 48;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("↑"), b -> {
            scroll = Math.max(0, scroll - 1);
            clearAndInit();
        }).dimensions(this.width - 54, top, 20, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("↓"), b -> {
            scroll = Math.min(Math.max(0, mods.size() - VISIBLE_ROWS), scroll + 1);
            clearAndInit();
        }).dimensions(this.width - 30, top, 20, 20).build());

        int end = Math.min(mods.size(), scroll + VISIBLE_ROWS);
        for (int i = scroll; i < end; i++) {
            ModContainer mod = mods.get(i);
            String id = mod.getMetadata().getId();
            boolean protectedMod = id.equals("fabricloader") || id.equals("minecraft") || id.equals("modswitch");
            boolean disabled = ModSwitchStore.queuedForDisable(id);
            boolean enabling = ModSwitchStore.queuedForEnable(id);
            String state = protectedMod ? "CORE" : disabled ? "OFF NEXT START" : enabling ? "ON NEXT START" : "ENABLED";
            String label = shorten(mod.getMetadata().getName(), 20) + "  [" + state + "]";
            int y = top + 28 + (i - scroll) * ROW_HEIGHT;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                ModSwitchStore.toggle(mod);
                clearAndInit();
            }).dimensions(left, y, 310, 20).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Changes apply after closing Minecraft and restarting."),
                width / 2, 31, 0xFFCC55);
        super.render(context, mouseX, mouseY, delta);

        int bottom = height - 53;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Only JAR filenames matching the mod ID are moved automatically."),
                width / 2, bottom, 0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Dependencies may break startup. Back up your mods folder first."),
                width / 2, bottom + 12, 0xFF8888);
    }

    private static String shorten(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max - 3) + "...";
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() { return false; }
}
