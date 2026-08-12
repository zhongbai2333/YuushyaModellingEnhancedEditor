package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldUi;
import com.zhongbai233.yuushya_editor.core.geometry.ZFightSaveCoordinator;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explicit save gate shown when transformed block faces are likely to depth-fight. */
public final class ZFightWarningScreen extends Screen {
    private static final int PANEL_WIDTH = 440;
    private static final int PANEL_HEIGHT = 170;
    private final int pairCount;
    private final int faceCount;
    private final Consumer<ZFightSaveCoordinator.Decision> decisionHandler;

    public ZFightWarningScreen(int pairCount, int faceCount,
            Consumer<ZFightSaveCoordinator.Decision> decisionHandler) {
        super(Component.translatable("screen.yuushya_modelling_enhanced_editor.zfight.title"));
        this.pairCount = pairCount;
        this.faceCount = faceCount;
        this.decisionHandler = Objects.requireNonNull(decisionHandler, "decisionHandler");
    }

    @Override
    protected void init() {
        int x = panelX();
        int y = panelY();
        int available = panelWidth() - 24;
        int firstWidth = 150;
        int secondWidth = 104;
        int thirdWidth = available - firstWidth - secondWidth - 8;
        int buttonY = y + panelHeight() - 32;
        addRenderableWidget(new BlackGoldButton(x + 12, buttonY, firstWidth, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.zfight.optimize"),
                button -> decisionHandler.accept(ZFightSaveCoordinator.Decision.OPTIMIZE_AND_SAVE),
                BlackGoldUi.CYAN));
        addRenderableWidget(new BlackGoldButton(x + 16 + firstWidth, buttonY, secondWidth, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.zfight.save_anyway"),
                button -> decisionHandler.accept(ZFightSaveCoordinator.Decision.SAVE_UNCHANGED),
                BlackGoldUi.GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(x + 20 + firstWidth + secondWidth, buttonY, thirdWidth, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.zfight.back"),
                button -> onClose(), 0xFFD04040));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xE005070A, 0xF00A0D12);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = panelX();
        int y = panelY();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        graphics.fillGradient(x, y, x + panelWidth, y + panelHeight,
                BlackGoldUi.PANEL_TOP, BlackGoldUi.PANEL_BOTTOM);
        graphics.outline(x, y, panelWidth, panelHeight, 0xFFD98C3F);
        graphics.fillGradient(x, y, x + panelWidth, y + 2, 0xFFFFB347, 0xFFFFB347);
        graphics.text(font, title, x + 12, y + 12, 0xFFFFB347, false);
        int textY = drawWrapped(graphics,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.zfight.summary",
                        pairCount, faceCount), x + 12, y + 37,
                panelWidth - 24, BlackGoldUi.TEXT_PRIMARY);
        textY = drawWrapped(graphics,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.zfight.explanation"),
                x + 12, textY + 4, panelWidth - 24, BlackGoldUi.TEXT_SECONDARY);
        drawWrapped(graphics,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.zfight.note"),
                x + 12, textY + 4, panelWidth - 24, BlackGoldUi.TEXT_DIM);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private int drawWrapped(GuiGraphicsExtractor graphics, Component text,
            int x, int y, int availableWidth, int color) {
        int nextY = y;
        for (var line : font.split(text, availableWidth)) {
            graphics.text(font, line, x, nextY, color, false);
            nextY += 11;
        }
        return nextY;
    }

    @Override
    public void onClose() {
        decisionHandler.accept(ZFightSaveCoordinator.Decision.RETURN_TO_EDITOR);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelWidth() {
        return Math.max(300, Math.min(PANEL_WIDTH, width - 24));
    }

    private int panelHeight() {
        return Math.min(PANEL_HEIGHT, height - 24);
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }
}
