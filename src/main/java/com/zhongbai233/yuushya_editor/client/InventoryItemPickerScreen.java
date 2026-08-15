package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldUi;
import com.zhongbai233.yuushya_editor.compat.YuushyaItemModelSupport;
import com.zhongbai233.yuushya_editor.core.EditorType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Read-only picker for copying an exact stack from the player's inventory into a model layer. */
public final class InventoryItemPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 244;
    private static final int PANEL_HEIGHT = 174;
    private static final int SLOT_SIZE = 20;
    private static final int GRID_COLUMNS = 9;
    private static final int MAIN_ROWS = 3;
    private static final int MAIN_FIRST_SLOT = 9;
    private static final int HOTBAR_FIRST_SLOT = 0;
    private final Screen parent;
    private final EditorType editorType;
    private final Consumer<ItemStack> selectionHandler;

    public InventoryItemPickerScreen(Screen parent, EditorType editorType,
            Consumer<ItemStack> selectionHandler) {
        super(Component.translatable("screen.yuushya_modelling_enhanced_editor.inventory_picker.title"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.editorType = Objects.requireNonNull(editorType, "editorType");
        if (editorType == EditorType.TEXT) {
            throw new IllegalArgumentException("Text models cannot contain inventory items");
        }
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
        graphics.fillGradient(x, y, x + PANEL_WIDTH, y + 2, BlackGoldUi.GOLD, BlackGoldUi.GOLD);
        graphics.text(font, title, x + 12, y + 9, BlackGoldUi.GOLD, false);
        Component help = Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.inventory_picker.help");
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font,
                        help.getString(), PANEL_WIDTH - 24)),
                x + 12, y + 23, BlackGoldUi.TEXT_DIM, false);

        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int column = 0; column < GRID_COLUMNS; column++) {
                int slot = MAIN_FIRST_SLOT + row * GRID_COLUMNS + column;
                drawSlot(graphics, slot, gridX() + column * SLOT_SIZE,
                        mainGridY() + row * SLOT_SIZE, mouseX, mouseY);
            }
        }
        for (int column = 0; column < GRID_COLUMNS; column++) {
            drawSlot(graphics, HOTBAR_FIRST_SLOT + column, gridX() + column * SLOT_SIZE,
                    hotbarY(), mouseX, mouseY);
        }
        graphics.centeredText(font, Component.translatable(
                        "screen.yuushya_modelling_enhanced_editor.inventory_picker.offhand"),
                offhandX() + SLOT_SIZE / 2, hotbarY() - 11, BlackGoldUi.TEXT_DIM);
        drawSlot(graphics, Inventory.SLOT_OFFHAND, offhandX(), hotbarY(), mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        SlotHit hovered = slotAt(mouseX, mouseY);
        if (hovered != null && !hovered.stack().isEmpty()) {
            if (selectable(hovered.stack())) {
                graphics.setTooltipForNextFrame(font, hovered.stack(), mouseX, mouseY);
            } else {
                List<Component> tooltip = new ArrayList<>(getTooltipFromItem(minecraft, hovered.stack()));
                tooltip.add(Component.translatable(
                        "screen.yuushya_modelling_enhanced_editor.inventory_picker.block_only"));
                graphics.setComponentTooltipForNextFrame(font, tooltip,
                        mouseX, mouseY, hovered.stack());
            }
        }
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int inventorySlot,
            int x, int y, int mouseX, int mouseY) {
        ItemStack stack = stackAt(inventorySlot);
        boolean hovered = contains(x, y, mouseX, mouseY);
        int background = hovered ? 0xFF2A2A20 : 0xFF15171B;
        graphics.fillGradient(x, y, x + SLOT_SIZE, y + SLOT_SIZE, background, background);
        graphics.outline(x, y, SLOT_SIZE, SLOT_SIZE,
                hovered ? BlackGoldUi.CYAN : 0xFF353941);
        if (stack.isEmpty()) return;
        graphics.item(stack, x + 2, y + 2);
        graphics.itemDecorations(font, stack, x + 2, y + 2);
        if (!selectable(stack)) {
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xA0808080);
            graphics.outline(x, y, SLOT_SIZE, SLOT_SIZE,
                    hovered ? 0xFFFF6B6B : 0xFF666666);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean cancelled) {
        if (!cancelled && event.button() == 0) {
            SlotHit hit = slotAt(event.x(), event.y());
            if (hit != null && !hit.stack().isEmpty() && selectable(hit.stack())) {
                selectionHandler.accept(hit.stack().copy());
                minecraft.setScreen(parent);
                return true;
            }
        }
        return super.mouseClicked(event, cancelled);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean selectable(ItemStack stack) {
        return editorType == EditorType.ITEM
                || YuushyaItemModelSupport.resolveBlockState(stack) != null;
    }

    private SlotHit slotAt(double mouseX, double mouseY) {
        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int column = 0; column < GRID_COLUMNS; column++) {
                int x = gridX() + column * SLOT_SIZE;
                int y = mainGridY() + row * SLOT_SIZE;
                if (contains(x, y, mouseX, mouseY)) {
                    int slot = MAIN_FIRST_SLOT + row * GRID_COLUMNS + column;
                    return new SlotHit(stackAt(slot));
                }
            }
        }
        for (int column = 0; column < GRID_COLUMNS; column++) {
            int x = gridX() + column * SLOT_SIZE;
            if (contains(x, hotbarY(), mouseX, mouseY)) {
                int slot = HOTBAR_FIRST_SLOT + column;
                return new SlotHit(stackAt(slot));
            }
        }
        return contains(offhandX(), hotbarY(), mouseX, mouseY)
                ? new SlotHit(stackAt(Inventory.SLOT_OFFHAND)) : null;
    }

    private ItemStack stackAt(int slot) {
        return minecraft.player == null ? ItemStack.EMPTY
                : minecraft.player.getInventory().getItem(slot);
    }

    private static boolean contains(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + SLOT_SIZE
                && mouseY >= y && mouseY < y + SLOT_SIZE;
    }

    private int panelX() { return (width - PANEL_WIDTH) / 2; }
    private int panelY() { return (height - PANEL_HEIGHT) / 2; }
    private int gridX() { return panelX() + 12; }
    private int mainGridY() { return panelY() + 41; }
    private int hotbarY() { return mainGridY() + MAIN_ROWS * SLOT_SIZE + 8; }
    private int offhandX() { return panelX() + PANEL_WIDTH - SLOT_SIZE - 12; }

    private record SlotHit(ItemStack stack) { }
}
