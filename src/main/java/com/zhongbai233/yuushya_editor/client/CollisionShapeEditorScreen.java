package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.scene_editor.core.camera.CameraFrame;
import com.zhongbai233.scene_editor.core.camera.CameraMatrices;
import com.zhongbai233.scene_editor.core.camera.EditorCameraController;
import com.zhongbai233.scene_editor.core.camera.EditorCameraMode;
import com.zhongbai233.scene_editor.core.camera.EditorCameraState;
import com.zhongbai233.scene_editor.core.gizmo.GizmoDragMath;
import com.zhongbai233.scene_editor.core.gizmo.GizmoHandle;
import com.zhongbai233.scene_editor.core.gizmo.GizmoMode;
import com.zhongbai233.scene_editor.core.gizmo.GizmoSizingPolicy;
import com.zhongbai233.scene_editor.core.gizmo.GizmoSnapPolicy;
import com.zhongbai233.scene_editor.core.projection.EditorProjection;
import com.zhongbai233.scene_editor.core.projection.EditorViewport;
import com.zhongbai233.scene_editor.core.projection.PickingRay;
import com.zhongbai233.scene_editor.core.render.LineWidthPolicy;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewFrame;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewLayer;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewGizmo;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewPipRenderState;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldToolButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldUi;
import com.zhongbai233.yuushya_editor.compat.YuushyaShapeToolBridge;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewPicking;
import com.zhongbai233.yuushya_editor.core.preview.CollisionBoxOperations;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import com.zhongbai233.yuushya_editor.core.preview.ViewportGizmoHitTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

/** Visual editor for Yuushya's custom collision-box list. */
public final class CollisionShapeEditorScreen extends Screen {
    private static final int LEFT_WIDTH = 154;
    private static final int RIGHT_WIDTH = 218;
    private static final int WARNING_THRESHOLD = 64;
    private static final double GIZMO_HIT_RADIUS = 6.0D;
    private static final Vector3d WORLD_UP = new Vector3d(0, 1, 0);
    private final Screen parent;
    private final List<SceneLayer<Object>> sourceLayers;
    private final List<BlockPreviewLayer> previewLayers;
    private final BiConsumer<CollisionShape, Integer> saveHandler;
    private final BlockPos target;
    private final List<CollisionShape.Box> boxes;
    private final EditBox[] fields = new EditBox[6];
    private final String[] syncedValues = new String[6];
    private final EditorCameraController cameraController = new EditorCameraController();
    private EditorCameraState camera = EditorCameraState.lookingAt(EditorCameraMode.ORBIT,
            new Vector3d(3.5D, 2.8D, 4.5D), new Vector3d(), WORLD_UP,
            45.0F, 5.0F, 0.05F, 2048.0F);
    private int selectedIndex;
    private int shapeToolSlot;
    private int listScroll;
    private boolean showPreview = true;
    private boolean draggingViewport;
    private int dragButton = -1;
    private GizmoMode gizmoMode = GizmoMode.MOVE;
    private GizmoHandle activeGizmoHandle = GizmoHandle.NONE;
    private GizmoDragSession gizmoDragSession;
    private Component status = Component.empty();
    private boolean statusError;
    private BlackGoldButton addButton;
    private BlackGoldButton deleteButton;
    private BlackGoldButton toolButton;
    private BlackGoldButton clipButton;

