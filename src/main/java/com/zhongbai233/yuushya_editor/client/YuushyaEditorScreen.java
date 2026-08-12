package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.yuushya_editor.compat.YuushyaEditorHost;
import com.zhongbai233.yuushya_editor.compat.YuushyaOverallScalePolicy;
import com.zhongbai233.yuushya_editor.compat.YuushyaTransformConversion;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewLayer;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewGizmo;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewPipRenderState;
import com.zhongbai233.yuushya_editor.client.renderer.PreviewLineWidthPolicy;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewFrame;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewManager;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldUi;
import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.ItemModelData;
import com.zhongbai233.yuushya_editor.core.SceneDocument;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import com.zhongbai233.yuushya_editor.core.TextModelData;
import com.zhongbai233.yuushya_editor.core.camera.CameraController;
import com.zhongbai233.yuushya_editor.core.camera.CameraFrame;
import com.zhongbai233.yuushya_editor.core.camera.CameraMatrices;
import com.zhongbai233.yuushya_editor.core.camera.CameraMode;
import com.zhongbai233.yuushya_editor.core.camera.CameraState;
import com.zhongbai233.yuushya_editor.core.camera.StandardCameraView;
import com.zhongbai233.yuushya_editor.core.command.CommandStack;
import com.zhongbai233.yuushya_editor.core.command.DragTransaction;
import com.zhongbai233.yuushya_editor.core.command.EditorCommand;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoDragMath;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoHandle;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoHitTesting;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoMode;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoSizingPolicy;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoSnapPolicy;
import com.zhongbai233.yuushya_editor.core.geometry.ZFightDetector;
import com.zhongbai233.yuushya_editor.core.geometry.ZFightOptimizer;
import com.zhongbai233.yuushya_editor.core.geometry.ZFightSaveCoordinator;
import com.zhongbai233.yuushya_editor.core.projection.PickingRay;
import com.zhongbai233.yuushya_editor.core.projection.ProjectedPoint;
import com.zhongbai233.yuushya_editor.core.projection.Projection;
import com.zhongbai233.yuushya_editor.core.projection.Viewport;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewTransform;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewPicking;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

/** NCPB scene-editor shell adapted to Yuushya show-block documents. */
public final class YuushyaEditorScreen extends Screen {
    private static final int GOLD = BlackGoldUi.GOLD;
    private static final int GOLD_DIM = BlackGoldUi.GOLD_DIM;
    private static final int TEXT = BlackGoldUi.TEXT_PRIMARY;
    private static final int TEXT_SECONDARY = BlackGoldUi.TEXT_SECONDARY;
    private static final int TEXT_DIM = BlackGoldUi.TEXT_DIM;
    private static final int ERROR = 0xFFFF6B6B;
    private static final int DEFAULT_LEFT_WIDTH = 156;
    private static final int DEFAULT_RIGHT_WIDTH = 226;
    private static final int PANEL_GAP = 4;
    private static final int ICON_WIDTH = 22;
    private static final int ICON_HEIGHT = 18;
    private static final int ICON_GAP = 3;
    private static final int ORIENTATION_WIDGET_RADIUS = 18;
    private static final int ORIENTATION_WIDGET_MARGIN_RIGHT = 30;
    private static final int ORIENTATION_WIDGET_MARGIN_BOTTOM = 40;
    private static final double GIZMO_HIT_RADIUS = 6.0D;
    private static final Vector3d WORLD_UP = new Vector3d(0.0D, 1.0D, 0.0D);

    private final YuushyaEditorHost<Object> host;
    private final Screen originalScreen;
    private final SceneDocument<Object> original;
    private final CommandStack<SceneDocument<Object>> history = new CommandStack<>(128);
    private final CameraController cameraController = new CameraController();
    private final EnvironmentPreviewManager environmentPreview = new EnvironmentPreviewManager();
    private final EditBox[] transformBoxes = new EditBox[7];
    private SceneDocument<Object> draft;
    private EnvironmentPreviewFrame environmentFrame = EnvironmentPreviewFrame.empty();
    private CameraState cachedFrameCamera;
    private Viewport cachedFrameViewport;
    private CameraFrame cachedCameraFrame;
    private SceneDocument<Object> cachedPreviewDraft;
    private List<BlockPreviewLayer> cachedPreviewLayers = List.of();
    private SceneLayer<Object> cachedGizmoLayer;
    private CameraFrame cachedGizmoFrame;
    private GizmoSizingPolicy.Sizes cachedGizmoSizes;
    private CameraState camera;
    private BlackGoldButton visibilityButton;
    private BlackGoldButton collisionShapeButton;
    private BlackGoldButton contentButton;
    private int layerScroll;
    private boolean draggingViewport;
    private int viewportDragButton = -1;
    private GizmoHandle activeGizmoHandle = GizmoHandle.NONE;
    private GizmoMode gizmoMode = GizmoMode.MOVE;
    private GizmoDragSession gizmoDragSession;
    private DragTransaction<SceneDocument<Object>> gizmoTransaction;
    private double lastMouseX;
    private double lastMouseY;
    private Component status = Component.empty();
    private boolean statusError;

    public YuushyaEditorScreen(YuushyaEditorHost<Object> host, Screen originalScreen) {
        super(Component.translatable("screen.yuushya_modelling_enhanced_editor.title"));
        this.host = Objects.requireNonNull(host, "host");
        this.originalScreen = Objects.requireNonNull(originalScreen, "originalScreen");
        this.original = host.loadDocument();
        SceneDocument<Object> startingDraft = host.initialLayer().map(original::append).orElse(original);
        this.draft = startingDraft.selectedLayerId() == null && !startingDraft.layers().isEmpty()
                ? startingDraft.select(startingDraft.layers().getFirst().id()) : startingDraft;
        this.camera = initialCamera(draft.layers());
    }

