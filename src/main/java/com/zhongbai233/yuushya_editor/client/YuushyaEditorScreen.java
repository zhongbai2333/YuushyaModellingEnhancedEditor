package com.zhongbai233.yuushya_editor.client;

import com.mojang.blaze3d.platform.Window;
import com.zhongbai233.yuushya_editor.compat.YuushyaEditorHost;
import com.zhongbai233.yuushya_editor.compat.YuushyaItemModelSupport;
import com.zhongbai233.yuushya_editor.compat.YuushyaScalePolicy;
import com.zhongbai233.yuushya_editor.compat.YuushyaShapeToolBridge;
import com.zhongbai233.yuushya_editor.compat.YuushyaTransformConversion;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewLayer;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewGizmo;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewPipRenderer;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewPipRenderState;
import com.zhongbai233.scene_editor.core.render.LineWidthPolicy;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewFrame;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewManager;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentModeledBlock;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldToolButton;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldUi;
import com.zhongbai233.yuushya_editor.core.EditorType;
import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.ItemModelData;
import com.zhongbai233.scene_editor.core.selection.MultiSelectionPolicy;
import com.zhongbai233.yuushya_editor.core.SceneDocument;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import com.zhongbai233.yuushya_editor.core.SceneLayerClipboard;
import com.zhongbai233.yuushya_editor.core.TextModelData;
import com.zhongbai233.scene_editor.core.camera.EditorCameraController;
import com.zhongbai233.scene_editor.core.camera.CameraFrame;
import com.zhongbai233.scene_editor.core.camera.CameraMatrices;
import com.zhongbai233.scene_editor.core.camera.EditorCameraMode;
import com.zhongbai233.scene_editor.core.camera.EditorCameraState;
import com.zhongbai233.scene_editor.core.camera.CursorWrapPolicy;
import com.zhongbai233.scene_editor.core.camera.StandardCameraView;
import com.zhongbai233.scene_editor.core.command.CommandStack;
import com.zhongbai233.scene_editor.core.transaction.DragTransaction;
import com.zhongbai233.scene_editor.core.command.EditorCommand;
import com.zhongbai233.scene_editor.core.session.EditorSessionPool;
import com.zhongbai233.scene_editor.core.gizmo.GizmoDragMath;
import com.zhongbai233.scene_editor.core.gizmo.GizmoHandle;
import com.zhongbai233.scene_editor.core.gizmo.GizmoMode;
import com.zhongbai233.scene_editor.core.gizmo.GizmoSizingPolicy;
import com.zhongbai233.scene_editor.core.gizmo.GizmoSnapPolicy;
import com.zhongbai233.yuushya_editor.core.geometry.SelectionTransforms;
import com.zhongbai233.yuushya_editor.core.geometry.SpatialOrdering;
import com.zhongbai233.yuushya_editor.core.geometry.ZFightDetector;
import com.zhongbai233.yuushya_editor.core.geometry.ZFightOptimizer;
import com.zhongbai233.yuushya_editor.core.geometry.ZFightSaveCoordinator;
import com.zhongbai233.scene_editor.core.projection.PickingRay;
import com.zhongbai233.scene_editor.core.projection.ProjectedPoint;
import com.zhongbai233.scene_editor.core.projection.EditorProjection;
import com.zhongbai233.scene_editor.core.projection.EditorViewport;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewTransform;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewPicking;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import com.zhongbai233.yuushya_editor.core.preview.ViewportGizmoHitTesting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

/** NCPB scene-editor shell adapted to Yuushya show-block documents. */
public final class YuushyaEditorScreen extends Screen {
    private static final int GOLD = BlackGoldUi.GOLD;
    private static final int GOLD_DIM = BlackGoldUi.GOLD_DIM;
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
    private static final double POINTER_DRAG_THRESHOLD = 2.0D;
    private static final double CURSOR_WRAP_MARGIN = 2.0D;
    private static final Vector3d WORLD_UP = new Vector3d(0.0D, 1.0D, 0.0D);

    private final YuushyaEditorHost<Object> host;
    private final Screen originalScreen;
    private final SceneDocument<Object> original;
    private final CommandStack<SceneDocument<Object>> history;
    private final Optional<EditorHistorySessions.Key> historySessionKey;
    private final SceneLayerClipboard<Object> layerClipboard = new SceneLayerClipboard<>();
    private final EditorCameraController cameraController = new EditorCameraController();
    private final EnvironmentPreviewManager environmentPreview = new EnvironmentPreviewManager();
    private final EditBox[] transformBoxes = new EditBox[9];
    private final String[] syncedTransformValues = new String[9];
    private SceneDocument<Object> draft;
    /** The document selection remains the primary inspector layer; this set is the full UI selection. */
    private final LinkedHashSet<UUID> selectedLayerIds = new LinkedHashSet<>();
    private EnvironmentPreviewFrame environmentFrame = EnvironmentPreviewFrame.empty();
    private EditorCameraState cachedFrameCamera;
    private EditorViewport cachedFrameViewport;
    private CameraFrame cachedCameraFrame;
    private SceneDocument<Object> cachedPreviewDraft;
    private EnvironmentPreviewFrame cachedPreviewEnvironmentFrame;
    private UUID cachedPreviewHoveredLayerId;
    private List<BlockPreviewLayer> cachedPreviewLayers = List.of();
    private EditorCameraState camera;
    private BlackGoldButton visibilityButton;
    private BlackGoldButton collisionShapeButton;
    private BlackGoldButton collisionEditButton;
    private BlackGoldButton contentButton;
    private int layerScroll;
    private boolean draggingViewport;
    private int viewportDragButton = -1;
    private GizmoHandle activeGizmoHandle = GizmoHandle.NONE;
    private GizmoMode gizmoMode = GizmoMode.MOVE;
    private GizmoDragSession gizmoDragSession;
    private DragTransaction<SceneDocument<Object>> gizmoTransaction;
    private double gizmoPressX;
    private double gizmoPressY;
    private boolean gizmoPointerMoved;
    private boolean gizmoPressShiftDown;
    private boolean gizmoPressControlDown;
    private boolean gizmoDragShiftDown;
    private boolean gizmoDragControlDown;
    private double viewportPressX;
    private double viewportPressY;
    private boolean viewportPointerMoved;
    private boolean viewportPressShiftDown;
    private boolean viewportPressControlDown;
    private double virtualDragMouseX;
    private double virtualDragMouseY;
    private Component status = Component.empty();
    private boolean statusError;
    private boolean layerClickShiftDown;
    private boolean layerClickControlDown;
    private int shapeToolInventorySlot = -1;

    public YuushyaEditorScreen(YuushyaEditorHost<Object> host, Screen originalScreen) {
        super(Component.translatable("screen.yuushya_modelling_enhanced_editor.title"));
        this.host = Objects.requireNonNull(host, "host");
        this.originalScreen = Objects.requireNonNull(originalScreen, "originalScreen");
        SceneDocument<Object> loadedDocument = host.loadDocument();
        SceneDocument<Object> startingDraft = host.initialLayer().map(loadedDocument::append).orElse(loadedDocument);
        this.historySessionKey = EditorHistorySessions.key(host);
        Optional<EditorSessionPool.Session<SceneDocument<Object>>> restored = historySessionKey
                .flatMap(key -> EditorHistorySessions.take(key, startingDraft));
        if (restored.isPresent() && host.rebindDocumentIdentity(restored.orElseThrow().document())) {
            this.original = restored.orElseThrow().document();
            this.draft = original;
            this.history = restored.orElseThrow().history();
        } else {
            this.original = loadedDocument;
            this.draft = startingDraft;
            this.history = new CommandStack<>(128);
        }
        this.draft = draft.selectedLayerId() == null && !draft.layers().isEmpty()
                ? draft.select(draft.layers().getFirst().id()) : draft;
        if (draft.selectedLayerId() != null) selectedLayerIds.add(draft.selectedLayerId());
        this.camera = initialCamera(draft.layers(), host.worldOrigin().orElse(null));
    }

