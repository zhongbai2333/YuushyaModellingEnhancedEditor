package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewLayer;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewPipRenderState;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldUi;
import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.scene_editor.core.camera.CameraFrame;
import com.zhongbai233.scene_editor.core.camera.CameraMatrices;
import com.zhongbai233.scene_editor.core.camera.EditorCameraMode;
import com.zhongbai233.scene_editor.core.camera.EditorCameraState;
import com.zhongbai233.scene_editor.core.projection.EditorViewport;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

/** Searchable item registry used by Yuushya's enhanced item modelling editor. */
public final class ItemPickerScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 470;
    private static final int PANEL_MARGIN = 18;
    private static final int HEADER_HEIGHT = 54;
    private static final int FOOTER_HEIGHT = 36;
    private static final int ROW_HEIGHT = 22;
    private static final int PREVIEW_COLUMN_MIN_WIDTH = 90;
    private static final int PREVIEW_COLUMN_MAX_WIDTH = 112;
    private static final Vector3d WORLD_UP = new Vector3d(0.0D, 1.0D, 0.0D);
    private final Screen parent;
    private final Consumer<ItemStack> selectionHandler;
    private final List<ItemEntry> allItems;
    private List<ItemEntry> filteredItems;
    private EditBox searchBox;
    private int scrollOffset;
    private ItemEntry cachedPreviewEntry;
    private BlockPreviewLayer cachedPreviewLayer;

    public ItemPickerScreen(Screen parent, Consumer<ItemStack> selectionHandler) {
        super(Component.translatable("screen.yuushya_modelling_enhanced_editor.add_item"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.selectionHandler = Objects.requireNonNull(selectionHandler, "selectionHandler");
        this.allItems = collectItems();
        this.filteredItems = allItems;
    }

    @Override
    protected void init() {
        searchBox = new EditBox(font, listX(), panelY() + 28, listWidth(), 18,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.search_item"));
        searchBox.setHint(Component.translatable("screen.yuushya_modelling_enhanced_editor.search_item_hint"));
        searchBox.setMaxLength(96);
        searchBox.setResponder(this::filter);
        addRenderableWidget(searchBox);
        addRenderableWidget(new BlackGoldButton(panelX() + 12, panelY() + panelHeight() - 27,
                76, 20, Component.translatable("screen.yuushya_modelling_enhanced_editor.cancel"),
                button -> onClose(), 0xFFD04040));
        setInitialFocus(searchBox);
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
        graphics.text(font, title, x + 12, y + 9, BlackGoldUi.GOLD, false);
        graphics.text(font, Component.translatable("screen.yuushya_modelling_enhanced_editor.item_picker_help"),
                x + 72, y + 9, BlackGoldUi.TEXT_DIM, false);
        drawItemPreview(graphics, mouseX, mouseY);
        drawRows(graphics, mouseX, mouseY);
        String count = Component.translatable("screen.yuushya_modelling_enhanced_editor.picker_count_item",
                filteredItems.size()).getString();
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, count, listWidth())),
                listX(), y + panelHeight() - 22, BlackGoldUi.TEXT_SECONDARY, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawItemPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = previewX();
        int y = previewY();
        int size = previewSize();
        graphics.fillGradient(x, y, x + size, y + size, 0xFF0B0E13, 0xFF151A22);
        graphics.outline(x, y, size, size, BlackGoldUi.GOLD_DIM);
        ItemEntry entry = previewEntry(mouseX, mouseY);
        if (entry == null) {
            graphics.centeredText(font, Component.translatable("screen.yuushya_modelling_enhanced_editor.no_preview"),
                    x + size / 2, y + size / 2 - 4, BlackGoldUi.TEXT_DIM);
            return;
        }

        EditorViewport viewport = new EditorViewport(x + 1, y + 1, Math.max(1, size - 2), Math.max(1, size - 2));
        EditorCameraState camera = EditorCameraState.lookingAt(EditorCameraMode.ORBIT,
                new Vector3d(2.5D, 2.0D, 3.0D), new Vector3d(), WORLD_UP,
                38.0F, 3.0F, 0.05F, 100.0F);
        CameraFrame frame = new CameraFrame(CameraMatrices.create(camera, viewport), viewport, camera.mode());
        graphics.submitPictureInPictureRenderState(new BlockPreviewPipRenderState(
                List.of(previewLayer(entry)), frame, null, 1.0F, false,
                viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + viewport.height(),
                graphics.peekScissorStack()));

        int textY = y + size + 6;
        int textWidth = previewColumnWidth() - 12;
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, entry.id(), textWidth)),
                x, textY, BlackGoldUi.CYAN, false);
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, entry.displayName(), textWidth)),
                x, textY + 12, BlackGoldUi.TEXT_SECONDARY, false);
    }

    private BlockPreviewLayer previewLayer(ItemEntry entry) {
        if (cachedPreviewEntry != entry) {
            cachedPreviewEntry = entry;
            cachedPreviewLayer = new BlockPreviewLayer(
                    new BlockPreviewLayer.ItemContent(entry.stack()), EditorTransform.IDENTITY, false);
        }
        return cachedPreviewLayer;
    }

    private void drawRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = listX();
        int y = panelY() + HEADER_HEIGHT;
        int rowWidth = listWidth();
        if (filteredItems.isEmpty()) {
            graphics.centeredText(font,
                    Component.translatable("screen.yuushya_modelling_enhanced_editor.no_items"),
                    x + rowWidth / 2, y + 18, BlackGoldUi.TEXT_DIM);
            return;
        }
        int end = Math.min(filteredItems.size(), scrollOffset + visibleRows());
        for (int index = scrollOffset; index < end; index++) {
            ItemEntry entry = filteredItems.get(index);
            int rowY = y + (index - scrollOffset) * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + rowWidth
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT - 2;
            int background = hovered ? 0xFF2A2A20 : 0xFF15171B;
            graphics.fillGradient(x, rowY, x + rowWidth, rowY + ROW_HEIGHT - 2, background, background);
            graphics.item(entry.stack(), x + 3, rowY + 2);
            String id = BlackGoldUi.ellipsize(font, entry.id(), Math.max(70, rowWidth / 2));
            graphics.text(font, Component.literal(id), x + 24, rowY + 6,
                    hovered ? BlackGoldUi.CYAN : BlackGoldUi.TEXT_PRIMARY, false);
            String displayName = BlackGoldUi.ellipsize(font, entry.displayName(), Math.max(40, rowWidth / 3));
            graphics.text(font, Component.literal(displayName), x + rowWidth - 6 - font.width(displayName),
                    rowY + 6, BlackGoldUi.TEXT_SECONDARY, false);
        }
        if (filteredItems.size() > visibleRows()) {
            int trackX = x + rowWidth - 3;
            int trackHeight = visibleRows() * ROW_HEIGHT - 2;
            int thumbHeight = Math.max(14, trackHeight * visibleRows() / filteredItems.size());
            int maximum = maxScroll();
            int thumbY = y + (maximum == 0 ? 0
                    : (trackHeight - thumbHeight) * scrollOffset / maximum);
            graphics.fillGradient(trackX, y, trackX + 3, y + trackHeight,
                    0xFF242424, 0xFF242424);
            graphics.fillGradient(trackX, thumbY, trackX + 3, thumbY + thumbHeight,
                    BlackGoldUi.GOLD_DIM, BlackGoldUi.GOLD_DIM);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean cancelled) {
        if (!cancelled && event.button() == 0) {
            int row = (int) ((event.y() - (panelY() + HEADER_HEIGHT)) / ROW_HEIGHT);
            if (event.x() >= listX() && event.x() < listX() + listWidth()
                    && row >= 0 && row < visibleRows()) {
                int index = scrollOffset + row;
                if (index < filteredItems.size()) {
                    choose(filteredItems.get(index));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, cancelled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D && mouseX >= listX() && mouseX < listX() + listWidth()
                && mouseY >= panelY() + HEADER_HEIGHT
                && mouseY < panelY() + panelHeight() - FOOTER_HEIGHT) {
            scrollOffset = Math.clamp(scrollOffset + (scrollY < 0.0D ? 3 : -3), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (!filteredItems.isEmpty()) choose(filteredItems.get(scrollOffset));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }

    private void choose(ItemEntry entry) {
        selectionHandler.accept(entry.stack());
        minecraft.setScreen(parent);
    }

    private void filter(String input) {
        String query = input == null ? "" : input.strip().toLowerCase(Locale.ROOT);
        filteredItems = query.isEmpty() ? allItems : allItems.stream()
                .filter(entry -> entry.searchText().contains(query)).toList();
        scrollOffset = 0;
        cachedPreviewEntry = null;
        cachedPreviewLayer = null;
    }

    private int panelWidth() { return Math.max(260, Math.min(PANEL_MAX_WIDTH, width - PANEL_MARGIN * 2)); }
    private int panelHeight() { return Math.max(170, height - PANEL_MARGIN * 2); }
    private int panelX() { return (width - panelWidth()) / 2; }
    private int panelY() { return (height - panelHeight()) / 2; }
    private int previewColumnWidth() {
        return Math.clamp(panelWidth() * 28 / 100,
                PREVIEW_COLUMN_MIN_WIDTH, PREVIEW_COLUMN_MAX_WIDTH);
    }
    private int previewX() { return panelX() + 12; }
    private int previewY() { return panelY() + HEADER_HEIGHT; }
    private int previewSize() {
        return Math.max(48, Math.min(previewColumnWidth() - 12,
                panelHeight() - HEADER_HEIGHT - FOOTER_HEIGHT - 8));
    }
    private int listX() { return panelX() + previewColumnWidth() + 12; }
    private int listWidth() { return Math.max(80, panelX() + panelWidth() - 12 - listX()); }
    private ItemEntry previewEntry(double mouseX, double mouseY) {
        if (filteredItems.isEmpty()) return null;
        int row = (int) ((mouseY - (panelY() + HEADER_HEIGHT)) / ROW_HEIGHT);
        if (mouseX >= listX() && mouseX < listX() + listWidth()
                && row >= 0 && row < visibleRows()) {
            int index = scrollOffset + row;
            if (index < filteredItems.size()) return filteredItems.get(index);
        }
        return filteredItems.get(Math.min(scrollOffset, filteredItems.size() - 1));
    }
    private int visibleRows() { return Math.max(4, (panelHeight() - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT); }
    private int maxScroll() { return Math.max(0, filteredItems.size() - visibleRows()); }

    private static List<ItemEntry> collectItems() {
        List<ItemEntry> entries = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            ItemStack stack = item.getDefaultInstance();
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            String displayName = stack.getHoverName().getString();
            entries.add(new ItemEntry(stack, id, displayName,
                    (id + ' ' + displayName).toLowerCase(Locale.ROOT)));
        }
        entries.sort((left, right) -> left.id().compareTo(right.id()));
        return List.copyOf(entries);
    }

    private record ItemEntry(ItemStack stack, String id, String displayName, String searchText) { }
}