    @Override
    protected void init() {
        int leftWidth = leftPanelWidth();
        int rightX = rightPanelX();
        int rightWidth = width - rightX;
        int boxWidth = compactLayout() ? Math.max(42, (rightWidth - 82) / 2) : 54;
        int boxHeight = compactLayout() ? 16 : 18;
        String[] labels = {
                "screen.yuushya_modelling_enhanced_editor.position_x",
                "screen.yuushya_modelling_enhanced_editor.position_y",
                "screen.yuushya_modelling_enhanced_editor.position_z",
                "screen.yuushya_modelling_enhanced_editor.rotation_x",
                "screen.yuushya_modelling_enhanced_editor.rotation_y",
                "screen.yuushya_modelling_enhanced_editor.rotation_z",
                "screen.yuushya_modelling_enhanced_editor.overall_scale"
        };
        for (int i = 0; i < transformBoxes.length; i++) {
            EditBox box = new EditBox(font, inspectorBoxX(rightX, i), transformRowY(i), boxWidth, boxHeight,
                    Component.translatable(labels[i]));
            box.setMaxLength(32);
            transformBoxes[i] = addRenderableWidget(box);
        }

        int inspectorActionY = transformRowY(6) + boxHeight + 8;
        int inspectorActionWidth = Math.max(48, (rightWidth - 28) / 2);
        visibilityButton = addRenderableWidget(new BlackGoldButton(rightX + 12, inspectorActionY,
                inspectorActionWidth, 20, Component.empty(), button -> toggleVisibility(), GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(rightX + 16 + inspectorActionWidth, inspectorActionY,
                inspectorActionWidth, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.update"),
                button -> commitInspector("numeric transform"), GOLD));
        int inspectorFullWidth = Math.max(48, rightWidth - 24);
        int collisionButtonX = compactLayout() ? rightX + 12 : rightX + 12;
        int collisionButtonY = compactLayout() ? 4 : inspectorActionY + 24;
        int collisionButtonWidth = compactLayout() ? 28 : inspectorFullWidth;
        collisionShapeButton = addRenderableWidget(new BlackGoldButton(collisionButtonX, collisionButtonY,
                collisionButtonWidth, 20, Component.empty(), button -> cycleCollisionShape(), GOLD_DIM));
        if (host.editorType() != com.zhongbai233.yuushya_editor.core.EditorType.BLOCK) {
            contentButton = addRenderableWidget(new BlackGoldButton(collisionButtonX, collisionButtonY + 24,
                    collisionButtonWidth, 20, Component.empty(), button -> editSelectedContent(),
                    BlackGoldUi.CYAN));
        } else {
            contentButton = null;
        }

        int visibleRows = visibleLayerRows();
        int end = Math.min(draft.layers().size(), layerScroll + visibleRows);
        for (int index = layerScroll; index < end; index++) {
            SceneLayer<Object> layer = draft.layers().get(index);
            int y = layerRowY() + (index - layerScroll) * layerRowStep();
            boolean selected = layer.id().equals(draft.selectedLayerId());
            String label = (selected ? "◆ " : "  ") + (layer.visible() ? "▣ " : "□ ") + layer.name();
            addRenderableWidget(new BlackGoldButton(8, y, leftWidth - 16, 20,
                    Component.literal(label), button -> selectLayer(layer.id()), GOLD));
        }
        int addWidth = Math.max(54, leftWidth - 72);
        addRenderableWidget(new BlackGoldButton(8, height - 76, addWidth, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.add_button",
                        Component.translatable(host.editorType().addTranslationKey())),
                button -> openContentPicker(),
                BlackGoldUi.CYAN));
        addRenderableWidget(new BlackGoldButton(12 + addWidth, height - 76,
                Math.max(32, leftWidth - addWidth - 20), 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.delete"),
                button -> removeSelectedLayer(), 0xFFD04040));
        int leftActionY = height - 52;
        int halfLeft = Math.max(32, (leftWidth - 20) / 2);
        addRenderableWidget(new BlackGoldButton(8, leftActionY, halfLeft, 18,
                Component.literal("▲"), button -> scrollLayers(-1), GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(12 + halfLeft, leftActionY, halfLeft, 18,
                Component.literal("▼"), button -> scrollLayers(1), GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(8, height - 28, halfLeft, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.undo"),
                button -> undo(), GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(12 + halfLeft, height - 28, halfLeft, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.redo"),
                button -> redo(), GOLD_DIM));

        int fullInspectorWidth = Math.max(80, rightWidth - 24);
        addRenderableWidget(new BlackGoldButton(rightX + 12, height - 76, fullInspectorWidth, 20,
                Component.translatable(compactLayout()
                        ? "screen.yuushya_modelling_enhanced_editor.original_short"
                        : "screen.yuushya_modelling_enhanced_editor.original"),
                button -> openOriginal(), GOLD_DIM));
        int halfInspector = Math.max(38, (fullInspectorWidth - 4) / 2);
        addRenderableWidget(new BlackGoldButton(rightX + 12, height - 52, halfInspector, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.discard"),
                button -> discardAndClose(), 0xFFD04040));
        addRenderableWidget(new BlackGoldButton(rightX + 16 + halfInspector, height - 52,
                fullInspectorWidth - halfInspector - 4, 20,
                Component.translatable(compactLayout()
                        ? "screen.yuushya_modelling_enhanced_editor.apply_short"
                        : "screen.yuushya_modelling_enhanced_editor.apply"),
                button -> applyAndClose(), GOLD));

        int toolX = width - 8 - (ICON_WIDTH * 4 + ICON_GAP * 3);
        addRenderableWidget(new BlackGoldButton(toolX, 4, ICON_WIDTH, ICON_HEIGHT,
                Component.literal("⇱"), button -> gizmoMode = GizmoMode.MOVE, GOLD));
        toolX += ICON_WIDTH + ICON_GAP;
        addRenderableWidget(new BlackGoldButton(toolX, 4, ICON_WIDTH, ICON_HEIGHT,
                Component.literal("↻"), button -> gizmoMode = GizmoMode.ROTATE, GOLD));
        toolX += ICON_WIDTH + ICON_GAP;
        addRenderableWidget(new BlackGoldButton(toolX, 4, ICON_WIDTH, ICON_HEIGHT,
                Component.literal("⇲"), button -> gizmoMode = GizmoMode.SCALE, GOLD));
        toolX += ICON_WIDTH + ICON_GAP;
        addRenderableWidget(new BlackGoldButton(toolX, 4, ICON_WIDTH, ICON_HEIGHT,
                Component.literal("✕"), button -> onClose(), 0xFFD04040));
        syncInspector();
        updateEnvironmentPreview();
    }

    @Override
    public void tick() {
        super.tick();
        updateEnvironmentPreview();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xD0000000, 0xE0050505);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawSidePanels(graphics);
        drawViewport(graphics);
        drawEditorHud(graphics);
        drawToolSelection(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawSidePanels(GuiGraphicsExtractor graphics) {
        int leftWidth = leftPanelWidth();
        int rightX = rightPanelX();
        graphics.fillGradient(0, 0, leftWidth, height, BlackGoldUi.PANEL_TOP, BlackGoldUi.PANEL_BOTTOM);
        graphics.fillGradient(rightX, 0, width, height, BlackGoldUi.PANEL_TOP, BlackGoldUi.PANEL_BOTTOM);
        graphics.fillGradient(leftWidth - 1, 0, leftWidth, height, GOLD_DIM, GOLD_DIM);
        graphics.fillGradient(rightX, 0, rightX + 1, height, GOLD_DIM, GOLD_DIM);
        graphics.text(font, Component.translatable("screen.yuushya_modelling_enhanced_editor.hierarchy"),
                10, 10, GOLD, false);
        graphics.text(font, Component.translatable("screen.yuushya_modelling_enhanced_editor.layers"),
                10, 21, TEXT_DIM, false);
        graphics.text(font, Component.translatable(compactLayout()
                        ? "screen.yuushya_modelling_enhanced_editor.inspector_short"
                        : "screen.yuushya_modelling_enhanced_editor.inspector"),
                rightX + 12, 10, GOLD, false);

        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected != null) {
            int available = Math.max(0, width - rightX - 24);
            if (compactLayout()) {
                graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font,
                        selected.name(), available)), rightX + 12, 30, TEXT_SECONDARY, false);
                graphics.text(font,
                        Component.translatable("screen.yuushya_modelling_enhanced_editor.transform_group"),
                        rightX + 12, 42, TEXT_DIM, false);
            } else {
                graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font,
                        Component.translatable(host.editorType().translationKey()).getString(), available)),
                        rightX + 12, 34, TEXT_SECONDARY, false);
                graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font,
                        Component.translatable("screen.yuushya_modelling_enhanced_editor.name_value",
                                selected.name()).getString(), available)), rightX + 12, 52, TEXT_DIM, false);
                graphics.text(font, Component.translatable(selected.visible()
                        ? "screen.yuushya_modelling_enhanced_editor.state_visible"
                        : "screen.yuushya_modelling_enhanced_editor.state_hidden"),
                        rightX + 12, 68, selected.visible() ? 0xFF75D6A0 : TEXT_DIM, false);
                graphics.text(font,
                        Component.translatable("screen.yuushya_modelling_enhanced_editor.transform_group"),
                        rightX + 12, 82, TEXT_DIM, false);
            }
        } else {
            graphics.text(font, Component.translatable("screen.yuushya_modelling_enhanced_editor.no_selection"),
                    rightX + 12, 34, TEXT_DIM, false);
        }
        drawInspectorLabels(graphics, rightX);
    }

    private void drawInspectorLabels(GuiGraphicsExtractor graphics, int rightX) {
        String[] labels = {"PX", "PY", "PZ", "RX", "RY", "RZ", "S·XYZ"};
        for (int i = 0; i < labels.length; i++) {
            int boxX = inspectorBoxX(rightX, i);
            int labelX = boxX - font.width(labels[i]) - 4;
            int color = i == 6 ? GOLD
                    : i % 3 == 0 ? 0xFFFF6B5E : i % 3 == 1 ? 0xFF7ED957 : 0xFF61A8FF;
            graphics.text(font, labels[i], labelX, transformRowY(i) + 5, color, false);
        }
    }

    private void drawViewport(GuiGraphicsExtractor graphics) {
        int previewX = previewX();
        int previewWidth = previewWidth();
        graphics.fillGradient(previewX, 0, previewX + previewWidth, height,
                BlackGoldUi.VIEW_TOP, BlackGoldUi.VIEW_BOTTOM);
        graphics.fillGradient(previewX, 0, previewX + previewWidth, 1, GOLD_DIM, GOLD_DIM);
        graphics.fillGradient(previewX, height - 1, previewX + previewWidth, height,
                0xFF2A2312, 0xFF2A2312);
        Component editorTitle = Component.translatable("screen.yuushya_modelling_enhanced_editor.workspace_title");
        String titleText = BlackGoldUi.ellipsize(font, editorTitle.getString(), Math.max(0, previewWidth - 20));
        graphics.text(font, Component.literal(titleText), previewX + 10, 8, TEXT_SECONDARY, false);

        Viewport viewport = editorViewport();
        CameraFrame frame = currentCameraFrame();
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        submitBlockPreview(graphics, viewport, frame, selected);
        drawSelectedScreenMarker(graphics, viewport, frame, selected);
        drawOrientationWidget(graphics, viewport.x() + viewport.width() - ORIENTATION_WIDGET_MARGIN_RIGHT,
                viewport.y() + viewport.height() - ORIENTATION_WIDGET_MARGIN_BOTTOM);
    }

    /**
     * Keeps selection visible when a transparent or newly added block overlaps an opaque layer.
     * The world-space gold box remains the precise outline; these short screen-space corners are
     * only an occlusion-independent marker.
     */
    private void drawSelectedScreenMarker(GuiGraphicsExtractor graphics, Viewport viewport,
            CameraFrame frame, SceneLayer<Object> selected) {
        if (selected == null || !selected.visible() || !(selected.hostData() instanceof BlockState)) return;
        Matrix4f transform = BlockPreviewTransform.matrix(selected.transform());
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        int projectedCorners = 0;
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    Vector3f world = transform.transformPosition(new Vector3f(x, y, z));
                    ProjectedPoint point = Projection.project(new Vector3d(world),
                            frame.matrices(), frame.viewport());
                    if (point.behind() || !Double.isFinite(point.screenX())
                            || !Double.isFinite(point.screenY())) continue;
                    minX = Math.min(minX, point.screenX());
                    minY = Math.min(minY, point.screenY());
                    maxX = Math.max(maxX, point.screenX());
                    maxY = Math.max(maxY, point.screenY());
                    projectedCorners++;
                }
            }
        }
        if (projectedCorners == 0 || maxX < viewport.x() || minX >= viewport.x() + viewport.width()
                || maxY < viewport.y() || minY >= viewport.y() + viewport.height()) return;

        int left = Math.clamp((int) Math.floor(minX) - 2,
                viewport.x(), viewport.x() + viewport.width() - 1);
        int top = Math.clamp((int) Math.floor(minY) - 2,
                viewport.y(), viewport.y() + viewport.height() - 1);
        int right = Math.clamp((int) Math.ceil(maxX) + 2,
                viewport.x(), viewport.x() + viewport.width() - 1);
        int bottom = Math.clamp((int) Math.ceil(maxY) + 2,
                viewport.y(), viewport.y() + viewport.height() - 1);
        int arm = Math.max(3, Math.min(7, Math.min(right - left, bottom - top) / 4));
        int color = 0xFFFFD769;
        graphics.fill(left, top, Math.min(right + 1, left + arm + 1), top + 1, color);
        graphics.fill(left, top, left + 1, Math.min(bottom + 1, top + arm + 1), color);
        graphics.fill(Math.max(left, right - arm), top, right + 1, top + 1, color);
        graphics.fill(right, top, right + 1, Math.min(bottom + 1, top + arm + 1), color);
        graphics.fill(left, bottom, Math.min(right + 1, left + arm + 1), bottom + 1, color);
        graphics.fill(left, Math.max(top, bottom - arm), left + 1, bottom + 1, color);
        graphics.fill(Math.max(left, right - arm), bottom, right + 1, bottom + 1, color);
        graphics.fill(right, Math.max(top, bottom - arm), right + 1, bottom + 1, color);
    }

    private void drawEditorHud(GuiGraphicsExtractor graphics) {
        int hudX = previewX() + 10;
        int hudWidth = Math.max(0, previewWidth() - 20);
        String toolTip = switch (gizmoMode) {
            case MOVE -> Component.translatable("screen.yuushya_modelling_enhanced_editor.hud.move").getString();
            case ROTATE -> Component.translatable("screen.yuushya_modelling_enhanced_editor.hud.rotate").getString();
            case SCALE -> Component.translatable("screen.yuushya_modelling_enhanced_editor.hud.scale").getString();
        };
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, toolTip, hudWidth)),
                hudX, height - 34, TEXT_SECONDARY, false);

        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        String projection = Component.translatable(camera.mode() == CameraMode.ORTHOGRAPHIC
                ? "screen.yuushya_modelling_enhanced_editor.projection.orthographic"
                : "screen.yuushya_modelling_enhanced_editor.projection.perspective").getString();
        String selection = selected == null
                ? Component.translatable("screen.yuushya_modelling_enhanced_editor.no_selection").getString()
                : Component.translatable("screen.yuushya_modelling_enhanced_editor.selection",
                        selected.name(), Component.translatable(selected.visible()
                                ? "screen.yuushya_modelling_enhanced_editor.visible"
                                : "screen.yuushya_modelling_enhanced_editor.hidden")).getString();
        String lineWidth = Component.translatable("screen.yuushya_modelling_enhanced_editor.line_width",
                String.format(Locale.ROOT, "%.2f", PreviewLineWidthPolicy.forCamera(camera))).getString();
        String environment = environmentFrame.complete()
                ? Component.translatable("screen.yuushya_modelling_enhanced_editor.environment_count",
                        environmentFrame.retainedBlocks()).getString()
                : Component.translatable("screen.yuushya_modelling_enhanced_editor.environment_loading",
                        environmentFrame.retainedBlocks(), environmentFrame.pendingSections()).getString();
        String information = Component.translatable("screen.yuushya_modelling_enhanced_editor.hud.info",
                projection, lineWidth, environment, selection).getString();
        if (!status.getString().isBlank()) information = status.getString() + "  |  " + information;
        graphics.text(font, Component.literal(BlackGoldUi.ellipsize(font, information, hudWidth)),
                hudX, height - 18, statusError ? ERROR : TEXT_DIM, false);
    }

    private void drawToolSelection(GuiGraphicsExtractor graphics) {
        int buttonCount = 4;
        int barWidth = (ICON_WIDTH + ICON_GAP) * buttonCount - ICON_GAP + 6;
        int barX = width - barWidth - 4;
        graphics.fillGradient(barX, 2, barX + barWidth + 2, ICON_HEIGHT + 6,
                0xD0080A0D, 0xD0111318);
        graphics.outline(barX, 2, barWidth, ICON_HEIGHT + 4, 0x8045E7FF);
        int activeX = barX + 2 + gizmoMode.ordinal() * (ICON_WIDTH + ICON_GAP);
        graphics.outline(activeX - 1, 3, ICON_WIDTH + 2, ICON_HEIGHT + 2, BlackGoldUi.CYAN);
    }

    private void drawOrientationWidget(GuiGraphicsExtractor graphics, int centerX, int centerY) {
        int panelRadius = ORIENTATION_WIDGET_RADIUS + 7;
        drawDisc(graphics, centerX, centerY, panelRadius, 0xA03A4658);
        drawDisc(graphics, centerX, centerY, panelRadius - 1, 0xD00B0E14);

        Matrix4f view = CameraMatrices.create(camera, new Viewport(0, 0, 1, 1)).view();
        List<OrientationAxis> axes = new ArrayList<>(6);
        addOrientationAxis(axes, view, new Vector3f(1.0F, 0.0F, 0.0F), "X", 0xFFD34242);
        addOrientationAxis(axes, view, new Vector3f(-1.0F, 0.0F, 0.0F), "", 0xFFD34242);
        addOrientationAxis(axes, view, new Vector3f(0.0F, 1.0F, 0.0F), "Y", 0xFF3EC45B);
        addOrientationAxis(axes, view, new Vector3f(0.0F, -1.0F, 0.0F), "", 0xFF3EC45B);
        addOrientationAxis(axes, view, new Vector3f(0.0F, 0.0F, 1.0F), "Z", 0xFF3B70D4);
        addOrientationAxis(axes, view, new Vector3f(0.0F, 0.0F, -1.0F), "", 0xFF3B70D4);
        axes.sort(java.util.Comparator.comparingDouble(OrientationAxis::depth));
        for (OrientationAxis axis : axes) {
            if (axis.depth() < 0.0D) drawOrientationAxis(graphics, centerX, centerY, axis, false);
        }
        drawDisc(graphics, centerX, centerY, 3, 0xFF252B36);
        for (OrientationAxis axis : axes) {
            if (axis.depth() >= 0.0D) drawOrientationAxis(graphics, centerX, centerY, axis, true);
        }
    }

    private void drawOrientationAxis(GuiGraphicsExtractor graphics, int centerX, int centerY,
            OrientationAxis axis, boolean front) {
        int endX = centerX + (int) Math.round(axis.screenX() * ORIENTATION_WIDGET_RADIUS);
        int endY = centerY + (int) Math.round(axis.screenY() * ORIENTATION_WIDGET_RADIUS);
        int color = front ? axis.color() : dimColor(axis.color());
        drawLine(graphics, centerX, centerY, endX, endY, dimColor(color));
        drawDisc(graphics, endX, endY, front ? 5 : 3, color);
        if (!axis.label().isEmpty()) {
            graphics.centeredText(font, Component.literal(axis.label()), endX, endY - 3,
                    front ? 0xFF101318 : 0xFF667080);
        }
    }

    private static void addOrientationAxis(List<OrientationAxis> axes, Matrix4f view, Vector3f worldAxis,
            String label, int color) {
        Vector3f cameraAxis = view.transformDirection(new Vector3f(worldAxis)).normalize();
        axes.add(new OrientationAxis(cameraAxis.x, -cameraAxis.y, cameraAxis.z, label, color));
    }

    private static int dimColor(int color) {
        return (color & 0xFF000000) | (((color >>> 16) & 0xFF) / 2 << 16)
                | (((color >>> 8) & 0xFF) / 2 << 8) | ((color & 0xFF) / 2);
    }

    private void submitBlockPreview(GuiGraphicsExtractor graphics, Viewport viewport, CameraFrame frame,
            SceneLayer<Object> selected) {
        List<BlockPreviewLayer> layers = previewLayers();
        GizmoSizingPolicy.Sizes gizmoSizes = selected != null && selected.visible()
                ? gizmoSizes(selected, frame) : null;
        BlockPreviewGizmo gizmo = gizmoSizes == null ? null
                : new BlockPreviewGizmo(layerPivot(selected), gizmoMode, activeGizmoHandle,
                        gizmoSizes.axisLength(), gizmoSizes.rotationRadius(), gizmoSizes.scaleHandleLength());
        graphics.submitPictureInPictureRenderState(new BlockPreviewPipRenderState(environmentFrame,
                layers, frame, gizmo, draft.collisionShape(), PreviewLineWidthPolicy.forCamera(camera), true,
                viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + viewport.height(),
                graphics.peekScissorStack()));
    }

    private List<BlockPreviewLayer> previewLayers() {
        if (cachedPreviewDraft == draft) return cachedPreviewLayers;
        List<BlockPreviewLayer> layers = new ArrayList<>();
        for (SceneLayer<Object> layer : draft.layers()) {
            if (!layer.visible()) continue;
            BlockPreviewLayer.Content content;
            if (layer.hostData() instanceof BlockState blockState) {
                content = new BlockPreviewLayer.BlockContent(blockState);
            } else if (layer.hostData() instanceof ItemModelData itemData) {
                content = itemData.enableBlock() && itemData.blockState() != null
                        ? new BlockPreviewLayer.BlockContent(itemData.blockState())
                        : new BlockPreviewLayer.ItemContent(itemData.itemStack());
            } else if (layer.hostData() instanceof TextModelData textData) {
                content = new BlockPreviewLayer.TextContent(YuushyaTextCodec.component(textData.textLines()),
                        textData.culled(), textData.mirror());
            } else {
                continue;
            }
            layers.add(new BlockPreviewLayer(content, layer.transform(),
                    layer.id().equals(draft.selectedLayerId())));
        }
        cachedPreviewDraft = draft;
        cachedPreviewLayers = List.copyOf(layers);
        return cachedPreviewLayers;
    }

    private GizmoSizingPolicy.Sizes gizmoSizes(SceneLayer<Object> selected, CameraFrame frame) {
        if (cachedGizmoLayer != selected || cachedGizmoFrame != frame) {
            cachedGizmoLayer = selected;
            cachedGizmoFrame = frame;
            cachedGizmoSizes = GizmoSizingPolicy.calculate(selected.transform(), frame);
        }
        return cachedGizmoSizes;
    }

    private static void drawLine(GuiGraphicsExtractor graphics, int startX, int startY,
            int endX, int endY, int color) {
        int x = startX;
        int y = startY;
        int dx = Math.abs(endX - startX);
        int sx = startX < endX ? 1 : -1;
        int dy = -Math.abs(endY - startY);
        int sy = startY < endY ? 1 : -1;
        int error = dx + dy;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == endX && y == endY) return;
            int doubled = error * 2;
            if (doubled >= dy) {
                error += dy;
                x += sx;
            }
            if (doubled <= dx) {
                error += dx;
                y += sy;
            }
        }
    }

    private static void drawDisc(GuiGraphicsExtractor graphics, int centerX, int centerY,
            int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int) Math.floor(Math.sqrt(radius * radius - y * y));
            graphics.fill(centerX - halfWidth, centerY + y, centerX + halfWidth + 1, centerY + y + 1, color);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean cancelled) {
        if (cancelled) return false;
        if (super.mouseClicked(event, false)) return true;
        if ((event.button() == 0 || event.button() == 1) && insideViewport(event.x(), event.y())) {
            if (event.button() == 0) {
                if (!commitInspector("numeric transform")) return true;
                CameraFrame frame = currentCameraFrame();
                SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
                GizmoHandle handle = selected != null && selected.visible()
                        ? gizmoHandleAt(event.x(), event.y(), selected, frame)
                        : GizmoHandle.NONE;
                if (handle != GizmoHandle.NONE && beginGizmoDrag(event.x(), event.y(), frame, handle)) {
                    draggingViewport = true;
                    viewportDragButton = event.button();
                    lastMouseX = event.x();
                    lastMouseY = event.y();
                    return true;
                }
                selectLayerAt(event.x(), event.y());
            }
            draggingViewport = true;
            viewportDragButton = event.button();
            lastMouseX = event.x();
            lastMouseY = event.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingViewport && event.button() == viewportDragButton) {
            if (activeGizmoHandle != GizmoHandle.NONE) {
                applyGizmoDrag(event.x(), event.y(), event.hasShiftDown(), event.hasControlDown());
                lastMouseX = event.x();
                lastMouseY = event.y();
                return true;
            }
            double dx = event.x() - lastMouseX;
            double dy = event.y() - lastMouseY;
            if (viewportDragButton == 1) {
                camera = cameraController.panPixels(camera, dx, dy, editorViewport());
            } else {
                camera = cameraController.orbit(camera, Math.toRadians(dx * 0.35D),
                        Math.toRadians(-dy * 0.30D), WORLD_UP);
            }
            lastMouseX = event.x();
            lastMouseY = event.y();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingViewport && event.button() == viewportDragButton) {
            finishGizmoDrag();
            draggingViewport = false;
            viewportDragButton = -1;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= 0.0D && mouseX < leftPanelWidth() && scrollY != 0.0D) {
            scrollLayers(scrollY < 0.0D ? 1 : -1);
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
            StandardCameraView standardView = standardViewForKey(event.key());
            if (standardView != null) {
                camera = cameraController.standardView(camera, standardView, WORLD_UP);
                return true;
            }
            if (event.hasControlDown() && event.key() == GLFW.GLFW_KEY_Z) {
                undo();
                return true;
            }
            if (event.hasControlDown() && event.key() == GLFW.GLFW_KEY_Y) {
                redo();
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_F) {
                focusSelected();
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_W) {
                gizmoMode = GizmoMode.MOVE;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_E) {
                gizmoMode = GizmoMode.ROTATE;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_R) {
                gizmoMode = GizmoMode.SCALE;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_DELETE || event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                removeSelectedLayer();
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_O) {
                camera = cameraController.switchProjection(camera, CameraMode.ORTHOGRAPHIC);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_P) {
                camera = cameraController.switchProjection(camera, CameraMode.PERSPECTIVE);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private static StandardCameraView standardViewForKey(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_KP_1 -> StandardCameraView.FRONT;
            case GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_KP_2 -> StandardCameraView.BACK;
            case GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_KP_3 -> StandardCameraView.LEFT;
            case GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_KP_4 -> StandardCameraView.RIGHT;
            case GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_KP_5 -> StandardCameraView.TOP;
            case GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_KP_6 -> StandardCameraView.BOTTOM;
            default -> null;
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void selectLayer(java.util.UUID id) {
        if (!commitInspector("numeric transform")) return;
        draft = draft.select(id);
        ensureSelectedLayerVisible();
        rebuildWidgets();
    }

    private void openBlockPicker() {
        if (!commitInspector("numeric transform")) return;
        minecraft.setScreen(new BlockPickerScreen(this, this::addBlock));
    }

    private void openContentPicker() {
        switch (host.editorType()) {
            case BLOCK -> openBlockPicker();
            case ITEM -> {
                if (!commitInspector("numeric transform")) return;
                minecraft.setScreen(new ItemPickerScreen(this, this::addItem));
            }
            case TEXT -> {
                if (!commitInspector("numeric transform")) return;
                minecraft.setScreen(TextContentEditorScreen.forNewLayer(this, this::addText));
            }
        }
    }

    private void addBlock(BlockState blockState) {
        Objects.requireNonNull(blockState, "blockState");
        UUID id = UUID.randomUUID();
        EditorTransform transform = EditorTransform.IDENTITY;
        String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        SceneLayer<Object> layer = new SceneLayer<>(id,
                (draft.layers().size() + 1) + "  " + blockId, blockState, transform, true);
        SceneDocument<Object> before = draft;
        SceneDocument<Object> after = draft.append(layer);
        draft = history.execute(before, new SnapshotCommand(before, after, "add block " + blockId));
        ensureSelectedLayerVisible();
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.added",
                blockId), false);
    }

    private void addItem(ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack");
        String itemName = itemStack.getHoverName().getString();
        appendLayer(new ItemModelData(itemStack, 0xFFFFFFFF, false), itemName, "add item " + itemName);
    }

    private void addText(TextModelData textData) {
        Objects.requireNonNull(textData, "textData");
        String name = textData.textLines().isEmpty()
                ? Component.translatable("screen.yuushya_modelling_enhanced_editor.type.text").getString()
                : Component.translatable("screen.yuushya_modelling_enhanced_editor.text_layer_name",
                        draft.layers().size() + 1).getString();
        appendLayer(textData, name, "add text");
    }

    private void appendLayer(Object hostData, String name, String description) {
        UUID id = UUID.randomUUID();
        SceneLayer<Object> layer = new SceneLayer<>(id,
                (draft.layers().size() + 1) + "  " + name, hostData, EditorTransform.IDENTITY, true);
        SceneDocument<Object> before = draft;
        SceneDocument<Object> after = draft.append(layer);
        draft = history.execute(before, new SnapshotCommand(before, after, description));
        ensureSelectedLayerVisible();
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.added", name), false);
    }

    private void editSelectedContent() {
        if (!commitInspector("numeric transform")) return;
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected == null) return;
        if (selected.hostData() instanceof ItemModelData) {
            minecraft.setScreen(new ItemPickerScreen(this,
                    itemStack -> replaceSelectedContent(((ItemModelData) selected.hostData())
                            .withItemStack(itemStack), "replace item")));
        } else if (selected.hostData() instanceof TextModelData textData) {
            minecraft.setScreen(TextContentEditorScreen.forExisting(this, textData,
                    value -> replaceSelectedContent(value, "edit text")));
        }
    }

    private void replaceSelectedContent(Object hostData, String description) {
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected == null || selected.hostData().equals(hostData)) return;
        SceneLayer<Object> replacement = new SceneLayer<>(selected.id(), selected.name(), hostData,
                selected.transform(), selected.visible());
        SceneDocument<Object> before = draft;
        SceneDocument<Object> after = draft.replace(replacement);
        draft = history.execute(before, new SnapshotCommand(before, after, description));
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.content_updated"), false);
    }

    private void removeSelectedLayer() {
        if (!commitInspector("numeric transform")) return;
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected == null) return;
        SceneDocument<Object> before = draft;
        SceneDocument<Object> after = draft.remove(selected.id());
        draft = history.execute(before, new SnapshotCommand(before, after, "remove " + selected.name()));
        int maxScroll = Math.max(0, draft.layers().size() - visibleLayerRows());
        layerScroll = Math.min(layerScroll, maxScroll);
        ensureSelectedLayerVisible();
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.deleted",
                selected.name()), false);
        rebuildWidgets();
    }

    private void selectLayerAt(double mouseX, double mouseY) {
        Viewport viewport = editorViewport();
        CameraMatrices matrices = CameraMatrices.create(camera, viewport);
        PickingRay ray = Projection.rayFromScreen(mouseX, mouseY, matrices, viewport);
        SceneLayer<Object> best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (SceneLayer<Object> layer : draft.layers()) {
            if (!layer.visible()) continue;
            double distance = BlockPreviewPicking.hitDistance(ray, layer.transform()).orElse(Double.POSITIVE_INFINITY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = layer;
            }
        }
        if (best != null) {
            draft = draft.select(best.id());
            ensureSelectedLayerVisible();
            rebuildWidgets();
        }
    }

    private boolean commitInspector(String description) {
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected == null) return true;
        try {
            double px = finiteDouble(0);
            double py = finiteDouble(1);
            double pz = finiteDouble(2);
            float rx = finiteFloat(3);
            float ry = finiteFloat(4);
            float rz = finiteFloat(5);
            float overallScale = finiteFloat(6);
            EditorTransform transform = YuushyaOverallScalePolicy.apply(selected.transform(),
                    new Vector3d(px, py, pz),
                    YuushyaTransformConversion.fromEulerDegrees(new Vector3f(rx, ry, rz)), overallScale);
            if (!transform.equals(selected.transform())) {
                SceneDocument<Object> before = draft;
                SceneDocument<Object> after = draft.replace(selected.withTransform(transform));
                draft = history.execute(before, new SnapshotCommand(before, after, description));
                syncInspector();
            }
            setStatus("", false);
            return true;
        } catch (IllegalArgumentException exception) {
            setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.error.transform"), true);
            return false;
        }
    }

    private double finiteDouble(int index) {
        double value;
        try {
            value = Double.parseDouble(transformBoxes[index].getValue().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid transform number", exception);
        }
        if (!Double.isFinite(value)) throw new IllegalArgumentException("invalid transform finite");
        return value;
    }

    private float finiteFloat(int index) {
        double value = finiteDouble(index);
        if (value > Float.MAX_VALUE || value < -Float.MAX_VALUE) {
            throw new IllegalArgumentException("invalid transform range");
        }
        return (float) value;
    }

    private void toggleVisibility() {
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected == null) return;
        SceneDocument<Object> before = draft;
        SceneDocument<Object> after = draft.replace(selected.withVisible(!selected.visible()));
        draft = history.execute(before, new SnapshotCommand(before, after, "visibility"));
        syncInspector();
    }

    private void cycleCollisionShape() {
        if (!commitInspector("numeric transform")) return;
        CollisionShape.Kind next = draft.collisionShape().kind().nextEditablePreset();
        SceneDocument<Object> before = draft;
        SceneDocument<Object> after = draft.withCollisionShape(CollisionShape.forKind(next));
        draft = history.execute(before, new SnapshotCommand(before, after, "collision shape"));
        syncInspector();
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.collision",
                Component.translatable(next.translationKey())), false);
    }

    private void undo() {
        draft = history.undo(draft);
        syncInspector();
    }

    private void redo() {
        draft = history.redo(draft);
        syncInspector();
    }

    private void requestSave(Runnable onSuccess) {
        Objects.requireNonNull(onSuccess, "onSuccess");
        if (!commitInspector("numeric transform")) return;
        // Match NCPB's fingerprint-based close autosave: merely opening or selecting does not emit packets.
        if (!hasChanges()) {
            onSuccess.run();
            return;
        }
        YuushyaEditorHost.ValidationResult validation = host.validate(draft);
        if (!validation.valid()) {
            setStatus(Component.translatable(validation.message(), validation.arguments()), true);
            return;
        }
        List<ZFightDetector.Conflict> conflicts = detectZFightConflicts();
        if (conflicts.isEmpty()) {
            if (submitDraft()) onSuccess.run();
            return;
        }
        Set<String> pairs = new java.util.HashSet<>();
        for (ZFightDetector.Conflict conflict : conflicts) {
            String first = conflict.firstLayerId().toString();
            String second = conflict.secondLayerId().toString();
            pairs.add(first.compareTo(second) <= 0 ? first + second : second + first);
        }
        minecraft.setScreen(new ZFightWarningScreen(pairs.size(), conflicts.size(), decision -> {
            minecraft.setScreen(this);
            ZFightSaveCoordinator.Result result = ZFightSaveCoordinator.execute(decision,
                    () -> applyZFightOptimization(conflicts), this::submitDraft);
            if (result == ZFightSaveCoordinator.Result.SAVED) onSuccess.run();
        }));
    }

    private boolean hasChanges() {
        return !draft.layers().equals(original.layers())
                || !draft.collisionShape().equals(original.collisionShape());
    }

    private boolean submitDraft() {
        try {
            host.submit(original, draft);
            return true;
        } catch (RuntimeException exception) {
            setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.error.autosave",
                    exception.getMessage() == null ? "" : exception.getMessage()), true);
            return false;
        }
    }

    private void applyAndClose() {
        requestSave(() -> {
            environmentPreview.close();
            minecraft.setScreen(null);
        });
    }

    @Override
    public void onClose() {
        applyAndClose();
    }

    private void discardAndClose() {
        environmentPreview.close();
        minecraft.setScreen(null);
    }

    private void openOriginal() {
        requestSave(() -> {
            environmentPreview.close();
            YuushyaScreenInterceptor.openOriginal(originalScreen);
        });
    }

    private List<ZFightDetector.Conflict> detectZFightConflicts() {
        List<ZFightDetector.Candidate> candidates = new ArrayList<>();
        for (SceneLayer<Object> layer : draft.layers()) {
            if (layer.visible() && layer.hostData() instanceof BlockState) {
                candidates.add(new ZFightDetector.Candidate(layer.id(), layer.transform()));
            }
        }
        return ZFightDetector.detect(candidates);
    }

    private void applyZFightOptimization(List<ZFightDetector.Conflict> conflicts) {
        Map<UUID, Vector3d> offsets = ZFightOptimizer.worldOffsets(
                draft.layers().stream().map(SceneLayer::id).toList(), conflicts,
                ZFightOptimizer.DEFAULT_WORLD_EPSILON);
        if (offsets.isEmpty()) return;
        List<SceneLayer<Object>> adjusted = new ArrayList<>(draft.layers().size());
        for (SceneLayer<Object> layer : draft.layers()) {
            Vector3d worldOffset = offsets.get(layer.id());
            if (worldOffset == null) {
                adjusted.add(layer);
                continue;
            }
            EditorTransform transform = layer.transform();
            Vector3d rawDelta = ZFightOptimizer.rawPositionDelta(transform, worldOffset);
            adjusted.add(layer.withTransform(transform.withPosition(transform.position().add(rawDelta))));
        }
        SceneDocument<Object> before = draft;
        SceneDocument<Object> after = new SceneDocument<>(adjusted, draft.selectedLayerId(),
                draft.collisionShape());
        draft = history.execute(before, new SnapshotCommand(before, after, "automatic Z-fighting optimization"));
        syncInspector();
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.zfight_optimized",
                offsets.size()), false);
    }

    private void scrollLayers(int direction) {
        int max = Math.max(0, draft.layers().size() - visibleLayerRows());
        int next = Math.clamp(layerScroll + direction, 0, max);
        if (next != layerScroll) {
            layerScroll = next;
            rebuildWidgets();
        }
    }

    private int visibleLayerRows() {
        return Math.max(1, (height - layerRowY() - 86) / layerRowStep());
    }

    private boolean compactLayout() {
        return width < 650 || height < 330;
    }

    private int leftPanelWidth() {
        if (!compactLayout()) return DEFAULT_LEFT_WIDTH;
        return Math.clamp(width * 29 / 100, 104, 132);
    }

    private int rightPanelX() {
        int leftWidth = leftPanelWidth();
        if (!compactLayout()) return Math.max(leftWidth + 150, width - DEFAULT_RIGHT_WIDTH);
        int desiredRightWidth = Math.clamp(width * 36 / 100, 144, 180);
        return Math.max(leftWidth + 96, width - desiredRightWidth);
    }

    private int inspectorBoxX(int rightX, int index) {
        if (!compactLayout()) return rightX + (index % 2 == 0 ? 62 : 164);
        int rightWidth = width - rightX;
        int boxWidth = Math.max(42, (rightWidth - 82) / 2);
        return index % 2 == 0 ? rightX + 42 : rightX + rightWidth - 12 - boxWidth;
    }

    private int transformRowY(int index) {
        return (compactLayout() ? 54 : 112) + index / 2 * (compactLayout() ? 20 : 22);
    }

    private int layerRowY() {
        return 34;
    }

    private int layerRowStep() {
        return 24;
    }

    private void ensureSelectedLayerVisible() {
        if (draft.selectedLayerId() == null) return;
        int index = -1;
        for (int i = 0; i < draft.layers().size(); i++) {
            if (draft.layers().get(i).id().equals(draft.selectedLayerId())) {
                index = i;
                break;
            }
        }
        if (index < layerScroll) layerScroll = index;
        if (index >= layerScroll + visibleLayerRows()) layerScroll = index - visibleLayerRows() + 1;
    }

    private void syncInspector() {
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        boolean active = selected != null;
        for (EditBox box : transformBoxes) {
            if (box != null) box.active = active;
        }
        if (collisionShapeButton != null) {
            collisionShapeButton.active = true;
            collisionShapeButton.setMessage(Component.translatable(compactLayout()
                    ? "screen.yuushya_modelling_enhanced_editor.collision_short"
                    : "screen.yuushya_modelling_enhanced_editor.collision",
                    Component.translatable(draft.collisionShape().kind().translationKey())));
        }
        if (contentButton != null) {
            contentButton.active = active;
            contentButton.setMessage(Component.translatable(host.editorType()
                    == com.zhongbai233.yuushya_editor.core.EditorType.ITEM
                    ? "screen.yuushya_modelling_enhanced_editor.replace_item"
                    : "screen.yuushya_modelling_enhanced_editor.edit_text"));
        }
        if (!active) {
            if (visibilityButton != null) visibilityButton.active = false;
            return;
        }
        EditorTransform transform = selected.transform();
        Vector3d position = transform.position();
        Vector3f rotation = YuushyaTransformConversion.toEulerDegrees(transform.rotation());
        Vector3f scale = transform.scale();
        double[] values = {position.x, position.y, position.z, rotation.x, rotation.y, rotation.z};
        for (int i = 0; i < values.length; i++) transformBoxes[i].setValue(format(values[i]));
        // Float.toString round-trips exactly, so committing another field cannot accidentally
        // normalize legacy non-uniform scale data through display rounding.
        transformBoxes[6].setValue(Float.toString(scale.x));
        visibilityButton.active = true;
        visibilityButton.setMessage(Component.translatable(selected.visible()
                ? "screen.yuushya_modelling_enhanced_editor.visible"
                : "screen.yuushya_modelling_enhanced_editor.hidden"));
    }

    private void focusSelected() {
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected == null) return;
        camera = cameraController.focus(camera, layerPivot(selected), 1.0D, editorViewport(), WORLD_UP);
    }

    private Viewport editorViewport() {
        return new Viewport(previewX() + 1, 26,
                Math.max(32, previewWidth() - 2), Math.max(32, height - 54));
    }

    private int previewX() {
        return leftPanelWidth() + PANEL_GAP;
    }

    private int previewWidth() {
        return Math.max(1, rightPanelX() - PANEL_GAP - previewX());
    }

    private CameraFrame currentCameraFrame() {
        Viewport viewport = editorViewport();
        if (cachedCameraFrame == null || cachedFrameCamera != camera || !viewport.equals(cachedFrameViewport)) {
            cachedFrameCamera = camera;
            cachedFrameViewport = viewport;
            cachedCameraFrame = new CameraFrame(CameraMatrices.create(camera, viewport), viewport, camera.mode());
        }
        return cachedCameraFrame;
    }

    private boolean beginGizmoDrag(double mouseX, double mouseY, CameraFrame frame, GizmoHandle handle) {
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected == null || handle == GizmoHandle.NONE) return false;
        Vector3d origin = layerPivot(selected);
        PickingRay ray = Projection.rayFromScreen(mouseX, mouseY, frame.matrices(), frame.viewport());
        Vector3d axis = handle == GizmoHandle.UNIFORM ? new Vector3d() : handle.axis();
        Vector3d startHit;
        double modelUnitsPerWorldUnit = 0.0D;
        double centerScreenX = 0.0D;
        double centerScreenY = 0.0D;
        double startScreenDistance = 0.0D;
        if (gizmoMode == GizmoMode.MOVE) {
            startHit = GizmoDragMath.intersectConstraint(ray, origin, axis, handle.constraint()).orElse(null);
            if (startHit == null) return false;
            Vector3f scale = selected.transform().scale();
            double scaleAlongAxis = axis.x * scale.x + axis.y * scale.y + axis.z * scale.z;
            modelUnitsPerWorldUnit = BlockPreviewTransform.MODEL_UNITS_PER_WORLD_UNIT / scaleAlongAxis;
        } else if (gizmoMode == GizmoMode.ROTATE) {
            startHit = GizmoDragMath.intersectConstraint(ray, origin, axis, handle.rotationConstraint())
                    .orElse(null);
            if (startHit == null) return false;
        } else {
            float initialScale = selected.transform().scale().x;
            if (!Float.isFinite(initialScale) || initialScale <= 0.0F) {
                setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.error.scale_positive"), true);
                return false;
            }
            ProjectedPoint projected = Projection.project(origin, frame.matrices(), frame.viewport());
            if (!projected.visible()) return false;
            centerScreenX = projected.screenX();
            centerScreenY = projected.screenY();
            startScreenDistance = Math.hypot(mouseX - centerScreenX, mouseY - centerScreenY);
            if (startScreenDistance <= 1.0E-9D) return false;
            startHit = new Vector3d(origin);
        }
        activeGizmoHandle = handle;
        gizmoDragSession = new GizmoDragSession(selected, frame, gizmoMode, origin, axis, startHit,
                modelUnitsPerWorldUnit, centerScreenX, centerScreenY, startScreenDistance);
        gizmoTransaction = new DragTransaction<>(draft, gizmoMode.label().toLowerCase(Locale.ROOT)
                + " " + handle.name());
        return true;
    }

    private void applyGizmoDrag(double mouseX, double mouseY, boolean shiftDown, boolean controlDown) {
        GizmoDragSession session = gizmoDragSession;
        DragTransaction<SceneDocument<Object>> transaction = gizmoTransaction;
        if (session == null || transaction == null) return;
        EditorTransform starting = session.layer().transform();
        EditorTransform changed;
        if (session.mode() == GizmoMode.SCALE) {
            double distance = Math.hypot(mouseX - session.centerScreenX(), mouseY - session.centerScreenY());
            double factor = GizmoDragMath.uniformScaleFactor(session.startScreenDistance(), distance);
            double requested = starting.scale().x * factor;
            double scaleDelta = GizmoSnapPolicy.snapDelta(requested - starting.scale().x,
                    GizmoSnapPolicy.step(GizmoMode.SCALE, shiftDown, controlDown));
            requested = starting.scale().x + scaleDelta;
            if (!Double.isFinite(requested) || requested <= 0.0D || requested > Float.MAX_VALUE) return;
            changed = YuushyaOverallScalePolicy.apply(starting, starting.position(), starting.rotation(),
                    (float) requested);
        } else {
            PickingRay ray = Projection.rayFromScreen(mouseX, mouseY, session.frame().matrices(),
                    session.frame().viewport());
            Vector3d currentHit = GizmoDragMath.intersectConstraint(ray, session.origin(), session.axis(),
                    session.mode() == GizmoMode.MOVE
                            ? activeGizmoHandle.constraint() : activeGizmoHandle.rotationConstraint())
                    .orElse(null);
            if (currentHit == null) return;
            if (session.mode() == GizmoMode.MOVE) {
                double worldDelta = new Vector3d(currentHit).sub(session.startHit()).dot(session.axis());
                double snappedWorldDelta = GizmoSnapPolicy.snapDelta(worldDelta,
                        GizmoSnapPolicy.step(GizmoMode.MOVE, shiftDown, controlDown));
                Vector3d modelPosition = starting.position().add(new Vector3d(session.axis())
                        .mul(snappedWorldDelta * session.modelUnitsPerWorldUnit()));
                changed = starting.withPosition(modelPosition);
            } else {
                float degrees = GizmoDragMath.rotationDeltaDegrees(session.origin(), session.axis(),
                        session.startHit(), currentHit);
                degrees = (float) GizmoSnapPolicy.snapDelta(degrees,
                        GizmoSnapPolicy.step(GizmoMode.ROTATE, shiftDown, controlDown));
                Quaternionf delta = new Quaternionf().rotationAxis((float) Math.toRadians(degrees),
                        (float) session.axis().x, (float) session.axis().y, (float) session.axis().z);
                changed = starting.withRotation(delta.mul(starting.rotation()));
            }
        }
        SceneLayer<Object> transformed = session.layer().withTransform(changed);
        draft = transaction.update(ignored -> transaction.before().replace(transformed));
        syncInspector();
    }

    private GizmoHandle gizmoHandleAt(double mouseX, double mouseY,
            SceneLayer<Object> selected, CameraFrame frame) {
        Vector3d origin = layerPivot(selected);
        GizmoSizingPolicy.Sizes sizes = gizmoSizes(selected, frame);
        return switch (gizmoMode) {
            case MOVE -> GizmoHitTesting.moveHandleAt(mouseX, mouseY, origin,
                    frame.matrices(), frame.viewport(), sizes.axisLength(), GIZMO_HIT_RADIUS);
            case ROTATE -> GizmoHitTesting.rotateHandleAt(mouseX, mouseY, origin,
                    frame.matrices(), frame.viewport(), sizes.rotationRadius(), GIZMO_HIT_RADIUS);
            case SCALE -> GizmoHitTesting.scaleHandleAt(mouseX, mouseY, origin,
                    frame.matrices(), frame.viewport(), sizes.scaleHandleLength(), GIZMO_HIT_RADIUS);
        };
    }

    private void updateEnvironmentPreview() {
        BlockPos origin = host.worldOrigin().orElse(null);
        if (origin == null || minecraft == null || minecraft.level == null) {
            environmentPreview.close();
            environmentFrame = EnvironmentPreviewFrame.empty();
            return;
        }
        environmentPreview.update(minecraft.level, origin);
        environmentPreview.tick();
        environmentFrame = environmentPreview.frame();
    }

    private String gizmoShortcutLabel() {
        if (compactLayout()) return switch (gizmoMode) {
            case MOVE -> "[W] Move";
            case ROTATE -> "[E] Rotate";
            case SCALE -> "[R] Scale";
        };
        return switch (gizmoMode) {
            case MOVE -> "[W] Move · E Rotate · R Scale";
            case ROTATE -> "W Move · [E] Rotate · R Scale";
            case SCALE -> "W Move · E Rotate · [R] Scale";
        };
    }

    private void finishGizmoDrag() {
        if (gizmoTransaction != null) draft = gizmoTransaction.commit(history);
        activeGizmoHandle = GizmoHandle.NONE;
        gizmoDragSession = null;
        gizmoTransaction = null;
        syncInspector();
    }

    private boolean insideViewport(double x, double y) {
        Viewport viewport = editorViewport();
        return x >= viewport.x() && x < viewport.x() + viewport.width()
                && y >= viewport.y() && y < viewport.y() + viewport.height();
    }

    private static Vector3d layerPivot(SceneLayer<Object> layer) {
        return BlockPreviewTransform.pivot(layer.transform());
    }

    private static CameraState initialCamera(List<SceneLayer<Object>> layers) {
        Vector3d focus = new Vector3d();
        if (!layers.isEmpty()) {
            for (SceneLayer<Object> layer : layers) focus.add(layerPivot(layer));
            focus.div(layers.size());
        }
        return CameraState.lookingAt(CameraMode.PERSPECTIVE,
                new Vector3d(focus).add(7.0D, 5.5D, 9.0D), focus, WORLD_UP,
                45.0F, 5.0F, 0.05F, 2048.0F);
    }

    private void setStatus(String message, boolean error) {
        setStatus(message == null ? Component.empty() : Component.literal(message), error);
    }

    private void setStatus(Component message, boolean error) {
        status = message == null ? Component.empty() : message;
        statusError = error;
    }

    private static String format(double value) {
        String result = String.format(Locale.ROOT, "%.5f", value);
        int end = result.length();
        while (end > 0 && result.charAt(end - 1) == '0') end--;
        if (end > 0 && result.charAt(end - 1) == '.') end--;
        return end == 0 || result.substring(0, end).equals("-0") ? "0" : result.substring(0, end);
    }

    private record OrientationAxis(double screenX, double screenY, double depth, String label, int color) { }

    private record SnapshotCommand(SceneDocument<Object> before, SceneDocument<Object> after,
            String description) implements EditorCommand<SceneDocument<Object>> {
        @Override
        public SceneDocument<Object> apply(SceneDocument<Object> state) {
            return after;
        }

        @Override
        public SceneDocument<Object> undo(SceneDocument<Object> state) {
            return before;
        }
    }

    private record GizmoDragSession(SceneLayer<Object> layer, CameraFrame frame, GizmoMode mode,
            Vector3d origin, Vector3d axis, Vector3d startHit, double modelUnitsPerWorldUnit,
            double centerScreenX, double centerScreenY, double startScreenDistance) {
        private GizmoDragSession {
            origin = new Vector3d(origin);
            axis = new Vector3d(axis);
            startHit = new Vector3d(startHit);
        }

        @Override
        public Vector3d origin() { return new Vector3d(origin); }

        @Override
        public Vector3d axis() { return new Vector3d(axis); }

        @Override
        public Vector3d startHit() { return new Vector3d(startHit); }
    }
}
