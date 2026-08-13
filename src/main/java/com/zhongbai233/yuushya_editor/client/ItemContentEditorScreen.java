package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewLayer;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewPipRenderState;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldUi;
import com.zhongbai233.yuushya_editor.compat.YuushyaItemModelSupport;
import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.ItemColorCodec;
import com.zhongbai233.yuushya_editor.core.ItemModelData;
import com.zhongbai233.yuushya_editor.core.camera.CameraFrame;
import com.zhongbai233.yuushya_editor.core.camera.CameraMatrices;
import com.zhongbai233.yuushya_editor.core.camera.CameraMode;
import com.zhongbai233.yuushya_editor.core.camera.CameraState;
import com.zhongbai233.yuushya_editor.core.projection.Viewport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Vector3d;

/** Complete item-layer editor: item replacement, native block mode, block state and colour. */
public final class ItemContentEditorScreen extends Screen {
    private static final Vector3d WORLD_UP = new Vector3d(0.0D, 1.0D, 0.0D);
    private static final int PROPERTY_ROW_HEIGHT = 22;
    private final Screen parent;
    private final Consumer<ItemModelData> saveHandler;
    private ItemModelData value;
    private EditBox colorBox;
    private BlackGoldButton modeButton;
    private int propertyScroll;
    private Component validationMessage = Component.empty();

