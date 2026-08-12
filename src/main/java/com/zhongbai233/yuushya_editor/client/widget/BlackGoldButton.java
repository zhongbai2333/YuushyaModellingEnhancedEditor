package com.zhongbai233.yuushya_editor.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** NCPB-style button: dark face, accent rail, bottom rule, and compact hover treatment. */
public final class BlackGoldButton extends Button {
    private final int accentColor;

    public BlackGoldButton(int x, int y, int width, int height, Component message,
            OnPress onPress, int accentColor) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.accentColor = accentColor;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int background;
        int border;
        if (!active) {
            background = 0xFF111111;
            border = 0xFF222222;
        } else if (isHoveredOrFocused()) {
            background = 0xFF2A2A20;
            border = accentColor;
        } else {
            background = 0xFF1A1A1A;
            border = 0xFF333333;
        }
        graphics.fillGradient(getX(), getY(), getX() + width, getY() + height, background, background);
        graphics.fillGradient(getX(), getY(), getX() + 2, getY() + height, border, border);
        graphics.fillGradient(getX(), getY() + height - 1, getX() + width, getY() + height, border, border);

        var font = Minecraft.getInstance().font;
        int textColor = active
                ? isHoveredOrFocused() ? accentColor : BlackGoldUi.TEXT_PRIMARY
                : BlackGoldUi.TEXT_DIM;
        Component visible = Component.literal(BlackGoldUi.ellipsize(font, getMessage().getString(),
                Math.max(0, width - 10)));
        graphics.centeredText(font, visible, getX() + width / 2, getY() + (height - 8) / 2, textColor);
    }
}