    public CollisionShapeEditorScreen(Screen parent, List<SceneLayer<Object>> sourceLayers,
            CollisionShape shape, int shapeToolSlot, BlockPos target,
            BiConsumer<CollisionShape, Integer> saveHandler) {
        super(Component.translatable("screen.yuushya_modelling_enhanced_editor.collision_editor.title"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.sourceLayers = List.copyOf(Objects.requireNonNull(sourceLayers, "sourceLayers"));
        this.previewLayers = buildPreviewLayers(this.sourceLayers);
        this.boxes = new ArrayList<>(Objects.requireNonNull(shape, "shape").boxes());
        this.selectedIndex = boxes.isEmpty() ? -1 : 0;
        this.shapeToolSlot = shapeToolSlot;
        this.target = Objects.requireNonNull(target, "target").immutable();
        this.saveHandler = Objects.requireNonNull(saveHandler, "saveHandler");
    }

    @Override
    protected void init() {
        int rightX = width - RIGHT_WIDTH;
        String[] labels = {"Min X", "Min Y", "Min Z", "Max X", "Max Y", "Max Z"};
        for (int index = 0; index < fields.length; index++) {
            int column = index % 3;
            int row = index / 3;
            int x = rightX + 12 + column * 68;
            int y = 58 + row * 38;
            EditBox field = new EditBox(font, x, y, 62, 18, Component.literal(labels[index]));
            field.setMaxLength(32);
            fields[index] = addRenderableWidget(field);
        }

        toolButton = addRenderableWidget(new BlackGoldButton(rightX + 12, 140, RIGHT_WIDTH - 24, 20,
                Component.empty(), button -> openToolPicker(), BlackGoldUi.CYAN));
        addRenderableWidget(new BlackGoldButton(rightX + 12, 164, 92, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.collision_editor.read_tool"),
                button -> readFromTool(), BlackGoldUi.GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(rightX + 110, 164, 96, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.collision_editor.write_tool"),
                button -> writeToTool(), BlackGoldUi.GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(rightX + 12, 190, 92, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.collision_editor.update"),
                button -> commitFields(), BlackGoldUi.GOLD));
        addRenderableWidget(new BlackGoldButton(rightX + 110, 190, 96, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.collision_editor.preview",
                        Component.translatable(showPreview
                                ? "screen.yuushya_modelling_enhanced_editor.on"
                                : "screen.yuushya_modelling_enhanced_editor.off")),
                button -> { showPreview = !showPreview; rebuildWidgets(); }, BlackGoldUi.GOLD_DIM));
        clipButton = addRenderableWidget(new BlackGoldButton(rightX + 12, 216, RIGHT_WIDTH - 24, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.collision_editor.clip"),
                button -> clipToUnitCell(), BlackGoldUi.GOLD_DIM));

        int listRows = visibleRows();
        for (int index = listScroll; index < Math.min(boxes.size(), listScroll + listRows); index++) {
            int rowIndex = index;
            CollisionShape.Box box = boxes.get(index);
            String label = (index == selectedIndex ? "◆ " : "  ")
                    + Component.translatable("screen.yuushya_modelling_enhanced_editor.collision_editor.element",
                            index + 1).getString() + "  " + dimensions(box);
            addRenderableWidget(new BlackGoldButton(8, 38 + (index - listScroll) * 23,
                    LEFT_WIDTH - 16, 20, Component.literal(label),
                    button -> select(rowIndex), index == selectedIndex ? BlackGoldUi.CYAN : BlackGoldUi.GOLD_DIM));
        }

        addButton = addRenderableWidget(new BlackGoldButton(8, height - 100, LEFT_WIDTH - 16, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.collision_editor.add"),
                button -> addBox(), BlackGoldUi.CYAN));
        deleteButton = addRenderableWidget(new BlackGoldButton(8, height - 76, LEFT_WIDTH - 16, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.collision_editor.delete"),
                button -> deleteBox(), 0xFFD04040));
        addRenderableWidget(new BlackGoldButton(8, height - 52, LEFT_WIDTH - 16, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.collision_editor.auto"),
                button -> autoGenerate(), BlackGoldUi.GOLD));
        addRenderableWidget(new BlackGoldButton(rightX + 12, height - 28, 92, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.cancel"),
                button -> onClose(), 0xFFD04040));
        addRenderableWidget(new BlackGoldButton(rightX + 110, height - 28, 96, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.apply_short"),
                button -> save(), BlackGoldUi.GOLD));
        int toolX = rightX - 52;
        addRenderableWidget(new BlackGoldToolButton(toolX, 4, 22, 18,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.tool.move"),
                button -> gizmoMode = GizmoMode.MOVE, BlackGoldUi.GOLD,
                BlackGoldToolButton.Icon.MOVE));
        addRenderableWidget(new BlackGoldToolButton(toolX + 26, 4, 22, 18,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.tool.scale"),
                button -> gizmoMode = GizmoMode.SCALE, BlackGoldUi.GOLD,
                BlackGoldToolButton.Icon.SCALE));
        syncWidgets();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xD0000000, 0xE0050505);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int rightX = width - RIGHT_WIDTH;
        graphics.fillGradient(0, 0, LEFT_WIDTH, height, BlackGoldUi.PANEL_TOP, BlackGoldUi.PANEL_BOTTOM);
        graphics.fillGradient(rightX, 0, width, height, BlackGoldUi.PANEL_TOP, BlackGoldUi.PANEL_BOTTOM);
        graphics.fillGradient(LEFT_WIDTH, 0, rightX, height, BlackGoldUi.VIEW_TOP, BlackGoldUi.VIEW_BOTTOM);
        graphics.outline(0, 0, LEFT_WIDTH, height, BlackGoldUi.GOLD_DIM);
        graphics.outline(rightX, 0, RIGHT_WIDTH, height, BlackGoldUi.GOLD_DIM);
        graphics.text(font, title, 10, 10, BlackGoldUi.GOLD, false);
        boolean outsideCell = CollisionBoxOperations.extendsOutsideUnitCell(boxes);
        graphics.text(font, Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.collision_editor.count", boxes.size()),
                10, 23, outsideCell ? 0xFFFF6B6B
                        : boxes.size() > WARNING_THRESHOLD ? 0xFFFFB347 : BlackGoldUi.TEXT_DIM, false);
        graphics.text(font, Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.collision_editor.inspector"),
                rightX + 12, 10, BlackGoldUi.GOLD, false);
        graphics.text(font, Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.collision_editor.coordinates"),
                rightX + 12, 34, BlackGoldUi.TEXT_DIM, false);
        String[] labels = {"Min X", "Min Y", "Min Z", "Max X", "Max Y", "Max Z"};
        for (int index = 0; index < labels.length; index++) {
            int x = rightX + 12 + index % 3 * 68;
            int y = 47 + index / 3 * 38;
            int color = index % 3 == 0 ? 0xFFFF6B5E : index % 3 == 1 ? 0xFF7ED957 : 0xFF61A8FF;
            graphics.text(font, labels[index], x, y, color, false);
        }
        if (outsideCell) {
            String warning = Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.collision_editor.out_of_cell_short").getString();
            graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, warning, RIGHT_WIDTH - 24)),
                    rightX + 12, 242, 0xFFFF6B6B, false);
        }
        EditorViewport viewport = viewport();
        CameraFrame frame = new CameraFrame(CameraMatrices.create(camera, viewport), viewport, camera.mode());
        CollisionShape visibleShape = showPreview ? CollisionShape.custom(boxes) : CollisionShape.custom(List.of());
        BlockPreviewGizmo gizmo = null;
        if (showPreview && selectedIndex >= 0 && selectedIndex < boxes.size()) {
            GizmoSizingPolicy.Sizes sizes = gizmoSizes(boxes.get(selectedIndex));
            GizmoHandle hovered = activeGizmoHandle == GizmoHandle.NONE && !draggingViewport
                    && insideViewport(mouseX, mouseY) ? gizmoHandleAt(mouseX, mouseY, frame) : GizmoHandle.NONE;
            gizmo = new BlockPreviewGizmo(boxCenter(boxes.get(selectedIndex)), gizmoMode,
                    activeGizmoHandle, hovered, sizes.axisLength(), sizes.rotationRadius(),
                    sizes.scaleHandleLength());
        }
        graphics.submitPictureInPictureRenderState(new BlockPreviewPipRenderState(
                EnvironmentPreviewFrame.empty(), previewLayers, frame, gizmo, visibleShape,
                showPreview ? selectedIndex : -1, LineWidthPolicy.forCamera(camera), true,
                viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + viewport.height(),
                graphics.peekScissorStack()));
        String footer = status.getString().isBlank()
                ? Component.translatable("screen.yuushya_modelling_enhanced_editor.collision_editor.help").getString()
                : status.getString();
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, footer,
                        Math.max(0, viewport.width() - 12))),
                viewport.x() + 6, height - 18, statusError ? 0xFFFF6B6B : BlackGoldUi.TEXT_DIM, false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean cancelled) {
        if (cancelled) return false;
        if (super.mouseClicked(event, false)) return true;
        if ((event.button() == 0 || event.button() == 1) && insideViewport(event.x(), event.y())) {
            if (event.button() == 0 && selectedIndex >= 0) {
                CameraFrame frame = currentFrame();
                GizmoHandle handle = gizmoHandleAt(event.x(), event.y(), frame);
                if (handle != GizmoHandle.NONE && beginGizmoDrag(event.x(), event.y(), frame, handle)) {
                    draggingViewport = true;
                    dragButton = 0;
                    return true;
                }
                selectBoxAt(event.x(), event.y());
            }
            draggingViewport = true;
            dragButton = event.button();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingViewport && event.button() == dragButton) {
            if (activeGizmoHandle != GizmoHandle.NONE) {
                applyGizmoDrag(event.x(), event.y(), event.hasShiftDown(), event.hasControlDown());
                return true;
            }
            if (dragButton == 1) camera = cameraController.panPixels(camera, dragX, dragY, viewport());
            else camera = cameraController.orbit(camera, Math.toRadians(dragX * 0.35D),
                    Math.toRadians(-dragY * 0.30D), WORLD_UP);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingViewport && event.button() == dragButton) {
            activeGizmoHandle = GizmoHandle.NONE;
            gizmoDragSession = null;
            draggingViewport = false;
            dragButton = -1;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < LEFT_WIDTH) {
            int max = Math.max(0, boxes.size() - visibleRows());
            listScroll = Math.clamp(listScroll + (scrollY < 0 ? 1 : -1), 0, max);
            rebuildWidgets();
            return true;
        }
        if (insideViewport(mouseX, mouseY)) {
            camera = cameraController.dolly(camera, scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!(getFocused() instanceof EditBox)) {
            if (event.key() == GLFW.GLFW_KEY_DELETE || event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                deleteBox();
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                commitFields();
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_W) { gizmoMode = GizmoMode.MOVE; return true; }
            if (event.key() == GLFW.GLFW_KEY_R) { gizmoMode = GizmoMode.SCALE; return true; }
        }
        return super.keyPressed(event);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private void openToolPicker() {
        minecraft.setScreen(new ShapeToolPickerScreen(this, slot -> {
            shapeToolSlot = slot;
            setStatus(Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.collision_editor.tool_selected", slot + 1), false);
            rebuildWidgets();
        }));
    }

    private void readFromTool() {
        if (!validTool()) { requireTool(); return; }
        try {
            boxes.clear();
            boxes.addAll(YuushyaShapeToolBridge.read(toolStack(), target).boxes());
            selectedIndex = boxes.isEmpty() ? -1 : 0;
            listScroll = 0;
            setStatus(Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.collision_editor.read_success", boxes.size()), false);
            warnShapeRisks();
            rebuildWidgets();
        } catch (RuntimeException exception) {
            setStatus(Component.literal(errorMessage(exception)), true);
        }
    }

    private void writeToTool() {
        if (!commitFields() || !validTool()) { if (!validTool()) requireTool(); return; }
        try {
            YuushyaShapeToolBridge.writeTool(shapeToolSlot, CollisionShape.custom(boxes), target);
            setStatus(Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.collision_editor.write_success", boxes.size()), false);
        } catch (RuntimeException exception) {
            setStatus(Component.literal(errorMessage(exception)), true);
        }
    }

    private void addBox() {
        if (!validTool()) { requireTool(); return; }
        if (!commitFields()) return;
        boxes.add(new CollisionShape.Box(0.25D, 0.25D, 0.25D, 0.75D, 0.75D, 0.75D));
        selectedIndex = boxes.size() - 1;
        ensureSelectedVisible();
        warnShapeRisks();
        rebuildWidgets();
    }

    private void deleteBox() {
        if (selectedIndex < 0 || selectedIndex >= boxes.size()) return;
        boxes.remove(selectedIndex);
        selectedIndex = boxes.isEmpty() ? -1 : Math.min(selectedIndex, boxes.size() - 1);
        ensureSelectedVisible();
        setStatus(Component.empty(), false);
        warnShapeRisks();
        rebuildWidgets();
    }

    private void autoGenerate() {
        if (!validTool()) { requireTool(); return; }
        CollisionShape generated = CollisionAutoGenerator.generate(sourceLayers);
        boxes.clear();
        boxes.addAll(generated.boxes());
        selectedIndex = boxes.isEmpty() ? -1 : 0;
        listScroll = 0;
        setStatus(Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.collision_editor.generated", boxes.size()), false);
        warnShapeRisks();
        rebuildWidgets();
    }

    private void clipToUnitCell() {
        if (!commitFields()) return;
        List<CollisionShape.Box> clipped = CollisionBoxOperations.clipToUnitCell(boxes);
        boxes.clear();
        boxes.addAll(clipped);
        selectedIndex = boxes.isEmpty() ? -1 : Math.min(Math.max(selectedIndex, 0), boxes.size() - 1);
        ensureSelectedVisible();
        setStatus(Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.collision_editor.clipped", boxes.size()), false);
        rebuildWidgets();
    }

    private void save() {
        if (!commitFields()) return;
        saveHandler.accept(CollisionShape.custom(boxes), shapeToolSlot);
        minecraft.setScreen(parent);
    }

    private boolean commitFields() {
        if (selectedIndex < 0) return true;
        try {
            double[] values = new double[6];
            for (int index = 0; index < fields.length; index++) {
                values[index] = Double.parseDouble(fields[index].getValue());
                if (!Double.isFinite(values[index])) throw new NumberFormatException();
            }
            boxes.set(selectedIndex, new CollisionShape.Box(values[0], values[1], values[2],
                    values[3], values[4], values[5]));
            setStatus(Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.collision_editor.updated"), false);
            warnShapeRisks();
            syncWidgets();
            return true;
        } catch (IllegalArgumentException exception) {
            setStatus(Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.collision_editor.invalid"), true);
            return false;
        }
    }

    private void select(int index) {
        if (!commitFields()) return;
        selectedIndex = index;
        syncWidgets();
        rebuildWidgets();
    }

    private void selectBoxAt(double mouseX, double mouseY) {
        CameraFrame frame = currentFrame();
        PickingRay ray = EditorProjection.rayFromScreen(mouseX, mouseY, frame.matrices(), frame.viewport());
        Matrix4f matrix = new Matrix4f().translate(-0.5F, -0.5F, -0.5F);
        int bestIndex = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int index = 0; index < boxes.size(); index++) {
            CollisionShape.Box box = boxes.get(index);
            double distance = BlockPreviewPicking.hitDistance(ray, matrix, box.minX(), box.minY(), box.minZ(),
                    box.maxX(), box.maxY(), box.maxZ()).orElse(Double.POSITIVE_INFINITY);
            if (distance < bestDistance) { bestDistance = distance; bestIndex = index; }
        }
        if (bestIndex >= 0) {
            selectedIndex = bestIndex;
            ensureSelectedVisible();
            rebuildWidgets();
        }
    }

    private boolean beginGizmoDrag(double mouseX, double mouseY, CameraFrame frame, GizmoHandle handle) {
        if (selectedIndex < 0 || handle == GizmoHandle.NONE || handle == GizmoHandle.UNIFORM) return false;
        CollisionShape.Box box = boxes.get(selectedIndex);
        Vector3d origin = boxCenter(box);
        Vector3d axis = handle.axis();
        PickingRay ray = EditorProjection.rayFromScreen(mouseX, mouseY, frame.matrices(), frame.viewport());
        Vector3d startHit;
        double centerX = 0.0D;
        double centerY = 0.0D;
        double screenAxisX = 0.0D;
        double screenAxisY = 0.0D;
        double startCoordinate = 0.0D;
        if (gizmoMode == GizmoMode.MOVE) {
            startHit = GizmoDragMath.intersectConstraint(ray, origin, axis, handle.constraint()).orElse(null);
            if (startHit == null) return false;
        } else {
            var projectedCenter = EditorProjection.project(origin, frame.matrices(), frame.viewport());
            var projectedAxis = EditorProjection.project(new Vector3d(origin).add(axis),
                    frame.matrices(), frame.viewport());
            if (!projectedCenter.visible() || !projectedAxis.visible()) return false;
            centerX = projectedCenter.screenX();
            centerY = projectedCenter.screenY();
            screenAxisX = projectedAxis.screenX() - centerX;
            screenAxisY = projectedAxis.screenY() - centerY;
            double length = Math.hypot(screenAxisX, screenAxisY);
            if (length <= 1.0e-9D) return false;
            screenAxisX /= length;
            screenAxisY /= length;
            startCoordinate = (mouseX - centerX) * screenAxisX + (mouseY - centerY) * screenAxisY;
            if (Math.abs(startCoordinate) <= 1.0e-9D) return false;
            startHit = origin;
        }
        activeGizmoHandle = handle;
        gizmoDragSession = new GizmoDragSession(box, frame, origin, axis, startHit,
                centerX, centerY, screenAxisX, screenAxisY, startCoordinate);
        return true;
    }

    private void applyGizmoDrag(double mouseX, double mouseY, boolean shiftDown, boolean controlDown) {
        GizmoDragSession session = gizmoDragSession;
        if (session == null || selectedIndex < 0 || selectedIndex >= boxes.size()) return;
        CollisionShape.Box before = session.box();
        CollisionShape.Box changed;
        if (gizmoMode == GizmoMode.MOVE) {
            PickingRay ray = EditorProjection.rayFromScreen(mouseX, mouseY,
                    session.frame().matrices(), session.frame().viewport());
            Vector3d hit = GizmoDragMath.intersectConstraint(ray, session.origin(), session.axis(),
                    activeGizmoHandle.constraint()).orElse(null);
            if (hit == null) return;
            double delta = new Vector3d(hit).sub(session.startHit()).dot(session.axis());
            delta = GizmoSnapPolicy.snapDelta(delta,
                    GizmoSnapPolicy.step(GizmoMode.MOVE, shiftDown, controlDown));
            changed = translate(before, activeGizmoHandle, delta);
        } else {
            double coordinate = (mouseX - session.centerX()) * session.screenAxisX()
                    + (mouseY - session.centerY()) * session.screenAxisY();
            double factor = coordinate / session.startCoordinate();
            double initialSize = size(before, activeGizmoHandle);
            double requestedSize = initialSize * factor;
            requestedSize = initialSize + GizmoSnapPolicy.snapDelta(requestedSize - initialSize,
                    GizmoSnapPolicy.step(GizmoMode.SCALE, shiftDown, controlDown));
            if (!Double.isFinite(requestedSize) || requestedSize <= 1.0e-6D) return;
            changed = resize(before, activeGizmoHandle, requestedSize);
        }
        boxes.set(selectedIndex, changed);
        warnShapeRisks();
        syncWidgets();
    }

    private GizmoHandle gizmoHandleAt(double mouseX, double mouseY, CameraFrame frame) {
        if (selectedIndex < 0 || selectedIndex >= boxes.size()) return GizmoHandle.NONE;
        CollisionShape.Box box = boxes.get(selectedIndex);
        Vector3d origin = boxCenter(box);
        GizmoSizingPolicy.Sizes sizes = gizmoSizes(box);
        double length = gizmoMode == GizmoMode.SCALE ? sizes.scaleHandleLength() : sizes.axisLength();
        return ViewportGizmoHitTesting.axisHandleAt(mouseX, mouseY, origin,
                frame.matrices(), frame.viewport(), length, GIZMO_HIT_RADIUS);
    }

    private CameraFrame currentFrame() {
        EditorViewport viewport = viewport();
        return new CameraFrame(CameraMatrices.create(camera, viewport), viewport, camera.mode());
    }

    private static GizmoSizingPolicy.Sizes gizmoSizes(CollisionShape.Box box) {
        double x = box.maxX() - box.minX();
        double y = box.maxY() - box.minY();
        double z = box.maxZ() - box.minZ();
        return GizmoSizingPolicy.calculate(Math.sqrt(x * x + y * y + z * z) * 0.5D);
    }

    private static Vector3d boxCenter(CollisionShape.Box box) {
        return new Vector3d((box.minX() + box.maxX()) * 0.5D - 0.5D,
                (box.minY() + box.maxY()) * 0.5D - 0.5D,
                (box.minZ() + box.maxZ()) * 0.5D - 0.5D);
    }

    private static CollisionShape.Box translate(CollisionShape.Box box, GizmoHandle axis, double delta) {
        return switch (axis) {
            case X -> new CollisionShape.Box(box.minX() + delta, box.minY(), box.minZ(),
                    box.maxX() + delta, box.maxY(), box.maxZ());
            case Y -> new CollisionShape.Box(box.minX(), box.minY() + delta, box.minZ(),
                    box.maxX(), box.maxY() + delta, box.maxZ());
            case Z -> new CollisionShape.Box(box.minX(), box.minY(), box.minZ() + delta,
                    box.maxX(), box.maxY(), box.maxZ() + delta);
            case NONE, UNIFORM -> box;
        };
    }

    private static CollisionShape.Box resize(CollisionShape.Box box, GizmoHandle axis, double requestedSize) {
        double centerX = (box.minX() + box.maxX()) * 0.5D;
        double centerY = (box.minY() + box.maxY()) * 0.5D;
        double centerZ = (box.minZ() + box.maxZ()) * 0.5D;
        double half = requestedSize * 0.5D;
        return switch (axis) {
            case X -> new CollisionShape.Box(centerX - half, box.minY(), box.minZ(),
                    centerX + half, box.maxY(), box.maxZ());
            case Y -> new CollisionShape.Box(box.minX(), centerY - half, box.minZ(),
                    box.maxX(), centerY + half, box.maxZ());
            case Z -> new CollisionShape.Box(box.minX(), box.minY(), centerZ - half,
                    box.maxX(), box.maxY(), centerZ + half);
            case NONE, UNIFORM -> box;
        };
    }

    private static double size(CollisionShape.Box box, GizmoHandle axis) {
        return switch (axis) {
            case X -> box.maxX() - box.minX();
            case Y -> box.maxY() - box.minY();
            case Z -> box.maxZ() - box.minZ();
            case NONE, UNIFORM -> 0.0D;
        };
    }

    private void syncWidgets() {
        boolean selected = selectedIndex >= 0 && selectedIndex < boxes.size();
        for (EditBox field : fields) if (field != null) field.active = selected;
        if (selected) {
            CollisionShape.Box box = boxes.get(selectedIndex);
            double[] values = {box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()};
            for (int index = 0; index < fields.length; index++) {
                syncedValues[index] = format(values[index]);
                fields[index].setValue(syncedValues[index]);
            }
        } else {
            for (EditBox field : fields) if (field != null) field.setValue("");
        }
        boolean tool = validTool();
        if (addButton != null) addButton.active = tool;
        if (deleteButton != null) deleteButton.active = selected;
        if (clipButton != null) clipButton.active = CollisionBoxOperations.extendsOutsideUnitCell(boxes);
        if (toolButton != null) toolButton.setMessage(Component.translatable(tool
                ? "screen.yuushya_modelling_enhanced_editor.collision_editor.tool_slot"
                : "screen.yuushya_modelling_enhanced_editor.collision_editor.choose_tool",
                tool ? shapeToolSlot + 1 : ""));
    }

    private boolean validTool() {
        return minecraft != null && minecraft.player != null && minecraft.player.isCreative()
                && shapeToolSlot >= 0 && shapeToolSlot < 36
                && YuushyaShapeToolBridge.isShapeTool(toolStack());
    }

    private ItemStack toolStack() {
        return minecraft == null || minecraft.player == null || shapeToolSlot < 0 || shapeToolSlot >= 36
                ? ItemStack.EMPTY : minecraft.player.getInventory().getItem(shapeToolSlot);
    }

    private void requireTool() {
        setStatus(Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.collision_editor.tool_required"), true);
    }

    private void warnShapeRisks() {
        if (CollisionBoxOperations.extendsOutsideUnitCell(boxes)) {
            setStatus(Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.collision_editor.out_of_cell_warning"), true);
        } else if (boxes.size() > WARNING_THRESHOLD) {
            setStatus(Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.collision_editor.complexity_warning",
                    boxes.size()), false);
        }
    }

    private void setStatus(Component value, boolean error) {
        status = value == null ? Component.empty() : value;
        statusError = error;
    }

    private void ensureSelectedVisible() {
        if (selectedIndex < listScroll) listScroll = selectedIndex;
        if (selectedIndex >= listScroll + visibleRows()) listScroll = selectedIndex - visibleRows() + 1;
        listScroll = Math.max(0, listScroll);
    }

    private int visibleRows() { return Math.max(1, (height - 150) / 23); }
    private EditorViewport viewport() {
        return new EditorViewport(LEFT_WIDTH + 5, 26,
                Math.max(32, width - LEFT_WIDTH - RIGHT_WIDTH - 10), Math.max(32, height - 54));
    }
    private boolean insideViewport(double x, double y) {
        EditorViewport viewport = viewport();
        return x >= viewport.x() && x < viewport.x() + viewport.width()
                && y >= viewport.y() && y < viewport.y() + viewport.height();
    }

    private static List<BlockPreviewLayer> buildPreviewLayers(List<SceneLayer<Object>> layers) {
        List<BlockPreviewLayer> result = new ArrayList<>();
        for (SceneLayer<Object> layer : layers) {
            if (!layer.visible()) continue;
            BlockPreviewLayer.Content content = YuushyaEditorScreen.previewContent(layer.hostData());
            if (content != null) result.add(new BlockPreviewLayer(content, layer.transform(), false));
        }
        return List.copyOf(result);
    }

    private static String dimensions(CollisionShape.Box box) {
        return format(box.maxX() - box.minX()) + "×" + format(box.maxY() - box.minY())
                + "×" + format(box.maxZ() - box.minZ());
    }

    private static String format(double value) {
        String result = String.format(Locale.ROOT, "%.5f", value);
        int end = result.length();
        while (end > 0 && result.charAt(end - 1) == '0') end--;
        if (end > 0 && result.charAt(end - 1) == '.') end--;
        return end == 0 || result.substring(0, end).equals("-0") ? "0" : result.substring(0, end);
    }

    private static String errorMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private record GizmoDragSession(CollisionShape.Box box, CameraFrame frame,
            Vector3d origin, Vector3d axis, Vector3d startHit,
            double centerX, double centerY, double screenAxisX, double screenAxisY,
            double startCoordinate) { }
}