    public ItemContentEditorScreen(Screen parent, ItemModelData initial,
            Consumer<ItemModelData> saveHandler) {
        super(Component.translatable("screen.yuushya_modelling_enhanced_editor.item_editor.title"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.value = Objects.requireNonNull(initial, "initial");
        this.saveHandler = Objects.requireNonNull(saveHandler, "saveHandler");
    }

    @Override
    protected void init() {
        int x = controlsX();
        int y = panelY();
        int width = controlsWidth();
        addRenderableWidget(new BlackGoldButton(x, y + 35, width, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.item_editor.replace"),
                button -> minecraft.setScreen(new ItemPickerScreen(this, this::replaceItem)), BlackGoldUi.CYAN));
        modeButton = addRenderableWidget(new BlackGoldButton(x, y + 59, width, 20,
                Component.empty(), button -> toggleBlockMode(), BlackGoldUi.GOLD_DIM));
        modeButton.active = value.blockState() != null;

        colorBox = new EditBox(font, x + 47, y + 85, Math.max(60, width - 47), 18,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.item_editor.color"));
        colorBox.setMaxLength(9);
        colorBox.setValue(String.format("#%08X", value.color()));
        addRenderableWidget(colorBox);

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
        syncModeButton();
    }

    private void replaceItem(net.minecraft.world.item.ItemStack stack) {
        value = YuushyaItemModelSupport.replace(value, stack);
        propertyScroll = 0;
        validationMessage = Component.empty();
    }

    private void toggleBlockMode() {
        if (value.blockState() == null) return;
        value = value.withBlockRendering(!value.enableBlock());
        syncModeButton();
    }

    private void syncModeButton() {
        if (modeButton == null) return;
        Component mode = Component.translatable(value.enableBlock()
                ? "screen.yuushya_modelling_enhanced_editor.item_editor.mode_block"
                : "screen.yuushya_modelling_enhanced_editor.item_editor.mode_item");
        modeButton.setMessage(Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.item_editor.render_mode", mode));
    }

    private void cycleProperty(Property<?> property, int direction) {
        BlockState state = value.blockState();
        if (state == null) return;
        BlockState changed = cycle(state, property, direction);
        value = YuushyaItemModelSupport.withBlockState(value, changed);
        rebuildWidgets();
    }

    private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property,
            int direction) {
        List<T> values = new ArrayList<>(property.getPossibleValues());
        if (values.isEmpty()) return state;
        int current = values.indexOf(state.getValue(property));
        int next = Math.floorMod(current + direction, values.size());
        return state.setValue(property, values.get(next));
    }

    private void save() {
        try {
            int color = ItemColorCodec.parse(colorBox.getValue());
            saveHandler.accept(value.withColor(color));
            minecraft.setScreen(parent);
        } catch (IllegalArgumentException exception) {
            validationMessage = Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.item_editor.invalid_color");
            colorBox.setTextColor(0xFFFF6B6B);
            setInitialFocus(colorBox);
        }
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
        String itemId = BuiltInRegistries.ITEM.getKey(value.itemStack().getItem()).toString();
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, itemId, controlsWidth)),
                controlsX, y + 23, BlackGoldUi.TEXT_SECONDARY, false);
        graphics.text(font, Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.item_editor.color"),
                controlsX, y + 90, BlackGoldUi.TEXT_PRIMARY, false);

        BlockState state = value.blockState();
        graphics.text(font, Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.item_editor.block_state"),
                controlsX, y + 113, state == null ? BlackGoldUi.TEXT_DIM : BlackGoldUi.GOLD, false);
        if (state == null) {
            graphics.text(font, Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.item_editor.not_block_item"),
                    controlsX, y + 131, BlackGoldUi.TEXT_DIM, false);
        } else {
            List<Property<?>> properties = properties();
            int end = Math.min(properties.size(), propertyScroll + visiblePropertyRows());
            for (int index = propertyScroll; index < end; index++) {
                Property<?> property = properties.get(index);
                String label = property.getName() + " = " + propertyValueName(state, property);
                int textWidth = Math.max(20, controlsWidth - 52);
                graphics.centeredText(font, Component.literal(BlackGoldUi.ellipsize(font, label, textWidth)),
                        controlsX + controlsWidth / 2,
                        propertiesY() + (index - propertyScroll) * PROPERTY_ROW_HEIGHT + 5,
                        BlackGoldUi.TEXT_PRIMARY);
            }
        }
        if (!validationMessage.getString().isEmpty()) {
            graphics.text(font, validationMessage, x + 12, y + panelHeight() - 21, 0xFFFF6B6B, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPreview(GuiGraphicsExtractor graphics) {
        int x = panelX() + 12;
        int y = panelY() + 35;
        int size = previewSize();
        graphics.fillGradient(x, y, x + size, y + size, 0xFF0B0E13, 0xFF151A22);
        graphics.outline(x, y, size, size, BlackGoldUi.GOLD_DIM);
        BlockPreviewLayer.Content content = value.enableBlock() && value.blockState() != null
                ? new BlockPreviewLayer.BlockContent(value.blockState(), false)
                : new BlockPreviewLayer.ItemContent(value.itemStack());
        Viewport viewport = new Viewport(x + 1, y + 1, size - 2, size - 2);
        CameraState camera = CameraState.lookingAt(CameraMode.PERSPECTIVE,
                new Vector3d(2.5D, 2.0D, 3.0D), new Vector3d(0.3D, 0.3D, 0.3D), WORLD_UP,
                38.0F, 3.0F, 0.05F, 100.0F);
        CameraFrame frame = new CameraFrame(CameraMatrices.create(camera, viewport), viewport, camera.mode());
        graphics.submitPictureInPictureRenderState(new BlockPreviewPipRenderState(
                List.of(new BlockPreviewLayer(content, EditorTransform.IDENTITY, false)),
                frame, null, 1.0F, false, viewport.x(), viewport.y(),
                viewport.x() + viewport.width(), viewport.y() + viewport.height(), graphics.peekScissorStack()));
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean cancelled) {
        if (colorBox != null) colorBox.setTextColor(BlackGoldUi.TEXT_PRIMARY);
        validationMessage = Component.empty();
        return super.mouseClicked(event, cancelled);
    }

    private List<Property<?>> properties() {
        return value.blockState() == null ? List.of() : List.copyOf(value.blockState().getProperties());
    }

    private static <T extends Comparable<T>> String propertyValueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
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
    private int propertiesY() { return panelY() + 127; }
    private int visiblePropertyRows() {
        return Math.max(1, (panelY() + panelHeight() - 33 - propertiesY()) / PROPERTY_ROW_HEIGHT);
    }
}