    @Override
    protected void init() {
        int leftWidth = leftPanelWidth();
        int rightX = rightPanelX();
        int rightWidth = width - rightX;
        int boxWidth = compactLayout() ? Math.max(24, (rightWidth - 24) / 3 - 16) : 54;
        int boxHeight = compactLayout() ? 16 : 18;
        String[] labels = {
                "screen.yuushya_modelling_enhanced_editor.position_x",
                "screen.yuushya_modelling_enhanced_editor.position_y",
                "screen.yuushya_modelling_enhanced_editor.position_z",
                "screen.yuushya_modelling_enhanced_editor.rotation_x",
                "screen.yuushya_modelling_enhanced_editor.rotation_y",
                "screen.yuushya_modelling_enhanced_editor.rotation_z",
                "screen.yuushya_modelling_enhanced_editor.scale_x",
                "screen.yuushya_modelling_enhanced_editor.scale_y",
                "screen.yuushya_modelling_enhanced_editor.scale_z"
        };
        for (int i = 0; i < transformBoxes.length; i++) {
            EditBox box = new EditBox(font, inspectorBoxX(rightX, i), transformRowY(i), boxWidth, boxHeight,
                    Component.translatable(labels[i]));
            box.setMaxLength(32);
            transformBoxes[i] = addRenderableWidget(box);
        }

        int inspectorActionY = transformRowY(8) + boxHeight + 8;
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
        int collisionButtonWidth = compactLayout() ? 28 : Math.max(24, inspectorFullWidth - 26);
        collisionShapeButton = addRenderableWidget(new BlackGoldButton(collisionButtonX, collisionButtonY,
                collisionButtonWidth, 20, Component.empty(), button -> cycleCollisionShape(), GOLD_DIM));
        collisionEditButton = addRenderableWidget(new BlackGoldButton(
                collisionButtonX + collisionButtonWidth + 4, collisionButtonY, 22, 20,
                Component.literal("✎"), button -> openCollisionEditor(), BlackGoldUi.CYAN));
        contentButton = addRenderableWidget(new BlackGoldButton(collisionButtonX, collisionButtonY + 24,
                compactLayout() ? collisionButtonWidth : inspectorFullWidth, 20,
                Component.empty(), button -> editSelectedContent(),
                BlackGoldUi.CYAN));

        int visibleRows = visibleLayerRows();
        int end = Math.min(draft.layers().size(), layerScroll + visibleRows);
        for (int index = layerScroll; index < end; index++) {
            SceneLayer<Object> layer = draft.layers().get(index);
            int y = layerRowY() + (index - layerScroll) * layerRowStep();
            boolean selected = selectedLayerIds.contains(layer.id());
            boolean primary = layer.id().equals(draft.selectedLayerId());
            String label = (primary ? "◆ " : selected ? "◇ " : "  ")
                    + (layer.visible() ? "▣ " : "□ ") + layer.name();
            addRenderableWidget(new BlackGoldButton(8, y, leftWidth - 16, 20,
                    Component.literal(label), button -> selectLayer(layer.id()), GOLD));
        }
        int addWidth = Math.max(54, leftWidth - 72);
        addRenderableWidget(new BlackGoldButton(8, height - 100, addWidth, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.add_button",
                        Component.translatable(host.editorType().addTranslationKey())),
                button -> openContentPicker(),
                BlackGoldUi.CYAN));
        addRenderableWidget(new BlackGoldButton(12 + addWidth, height - 100,
                Math.max(32, leftWidth - addWidth - 20), 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.delete"),
                button -> removeSelectedLayer(), 0xFFD04040));
        if (supportsInventoryPicker()) {
            addRenderableWidget(new BlackGoldButton(8, height - 124, leftWidth - 16, 20,
                    Component.translatable(
                            "screen.yuushya_modelling_enhanced_editor.add_from_inventory"),
                    button -> openInventoryPicker(), BlackGoldUi.CYAN));
        }
        int halfLeft = Math.max(32, (leftWidth - 20) / 2);
        addRenderableWidget(new BlackGoldButton(8, height - 76, halfLeft, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.copy_layer"),
                button -> copySelectedLayer(), GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(12 + halfLeft, height - 76, halfLeft, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.paste_layer"),
                button -> pasteLayer(), GOLD_DIM));
        int leftActionY = height - 52;
        int thirdLeft = Math.max(22, (leftWidth - 24) / 3);
        addRenderableWidget(new BlackGoldButton(8, leftActionY, thirdLeft, 18,
                Component.literal("▲"), button -> scrollLayers(-1), GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(12 + thirdLeft, leftActionY, thirdLeft, 18,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.organize_short"),
                button -> organizeLayersSpatially(), BlackGoldUi.CYAN));
        addRenderableWidget(new BlackGoldButton(16 + thirdLeft * 2, leftActionY,
                leftWidth - thirdLeft * 2 - 24, 18,
                Component.literal("▼"), button -> scrollLayers(1), GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(8, height - 28, halfLeft, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.undo"),
                button -> undo(), GOLD_DIM));
        addRenderableWidget(new BlackGoldButton(12 + halfLeft, height - 28, halfLeft, 20,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.redo"),
                button -> redo(), GOLD_DIM));

        int fullInspectorWidth = Math.max(80, rightWidth - 24);
        int halfInspector = Math.max(38, (fullInspectorWidth - 4) / 2);
        if (compactLayout()) {
            int compactWidth = Math.max(24, (fullInspectorWidth - 8) / 3);
            addRenderableWidget(new BlackGoldButton(rightX + 12, height - 76, compactWidth, 20,
                    Component.translatable("screen.yuushya_modelling_enhanced_editor.original_short"),
                    button -> openOriginal(), GOLD_DIM));
            addRenderableWidget(new BlackGoldButton(rightX + 16 + compactWidth, height - 76,
                    compactWidth, 20,
                    Component.translatable("screen.yuushya_modelling_enhanced_editor.export_native_short"),
                    button -> exportNative(), GOLD_DIM));
            addRenderableWidget(new BlackGoldButton(rightX + 20 + compactWidth * 2, height - 76,
                    fullInspectorWidth - compactWidth * 2 - 8, 20,
                    Component.translatable("screen.yuushya_modelling_enhanced_editor.import_native_short"),
                    button -> importNative(), GOLD_DIM));
        } else {
            addRenderableWidget(new BlackGoldButton(rightX + 12, height - 100, fullInspectorWidth, 20,
                    Component.translatable("screen.yuushya_modelling_enhanced_editor.original"),
                    button -> openOriginal(), GOLD_DIM));
            addRenderableWidget(new BlackGoldButton(rightX + 12, height - 76, halfInspector, 20,
                    Component.translatable("screen.yuushya_modelling_enhanced_editor.export_native"),
                    button -> exportNative(), GOLD_DIM));
            addRenderableWidget(new BlackGoldButton(rightX + 16 + halfInspector, height - 76,
                    fullInspectorWidth - halfInspector - 4, 20,
                    Component.translatable("screen.yuushya_modelling_enhanced_editor.import_native"),
                    button -> importNative(), GOLD_DIM));
        }
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
        addRenderableWidget(new BlackGoldToolButton(toolX, 4, ICON_WIDTH, ICON_HEIGHT,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.tool.move"),
                button -> gizmoMode = GizmoMode.MOVE, GOLD, BlackGoldToolButton.Icon.MOVE));
        toolX += ICON_WIDTH + ICON_GAP;
        addRenderableWidget(new BlackGoldToolButton(toolX, 4, ICON_WIDTH, ICON_HEIGHT,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.tool.rotate"),
                button -> gizmoMode = GizmoMode.ROTATE, GOLD, BlackGoldToolButton.Icon.ROTATE));
        toolX += ICON_WIDTH + ICON_GAP;
        addRenderableWidget(new BlackGoldToolButton(toolX, 4, ICON_WIDTH, ICON_HEIGHT,
                Component.translatable("screen.yuushya_modelling_enhanced_editor.tool.scale"),
                button -> gizmoMode = GizmoMode.SCALE, GOLD, BlackGoldToolButton.Icon.SCALE));
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
        drawViewport(graphics, mouseX, mouseY);
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
        String[] labels = {"PX", "PY", "PZ", "RX", "RY", "RZ", "SX", "SY", "SZ"};
        for (int i = 0; i < labels.length; i++) {
            int boxX = inspectorBoxX(rightX, i);
            int labelX = boxX - font.width(labels[i]) - 4;
            int color = i % 3 == 0 ? 0xFFFF6B5E : i % 3 == 1 ? 0xFF7ED957 : 0xFF61A8FF;
            graphics.text(font, labels[i], labelX, transformRowY(i) + 5, color, false);
        }
    }

    private void drawViewport(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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

        EditorViewport viewport = editorViewport();
        CameraFrame frame = currentCameraFrame();
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        submitBlockPreview(graphics, viewport, frame, selected, mouseX, mouseY);
        drawSelectedScreenMarker(graphics, viewport, frame, selected);
        drawOrientationWidget(graphics, viewport.x() + viewport.width() - ORIENTATION_WIDGET_MARGIN_RIGHT,
                viewport.y() + viewport.height() - ORIENTATION_WIDGET_MARGIN_BOTTOM);
    }

    /**
     * Keeps selection visible when a transparent or newly added block overlaps an opaque layer.
     * The world-space gold box remains the precise outline; these short screen-space corners are
     * only an occlusion-independent marker.
     */
    private void drawSelectedScreenMarker(GuiGraphicsExtractor graphics, EditorViewport viewport,
            CameraFrame frame, SceneLayer<Object> selected) {
        if (selected == null || !selected.visible() || !(selected.hostData() instanceof BlockState)) return;
        Matrix4f transform = BlockPreviewTransform.matrix(selected.transform());
        AABB bounds = BlockPreviewPipRenderer.selectionBounds((BlockState) selected.hostData());
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        int projectedCorners = 0;
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    float localX = (float) (x == 0 ? bounds.minX : bounds.maxX);
                    float localY = (float) (y == 0 ? bounds.minY : bounds.maxY);
                    float localZ = (float) (z == 0 ? bounds.minZ : bounds.maxZ);
                    Vector3f world = transform.transformPosition(new Vector3f(localX, localY, localZ));
                    ProjectedPoint point = EditorProjection.project(new Vector3d(world),
                            frame.matrices(), frame.viewport());
                    if (point.behindCamera() || !Double.isFinite(point.screenX())
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
        String projection = Component.translatable(camera.mode() == EditorCameraMode.ORTHOGRAPHIC
                ? "screen.yuushya_modelling_enhanced_editor.projection.orthographic"
                : "screen.yuushya_modelling_enhanced_editor.projection.perspective").getString();
        String selection;
        if (selected == null) {
            selection = Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.no_selection").getString();
        } else {
            Component visibility = Component.translatable(selected.visible()
                    ? "screen.yuushya_modelling_enhanced_editor.visible"
                    : "screen.yuushya_modelling_enhanced_editor.hidden");
            selection = selectedLayerIds.size() > 1
                    ? Component.translatable("screen.yuushya_modelling_enhanced_editor.multi_selection",
                            selectedLayerIds.size(), selected.name(), visibility).getString()
                    : Component.translatable("screen.yuushya_modelling_enhanced_editor.selection",
                            selected.name(), visibility).getString();
        }
        String lineWidth = Component.translatable("screen.yuushya_modelling_enhanced_editor.line_width",
                String.format(Locale.ROOT, "%.2f", LineWidthPolicy.forCamera(camera))).getString();
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

        Matrix4f view = CameraMatrices.create(camera, new EditorViewport(0, 0, 1, 1)).view();
        List<OrientationAxis> axes = new ArrayList<>(6);
        addOrientationAxis(axes, view, new Vector3f(1.0F, 0.0F, 0.0F), "X", 0xFFD34242);
        addOrientationAxis(axes, view, new Vector3f(-1.0F, 0.0F, 0.0F), "", 0xFFD34242);
        addOrientationAxis(axes, view, new Vector3f(0.0F, 1.0F, 0.0F), "Y", 0xFF3EC45B);
        addOrientationAxis(axes, view, new Vector3f(0.0F, -1.0F, 0.0F), "", 0xFF3EC45B);
        addOrientationAxis(axes, view, new Vector3f(0.0F, 0.0F, 1.0F), "Z", 0xFF3B70D4);
        addOrientationAxis(axes, view, new Vector3f(0.0F, 0.0F, -1.0F), "", 0xFF3B70D4);
        axes.sort((left, right) -> Double.compare(left.depth(), right.depth()));
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

    private void submitBlockPreview(GuiGraphicsExtractor graphics, EditorViewport viewport, CameraFrame frame,
            SceneLayer<Object> selected, double mouseX, double mouseY) {
        boolean anySelectedVisible = false;
        for (SceneLayer<Object> layer : selectedLayers()) {
            if (layer.visible()) {
                anySelectedVisible = true;
                break;
            }
        }
        GizmoSizingPolicy.Sizes gizmoSizes = selected != null && anySelectedVisible
                ? gizmoSizes(selected, frame) : null;
        GizmoHandle hoveredHandle = GizmoHandle.NONE;
        if (gizmoSizes != null && activeGizmoHandle == GizmoHandle.NONE && !draggingViewport
                && insideViewport(mouseX, mouseY)) {
            hoveredHandle = gizmoHandleAt(mouseX, mouseY, selected, frame);
        }
        SceneLayer<Object> hoveredLayer = hoveredHandle == GizmoHandle.NONE && !draggingViewport
                && insideViewport(mouseX, mouseY) ? layerAt(mouseX, mouseY, frame) : null;
        UUID hoveredLayerId = hoveredLayer == null ? null : hoveredLayer.id();
        List<BlockPreviewLayer> layers = previewLayers(hoveredLayerId);
        Vector3d pivot = selectionPivot();
        BlockPreviewGizmo gizmo = gizmoSizes == null ? null
                : new BlockPreviewGizmo(pivot, gizmoMode, activeGizmoHandle, hoveredHandle,
                        gizmoSizes.axisLength(), gizmoSizes.rotationRadius(), gizmoSizes.scaleHandleLength());
        graphics.submitPictureInPictureRenderState(new BlockPreviewPipRenderState(environmentFrame,
                layers, frame, gizmo, draft.collisionShape(), LineWidthPolicy.forCamera(camera), true,
                viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + viewport.height(),
                graphics.peekScissorStack()));
    }

    private List<BlockPreviewLayer> previewLayers(UUID hoveredLayerId) {
        if (cachedPreviewDraft == draft && cachedPreviewEnvironmentFrame == environmentFrame
                && Objects.equals(cachedPreviewHoveredLayerId, hoveredLayerId)) {
            return cachedPreviewLayers;
        }
        List<BlockPreviewLayer> layers = new ArrayList<>();
        for (SceneLayer<Object> layer : draft.layers()) {
            if (!layer.visible()) continue;
            BlockPreviewLayer.Content content = previewContent(layer.hostData());
            if (content == null) continue;
            layers.add(new BlockPreviewLayer(content, layer.transform(),
                    selectedLayerIds.contains(layer.id()), layer.id().equals(hoveredLayerId), new Vector3d()));
        }
        for (EnvironmentModeledBlock modeledBlock : environmentFrame.modeledBlocks()) {
            Vector3d offset = new Vector3d(
                    modeledBlock.position().getX() - environmentFrame.originX(),
                    modeledBlock.position().getY() - environmentFrame.originY(),
                    modeledBlock.position().getZ() - environmentFrame.originZ());
            for (SceneLayer<Object> layer : modeledBlock.layers()) {
                if (!layer.visible()) continue;
                BlockPreviewLayer.Content content = previewContent(layer.hostData());
                if (content != null) {
                    layers.add(new BlockPreviewLayer(content, layer.transform(), false, offset));
                }
            }
        }
        cachedPreviewDraft = draft;
        cachedPreviewEnvironmentFrame = environmentFrame;
        cachedPreviewHoveredLayerId = hoveredLayerId;
        cachedPreviewLayers = List.copyOf(layers);
        return cachedPreviewLayers;
    }

    static BlockPreviewLayer.Content previewContent(Object hostData) {
        if (hostData instanceof BlockState blockState) {
            return new BlockPreviewLayer.BlockContent(blockState);
        }
        if (hostData instanceof ItemModelData itemData) {
            return itemData.enableBlock() && itemData.blockState() != null
                    ? new BlockPreviewLayer.BlockContent(itemData.blockState(), false)
                    : new BlockPreviewLayer.ItemContent(itemData.itemStack());
        }
        if (hostData instanceof TextModelData textData) {
            return new BlockPreviewLayer.TextContent(YuushyaTextCodec.component(textData.textLines()),
                    textData.culled(), textData.mirror());
        }
        return null;
    }

    private GizmoSizingPolicy.Sizes gizmoSizes(SceneLayer<Object> selected, CameraFrame frame) {
        Vector3d pivot = selectionPivot();
        double radius = 0.0D;
        for (SceneLayer<Object> layer : selectedLayers()) {
            radius = Math.max(radius,
                    layerPivot(layer).distance(pivot) + layerBoundingRadius(layer));
        }
        return GizmoSizingPolicy.calculate(radius);
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
        event = normalizeControlClick(event);
        layerClickShiftDown = event.hasShiftDown();
        layerClickControlDown = event.hasControlDown();
        if (super.mouseClicked(event, false)) {
            layerClickShiftDown = false;
            layerClickControlDown = false;
            return true;
        }
        layerClickShiftDown = false;
        layerClickControlDown = false;
        if ((event.button() == 0 || event.button() == 1) && insideViewport(event.x(), event.y())) {
            if (event.button() == 0) {
                if (!commitInspector("numeric transform")) return true;
                CameraFrame frame = currentCameraFrame();
                SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
                GizmoHandle handle = selected != null && selected.visible()
                        ? gizmoHandleAt(event.x(), event.y(), selected, frame)
                        : GizmoHandle.NONE;
                if (handle != GizmoHandle.NONE && beginGizmoDrag(event.x(), event.y(), frame, handle)) {
                    gizmoPressX = event.x();
                    gizmoPressY = event.y();
                    gizmoPointerMoved = false;
                    gizmoPressShiftDown = event.hasShiftDown();
                    gizmoPressControlDown = event.hasControlDown();
                    gizmoDragShiftDown = event.hasShiftDown();
                    gizmoDragControlDown = event.hasControlDown();
                    draggingViewport = true;
                    viewportDragButton = event.button();
                    virtualDragMouseX = event.x();
                    virtualDragMouseY = event.y();
                    return true;
                }
            }
            viewportPressX = event.x();
            viewportPressY = event.y();
            viewportPointerMoved = false;
            viewportPressShiftDown = event.hasShiftDown();
            viewportPressControlDown = event.hasControlDown();
            draggingViewport = true;
            viewportDragButton = event.button();
            virtualDragMouseX = event.x();
            virtualDragMouseY = event.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        event = normalizeControlClick(event);
        if (draggingViewport && event.button() == viewportDragButton) {
            virtualDragMouseX += dragX;
            virtualDragMouseY += dragY;
            if (activeGizmoHandle != GizmoHandle.NONE) {
                if (!gizmoPointerMoved && Math.hypot(
                        virtualDragMouseX - gizmoPressX,
                        virtualDragMouseY - gizmoPressY) < POINTER_DRAG_THRESHOLD) {
                    return true;
                }
                gizmoPointerMoved = true;
                applyGizmoDrag(virtualDragMouseX, virtualDragMouseY,
                        gizmoDragShiftDown, gizmoDragControlDown);
                wrapViewportDragCursor(event.x(), event.y());
                return true;
            }
            double cameraDragX = dragX;
            double cameraDragY = dragY;
            if (viewportDragButton == 0 && !viewportPointerMoved) {
                double totalDragX = virtualDragMouseX - viewportPressX;
                double totalDragY = virtualDragMouseY - viewportPressY;
                if (Math.hypot(totalDragX, totalDragY) < POINTER_DRAG_THRESHOLD) return true;
                viewportPointerMoved = true;
                cameraDragX = totalDragX;
                cameraDragY = totalDragY;
            }
            if (viewportDragButton == 1) {
                camera = cameraController.panPixels(camera, dragX, dragY, editorViewport());
            } else {
                camera = cameraController.orbit(camera, Math.toRadians(cameraDragX * 0.35D),
                        Math.toRadians(-cameraDragY * 0.30D), WORLD_UP);
            }
            wrapViewportDragCursor(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private void wrapViewportDragCursor(double physicalMouseX, double physicalMouseY) {
        java.util.OptionalDouble targetX = CursorWrapPolicy.horizontalTarget(
                physicalMouseX, width, CURSOR_WRAP_MARGIN);
        java.util.OptionalDouble targetY = CursorWrapPolicy.verticalTarget(
                physicalMouseY, height, CURSOR_WRAP_MARGIN);
        if (targetX.isEmpty() && targetY.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        double windowX = targetX.isPresent()
                ? targetX.orElseThrow() * window.getScreenWidth() / window.getGuiScaledWidth()
                : minecraft.mouseHandler.xpos();
        double windowY = targetY.isPresent()
                ? targetY.orElseThrow() * window.getScreenHeight() / window.getGuiScaledHeight()
                : minecraft.mouseHandler.ypos();
        minecraft.mouseHandler.setIgnoreFirstMove();
        GLFW.glfwSetCursorPos(window.handle(), windowX, windowY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        event = normalizeControlClick(event);
        if (draggingViewport && event.button() == viewportDragButton) {
            boolean gizmoGesture = activeGizmoHandle != GizmoHandle.NONE;
            UUID clickedGizmoLayer = gizmoDragSession == null ? null : gizmoDragSession.layer().id();
            boolean gizmoSelectionClick = gizmoGesture && !gizmoPointerMoved
                    && (gizmoPressShiftDown || gizmoPressControlDown);
            boolean plainSelectionClick = !gizmoGesture && viewportDragButton == 0 && !viewportPointerMoved;
            boolean gizmoShiftDown = gizmoPressShiftDown;
            boolean gizmoControlDown = gizmoPressControlDown;
            boolean viewportShiftDown = viewportPressShiftDown;
            boolean viewportControlDown = viewportPressControlDown;
            if (gizmoGesture) finishGizmoDrag();
            draggingViewport = false;
            viewportDragButton = -1;
            viewportPointerMoved = false;
            viewportPressShiftDown = false;
            viewportPressControlDown = false;
            if (gizmoSelectionClick && clickedGizmoLayer != null) {
                applySelectionClick(clickedGizmoLayer, gizmoShiftDown, gizmoControlDown);
                ensureSelectedLayerVisible();
                rebuildWidgets();
            } else if (plainSelectionClick) {
                selectLayerAt(event.x(), event.y(), viewportShiftDown, viewportControlDown);
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    /** macOS can report Control + primary click as a secondary mouse button. */
    private static MouseButtonEvent normalizeControlClick(MouseButtonEvent event) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_RIGHT || !event.hasControlDown()) return event;
        return new MouseButtonEvent(event.x(), event.y(),
                new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, event.modifiers()));
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
        updateGizmoDragModifier(event.key(), true);
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
            if (event.hasControlDown() && event.key() == GLFW.GLFW_KEY_C) {
                copySelectedLayer();
                return true;
            }
            if (event.hasControlDown() && event.key() == GLFW.GLFW_KEY_V) {
                pasteLayer();
                return true;
            }
            if (event.hasControlDown() && event.key() == GLFW.GLFW_KEY_A) {
                selectAllLayers();
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
                camera = cameraController.switchProjection(camera, EditorCameraMode.ORTHOGRAPHIC);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_P) {
                camera = cameraController.switchProjection(camera, EditorCameraMode.ORBIT);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        updateGizmoDragModifier(event.key(), false);
        return super.keyReleased(event);
    }

    private void updateGizmoDragModifier(int key, boolean pressed) {
        if (activeGizmoHandle == GizmoHandle.NONE) return;
        if (key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            gizmoDragShiftDown = pressed;
        } else if (key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            gizmoDragControlDown = pressed;
        }
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
        applySelectionClick(id, layerClickShiftDown, layerClickControlDown);
        ensureSelectedLayerVisible();
        rebuildWidgets();
    }

    private void applySelectionClick(UUID id, boolean shiftDown, boolean controlDown) {
        List<UUID> orderedIds = new ArrayList<>(draft.layers().size());
        for (SceneLayer<Object> layer : draft.layers()) orderedIds.add(layer.id());
        MultiSelectionPolicy.Result selection = MultiSelectionPolicy.click(orderedIds, selectedLayerIds,
                draft.selectedLayerId(), id, shiftDown, controlDown);
        selectedLayerIds.clear();
        selectedLayerIds.addAll(selection.selectedElementIds());
        draft = draft.select(selection.primaryElementId());
        cachedPreviewDraft = null;
    }

    private void clearSelection() {
        if (selectedLayerIds.isEmpty() && draft.selectedLayerId() == null) return;
        selectedLayerIds.clear();
        draft = draft.select(null);
        cachedPreviewDraft = null;
    }

    private void selectAllLayers() {
        if (!commitInspector("numeric transform")) return;
        selectedLayerIds.clear();
        for (SceneLayer<Object> layer : draft.layers()) selectedLayerIds.add(layer.id());
        if (draft.selectedLayerId() == null && !draft.layers().isEmpty()) {
            draft = draft.select(draft.layers().getFirst().id());
        }
        cachedPreviewDraft = null;
        rebuildWidgets();
    }

    private int layerIndex(UUID id) {
        for (int index = 0; index < draft.layers().size(); index++) {
            if (draft.layers().get(index).id().equals(id)) return index;
        }
        return -1;
    }

    private List<SceneLayer<Object>> selectedLayers() {
        List<SceneLayer<Object>> result = new ArrayList<>();
        for (SceneLayer<Object> layer : draft.layers()) {
            if (selectedLayerIds.contains(layer.id())) result.add(layer);
        }
        return List.copyOf(result);
    }

    private void selectOnly(UUID id) {
        selectedLayerIds.clear();
        if (id != null) selectedLayerIds.add(id);
    }

    private void reconcileSelection() {
        Set<UUID> available = new java.util.HashSet<>();
        for (SceneLayer<Object> layer : draft.layers()) available.add(layer.id());
        selectedLayerIds.retainAll(available);
        if (draft.selectedLayerId() != null) selectedLayerIds.add(draft.selectedLayerId());
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

    private void openInventoryPicker() {
        if (!supportsInventoryPicker() || !commitInspector("numeric transform")) return;
        minecraft.setScreen(new InventoryItemPickerScreen(this, host.editorType(),
                this::addInventoryItem));
    }

    private void addInventoryItem(ItemStack stack) {
        switch (host.editorType()) {
            case BLOCK -> {
                BlockState blockState = YuushyaItemModelSupport.resolveBlockState(stack);
                if (blockState != null) addBlock(blockState);
            }
            case ITEM -> addItem(stack);
            case TEXT -> { }
        }
    }

    private boolean supportsInventoryPicker() {
        return host.editorType() != EditorType.TEXT;
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
        selectOnly(draft.selectedLayerId());
        ensureSelectedLayerVisible();
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.added",
                blockId), false);
    }

    private void addItem(ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack");
        String itemName = itemStack.getHoverName().getString();
        appendLayer(YuushyaItemModelSupport.create(itemStack), itemName, "add item " + itemName);
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
        selectOnly(draft.selectedLayerId());
        ensureSelectedLayerVisible();
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.added", name), false);
    }

    private void editSelectedContent() {
        if (!commitInspector("numeric transform")) return;
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected == null) return;
        if (selected.hostData() instanceof BlockState blockState) {
            minecraft.setScreen(new BlockStateEditorScreen(this, blockState,
                    value -> replaceSelectedContent(value, "edit block state")));
        } else if (selected.hostData() instanceof ItemModelData itemData) {
            minecraft.setScreen(new ItemContentEditorScreen(this, itemData,
                    value -> replaceSelectedContent(value, "edit item")));
        } else if (selected.hostData() instanceof TextModelData textData) {
            minecraft.setScreen(TextContentEditorScreen.forExisting(this, textData,
                    value -> replaceSelectedContent(value, "edit text")));
        }
    }

    private void replaceSelectedContent(Object hostData, String description) {
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected == null || selected.hostData().equals(hostData)) return;
        String name = selected.name();
        if (hostData instanceof BlockState blockState) {
            name = (layerIndex(selected.id()) + 1) + "  "
                    + BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        } else if (hostData instanceof ItemModelData itemData) {
            name = (layerIndex(selected.id()) + 1) + "  " + itemData.itemStack().getHoverName().getString();
        }
        SceneLayer<Object> replacement = new SceneLayer<>(selected.id(), name, hostData,
                selected.transform(), selected.visible());
        SceneDocument<Object> before = draft;
        SceneDocument<Object> after = draft.replace(replacement);
        draft = history.execute(before, new SnapshotCommand(before, after, description));
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.content_updated"), false);
    }

    private void removeSelectedLayer() {
        if (!commitInspector("numeric transform")) return;
        List<SceneLayer<Object>> selected = selectedLayers();
        if (selected.isEmpty()) return;
        SceneDocument<Object> before = draft;
        SceneDocument<Object> after = draft;
        for (SceneLayer<Object> layer : selected) after = after.remove(layer.id());
        draft = history.execute(before, new SnapshotCommand(before, after, "remove layers"));
        selectOnly(draft.selectedLayerId());
        int maxScroll = Math.max(0, draft.layers().size() - visibleLayerRows());
        layerScroll = Math.min(layerScroll, maxScroll);
        ensureSelectedLayerVisible();
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.layers_deleted",
                selected.size()), false);
        rebuildWidgets();
    }

    private void copySelectedLayer() {
        List<SceneLayer<Object>> selected = selectedLayers();
        if (selected.isEmpty()) return;
        layerClipboard.copy(selected);
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.layers_copied",
                selected.size()), false);
    }

    private void pasteLayer() {
        if (!layerClipboard.hasValue()) {
            setStatus(Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.error.layer_clipboard_empty"), true);
            return;
        }
        SceneDocument<Object> before = draft;
        List<SceneLayer<Object>> pastedLayers = layerClipboard.pasteAll();
        SceneDocument<Object> after = before;
        for (SceneLayer<Object> pasted : pastedLayers) {
            String name = Component.translatable("screen.yuushya_modelling_enhanced_editor.layer_copy_name",
                    pasted.name()).getString();
            after = after.append(new SceneLayer<>(pasted.id(), name, pasted.hostData(),
                    pasted.transform(), pasted.visible()));
        }
        YuushyaEditorHost.ValidationResult validation = host.validate(after);
        if (!validation.valid()) {
            setStatus(Component.translatable(validation.message(), validation.arguments()), true);
            return;
        }
        draft = history.execute(before, new SnapshotCommand(before, after, "paste layer"));
        selectedLayerIds.clear();
        for (SceneLayer<Object> pasted : pastedLayers) selectedLayerIds.add(pasted.id());
        ensureSelectedLayerVisible();
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.layers_pasted",
                pastedLayers.size()), false);
        rebuildWidgets();
    }

    private void organizeLayersSpatially() {
        if (!commitInspector("numeric transform") || draft.layers().size() < 2) return;
        List<SceneLayer<Object>> ordered = SpatialOrdering.nearestNeighbor(draft.layers(),
                YuushyaEditorScreen::layerPivot);
        List<SceneLayer<Object>> renumbered = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            SceneLayer<Object> layer = ordered.get(index);
            renumbered.add(new SceneLayer<>(layer.id(), (index + 1) + "  " + withoutLayerOrdinal(layer.name()),
                    layer.hostData(), layer.transform(), layer.visible()));
        }
        SceneDocument<Object> before = draft;
        SceneDocument<Object> after = new SceneDocument<>(renumbered,
                draft.selectedLayerId(), draft.collisionShape());
        if (after.equals(before)) {
            setStatus(Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.status.layers_already_organized"), false);
            return;
        }
        draft = history.execute(before, new SnapshotCommand(before, after, "organize layers spatially"));
        ensureSelectedLayerVisible();
        setStatus(Component.translatable(
                "screen.yuushya_modelling_enhanced_editor.status.layers_organized", ordered.size()), false);
        rebuildWidgets();
    }

    private static String withoutLayerOrdinal(String name) {
        int separator = name.indexOf("  ");
        if (separator <= 0) return name;
        for (int index = 0; index < separator; index++) {
            if (!Character.isDigit(name.charAt(index))) return name;
        }
        String value = name.substring(separator + 2);
        return value.isBlank() ? name : value;
    }

    private void exportNative() {
        if (!commitInspector("numeric transform")) return;
        try {
            String serialized = host.exportDocument(draft);
            TextFieldHelper.setClipboardContents(minecraft, serialized);
            setStatus(Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.status.native_exported",
                    draft.layers().size()), false);
        } catch (RuntimeException exception) {
            setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.error.native_export",
                    errorMessage(exception)), true);
        }
    }

