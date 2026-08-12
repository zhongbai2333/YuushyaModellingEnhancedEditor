package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldUi;
import com.zhongbai233.yuushya_editor.core.TextModelData;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Plain-text content editor that preserves existing styled JSON unless its text is changed. */
public final class TextContentEditorScreen extends Screen {
    private final Screen parent;
    private final TextModelData initial;
    private final Consumer<TextModelData> saveHandler;
    private final String initialPlainText;
    private MultiLineEditBox editor;
    private boolean culled;
    private boolean mirror;
    private BlackGoldButton cullButton;
    private BlackGoldButton mirrorButton;

    private TextContentEditorScreen(Screen parent, TextModelData initial, Consumer<TextModelData> saveHandler) {
        super(Component.translatable("screen.yuushya_modelling_enhanced_editor.text_editor.title"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.initial = Objects.requireNonNull(initial, "initial");
        this.saveHandler = Objects.requireNonNull(saveHandler, "saveHandler");
        this.initialPlainText = YuushyaTextCodec.plainText(initial.textLines());
        this.culled = initial.culled();
        this.mirror = initial.mirror();
    }

    public static TextContentEditorScreen forNewLayer(Screen parent, Consumer<TextModelData> saveHandler) {
        return new TextContentEditorScreen(parent, new TextModelData(java.util.List.of(), false, false),
                saveHandler);
    }

    public static TextContentEditorScreen forExisting(Screen parent, TextModelData data,
            Consumer<TextModelData> saveHandler) {
        return new TextContentEditorScreen(parent, data, saveHandler);
    }

    @Override
    protected void init() {
        int x = panelX();
        int y = panelY();
        editor = MultiLineEditBox.builder().setX(x + 12).setY(y + 34)
                .setPlaceholder(Component.translatable("screen.yuushya_modelling_enhanced_editor.text_editor.placeholder"))
                .build(font, panelWidth() - 24, panelHeight() - 92,
                        Component.translatable("screen.yuushya_modelling_enhanced_editor.text_editor.content"));
        editor.setCharacterLimit(8192);
        editor.setValue(initialPlainText);
        addRenderableWidget(editor);
        cullButton = addRenderableWidget(new BlackGoldButton(x + 12, y + panelHeight() - 50,
                104, 20, Component.empty(), button -> { culled = !culled; syncButtons(); }, BlackGoldUi.GOLD_DIM));
        mirrorButton = addRenderableWidget(new BlackGoldButton(x + 120, y + panelHeight() - 50,
                104, 20, Component.empty(), button -> { mirror = !mirror; syncButtons(); }, BlackGoldUi.GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(x + panelWidth() - 172, y + panelHeight() - 26,
                76, 20, Component.translatable("screen.yuushya_modelling_enhanced_editor.cancel"),
                button -> onClose(), 0xFFD04040));
        addRenderableWidget(new BlackGoldButton(x + panelWidth() - 92, y + panelHeight() - 26,
                80, 20, Component.translatable("screen.yuushya_modelling_enhanced_editor.apply_short"),
                button -> save(), BlackGoldUi.CYAN));
        setInitialFocus(editor);
        syncButtons();
    }

    private void syncButtons() {
        cullButton.setMessage(Component.translatable("screen.yuushya_modelling_enhanced_editor.text_editor.cull",
                Component.translatable(culled ? "screen.yuushya_modelling_enhanced_editor.on"
                        : "screen.yuushya_modelling_enhanced_editor.off")));
        mirrorButton.setMessage(Component.translatable("screen.yuushya_modelling_enhanced_editor.text_editor.mirror",
                Component.translatable(mirror ? "screen.yuushya_modelling_enhanced_editor.on"
                        : "screen.yuushya_modelling_enhanced_editor.off")));
    }

    private void save() {
        String value = editor.getValue();
        java.util.List<String> lines = value.equals(initialPlainText)
                ? initial.textLines() : YuushyaTextCodec.encodePlainLines(value);
        saveHandler.accept(new TextModelData(lines, culled, mirror));
        minecraft.setScreen(parent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xE005070A, 0xF00A0D12);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = panelX();
        int y = panelY();
        graphics.fillGradient(x, y, x + panelWidth(), y + panelHeight(),
                BlackGoldUi.PANEL_TOP, BlackGoldUi.PANEL_BOTTOM);
        graphics.outline(x, y, panelWidth(), panelHeight(), BlackGoldUi.GOLD_DIM);
        graphics.fillGradient(x, y, x + panelWidth(), y + 2, BlackGoldUi.GOLD, BlackGoldUi.GOLD);
        graphics.text(font, title, x + 12, y + 11, BlackGoldUi.GOLD, false);
        graphics.text(font, Component.translatable("screen.yuushya_modelling_enhanced_editor.text_editor.preserve_json"),
                x + 112, y + 11, BlackGoldUi.TEXT_DIM, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }

    private int panelWidth() { return Math.max(300, Math.min(520, width - 24)); }
    private int panelHeight() { return Math.max(180, Math.min(330, height - 24)); }
    private int panelX() { return (width - panelWidth()) / 2; }
    private int panelY() { return (height - panelHeight()) / 2; }
}
