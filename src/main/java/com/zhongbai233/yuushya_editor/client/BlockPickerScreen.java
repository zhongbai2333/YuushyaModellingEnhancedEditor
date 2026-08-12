package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewLayer;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewPipRenderState;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldUi;
import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.camera.CameraFrame;
import com.zhongbai233.yuushya_editor.core.camera.CameraMatrices;
import com.zhongbai233.yuushya_editor.core.camera.CameraMode;
import com.zhongbai233.yuushya_editor.core.camera.CameraState;
import com.zhongbai233.yuushya_editor.core.projection.Viewport;
import java.util.ArrayList;
import java.util.Comparator;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

/** Searchable registry-backed block picker used by the enhanced Yuushya editor. */
public final class BlockPickerScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 470;
    private static final int PANEL_MARGIN = 18;
    private static final int HEADER_HEIGHT = 54;
    private static final int FOOTER_HEIGHT = 38;
    private static final int ROW_HEIGHT = 20;
    private static final int PREVIEW_COLUMN_MIN_WIDTH = 90;
    private static final int PREVIEW_COLUMN_MAX_WIDTH = 112;
    private static final Vector3d WORLD_UP = new Vector3d(0.0D, 1.0D, 0.0D);
    private final Screen parent;
    private final Consumer<BlockState> selectionHandler;
    private final List<BlockEntry> allBlocks;
    private List<BlockEntry> filteredBlocks;
    private EditBox searchBox;
    private int scrollOffset;

    public BlockPickerScreen(Screen parent, Consumer<BlockState> selectionHandler) {
        super(Component.translatable("screen.yuushya_modelling_enhanced_editor.add_block"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.selectionHandler = Objects.requireNonNull(selectionHandler, "selectionHandler");
        this.allBlocks = collectBlocks();
        this.filteredBlocks = allBlocks;
    }

    @Override
    protected void init() {
        int panelX = panelX();
        int panelY = panelY();
        int panelWidth = panelWidth();
        searchBox = new EditBox(font, listX(), panelY + 28, listWidth(), 18,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.search_block"));
        searchBox.setHint(Component.translatable("screen.yuushya_modelling_enhanced_editor.search_block_hint"));
        searchBox.setMaxLength(96);
        searchBox.setResponder(this::filter);
        addRenderableWidget(searchBox);
        addRenderableWidget(new BlackGoldButton(panelX + 12, panelY + panelHeight() - 28,
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
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        graphics.fillGradient(x, y, x + panelWidth, y + panelHeight,
                BlackGoldUi.PANEL_TOP, BlackGoldUi.PANEL_BOTTOM);
        graphics.outline(x, y, panelWidth, panelHeight, BlackGoldUi.GOLD_DIM);
        graphics.fillGradient(x, y, x + panelWidth, y + 2, BlackGoldUi.GOLD, BlackGoldUi.GOLD);
        graphics.text(font, title, x + 12, y + 9, BlackGoldUi.GOLD, false);
        graphics.text(font, Component.translatable("screen.yuushya_modelling_enhanced_editor.block_picker_help"),
                x + 72, y + 9, BlackGoldUi.TEXT_DIM, false);
        drawBlockPreview(graphics, mouseX, mouseY);
        drawBlockRows(graphics, mouseX, mouseY);
        String count = Component.translatable("screen.yuushya_modelling_enhanced_editor.picker_count_block",
                filteredBlocks.size()).getString();
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, count, listWidth())),
                listX(), y + panelHeight - 22, BlackGoldUi.TEXT_SECONDARY, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBlockPreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = previewX();
        int y = previewY();
        int size = previewSize();
        graphics.fillGradient(x, y, x + size, y + size, 0xFF0B0E13, 0xFF151A22);
        graphics.outline(x, y, size, size, BlackGoldUi.GOLD_DIM);
        BlockEntry entry = previewEntry(mouseX, mouseY);
        if (entry == null) {
            graphics.centeredText(font, Component.translatable("screen.yuushya_modelling_enhanced_editor.no_preview"),
                    x + size / 2,
                    y + size / 2 - 4, BlackGoldUi.TEXT_DIM);
            return;
        }

        Viewport viewport = new Viewport(x + 1, y + 1, Math.max(1, size - 2), Math.max(1, size - 2));
        CameraState camera = CameraState.lookingAt(CameraMode.PERSPECTIVE,
                new Vector3d(2.5D, 2.0D, 3.0D), new Vector3d(), WORLD_UP,
                38.0F, 3.0F, 0.05F, 100.0F);
        CameraFrame frame = new CameraFrame(CameraMatrices.create(camera, viewport), viewport, camera.mode());
        graphics.submitPictureInPictureRenderState(new BlockPreviewPipRenderState(
                List.of(new BlockPreviewLayer(entry.state(), EditorTransform.IDENTITY, false)),
                frame, null, 1.0F, false,
                viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + viewport.height(),
                graphics.peekScissorStack()));

        int textY = y + size + 6;
        int textWidth = previewColumnWidth() - 12;
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, entry.id(), textWidth)),
                x, textY, BlackGoldUi.CYAN, false);
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, entry.displayName(), textWidth)),
                x, textY + 12, BlackGoldUi.TEXT_SECONDARY, false);
    }

    private void drawBlockRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int listX = listX();
        int listWidth = listWidth();
        int listY = panelY() + HEADER_HEIGHT;
        int rows = visibleRows();
        if (filteredBlocks.isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.yuushya_modelling_enhanced_editor.no_blocks"),
                    listX + listWidth / 2, listY + 18, BlackGoldUi.TEXT_DIM);
            return;
        }
        int end = Math.min(filteredBlocks.size(), scrollOffset + rows);
        for (int index = scrollOffset; index < end; index++) {
            BlockEntry entry = filteredBlocks.get(index);
            int rowY = listY + (index - scrollOffset) * ROW_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX < listX + listWidth
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT - 2;
            int background = hovered ? 0xFF2A2A20 : 0xFF15171B;
            graphics.fillGradient(listX, rowY, listX + listWidth,
                    rowY + ROW_HEIGHT - 2, background, background);
            graphics.fillGradient(listX, rowY, listX + 2, rowY + ROW_HEIGHT - 2,
                    hovered ? BlackGoldUi.CYAN : 0xFF333333,
                    hovered ? BlackGoldUi.CYAN : 0xFF333333);
            String id = BlackGoldUi.ellipsize(font, entry.id(), Math.max(54, listWidth * 54 / 100));
            graphics.text(font, Component.literal(id), listX + 7, rowY + 5,
                    hovered ? BlackGoldUi.CYAN : BlackGoldUi.TEXT_PRIMARY, false);
            String displayName = BlackGoldUi.ellipsize(font, entry.displayName(),
                    Math.max(30, listWidth - 28 - font.width(id)));
            graphics.text(font, Component.literal(displayName), listX + listWidth - 6 - font.width(displayName),
                    rowY + 5, BlackGoldUi.TEXT_SECONDARY, false);
        }
        if (filteredBlocks.size() > rows) {
            int trackX = listX + listWidth - 3;
            int trackY = listY;
            int trackHeight = rows * ROW_HEIGHT - 2;
            int thumbHeight = Math.max(14, trackHeight * rows / filteredBlocks.size());
            int maxScroll = maxScroll();
            int thumbY = trackY + (maxScroll == 0 ? 0
                    : (trackHeight - thumbHeight) * scrollOffset / maxScroll);
            graphics.fillGradient(trackX, trackY, trackX + 3, trackY + trackHeight,
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
                if (index < filteredBlocks.size()) {
                    choose(filteredBlocks.get(index));
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
            if (!filteredBlocks.isEmpty()) choose(filteredBlocks.get(scrollOffset));
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void choose(BlockEntry entry) {
        selectionHandler.accept(entry.state());
        minecraft.setScreen(parent);
    }

    private void filter(String input) {
        String query = input == null ? "" : input.strip().toLowerCase(Locale.ROOT);
        filteredBlocks = query.isEmpty() ? allBlocks : allBlocks.stream()
                .filter(entry -> entry.searchText().contains(query)).toList();
        scrollOffset = 0;
    }

    private int panelWidth() {
        return Math.max(260, Math.min(PANEL_MAX_WIDTH, width - PANEL_MARGIN * 2));
    }

    private int panelHeight() {
        return Math.max(170, height - PANEL_MARGIN * 2);
    }

    private int panelX() {
        return (width - panelWidth()) / 2;
    }

    private int panelY() {
        return (height - panelHeight()) / 2;
    }

    private int previewColumnWidth() {
        return Math.clamp(panelWidth() * 28 / 100,
                PREVIEW_COLUMN_MIN_WIDTH, PREVIEW_COLUMN_MAX_WIDTH);
    }

    private int previewX() {
        return panelX() + 12;
    }

    private int previewY() {
        return panelY() + HEADER_HEIGHT;
    }

    private int previewSize() {
        return Math.max(48, Math.min(previewColumnWidth() - 12,
                panelHeight() - HEADER_HEIGHT - FOOTER_HEIGHT - 8));
    }

    private int listX() {
        return panelX() + previewColumnWidth() + 12;
    }

    private int listWidth() {
        return Math.max(80, panelX() + panelWidth() - 12 - listX());
    }

    private BlockEntry previewEntry(double mouseX, double mouseY) {
        if (filteredBlocks.isEmpty()) return null;
        int row = (int) ((mouseY - (panelY() + HEADER_HEIGHT)) / ROW_HEIGHT);
        if (mouseX >= listX() && mouseX < listX() + listWidth()
                && row >= 0 && row < visibleRows()) {
            int index = scrollOffset + row;
            if (index < filteredBlocks.size()) return filteredBlocks.get(index);
        }
        return filteredBlocks.get(Math.min(scrollOffset, filteredBlocks.size() - 1));
    }

    private int visibleRows() {
        return Math.max(4, (panelHeight() - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, filteredBlocks.size() - visibleRows());
    }

    private static List<BlockEntry> collectBlocks() {
        List<BlockEntry> entries = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block == Blocks.AIR || block.asItem() == Items.AIR) continue;
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            String displayName = block.getName().getString();
            entries.add(new BlockEntry(block.defaultBlockState(), id, displayName,
                    (id + ' ' + displayName).toLowerCase(Locale.ROOT)));
        }
        entries.sort(Comparator.comparing(BlockEntry::id));
        return List.copyOf(entries);
    }

    private record BlockEntry(BlockState state, String id, String displayName, String searchText) { }
}
