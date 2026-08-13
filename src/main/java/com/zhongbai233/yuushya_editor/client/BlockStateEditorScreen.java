package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.scene_editor.core.camera.CameraFrame;
import com.zhongbai233.scene_editor.core.camera.CameraMatrices;
import com.zhongbai233.scene_editor.core.camera.EditorCameraMode;
import com.zhongbai233.scene_editor.core.camera.EditorCameraState;
import com.zhongbai233.scene_editor.core.projection.EditorViewport;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewLayer;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewPipRenderState;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldUi;
import com.zhongbai233.yuushya_editor.core.BlockStatePropertySupport;
import com.zhongbai233.yuushya_editor.core.EditorTransform;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Vector3d;

/** Edits every property exposed by a modeled block's Minecraft {@link BlockState}. */
public final class BlockStateEditorScreen extends Screen {
    private static final Vector3d WORLD_UP = new Vector3d(0.0D, 1.0D, 0.0D);
    private static final int PROPERTY_ROW_HEIGHT = 22;

    private final Screen parent;
    private final Consumer<BlockState> saveHandler;
    private BlockState value;
    private int propertyScroll;

    public BlockStateEditorScreen(Screen parent, BlockState initial,
            Consumer<BlockState> saveHandler) {
        super(Component.translatable("screen.yuushya_modelling_enhanced_editor.block_state_editor.title"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.value = Objects.requireNonNull(initial, "initial");
        this.saveHandler = Objects.requireNonNull(saveHandler, "saveHandler");
    }

    @Override
    protected void init() {
        int x = controlsX();
        int width = controlsWidth();
        List<Property<?>> properties = properties();
        int end = Math.min(properties.size(), propertyScroll + visiblePropertyRows());
        for (int index = propertyScroll; index < end; index++) {
            Property<?> property = properties.get(index);
            int rowY = propertiesY() + (index - propertyScroll) * PROPERTY_ROW_HEIGHT;
            addRenderableWidget(new BlackGoldButton(x, rowY, 22, 18, Component.literal("<"),
                    button -> cycleProperty(property, -1), BlackGoldUi.GOLD_DIM));
            addRenderableWidget(new BlackGoldButton(x + width - 22, rowY, 22, 18, Component.literal(">"),
                    button -> cycleProperty(property, 1), BlackGoldUi.GOLD_DIM));
        }

        addRenderableWidget(new BlackGoldButton(panelX() + panelWidth() - 172,
                panelY() + panelHeight() - 26, 76, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.cancel"),
                button -> onClose(), 0xFFD04040));
        addRenderableWidget(new BlackGoldButton(panelX() + panelWidth() - 92,
                panelY() + panelHeight() - 26, 80, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.apply_short"),
                button -> save(), BlackGoldUi.CYAN));
    }

    private void cycleProperty(Property<?> property, int direction) {
        value = cycle(value, property, direction);
        rebuildWidgets();
    }

    private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property,
            int direction) {
        return BlockStatePropertySupport.cycle(state, property, direction);
    }

    private void save() {
        saveHandler.accept(value);
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

        drawPreview(graphics);
        int controlsX = controlsX();
        int controlsWidth = controlsWidth();
        String blockId = BuiltInRegistries.BLOCK.getKey(value.getBlock()).toString();
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, blockId, controlsWidth)),
                controlsX, y + 35, BlackGoldUi.TEXT_SECONDARY, false);
        graphics.text(font, Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.block_state_editor.properties"),
                controlsX, y + 54, BlackGoldUi.GOLD, false);

        List<Property<?>> properties = properties();
        if (properties.isEmpty()) {
            graphics.text(font, Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.block_state_editor.no_properties"),
                    controlsX, propertiesY() + 5, BlackGoldUi.TEXT_DIM, false);
        } else {
            int end = Math.min(properties.size(), propertyScroll + visiblePropertyRows());
            for (int index = propertyScroll; index < end; index++) {
                Property<?> property = properties.get(index);
                String label = property.getName() + " = " + propertyValueName(value, property);
                graphics.centeredText(font, Component.literal(BlackGoldUi.ellipsize(font, label,
                                Math.max(20, controlsWidth - 52))),
                        controlsX + controlsWidth / 2,
                        propertiesY() + (index - propertyScroll) * PROPERTY_ROW_HEIGHT + 5,
                        BlackGoldUi.TEXT_PRIMARY);
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        int x = panelX() + 12;
        int y = panelY() + 35;
        int size = previewSize();
        graphics.fillGradient(x, y, x + size, y + size, 0xFF0B0E13, 0xFF151A22);
        graphics.outline(x, y, size, size, BlackGoldUi.GOLD_DIM);
        EditorViewport viewport = new EditorViewport(x + 1, y + 1, size - 2, size - 2);
        EditorCameraState camera = EditorCameraState.lookingAt(EditorCameraMode.ORBIT,
                new Vector3d(2.5D, 2.0D, 3.0D), new Vector3d(0.5D, 0.5D, 0.5D), WORLD_UP,
                38.0F, 3.0F, 0.05F, 100.0F);
        CameraFrame frame = new CameraFrame(CameraMatrices.create(camera, viewport), viewport, camera.mode());
        graphics.submitPictureInPictureRenderState(new BlockPreviewPipRenderState(
                List.of(new BlockPreviewLayer(new BlockPreviewLayer.BlockContent(value),
                        EditorTransform.IDENTITY, false)),
                frame, null, 1.0F, false, viewport.x(), viewport.y(),
                viewport.x() + viewport.width(), viewport.y() + viewport.height(),
                graphics.peekScissorStack()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D && mouseX >= controlsX() && mouseX < controlsX() + controlsWidth()
                && mouseY >= propertiesY() && mouseY < panelY() + panelHeight() - 31) {
            int max = Math.max(0, properties().size() - visiblePropertyRows());
            propertyScroll = Math.clamp(propertyScroll + (scrollY < 0.0D ? 1 : -1), 0, max);
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private List<Property<?>> properties() {
        return BlockStatePropertySupport.properties(value);
    }

    private static <T extends Comparable<T>> String propertyValueName(BlockState state,
            Property<T> property) {
        return BlockStatePropertySupport.valueName(state, property);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }

    private int panelWidth() { return Math.max(340, Math.min(540, width - 24)); }
    private int panelHeight() { return Math.max(230, Math.min(370, height - 24)); }
    private int panelX() { return (width - panelWidth()) / 2; }
    private int panelY() { return (height - panelHeight()) / 2; }
    private int previewSize() { return Math.min(124, panelHeight() - 75); }
    private int controlsX() { return panelX() + previewSize() + 24; }
    private int controlsWidth() { return panelX() + panelWidth() - 12 - controlsX(); }
    private int propertiesY() { return panelY() + 68; }
    private int visiblePropertyRows() {
        return Math.max(1, (panelY() + panelHeight() - 33 - propertiesY()) / PROPERTY_ROW_HEIGHT);
    }
}
