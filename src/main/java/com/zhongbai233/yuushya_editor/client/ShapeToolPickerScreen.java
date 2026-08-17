package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldUi;
import com.zhongbai233.yuushya_editor.compat.YuushyaShapeToolBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Inventory-slot picker that accepts only Yuushya's Shape Tool. */
public final class ShapeToolPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 224;
    private static final int PANEL_HEIGHT = 160;
    private static final int SLOT_SIZE = 20;
    private static final int GRID_COLUMNS = 9;
    private final Screen parent;
    private final IntConsumer selectionHandler;

    public ShapeToolPickerScreen(Screen parent, IntConsumer selectionHandler) {
        super(Component.translatable("screen.yuushya_modelling_enhanced_editor.shape_tool_picker.title"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.selectionHandler = Objects.requireNonNull(selectionHandler, "selectionHandler");
    }

    @Override
    protected void init() {
        addRenderableWidget(new BlackGoldButton(panelX() + PANEL_WIDTH - 88,
                panelY() + PANEL_HEIGHT - 27, 76, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.cancel"),
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
        graphics.fillGradient(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT,
                BlackGoldUi.PANEL_TOP, BlackGoldUi.PANEL_BOTTOM);
        graphics.outline(x, y, PANEL_WIDTH, PANEL_HEIGHT, BlackGoldUi.GOLD_DIM);
        graphics.text(font, title, x + 12, y + 9, BlackGoldUi.GOLD, false);
        Component help = Component.translatable("screen.yuushya_modelling_enhanced_editor.shape_tool_picker.help");
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, help.getString(), PANEL_WIDTH - 24)),
                x + 12, y + 23, BlackGoldUi.TEXT_DIM, false);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < GRID_COLUMNS; column++) {
                drawSlot(graphics, 9 + row * GRID_COLUMNS + column,
                        gridX() + column * SLOT_SIZE, gridY() + row * SLOT_SIZE, mouseX, mouseY);
            }
        }
        for (int column = 0; column < GRID_COLUMNS; column++) {
            drawSlot(graphics, column, gridX() + column * SLOT_SIZE,
                    gridY() + 68, mouseX, mouseY);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        SlotHit hit = slotAt(mouseX, mouseY);
        if (hit != null && !hit.stack().isEmpty()) {
            if (YuushyaShapeToolBridge.isShapeTool(hit.stack())) {
                graphics.setTooltipForNextFrame(font, hit.stack(), mouseX, mouseY);
            } else {
                List<Component> tooltip = new ArrayList<>(getTooltipFromItem(minecraft, hit.stack()));
                tooltip.add(Component.translatable(
                        "screen.yuushya_modelling_enhanced_editor.shape_tool_picker.unavailable"));
                graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY, hit.stack());
            }
        }
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int slot, int x, int y, int mouseX, int mouseY) {
        ItemStack stack = stackAt(slot);
        boolean hovered = contains(x, y, mouseX, mouseY);
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, hovered ? 0xFF2A2A20 : 0xFF15171B);
        graphics.outline(x, y, SLOT_SIZE, SLOT_SIZE, hovered ? BlackGoldUi.CYAN : 0xFF353941);
        if (stack.isEmpty()) return;
        graphics.item(stack, x + 2, y + 2);
        graphics.itemDecorations(font, stack, x + 2, y + 2);
        if (!YuushyaShapeToolBridge.isShapeTool(stack)) {
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xA0808080);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean cancelled) {
        if (!cancelled && event.button() == 0) {
            SlotHit hit = slotAt(event.x(), event.y());
            if (hit != null && YuushyaShapeToolBridge.isShapeTool(hit.stack())) {
                selectionHandler.accept(hit.slot());
                minecraft.setScreen(parent);
                return true;
            }
        }
        return super.mouseClicked(event, cancelled);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private SlotHit slotAt(double mouseX, double mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < GRID_COLUMNS; column++) {
                int x = gridX() + column * SLOT_SIZE;
                int y = gridY() + row * SLOT_SIZE;
                if (contains(x, y, mouseX, mouseY)) return hit(9 + row * GRID_COLUMNS + column);
            }
        }
        for (int column = 0; column < GRID_COLUMNS; column++) {
            int x = gridX() + column * SLOT_SIZE;
            if (contains(x, gridY() + 68, mouseX, mouseY)) return hit(column);
        }
        return null;
    }

    private SlotHit hit(int slot) { return new SlotHit(slot, stackAt(slot)); }
    private ItemStack stackAt(int slot) {
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getInventory().getItem(slot);
    }
    private static boolean contains(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
    }
    private int panelX() { return (width - PANEL_WIDTH) / 2; }
    private int panelY() { return (height - PANEL_HEIGHT) / 2; }
    private int gridX() { return panelX() + 12; }
    private int gridY() { return panelY() + 41; }
    private record SlotHit(int slot, ItemStack stack) { }
}