    private void importNative() {
        if (!commitInspector("numeric transform")) return;
        try {
            SceneDocument<Object> imported = host.importDocument(
                    TextFieldHelper.getClipboardContents(minecraft), draft);
            YuushyaEditorHost.ValidationResult validation = host.validate(imported);
            if (!validation.valid()) {
                setStatus(Component.translatable(validation.message(), validation.arguments()), true);
                return;
            }
            SceneDocument<Object> before = draft;
            draft = history.execute(before, new SnapshotCommand(before, imported, "import Yuushya model"));
            selectOnly(draft.selectedLayerId());
            layerScroll = 0;
            ensureSelectedLayerVisible();
            syncInspector();
            setStatus(Component.translatable(
                    "screen.yuushya_modelling_enhanced_editor.status.native_imported",
                    draft.layers().size()), false);
        } catch (RuntimeException exception) {
            setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.error.native_import",
                    errorMessage(exception)), true);
        }
    }

    private static String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private void selectLayerAt(double mouseX, double mouseY, boolean shiftDown, boolean controlDown) {
        SceneLayer<Object> best = layerAt(mouseX, mouseY, currentCameraFrame());
        if (best != null) {
            applySelectionClick(best.id(), shiftDown, controlDown);
            ensureSelectedLayerVisible();
        } else {
            clearSelection();
        }
        rebuildWidgets();
    }

    private SceneLayer<Object> layerAt(double mouseX, double mouseY, CameraFrame frame) {
        PickingRay ray = EditorProjection.rayFromScreen(mouseX, mouseY,
                frame.matrices(), frame.viewport());
        SceneLayer<Object> best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (SceneLayer<Object> layer : draft.layers()) {
            if (!layer.visible()) continue;
            BlockPreviewLayer.Content content = previewContent(layer.hostData());
            if (content == null) continue;
            Matrix4f matrix = BlockPreviewPipRenderer.previewMatrix(content, layer.transform());
            AABB bounds = BlockPreviewPipRenderer.interactionBounds(content);
            double distance = BlockPreviewPicking.hitDistance(ray, matrix,
                    bounds.minX, bounds.minY, bounds.minZ,
                    bounds.maxX, bounds.maxY, bounds.maxZ).orElse(Double.POSITIVE_INFINITY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = layer;
            }
        }
        return best;
    }

    private boolean commitInspector(String description) {
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected == null) return true;
        boolean edited = false;
        for (int index = 0; index < transformBoxes.length; index++) {
            if (transformBoxes[index] != null
                    && !Objects.equals(transformBoxes[index].getValue(), syncedTransformValues[index])) {
                edited = true;
                break;
            }
        }
        if (!edited) return true;
        try {
            EditorTransform current = selected.transform();
            Vector3d currentPosition = current.position();
            double px = fieldEdited(0) ? finiteDouble(0) : currentPosition.x;
            double py = fieldEdited(1) ? finiteDouble(1) : currentPosition.y;
            double pz = fieldEdited(2) ? finiteDouble(2) : currentPosition.z;
            // Yuushya persists Euler angles. Rebuild from the displayed Euler triplet whenever
            // any inspector field is committed so an unrelated position/scale edit round-trips
            // the host values instead of serializing quaternion decomposition drift.
            Quaternionf enteredRotation = YuushyaTransformConversion.fromEulerDegrees(new Vector3f(
                    finiteFloat(3), finiteFloat(4), finiteFloat(5)));
            Vector3f currentScale = current.scale();
            Vector3f requestedScale = new Vector3f(
                    fieldEdited(6) ? finiteFloat(6) : currentScale.x,
                    fieldEdited(7) ? finiteFloat(7) : currentScale.y,
                    fieldEdited(8) ? finiteFloat(8) : currentScale.z);
            EditorTransform transform = YuushyaScalePolicy.apply(selected.transform(),
                    new Vector3d(px, py, pz),
                    enteredRotation, requestedScale);
            if (!transform.equals(selected.transform())) {
                SceneDocument<Object> before = draft;
                SceneDocument<Object> after = before;
                Vector3d groupPivot = selectionPivot(before, selectedLayerIds);
                if (!transform.scale().equals(selected.transform().scale())) {
                    Vector3f previousScale = selected.transform().scale();
                    Vector3f nextScale = transform.scale();
                    Vector3d factors = new Vector3d(nextScale.x / (double) previousScale.x,
                            nextScale.y / (double) previousScale.y,
                            nextScale.z / (double) previousScale.z);
                    after = scaleSelectionAroundModelCenters(after, selectedLayerIds, groupPivot, factors);
                }
                if (!transform.rotation().equals(selected.transform().rotation())) {
                    Quaternionf inverse = selected.transform().rotation().invert(new Quaternionf());
                    Quaternionf delta = transform.rotation().mul(inverse, new Quaternionf()).normalize();
                    after = rotateSelectionAroundModelCenters(after, selectedLayerIds, groupPivot, delta);
                }
                SceneLayer<Object> provisionalPrimary = after.selectedLayer().orElseThrow();
                Vector3d translation = BlockPreviewTransform.pivot(transform)
                        .sub(BlockPreviewTransform.pivot(provisionalPrimary.transform()));
                if (translation.lengthSquared() > 0.0D) {
                    after = SelectionTransforms.translate(after, selectedLayerIds, translation);
                }
                // Preserve Yuushya's exact primary-layer scale semantics, including legacy data.
                after = after.replace(selected.withTransform(transform));
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

    private boolean fieldEdited(int index) {
        return !Objects.equals(transformBoxes[index].getValue(), syncedTransformValues[index]);
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
        boolean visible = !selected.visible();
        List<SceneLayer<Object>> layers = new ArrayList<>(draft.layers().size());
        for (SceneLayer<Object> layer : draft.layers()) {
            layers.add(selectedLayerIds.contains(layer.id()) ? layer.withVisible(visible) : layer);
        }
        SceneDocument<Object> after = new SceneDocument<>(layers, draft.selectedLayerId(), draft.collisionShape());
        draft = history.execute(before, new SnapshotCommand(before, after, "visibility"));
        syncInspector();
        rebuildWidgets();
    }

    private void cycleCollisionShape() {
        if (!commitInspector("numeric transform")) return;
        CollisionShape.Kind next = draft.collisionShape().kind().nextEditablePreset();
        if (host.editorType() != EditorType.BLOCK && next == CollisionShape.Kind.CUSTOM) {
            next = CollisionShape.Kind.NONE;
        }
        SceneDocument<Object> before = draft;
        SceneDocument<Object> after = draft.withCollisionShape(CollisionShape.forKind(next));
        draft = history.execute(before, new SnapshotCommand(before, after, "collision shape"));
        syncInspector();
        setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.status.collision",
                Component.translatable(next.translationKey())), false);
    }

    private void openCollisionEditor() {
        if (host.editorType() != EditorType.BLOCK
                || draft.collisionShape().kind() != CollisionShape.Kind.CUSTOM) return;
        if (!commitInspector("numeric transform")) return;
        minecraft.setScreen(new CollisionShapeEditorScreen(this, draft.layers(),
                draft.collisionShape(), shapeToolInventorySlot, host.worldOrigin().orElseThrow(),
                (shape, toolSlot) -> {
                    shapeToolInventorySlot = toolSlot;
                    SceneDocument<Object> before = draft;
                    SceneDocument<Object> after = draft.withCollisionShape(shape);
                    if (!after.equals(before)) {
                        draft = history.execute(before,
                                new SnapshotCommand(before, after, "custom collision shape"));
                    }
                    syncInspector();
                    setStatus(Component.translatable(
                            "screen.yuushya_modelling_enhanced_editor.status.custom_collision",
                            shape.boxes().size()), false);
                }));
    }

    private void undo() {
        draft = history.undo(draft);
        refreshAfterHistoryNavigation();
    }

    private void redo() {
        draft = history.redo(draft);
        refreshAfterHistoryNavigation();
    }

    private void refreshAfterHistoryNavigation() {
        reconcileSelection();
        int maxScroll = Math.max(0, draft.layers().size() - visibleLayerRows());
        layerScroll = Math.clamp(layerScroll, 0, maxScroll);
        ensureSelectedLayerVisible();
        rebuildWidgets();
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
            boolean customShapeChanged = draft.collisionShape().kind() == CollisionShape.Kind.CUSTOM
                    && !draft.collisionShape().equals(original.collisionShape());
            if (customShapeChanged) {
                if (host.editorType() != EditorType.BLOCK || host.worldOrigin().isEmpty()) {
                    throw new IllegalStateException(Component.translatable(
                            "screen.yuushya_modelling_enhanced_editor.error.custom_collision_target").getString());
                }
                if (!validSelectedShapeTool()) {
                    throw new IllegalStateException(Component.translatable(
                            "screen.yuushya_modelling_enhanced_editor.error.shape_tool_required").getString());
                }
                YuushyaShapeToolBridge.apply(shapeToolInventorySlot,
                        draft.collisionShape(), host.worldOrigin().orElseThrow());
            }
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
            retainHistorySession();
            environmentPreview.close();
            minecraft.setScreen(null);
        });
    }

    @Override
    public void onClose() {
        applyAndClose();
    }

    private void discardAndClose() {
        historySessionKey.ifPresent(EditorHistorySessions::discard);
        environmentPreview.close();
        minecraft.setScreen(null);
    }

    private void openOriginal() {
        requestSave(() -> {
            retainHistorySession();
            environmentPreview.close();
            YuushyaScreenInterceptor.openOriginal(originalScreen);
        });
    }

    private void retainHistorySession() {
        historySessionKey.ifPresent(key -> EditorHistorySessions.put(key, draft, history));
    }

    private List<ZFightDetector.Conflict> detectZFightConflicts() {
        List<ZFightDetector.Candidate> candidates = new ArrayList<>();
        for (SceneLayer<Object> layer : draft.layers()) {
            if (!layer.visible()) continue;
            BlockState blockState = null;
            boolean centerOnPivot = true;
            if (layer.hostData() instanceof BlockState state) {
                blockState = state;
            } else if (layer.hostData() instanceof ItemModelData itemData
                    && itemData.enableBlock() && itemData.blockState() != null) {
                blockState = itemData.blockState();
                centerOnPivot = false;
            }
            if (blockState != null) {
                List<ZFightDetector.Box> boxes = zFightBoxes(blockState);
                if (!boxes.isEmpty()) {
                    candidates.add(new ZFightDetector.Candidate(layer.id(), layer.transform(), boxes,
                            centerOnPivot));
                }
            }
        }
        return ZFightDetector.detect(candidates);
    }

    private static List<ZFightDetector.Box> zFightBoxes(BlockState blockState) {
        try {
            List<ZFightDetector.Box> result = new ArrayList<>();
            for (AABB box : blockState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs()) {
                if (box.getXsize() > 0.0D && box.getYsize() > 0.0D && box.getZsize() > 0.0D) {
                    result.add(new ZFightDetector.Box(box.minX, box.minY, box.minZ,
                            box.maxX, box.maxY, box.maxZ));
                }
            }
            return List.copyOf(result);
        } catch (RuntimeException ignored) {
            // A context-dependent/custom block that cannot expose a stable editor shape is safer
            // to skip than to mutate on the false assumption that it is a full cube.
            return List.of();
        }
    }

    private void applyZFightOptimization(List<ZFightDetector.Conflict> conflicts) {
        List<UUID> layerIds = new ArrayList<>(draft.layers().size());
        for (SceneLayer<Object> layer : draft.layers()) layerIds.add(layer.id());
        Map<UUID, Vector3d> offsets = ZFightOptimizer.worldOffsets(layerIds, conflicts,
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
        int controlsHeight = supportsInventoryPicker() ? 134 : 110;
        return Math.max(1, (height - layerRowY() - controlsHeight) / layerRowStep());
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
        int cellWidth = (rightWidth - 24) / 3;
        return rightX + 12 + index % 3 * cellWidth + 16;
    }

    private int transformRowY(int index) {
        return compactLayout() ? 54 + index / 3 * 20 : 112 + index / 2 * 22;
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
        if (collisionEditButton != null) {
            collisionEditButton.visible = host.editorType() == EditorType.BLOCK
                    && draft.collisionShape().kind() == CollisionShape.Kind.CUSTOM;
            collisionEditButton.active = collisionEditButton.visible;
        }
        if (contentButton != null) {
            contentButton.active = active && selected != null
                    && (selected.hostData() instanceof BlockState
                            || selected.hostData() instanceof ItemModelData
                            || selected.hostData() instanceof TextModelData);
            String key = selected != null && selected.hostData() instanceof BlockState
                    ? "screen.yuushya_modelling_enhanced_editor.edit_block_state"
                    : host.editorType() == com.zhongbai233.yuushya_editor.core.EditorType.ITEM
                            ? "screen.yuushya_modelling_enhanced_editor.replace_item"
                            : "screen.yuushya_modelling_enhanced_editor.edit_text";
            contentButton.setMessage(Component.translatable(key));
        }
        if (selected == null) {
            if (visibilityButton != null) visibilityButton.active = false;
            return;
        }
        EditorTransform transform = selected.transform();
        Vector3d position = transform.position();
        Vector3f rotation = YuushyaTransformConversion.toEulerDegrees(transform.rotation());
        Vector3f scale = transform.scale();
        double[] values = {position.x, position.y, position.z, rotation.x, rotation.y, rotation.z};
        for (int i = 0; i < values.length; i++) {
            syncedTransformValues[i] = format(values[i]);
            transformBoxes[i].setValue(syncedTransformValues[i]);
        }
        // Float.toString round-trips exactly, so committing another field preserves every axis.
        float[] scaleValues = {scale.x, scale.y, scale.z};
        for (int i = 0; i < scaleValues.length; i++) {
            int index = 6 + i;
            syncedTransformValues[index] = Float.toString(scaleValues[i]);
            transformBoxes[index].setValue(syncedTransformValues[index]);
        }
        visibilityButton.active = true;
        visibilityButton.setMessage(Component.translatable(selected.visible()
                ? "screen.yuushya_modelling_enhanced_editor.visible"
                : "screen.yuushya_modelling_enhanced_editor.hidden"));
    }

    private void focusSelected() {
        SceneLayer<Object> selected = draft.selectedLayer().orElse(null);
        if (selected == null) return;
        Vector3d pivot = selectionPivot();
        double radius = selectedLayers().stream().mapToDouble(layer ->
                layerPivot(layer).distance(pivot) + layerBoundingRadius(layer)).max().orElse(1.0D);
        camera = cameraController.focus(camera, pivot, Math.max(1.0D, radius * 2.0D),
                editorViewport(), WORLD_UP);
    }

    private EditorViewport editorViewport() {
        return new EditorViewport(previewX() + 1, 26,
                Math.max(32, previewWidth() - 2), Math.max(32, height - 54));
    }

    private int previewX() {
        return leftPanelWidth() + PANEL_GAP;
    }

    private int previewWidth() {
        return Math.max(1, rightPanelX() - PANEL_GAP - previewX());
    }

    private CameraFrame currentCameraFrame() {
        EditorViewport viewport = editorViewport();
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
        Vector3d origin = selectionPivot();
        PickingRay ray = EditorProjection.rayFromScreen(mouseX, mouseY, frame.matrices(), frame.viewport());
        Vector3d axis = handle.axis();
        Vector3d startHit;
        double centerScreenX = 0.0D;
        double centerScreenY = 0.0D;
        double scaleScreenAxisX = 0.0D;
        double scaleScreenAxisY = 0.0D;
        double startScaleCoordinate = 0.0D;
        double rotationRadius = GizmoSizingPolicy.MIN_ROTATION_RADIUS;
        if (gizmoMode == GizmoMode.MOVE) {
            startHit = GizmoDragMath.intersectConstraint(ray, origin, axis, handle.constraint()).orElse(null);
            if (startHit == null) return false;
        } else if (gizmoMode == GizmoMode.ROTATE) {
            startHit = GizmoDragMath.intersectConstraint(ray, origin, axis, handle.rotationConstraint())
                    .orElse(null);
            if (startHit == null) return false;
            rotationRadius = gizmoSizes(selected, frame).rotationRadius();
        } else {
            float initialScale = scaleComponent(selected.transform().scale(), handle);
            if (!Float.isFinite(initialScale) || initialScale <= 0.0F) {
                setStatus(Component.translatable("screen.yuushya_modelling_enhanced_editor.error.scale_positive"), true);
                return false;
            }
            ProjectedPoint projected = EditorProjection.project(origin, frame.matrices(), frame.viewport());
            ProjectedPoint projectedAxis = EditorProjection.project(new Vector3d(origin).add(axis),
                    frame.matrices(), frame.viewport());
            if (!projected.visible() || !projectedAxis.visible()) return false;
            centerScreenX = projected.screenX();
            centerScreenY = projected.screenY();
            scaleScreenAxisX = projectedAxis.screenX() - centerScreenX;
            scaleScreenAxisY = projectedAxis.screenY() - centerScreenY;
            double screenAxisLength = Math.hypot(scaleScreenAxisX, scaleScreenAxisY);
            if (screenAxisLength <= 1.0E-9D) return false;
            scaleScreenAxisX /= screenAxisLength;
            scaleScreenAxisY /= screenAxisLength;
            startScaleCoordinate = (mouseX - centerScreenX) * scaleScreenAxisX
                    + (mouseY - centerScreenY) * scaleScreenAxisY;
            if (Math.abs(startScaleCoordinate) <= 1.0E-9D) return false;
            startHit = new Vector3d(origin);
        }
        activeGizmoHandle = handle;
        gizmoDragSession = new GizmoDragSession(selected, List.copyOf(selectedLayerIds), frame,
                gizmoMode, origin, axis, startHit,
                centerScreenX, centerScreenY, scaleScreenAxisX, scaleScreenAxisY, startScaleCoordinate,
                rotationRadius);
        gizmoTransaction = new DragTransaction<>(draft, gizmoMode.label().toLowerCase(Locale.ROOT)
                + " " + handle.name());
        return true;
    }

    private void applyGizmoDrag(double mouseX, double mouseY, boolean shiftDown, boolean controlDown) {
        GizmoDragSession session = gizmoDragSession;
        DragTransaction<SceneDocument<Object>> transaction = gizmoTransaction;
        if (session == null || transaction == null) return;
        EditorTransform starting = session.layer().transform();
        SceneDocument<Object> changedDocument;
        if (session.mode() == GizmoMode.SCALE) {
            double coordinate = (mouseX - session.centerScreenX()) * session.scaleScreenAxisX()
                    + (mouseY - session.centerScreenY()) * session.scaleScreenAxisY();
            double factor = coordinate / session.startScaleCoordinate();
            double initial = scaleComponent(starting.scale(), activeGizmoHandle);
            double requested = initial * factor;
            double scaleDelta = GizmoSnapPolicy.snapDelta(requested - initial,
                    GizmoSnapPolicy.step(GizmoMode.SCALE, shiftDown, controlDown));
            requested = initial + scaleDelta;
            if (!Double.isFinite(requested) || requested <= 0.0D || requested > Float.MAX_VALUE) return;
            double groupFactor = requested / initial;
            Vector3d factors = new Vector3d(1.0D);
            setScaleComponent(factors, activeGizmoHandle, groupFactor);
            changedDocument = scaleSelectionAroundModelCenters(transaction.before(),
                    session.selectedLayerIds(), session.origin(), factors);
        } else {
            PickingRay ray = EditorProjection.rayFromScreen(mouseX, mouseY, session.frame().matrices(),
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
                Vector3d delta = new Vector3d(session.axis()).mul(snappedWorldDelta);
                changedDocument = SelectionTransforms.translate(transaction.before(),
                        session.selectedLayerIds(), delta);
            } else {
                float degrees = GizmoDragMath.rotationDeltaDegrees(session.origin(), session.axis(),
                        session.startHit(), currentHit);
                degrees = GizmoDragMath.scaleIndependentRotationDegrees(degrees, session.rotationRadius());
                degrees = (float) GizmoSnapPolicy.snapDelta(degrees,
                        GizmoSnapPolicy.step(GizmoMode.ROTATE, shiftDown, controlDown));
                Quaternionf delta = new Quaternionf().rotationAxis((float) Math.toRadians(degrees),
                        (float) session.axis().x, (float) session.axis().y, (float) session.axis().z);
                changedDocument = rotateSelectionAroundModelCenters(transaction.before(),
                        session.selectedLayerIds(), session.origin(), delta);
            }
        }
        draft = transaction.update(ignored -> changedDocument);
        syncInspector();
    }

    private GizmoHandle gizmoHandleAt(double mouseX, double mouseY,
            SceneLayer<Object> selected, CameraFrame frame) {
        Vector3d origin = selectionPivot();
        GizmoSizingPolicy.Sizes sizes = gizmoSizes(selected, frame);
        return switch (gizmoMode) {
            case MOVE -> ViewportGizmoHitTesting.axisHandleAt(mouseX, mouseY, origin,
                    frame.matrices(), frame.viewport(), sizes.axisLength(), GIZMO_HIT_RADIUS);
            case ROTATE -> ViewportGizmoHitTesting.rotationHandleAt(mouseX, mouseY, origin,
                    frame.matrices(), frame.viewport(), sizes.rotationRadius(), GIZMO_HIT_RADIUS);
            // Scale axes use the same visible bidirectional segments as move axes. Reusing the
            // segment hit test makes both the shaft and its end marker draggable while preserving
            // the center dead zone that prevents ambiguous axis selection.
            case SCALE -> ViewportGizmoHitTesting.axisHandleAt(mouseX, mouseY, origin,
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

    private void finishGizmoDrag() {
        if (gizmoTransaction != null) draft = gizmoTransaction.commit(history);
        activeGizmoHandle = GizmoHandle.NONE;
        gizmoDragSession = null;
        gizmoTransaction = null;
        gizmoPointerMoved = false;
        gizmoPressShiftDown = false;
        gizmoPressControlDown = false;
        gizmoDragShiftDown = false;
        gizmoDragControlDown = false;
        syncInspector();
    }

    private boolean insideViewport(double x, double y) {
        EditorViewport viewport = editorViewport();
        return x >= viewport.x() && x < viewport.x() + viewport.width()
                && y >= viewport.y() && y < viewport.y() + viewport.height();
    }

    private static Vector3d layerPivot(SceneLayer<Object> layer) {
        BlockPreviewLayer.Content content = previewContent(layer.hostData());
        if (content == null) return BlockPreviewTransform.pivot(layer.transform());
        AABB bounds = BlockPreviewPipRenderer.interactionBounds(content);
        Vector3f localCenter = new Vector3f((float) ((bounds.minX + bounds.maxX) * 0.5D),
                (float) ((bounds.minY + bounds.maxY) * 0.5D),
                (float) ((bounds.minZ + bounds.maxZ) * 0.5D));
        Matrix4f matrix = BlockPreviewPipRenderer.previewMatrix(content, layer.transform());
        return new Vector3d(matrix.transformPosition(localCenter));
    }

    private static double layerBoundingRadius(SceneLayer<Object> layer) {
        BlockPreviewLayer.Content content = previewContent(layer.hostData());
        if (content == null) return GizmoSizingPolicy.worldBoundingRadius(layer.transform().scale());
        AABB bounds = BlockPreviewPipRenderer.interactionBounds(content);
        Matrix4f matrix = BlockPreviewPipRenderer.previewMatrix(content, layer.transform());
        Vector3d center = layerPivot(layer);
        double radius = 0.0D;
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    Vector3f corner = matrix.transformPosition(new Vector3f(
                            (float) (x == 0 ? bounds.minX : bounds.maxX),
                            (float) (y == 0 ? bounds.minY : bounds.maxY),
                            (float) (z == 0 ? bounds.minZ : bounds.maxZ)));
                    radius = Math.max(radius, new Vector3d(corner).distance(center));
                }
            }
        }
        return radius;
    }

    private static Vector3d selectionPivot(SceneDocument<Object> document, Collection<UUID> selection) {
        Set<UUID> ids = Set.copyOf(selection);
        Vector3d result = new Vector3d();
        int count = 0;
        for (SceneLayer<Object> layer : document.layers()) {
            if (!ids.contains(layer.id())) continue;
            result.add(layerPivot(layer));
            count++;
        }
        return count == 0 ? result : result.div(count);
    }

    private static SceneDocument<Object> rotateSelectionAroundModelCenters(SceneDocument<Object> before,
            Collection<UUID> selection, Vector3d groupPivot, Quaternionf delta) {
        SceneDocument<Object> after = SelectionTransforms.rotate(before, selection, groupPivot, delta);
        return correctModelCenters(before, after, selection, oldCenter -> {
            Vector3d target = oldCenter.sub(groupPivot);
            delta.transform(target);
            return target.add(groupPivot);
        });
    }

    private static SceneDocument<Object> scaleSelectionAroundModelCenters(SceneDocument<Object> before,
            Collection<UUID> selection, Vector3d groupPivot, Vector3d factors) {
        SceneDocument<Object> after = SelectionTransforms.scale(before, selection, groupPivot, factors);
        return correctModelCenters(before, after, selection,
                oldCenter -> oldCenter.sub(groupPivot).mul(factors).add(groupPivot));
    }

    private static SceneDocument<Object> correctModelCenters(SceneDocument<Object> before,
            SceneDocument<Object> after, Collection<UUID> selection,
            java.util.function.UnaryOperator<Vector3d> targetCenter) {
        Set<UUID> ids = Set.copyOf(selection);
        SceneDocument<Object> corrected = after;
        for (SceneLayer<Object> oldLayer : before.layers()) {
            if (!ids.contains(oldLayer.id())) continue;
            SceneLayer<Object> current = corrected.layers().stream()
                    .filter(layer -> layer.id().equals(oldLayer.id())).findFirst().orElseThrow();
            Vector3d correction = targetCenter.apply(layerPivot(oldLayer)).sub(layerPivot(current));
            if (correction.lengthSquared() > 1.0e-18D) {
                corrected = SelectionTransforms.translate(corrected, Set.of(oldLayer.id()), correction);
            }
        }
        return corrected;
    }

    private static float scaleComponent(Vector3f scale, GizmoHandle handle) {
        return switch (handle) {
            case X -> scale.x;
            case Y -> scale.y;
            case Z -> scale.z;
            case NONE, UNIFORM -> throw new IllegalArgumentException(handle + " is not a scale axis");
        };
    }

    private static void setScaleComponent(Vector3d scale, GizmoHandle handle, double value) {
        switch (handle) {
            case X -> scale.x = value;
            case Y -> scale.y = value;
            case Z -> scale.z = value;
            case NONE, UNIFORM -> throw new IllegalArgumentException(handle + " is not a scale axis");
        }
    }

    private Vector3d selectionPivot() {
        return selectionPivot(draft, selectedLayerIds);
    }

    private static EditorCameraState initialCamera(List<SceneLayer<Object>> layers, BlockPos worldOrigin) {
        if (worldOrigin != null) {
            net.minecraft.client.Camera worldCamera = Minecraft.getInstance().gameRenderer.getMainCamera();
            if (worldCamera.isInitialized()) {
                net.minecraft.world.phys.Vec3 position = worldCamera.position();
                Vector3d relative = new Vector3d(position.x - worldOrigin.getX() - 0.5D,
                        position.y - worldOrigin.getY() - 0.5D,
                        position.z - worldOrigin.getZ() - 0.5D);
                if (relative.lengthSquared() > 0.0025D) {
                    float fov = Math.clamp(worldCamera.getFov(), 2.0F, 178.0F);
                    Vector3d focus = new Vector3d(relative).add(new Vector3d(worldCamera.forwardVector())
                            .mul(relative.length()));
                    return new EditorCameraState(EditorCameraMode.ORBIT, relative,
                            worldCamera.rotation(), focus, fov, 5.0F, 0.05F, 2048.0F);
                }
            }
        }
        Vector3d focus = new Vector3d();
        if (!layers.isEmpty()) {
            for (SceneLayer<Object> layer : layers) focus.add(layerPivot(layer));
            focus.div(layers.size());
        }
        return EditorCameraState.lookingAt(EditorCameraMode.ORBIT,
                new Vector3d(focus).add(7.0D, 5.5D, 9.0D), focus, WORLD_UP,
                45.0F, 5.0F, 0.05F, 2048.0F);
    }

    private void setStatus(String message, boolean error) {
        setStatus(message == null ? Component.empty() : Component.literal(message), error);
    }

    private boolean validSelectedShapeTool() {
        return minecraft != null && minecraft.player != null && minecraft.player.isCreative()
                && shapeToolInventorySlot >= 0 && shapeToolInventorySlot < 36
                && YuushyaShapeToolBridge.isShapeTool(
                        minecraft.player.getInventory().getItem(shapeToolInventorySlot));
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

    private record GizmoDragSession(SceneLayer<Object> layer, List<UUID> selectedLayerIds,
            CameraFrame frame, GizmoMode mode,
            Vector3d origin, Vector3d axis, Vector3d startHit,
            double centerScreenX, double centerScreenY,
            double scaleScreenAxisX, double scaleScreenAxisY, double startScaleCoordinate,
            double rotationRadius) {
        private GizmoDragSession {
            origin = new Vector3d(origin);
            axis = new Vector3d(axis);
            startHit = new Vector3d(startHit);
            if (!Double.isFinite(rotationRadius) || rotationRadius <= 0.0D) {
                throw new IllegalArgumentException("rotationRadius must be positive and finite");
            }
        }

        @Override
        public Vector3d origin() { return new Vector3d(origin); }

        @Override
        public Vector3d axis() { return new Vector3d(axis); }

        @Override
        public Vector3d startHit() { return new Vector3d(startHit); }
    }
}
