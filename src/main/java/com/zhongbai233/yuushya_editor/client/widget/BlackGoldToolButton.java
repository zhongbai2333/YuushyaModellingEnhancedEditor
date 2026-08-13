package com.zhongbai233.yuushya_editor.client.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Compact black-gold toolbar button with code-drawn, font-independent tool icons. */
public final class BlackGoldToolButton extends Button {
    private final int accentColor;
    private final Icon icon;

    public BlackGoldToolButton(int x, int y, int width, int height, Component narration,
            OnPress onPress, int accentColor, Icon icon) {
        super(x, y, width, height, narration, onPress, DEFAULT_NARRATION);
        this.accentColor = accentColor;
        this.icon = java.util.Objects.requireNonNull(icon, "icon");
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

        int iconColor = active
                ? isHoveredOrFocused() ? accentColor : BlackGoldUi.TEXT_PRIMARY
                : BlackGoldUi.TEXT_DIM;
        icon.draw(graphics, getX() + width / 2, getY() + height / 2, iconColor);
    }

    public enum Icon {
        MOVE {
            @Override
            void draw(GuiGraphicsExtractor graphics, int x, int y, int color) {
                line(graphics, x - 5, y, x + 5, y, color);
                line(graphics, x, y - 5, x, y + 5, color);
                pixel(graphics, x - 4, y - 1, color);
                pixel(graphics, x - 4, y + 1, color);
                pixel(graphics, x + 4, y - 1, color);
                pixel(graphics, x + 4, y + 1, color);
                pixel(graphics, x - 1, y - 4, color);
                pixel(graphics, x + 1, y - 4, color);
                pixel(graphics, x - 1, y + 4, color);
                pixel(graphics, x + 1, y + 4, color);
            }
        },
        ROTATE {
            @Override
            void draw(GuiGraphicsExtractor graphics, int x, int y, int color) {
                int previousX = x + 4;
                int previousY = y - 3;
                for (int degrees = -35; degrees <= 285; degrees += 20) {
                    double radians = Math.toRadians(degrees);
                    int nextX = x + (int) Math.round(Math.cos(radians) * 5.0D);
                    int nextY = y + (int) Math.round(Math.sin(radians) * 5.0D);
                    line(graphics, previousX, previousY, nextX, nextY, color);
                    previousX = nextX;
                    previousY = nextY;
                }
                line(graphics, x + 1, y - 5, x + 5, y - 5, color);
                line(graphics, x + 5, y - 5, x + 5, y - 1, color);
            }
        },
        SCALE {
            @Override
            void draw(GuiGraphicsExtractor graphics, int x, int y, int color) {
                line(graphics, x - 4, y + 4, x + 4, y - 4, color);
                square(graphics, x - 5, y + 3, color);
                square(graphics, x + 3, y - 5, color);
            }
        };

        abstract void draw(GuiGraphicsExtractor graphics, int x, int y, int color);
    }

    private static void square(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 3, y + 3, color);
    }

    private static void pixel(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 1, y + 1, color);
    }

    private static void line(GuiGraphicsExtractor graphics, int startX, int startY,
            int endX, int endY, int color) {
        int x = startX;
        int y = startY;
        int dx = Math.abs(endX - startX);
        int dy = Math.abs(endY - startY);
        int stepX = startX < endX ? 1 : -1;
        int stepY = startY < endY ? 1 : -1;
        int error = dx - dy;
        while (true) {
            pixel(graphics, x, y, color);
            if (x == endX && y == endY) return;
            int doubled = error * 2;
            if (doubled > -dy) {
                error -= dy;
                x += stepX;
            }
            if (doubled < dx) {
                error += dx;
                y += stepY;
            }
        }
    }
}
