package com.zhongbai233.yuushya_editor.bench;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.client.anvilcraft.rendering.CachedModeClient;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import com.yuushya.modelling.registries.BlockRegistry;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.zhongbai233.bench.api.BenchApiVersion;
import com.zhongbai233.bench.api.BenchCompatibility;
import com.zhongbai233.bench.api.ScenarioDescriptor;
import com.zhongbai233.bench.api.client.gui.BenchGuiCaptureOptions;
import com.zhongbai233.bench.api.client.gui.BenchGuiSelector;
import com.zhongbai233.bench.api.client.gui.BenchScreenSnapshot;
import com.zhongbai233.bench.api.neoforge.client.BenchCaptureOptions;
import com.zhongbai233.bench.api.neoforge.client.BenchClientContext;
import com.zhongbai233.bench.api.neoforge.client.BenchClientProvider;
import com.zhongbai233.bench.api.neoforge.client.BenchClientRegistrar;
import com.zhongbai233.bench.api.neoforge.client.BenchClientScenario;
import com.zhongbai233.bench.api.neoforge.client.BenchClientStepResult;
import com.zhongbai233.bench.api.neoforge.client.BenchGuiSession;
import com.zhongbai233.yuushya_editor.client.YuushyaEditorScreen;
import com.zhongbai233.yuushya_editor.client.BlockPickerScreen;
import com.zhongbai233.yuushya_editor.client.BlockStateEditorScreen;
import com.zhongbai233.yuushya_editor.client.ItemPickerScreen;
import com.zhongbai233.yuushya_editor.client.ItemContentEditorScreen;
import com.zhongbai233.yuushya_editor.client.TextContentEditorScreen;
import com.zhongbai233.yuushya_editor.client.ZFightWarningScreen;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewPipRenderer;
import com.zhongbai233.scene_editor.core.render.LineWidthPolicy;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewFrame;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewManager;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.compat.YuushyaTransformConversion;
import com.zhongbai233.yuushya_editor.compat.YuushyaEditorHost;
import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.EditorType;
import com.zhongbai233.yuushya_editor.core.ItemModelData;
import com.zhongbai233.yuushya_editor.core.SceneDocument;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import com.zhongbai233.yuushya_editor.core.TextModelData;
import com.zhongbai233.scene_editor.core.camera.CameraFrame;
import com.zhongbai233.scene_editor.core.camera.EditorCameraState;
import com.zhongbai233.scene_editor.core.gizmo.GizmoHandle;
import com.zhongbai233.scene_editor.core.gizmo.GizmoHitTesting;
import com.zhongbai233.scene_editor.core.gizmo.GizmoMode;
import com.zhongbai233.scene_editor.core.gizmo.GizmoSizingPolicy;
import com.zhongbai233.scene_editor.core.gizmo.GizmoSnapPolicy;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewTransform;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import com.zhongbai233.scene_editor.core.projection.ProjectedPoint;
import com.zhongbai233.scene_editor.core.projection.EditorProjection;
import com.zhongbai233.scene_editor.core.projection.EditorViewport;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

/** Unattended integrated-client acceptance test for the real Yuushya show-block editor path. */
public final class YuushyaEditorBenchProvider implements BenchClientProvider {
    private static final String SCENARIO_ID = "yuushya-editor.axis-scale-apply";

    public YuushyaEditorBenchProvider() { }

    @Override
    public String id() {
        return "yuushya-editor";
    }

    @Override
    public BenchCompatibility compatibility() {
        return BenchApiVersion.currentCompatibility();
    }

    @Override
    public void registerClient(BenchClientRegistrar registrar) {
        registrar.register(new ScenarioDescriptor(
                SCENARIO_ID,
                "Yuushya editor axis-scale apply",
                Set.of("client", "gui", "yuushya", "editor"),
                Duration.ofSeconds(60)),
                ignored -> new AxisScaleScenario());
    }

    private static final class AxisScaleScenario implements BenchClientScenario {
        private static final double EPSILON = 1.0E-6D;
        private static final float FLOAT_EPSILON = 1.0E-6F;
        private static final Vector3d INITIAL_POSITION = new Vector3d(8.0D, 4.0D, -2.0D);
        private static final Vector3d EXPECTED_POSITION = new Vector3d(4.0D, 4.0D, -2.0D);
        private static final Vector3f INITIAL_ROTATION = new Vector3f(10.0F, 20.0F, 30.0F);
        private static final Vector3f EXPECTED_SCALE = new Vector3f(2.0F, 1.0F, 1.0F);
        private static final BenchGuiSelector SCALE_X = BenchGuiSelector.semanticName("scale-x");
        private static final BlockPos ENVIRONMENT_STONE_OFFSET = new BlockPos(3, -1, 2);
        private static final BlockPos ENVIRONMENT_GOLD_OFFSET = new BlockPos(-3, 0, -2);
        private static final int ENVIRONMENT_PLATFORM_RADIUS = 12;
        private static final int PIP_CACHE_STABILITY_TICKS = 8;
        private static final long MAX_ENVIRONMENT_CAPTURE_SLICE_NANOS = 10_000_000L;
        private static final long MAX_ENVIRONMENT_CAPTURE_TICK_NANOS = 12_000_000L;
        private static final BlockPos ITEM_FIXTURE_OFFSET = new BlockPos(5, 0, 0);
        private static final BlockPos TEXT_FIXTURE_OFFSET = new BlockPos(7, 0, 0);
        private static final BlockPos SHOW_FIXTURE_OFFSET = new BlockPos(-5, 0, 0);

        private final AtomicBoolean fixtureReady = new AtomicBoolean();
        private final AtomicBoolean serverCheckPending = new AtomicBoolean();
        private final AtomicBoolean serverUpdateVerified = new AtomicBoolean();
        private final AtomicBoolean fixtureRemoved = new AtomicBoolean();
        private final AtomicBoolean itemServerVerified = new AtomicBoolean();
        private final AtomicBoolean textServerVerified = new AtomicBoolean();
        private final AtomicReference<Throwable> asyncFailure = new AtomicReference<>();
        private final AtomicReference<String> serverVerification = new AtomicReference<>("");

        private MinecraftServer server;
        private UUID playerId;
        private ResourceKey<Level> dimension;
        private BlockPos fixturePos;
        private BenchGuiSession guiSession;
        private BenchScreenSnapshot guiSnapshot;
        private EditBox[] transformBoxes;
        private CompletableFuture<Path> editorScreenshot;
        private CompletableFuture<Path> scaleScreenshot;
        private CompletableFuture<Path> farLineWidthScreenshot;
        private CompletableFuture<Path> blockPickerScreenshot;
        private CompletableFuture<Path> addedBlockScreenshot;
        private CompletableFuture<Path> blockStateSettingsScreenshot;
        private CompletableFuture<Path> zFightWarningScreenshot;
        private CompletableFuture<Path> itemPickerScreenshot;
        private CompletableFuture<Path> itemSettingsScreenshot;
        private CompletableFuture<Path> textEditorScreenshot;
        private boolean screenOpened;
        private boolean inspectorCommitted;
        private boolean gizmoInteractionsVerified;
        private boolean selectionInteractionsVerified;
        private boolean collisionShapeChanged;
        private boolean renderedLayersVerified;
        private boolean farCameraRestored;
        private boolean blockPickerOpened;
        private boolean blockAdditionVerified;
        private boolean blockStateSettingsOpened;
        private boolean blockStateSettingsApplied;
        private boolean zFightDuplicateAdded;
        private boolean zFightWarningOpened;
        private boolean autosaveSent;
        private boolean itemEditorOpened;
        private boolean itemPickerOpened;
        private boolean itemPickerReturned;
        private boolean itemSettingsOpened;
        private boolean itemSettingsApplied;
        private boolean itemDeleteSent;
        private boolean itemRenderCacheInvalidated;
        private boolean textEditorOpened;
        private boolean textSettingsOpened;
        private boolean textSettingsApplied;
        private boolean textLayerAdded;
        private boolean textSaveSent;
        private boolean environmentPreviewVerified;
        private boolean initialCameraVerified;
        private boolean layerClipboardVerified;
        private boolean nativeBlockTransferVerified;
        private boolean nativeItemTransferVerified;
        private boolean nativeTextTransferVerified;
        private boolean environmentMutationRequested;
        private boolean environmentMutationVerified;
        private int warmupTicks;
        private String renderedLayerVerification = "";
        private float initialLineWidthScale;
        private float farLineWidthScale;
        private int environmentRetainedBlocks;
        private int environmentShellRetained;
        private int environmentShellCandidates;
        private BlockPreviewPipRenderer.PerformanceSnapshot pipCacheWarmupStart;
        private BlockPreviewPipRenderer.PerformanceSnapshot pipCacheWarmupEnd;
        private EnvironmentPreviewManager.PerformanceSnapshot environmentPerformance;

        @Override
        public void setup(BenchClientContext context) {
            server = context.minecraft().getSingleplayerServer();
            if (server == null) throw new IllegalStateException("ModBench did not create an integrated server");
            playerId = context.player().getUUID();
            dimension = context.level().dimension();
            // The void fixture preset can spawn the player below the legal build-height
            // floor (for example around Y=-74 in MC 26.1.2).  Keep the fixture close to
            // the player while placing it in a section that the server can actually edit.
            BlockPos playerPos = context.player().blockPosition();
            int fixtureY = Math.max(playerPos.getY() + 2, context.level().dimensionType().minY() + 4);
            fixturePos = new BlockPos(playerPos.getX() + 2, fixtureY, playerPos.getZ() + 2).immutable();
            server.execute(() -> createFixture(serverLevel()));
        }

        @Override
        public BenchClientStepResult stabilize(BenchClientContext context) throws Exception {
            throwAsyncFailure();
            if (!fixtureReady.get()) return BenchClientStepResult.CONTINUE;

            ShowBlockEntity clientEntity = clientFixture(context);
            if (clientEntity == null || !fixtureReachedClient(clientEntity)
                    || !environmentFixtureReachedClient(context)) {
                return BenchClientStepResult.CONTINUE;
            }
            if (!context.environment().readiness().ready() || context.frames().sampleCount() < 2) {
                return BenchClientStepResult.CONTINUE;
            }

            if (!screenOpened) {
                BlockPreviewPipRenderer.resetPerformanceCounters();
                net.minecraft.client.Camera enteringCamera = context.minecraft().gameRenderer.getMainCamera();
                Vector3d expectedCameraPosition = new Vector3d(
                        enteringCamera.position().x - fixturePos.getX() - 0.5D,
                        enteringCamera.position().y - fixturePos.getY() - 0.5D,
                        enteringCamera.position().z - fixturePos.getZ() - 0.5D);
                Quaternionf expectedCameraRotation = new Quaternionf(enteringCamera.rotation());
                context.minecraft().setScreen(new ShowBlockScreen(clientEntity, null));
                if (!(context.minecraft().screen instanceof YuushyaEditorScreen enhanced)) {
                    throw new AssertionError("ShowBlockScreen was not intercepted by YuushyaEditorScreen: "
                            + (context.minecraft().screen == null ? "null"
                            : context.minecraft().screen.getClass().getName()));
                }
                assertInitialCamera(enhanced, expectedCameraPosition, expectedCameraRotation,
                        enteringCamera.getFov());
                initialCameraVerified = true;
                verifyLayerClipboard(enhanced);
                layerClipboardVerified = true;
                verifyNativeRoundTrip(enhanced);
                nativeBlockTransferVerified = true;
                invokePrivate(enhanced, "focusSelected", new Class<?>[0]);
                setStableGizmoCamera(enhanced);
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                transformBoxes = transformBoxes(enhanced);
                if (transformBoxes.length != 9) {
                    throw new AssertionError("Expected nine inspector fields, found " + transformBoxes.length);
                }
                guiSession.name(transformBoxes[6], "scale-x");
                guiSession.name(transformBoxes[7], "scale-y");
                guiSession.name(transformBoxes[8], "scale-z");
                guiSession.name(collisionShapeButton(enhanced), "collision-shape");
                guiSnapshot = guiSession.snapshot();
                verifySnapshot(guiSnapshot);
                context.artifacts().write("yuushya-editor-gui.txt", "text/plain", formatSnapshot(guiSnapshot));
                screenOpened = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!environmentPreviewVerified) {
                YuushyaEditorScreen enhanced = requireEnhancedScreen(context);
                EnvironmentPreviewFrame frame = environmentPreview(enhanced);
                if (!frame.complete()) return BenchClientStepResult.CONTINUE;
                EnvironmentVerification verification = assertEnvironmentPreview(frame);
                environmentRetainedBlocks = verification.retainedBlocks();
                environmentShellRetained = verification.shellRetained();
                environmentShellCandidates = verification.shellCandidates();
                BlockPreviewPipRenderer.PerformanceSnapshot performance =
                        BlockPreviewPipRenderer.performanceSnapshot();
                if (performance.environmentCompiledSections() < frame.sections().size()) {
                    return BenchClientStepResult.CONTINUE;
                }
                environmentPreviewVerified = true;
            }

            return context.environment().readiness().ready() && context.environment().isFrameStable(4)
                    ? BenchClientStepResult.COMPLETE : BenchClientStepResult.CONTINUE;
        }

        @Override
        public BenchClientStepResult warmup(BenchClientContext context) {
            requireEnhancedScreen(context);
            if (warmupTicks == 0) {
                pipCacheWarmupStart = BlockPreviewPipRenderer.performanceSnapshot();
            }
            if (++warmupTicks < PIP_CACHE_STABILITY_TICKS) {
                return BenchClientStepResult.CONTINUE;
            }
            pipCacheWarmupEnd = BlockPreviewPipRenderer.performanceSnapshot();
            return BenchClientStepResult.COMPLETE;
        }

        @Override
        public BenchClientStepResult measure(BenchClientContext context) throws Exception {
            throwAsyncFailure();
            if (!inspectorCommitted) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                EditorTransform beforeAxisScale = selectedTransform(screen);
                transformBoxes[6].setValue("2.0");
                Object committed = invokePrivate(screen, "commitInspector",
                        new Class<?>[] {String.class}, "ModBench X-axis scale");
                if (!Boolean.TRUE.equals(committed)) {
                    throw new AssertionError("Editor rejected the valid X-axis scale value");
                }
                assertFieldValue(0, EXPECTED_POSITION.x);
                assertFieldValue(1, EXPECTED_POSITION.y);
                assertFieldValue(2, EXPECTED_POSITION.z);
                assertFieldValue(6, EXPECTED_SCALE.x);
                assertFieldValue(7, EXPECTED_SCALE.y);
                assertFieldValue(8, EXPECTED_SCALE.z);
                EditorTransform afterAxisScale = selectedTransform(screen);
                if (!afterAxisScale.scale().equals(EXPECTED_SCALE)
                        || BlockPreviewTransform.pivot(afterAxisScale)
                                .distance(BlockPreviewTransform.pivot(beforeAxisScale)) > EPSILON) {
                    throw new AssertionError("Inspector X-only scale changed another axis or moved the pivot: "
                            + afterAxisScale);
                }
                verifySnapshot(guiSession.snapshot());
                inspectorCommitted = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!gizmoInteractionsVerified) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                verifySelectionInteractions(screen);
                selectionInteractionsVerified = true;
                verifyRotateAndScaleGizmos(screen);
                // History navigation rebuilds the editor widgets so hierarchy rows,
                // scrolling and inspector values all reflect the restored document.
                // Bind semantic names only after those interactions, otherwise the
                // GUI session would retain references to widgets that no longer exist.
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                transformBoxes = transformBoxes(screen);
                guiSession.name(transformBoxes[6], "scale-x");
                guiSession.name(transformBoxes[7], "scale-y");
                guiSession.name(transformBoxes[8], "scale-z");
                guiSession.name(collisionShapeButton(screen), "collision-shape");
                guiSnapshot = guiSession.snapshot();
                verifySnapshot(guiSession.snapshot());
                gizmoInteractionsVerified = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!collisionShapeChanged) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                BlackGoldButton button = collisionShapeButton(screen);
                button.onPress(new MouseButtonEvent(button.getX() + button.getWidth() * 0.5D,
                        button.getY() + button.getHeight() * 0.5D,
                        new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)));
                assertDraftCollisionShape(screen, CollisionShape.Kind.FENCE);
                verifySnapshot(guiSession.snapshot());
                collisionShapeChanged = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!environmentMutationRequested) {
                environmentMutationRequested = true;
                server.execute(() -> {
                    try {
                        serverLevel().setBlockAndUpdate(fixturePos.offset(ENVIRONMENT_STONE_OFFSET),
                                Blocks.EMERALD_BLOCK.defaultBlockState());
                    } catch (Throwable throwable) {
                        asyncFailure.compareAndSet(null, throwable);
                    }
                });
                return BenchClientStepResult.CONTINUE;
            }

            if (!environmentMutationVerified) {
                if (!context.level().getBlockState(fixturePos.offset(ENVIRONMENT_STONE_OFFSET))
                        .is(Blocks.EMERALD_BLOCK)) {
                    return BenchClientStepResult.CONTINUE;
                }
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                EnvironmentPreviewFrame frame = environmentPreview(screen);
                if (!frameContains(frame, fixturePos.offset(ENVIRONMENT_STONE_OFFSET), Blocks.EMERALD_BLOCK)) {
                    return BenchClientStepResult.CONTINUE;
                }
                environmentPerformance = environmentManager(screen).performanceSnapshot();
                if (environmentPerformance.dirtySectionsScheduled() <= 0L) {
                    throw new AssertionError("Incremental block update did not dirty an environment section");
                }
                environmentMutationVerified = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (editorScreenshot == null) {
                BenchCaptureOptions fullOptions = guiCaptureOptions();
                editorScreenshot = context.automation().captureScreenshot(
                        "yuushya-editor-axis-scale", fullOptions);
                scaleScreenshot = guiSession.captureWidget(
                        "yuushya-editor-axis-scale-control", SCALE_X,
                        BenchGuiCaptureOptions.defaults().withPadding(4).withStableFrames(4)
                                .failingOnClippedBounds());
                return BenchClientStepResult.CONTINUE;
            }

            if (!editorScreenshot.isDone() || !scaleScreenshot.isDone()) {
                return BenchClientStepResult.CONTINUE;
            }
            Path fullScreenshot = requirePng(editorScreenshot);
            requirePng(scaleScreenshot);
            if (!renderedLayersVerified) {
                renderedLayerVerification = verifyRenderedLayers(
                        fullScreenshot, requireEnhancedScreen(context), guiSnapshot);
                renderedLayersVerified = true;
                context.artifacts().write("yuushya-editor-render-layers.txt", "text/plain",
                        renderedLayerVerification);
            }
            if (!gizmoInteractionsVerified) {
                throw new AssertionError("Rotate and axis-scale Gizmo interactions were not verified");
            }

            if (farLineWidthScreenshot == null) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                CameraFrame frame = currentCameraFrame(screen);
                initialLineWidthScale = LineWidthPolicy.forCamera(currentCamera(screen));
                screen.mouseScrolled(frame.viewport().x() + frame.viewport().width() * 0.5D,
                        frame.viewport().y() + frame.viewport().height() * 0.5D,
                        0.0D, -8.0D);
                farLineWidthScale = LineWidthPolicy.forCamera(currentCamera(screen));
                if (!(farLineWidthScale > initialLineWidthScale * 1.5F)) {
                    throw new AssertionError("Pulling the camera away did not visibly increase line width: "
                            + initialLineWidthScale + " -> " + farLineWidthScale);
                }
                farLineWidthScreenshot = context.automation().captureScreenshot(
                        "yuushya-editor-far-line-width", guiCaptureOptions());
                return BenchClientStepResult.CONTINUE;
            }

            if (!farLineWidthScreenshot.isDone()) return BenchClientStepResult.CONTINUE;
            requirePng(farLineWidthScreenshot);
            if (!farCameraRestored) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                CameraFrame frame = currentCameraFrame(screen);
                screen.mouseScrolled(frame.viewport().x() + frame.viewport().width() * 0.5D,
                        frame.viewport().y() + frame.viewport().height() * 0.5D,
                        0.0D, 8.0D);
                float restored = LineWidthPolicy.forCamera(currentCamera(screen));
                if (Math.abs(restored - initialLineWidthScale) > 0.02F) {
                    throw new AssertionError("Line-width zoom verification did not restore the camera: "
                            + initialLineWidthScale + " -> " + restored);
                }
                farCameraRestored = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!blockStateSettingsOpened) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                UUID cutoutLayer = draft(screen).layers().get(1).id();
                invokePrivate(screen, "selectLayer", new Class<?>[] {UUID.class}, cutoutLayer);
                closeGuiSession();
                invokePrivate(screen, "editSelectedContent", new Class<?>[0]);
                if (!(context.minecraft().screen instanceof BlockStateEditorScreen settings)) {
                    throw new AssertionError("Block layer did not open the block-state property editor");
                }
                invokePrivate(settings, "cycleProperty", new Class<?>[] {Property.class, int.class},
                        BlockStateProperties.WATERLOGGED, 1);
                guiSession = context.automation().beginGuiSession(BlockStateEditorScreen.class);
                blockStateSettingsScreenshot = context.automation().captureScreenshot(
                        "yuushya-editor-block-state-settings", guiCaptureOptions());
                blockStateSettingsOpened = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!blockStateSettingsScreenshot.isDone()) return BenchClientStepResult.CONTINUE;
            requirePng(blockStateSettingsScreenshot);
            if (!blockStateSettingsApplied) {
                if (!(context.minecraft().screen instanceof BlockStateEditorScreen settings)) {
                    throw new AssertionError("Block-state property editor closed before apply");
                }
                closeGuiSession();
                invokePrivate(settings, "save", new Class<?>[0]);
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                assertEditedBlockState(screen);
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                blockStateSettingsApplied = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!blockPickerOpened) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                // The main editor session deliberately pins YuushyaEditorScreen. Close it before
                // opening the child picker, otherwise Bench's screenshot gate correctly reports
                // the temporary BlockPickerScreen as an unexpected screen.
                if (guiSession != null && guiSession.active()) {
                    guiSession.close();
                    guiSession = null;
                }
                invokePrivate(screen, "openBlockPicker", new Class<?>[0]);
                if (!(context.minecraft().screen instanceof BlockPickerScreen picker)) {
                    throw new AssertionError("Add Block did not open the registry-backed picker");
                }
                guiSession = context.automation().beginGuiSession(BlockPickerScreen.class);
                filterBlockPicker(picker, "minecraft:glass");
                assertBlockPickerPreview(picker);
                blockPickerScreenshot = context.automation().captureScreenshot(
                        "yuushya-editor-block-picker", guiCaptureOptions());
                blockPickerOpened = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!blockPickerScreenshot.isDone()) return BenchClientStepResult.CONTINUE;
            requirePng(blockPickerScreenshot);
            if (!blockAdditionVerified) {
                if (!(context.minecraft().screen instanceof BlockPickerScreen picker)) {
                    throw new AssertionError("Block picker closed before the scripted choice");
                }
                chooseFirstFilteredBlock(picker);
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                assertGlassLayerAdded(screen);
                addedBlockScreenshot = context.automation().captureScreenshot(
                        "yuushya-editor-added-glass-selected", guiCaptureOptions());
                blockAdditionVerified = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!addedBlockScreenshot.isDone()) return BenchClientStepResult.CONTINUE;
            requirePng(addedBlockScreenshot);
            requirePng(blockStateSettingsScreenshot);

            if (!zFightDuplicateAdded) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                invokePrivate(screen, "addBlock", new Class<?>[] {BlockState.class},
                        Blocks.GLASS.defaultBlockState());
                assertDuplicateGlassLayers(screen);
                zFightDuplicateAdded = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!zFightWarningOpened) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                closeGuiSession();
                screen.onClose();
                if (!(context.minecraft().screen instanceof ZFightWarningScreen)) {
                    throw new AssertionError("Coplanar duplicate blocks did not open the Z-fighting save gate");
                }
                guiSession = context.automation().beginGuiSession(ZFightWarningScreen.class);
                zFightWarningScreenshot = context.automation().captureScreenshot(
                        "yuushya-editor-zfight-warning", guiCaptureOptions());
                zFightWarningOpened = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!zFightWarningScreenshot.isDone()) return BenchClientStepResult.CONTINUE;
            requirePng(zFightWarningScreenshot);
            if (!autosaveSent) {
                if (!(context.minecraft().screen instanceof ZFightWarningScreen warning)) {
                    throw new AssertionError("Z-fighting warning closed before the optimize choice");
                }
                pressFirstWarningButton(warning);
                if (context.minecraft().screen != null) {
                    throw new AssertionError("Optimize-and-save did not close the enhanced editor");
                }
                closeGuiSession();
                autosaveSent = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!serverUpdateVerified.get()) pollServerUpdate();
            throwAsyncFailure();
            if (!serverUpdateVerified.get()) return BenchClientStepResult.CONTINUE;

            if (!itemEditorOpened) {
                ItemBlockEntity entity = clientItemFixture(context);
                if (entity == null || entity.getTransformData().size() != 2) return BenchClientStepResult.CONTINUE;
                context.minecraft().setScreen(new ItemBlockScreen(entity, ItemStack.EMPTY));
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                assertEditorType(screen, EditorType.ITEM);
                verifyNativeRoundTrip(screen);
                nativeItemTransferVerified = true;
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                itemEditorOpened = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!itemPickerOpened) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                closeGuiSession();
                invokePrivate(screen, "openContentPicker", new Class<?>[0]);
                if (!(context.minecraft().screen instanceof ItemPickerScreen picker)) {
                    throw new AssertionError("Item editor did not open the registry-backed item picker");
                }
                guiSession = context.automation().beginGuiSession(ItemPickerScreen.class);
                filterItemPicker(picker, "minecraft:diamond");
                assertItemPickerPreview(picker);
                itemPickerScreenshot = context.automation().captureScreenshot(
                        "yuushya-editor-item-picker", guiCaptureOptions());
                itemPickerOpened = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!itemPickerScreenshot.isDone()) return BenchClientStepResult.CONTINUE;
            requirePng(itemPickerScreenshot);
            if (!itemPickerReturned) {
                if (!(context.minecraft().screen instanceof ItemPickerScreen picker)) {
                    throw new AssertionError("Item picker closed before its preview was verified");
                }
                closeGuiSession();
                picker.onClose();
                requireEnhancedScreen(context);
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                itemPickerReturned = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!itemSettingsOpened) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                closeGuiSession();
                invokePrivate(screen, "editSelectedContent", new Class<?>[0]);
                if (!(context.minecraft().screen instanceof ItemContentEditorScreen settings)) {
                    throw new AssertionError("Item layer did not open the complete settings editor");
                }
                invokePrivate(settings, "replaceItem", new Class<?>[] {ItemStack.class},
                        Items.OAK_STAIRS.getDefaultInstance());
                invokePrivate(settings, "toggleBlockMode", new Class<?>[0]);
                invokePrivate(settings, "cycleProperty", new Class<?>[] {Property.class, int.class},
                        BlockStateProperties.HORIZONTAL_FACING, 1);
                Field colorField = ItemContentEditorScreen.class.getDeclaredField("colorBox");
                if (!colorField.trySetAccessible()) {
                    throw new IllegalStateException("Cannot access item settings colour field");
                }
                ((EditBox) colorField.get(settings)).setValue("#7F336699");
                guiSession = context.automation().beginGuiSession(ItemContentEditorScreen.class);
                itemSettingsScreenshot = context.automation().captureScreenshot(
                        "yuushya-editor-item-settings", guiCaptureOptions());
                itemSettingsOpened = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!itemSettingsScreenshot.isDone()) return BenchClientStepResult.CONTINUE;
            requirePng(itemSettingsScreenshot);
            if (!itemSettingsApplied) {
                if (!(context.minecraft().screen instanceof ItemContentEditorScreen settings)) {
                    throw new AssertionError("Item settings closed before its state was verified");
                }
                closeGuiSession();
                invokePrivate(settings, "save", new Class<?>[0]);
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                assertEditedBlockItem(screen);
                SceneDocument<Object> itemDraft = draft(screen);
                UUID secondLayer = itemDraft.layers().get(1).id();
                invokePrivate(screen, "selectLayer", new Class<?>[] {UUID.class}, secondLayer);
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                itemSettingsApplied = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!itemDeleteSent) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                invokePrivate(screen, "removeSelectedLayer", new Class<?>[0]);
                ChunkPos itemChunk = ChunkPos.containing(fixturePos.offset(ITEM_FIXTURE_OFFSET));
                CachedModeClient.INSTANCE.safeSet.remove(itemChunk);
                screen.onClose();
                if (context.minecraft().screen != null) {
                    throw new AssertionError("Item editor delete/autosave did not close");
                }
                if (!CachedModeClient.INSTANCE.safeSet.contains(itemChunk)) {
                    throw new AssertionError("Item editor save did not invalidate Yuushya's cached world renderer");
                }
                itemRenderCacheInvalidated = true;
                closeGuiSession();
                itemDeleteSent = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!itemServerVerified.get()) {
                pollStructuredUpdate(false);
                return BenchClientStepResult.CONTINUE;
            }
            if (!textEditorOpened) {
                TextBlockEntity entity = clientTextFixture(context);
                if (entity == null || entity.getTransformData().size() != 1) return BenchClientStepResult.CONTINUE;
                context.minecraft().setScreen(new TextBlockScreen(entity, List.of()));
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                assertEditorType(screen, EditorType.TEXT);
                verifyNativeRoundTrip(screen);
                nativeTextTransferVerified = true;
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                textEditorOpened = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!textSettingsOpened) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                closeGuiSession();
                invokePrivate(screen, "editSelectedContent", new Class<?>[0]);
                if (!(context.minecraft().screen instanceof TextContentEditorScreen editor)) {
                    throw new AssertionError("Text layer did not open the text content editor");
                }
                setTextEditorValue(editor, "Bench Updated\nSecond Line");
                pressTextOption(editor, "cullButton");
                pressTextOption(editor, "mirrorButton");
                guiSession = context.automation().beginGuiSession(TextContentEditorScreen.class);
                textEditorScreenshot = context.automation().captureScreenshot(
                        "yuushya-editor-text-editor", guiCaptureOptions());
                textSettingsOpened = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!textEditorScreenshot.isDone()) return BenchClientStepResult.CONTINUE;
            requirePng(textEditorScreenshot);
            if (!textSettingsApplied) {
                if (!(context.minecraft().screen instanceof TextContentEditorScreen editor)) {
                    throw new AssertionError("Text editor closed before its content was verified");
                }
                closeGuiSession();
                invokePrivate(editor, "save", new Class<?>[0]);
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                assertEditedTextLayer(screen);
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                textSettingsApplied = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!textLayerAdded) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                closeGuiSession();
                invokePrivate(screen, "openContentPicker", new Class<?>[0]);
                if (!(context.minecraft().screen instanceof TextContentEditorScreen editor)) {
                    throw new AssertionError("Add text did not open the text content editor");
                }
                setTextEditorValue(editor, "Bench Added");
                invokePrivate(editor, "save", new Class<?>[0]);
                screen = requireEnhancedScreen(context);
                assertAddedTextLayer(screen);
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                textLayerAdded = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!textSaveSent) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                screen.onClose();
                if (context.minecraft().screen != null) {
                    throw new AssertionError("Text editor content/autosave did not close");
                }
                closeGuiSession();
                textSaveSent = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!textServerVerified.get()) pollStructuredUpdate(true);
            throwAsyncFailure();
            return textServerVerified.get() && fixtureRemoved.get()
                    ? BenchClientStepResult.COMPLETE : BenchClientStepResult.CONTINUE;
        }

        @Override
        public void verify(BenchClientContext context) throws Exception {
            throwAsyncFailure();
            if (!serverUpdateVerified.get()) {
                throw new AssertionError("Integrated server never observed the applied editor values");
            }
            if (!itemServerVerified.get() || !textServerVerified.get()) {
                throw new AssertionError("Yuushya item/text editor packets were not verified on the server");
            }
            if (!itemRenderCacheInvalidated) {
                throw new AssertionError("Yuushya item world-render cache invalidation was not verified");
            }
            if (!selectionInteractionsVerified) {
                throw new AssertionError("EditorViewport selection interactions were not verified");
            }
            if (!fixtureRemoved.get()) throw new AssertionError("Benchmark fixture was not removed precisely");
            if (context.minecraft().screen != null) throw new AssertionError("A Screen remained open after Apply");
            if (!context.environment().isValid()) {
                throw new AssertionError("Client environment became invalid: " + context.environment().invalidations());
            }
            requirePng(editorScreenshot);
            requirePng(scaleScreenshot);
            requirePng(farLineWidthScreenshot);
            requirePng(blockPickerScreenshot);
            requirePng(addedBlockScreenshot);
            requirePng(blockStateSettingsScreenshot);
            requirePng(zFightWarningScreenshot);
            requirePng(itemPickerScreenshot);
            requirePng(itemSettingsScreenshot);
            requirePng(textEditorScreenshot);
            if (!renderedLayersVerified) {
                throw new AssertionError("Opaque/cutout/translucent preview layers were not visually verified");
            }
            if (!blockAdditionVerified) throw new AssertionError("Registry block addition was not verified");
            if (!blockStateSettingsOpened || !blockStateSettingsApplied) {
                throw new AssertionError("Block-state property menu and apply flow were not verified");
            }
            if (!zFightDuplicateAdded || !zFightWarningOpened) {
                throw new AssertionError("Z-fighting warning and optimization were not verified");
            }
            if (!itemPickerOpened || !itemPickerReturned || !itemSettingsOpened || !itemSettingsApplied) {
                throw new AssertionError("Item picker/settings/block-state flow was not verified");
            }
            if (!textEditorOpened || !textSettingsOpened || !textSettingsApplied
                    || !textLayerAdded || !textSaveSent) {
                throw new AssertionError("Text edit/add/multiline/flag flow was not verified");
            }
            if (!environmentPreviewVerified) throw new AssertionError("World environment preview was not verified");
            if (!environmentMutationVerified || environmentPerformance == null) {
                throw new AssertionError("Event-driven environment invalidation was not verified");
            }
            if (!collisionShapeChanged) throw new AssertionError("Collision-shape control was not verified");
            if (!initialCameraVerified || !layerClipboardVerified) {
                throw new AssertionError("Initial camera or layer clipboard behavior was not verified");
            }
            if (!nativeBlockTransferVerified || !nativeItemTransferVerified || !nativeTextTransferVerified) {
                throw new AssertionError("Yuushya native block/item/text transfer was not round-trip verified");
            }
            if (environmentPerformance.maxCaptureSliceNanos() > MAX_ENVIRONMENT_CAPTURE_SLICE_NANOS
                    || environmentPerformance.maxTickCaptureNanos() > MAX_ENVIRONMENT_CAPTURE_TICK_NANOS) {
                throw new AssertionError("Environment capture exceeded the client-thread budget: "
                        + environmentPerformance);
            }
            BlockPreviewPipRenderer.PerformanceSnapshot performance =
                    BlockPreviewPipRenderer.performanceSnapshot();
            if (pipCacheWarmupStart == null || pipCacheWarmupEnd == null) {
                throw new AssertionError("PIP texture cache stability window was not measured");
            }
            long stableRenderedTextures = pipCacheWarmupEnd.renderedTextures()
                    - pipCacheWarmupStart.renderedTextures();
            long stableReusedTextures = pipCacheWarmupEnd.reusedTextures()
                    - pipCacheWarmupStart.reusedTextures();
            if (stableReusedTextures <= stableRenderedTextures) {
                throw new AssertionError("PIP texture cache did not eliminate most unchanged redraws during "
                        + "the stable warmup window: rendered=" + stableRenderedTextures
                        + ", reused=" + stableReusedTextures + ", overall=" + performance);
            }
            if (performance.modelCacheMisses() >= performance.submittedBlocks()) {
                throw new AssertionError("BlockState model cache did not reduce model resolutions: " + performance);
            }
            if (performance.sceneGeometryFlushes() <= 0L) {
                throw new AssertionError("Scene geometry was not flushed before editor line overlays: "
                        + performance);
            }
            if (performance.environmentCompiledSections() <= 0
                    || performance.environmentRenderedSections()
                            <= performance.environmentCompiledSections()) {
                throw new AssertionError("Persistent environment section meshes were not reused: " + performance);
            }
            context.artifacts().write("yuushya-editor-performance.txt", "text/plain",
                    "workload.environmentDiameter=25\n"
                            + "workload.environmentEdgeDither=true\n"
                            + "pip.renderedTextures=" + performance.renderedTextures() + "\n"
                            + "pip.reusedTextures=" + performance.reusedTextures() + "\n"
                            + "pip.stableWindowTicks=" + PIP_CACHE_STABILITY_TICKS + "\n"
                            + "pip.stableRenderedTextures=" + stableRenderedTextures + "\n"
                            + "pip.stableReusedTextures=" + stableReusedTextures + "\n"
                            + "blocks.submitted=" + performance.submittedBlocks() + "\n"
                            + "blocks.frustumCulled=" + performance.frustumCulledBlocks() + "\n"
                            + "models.cacheMisses=" + performance.modelCacheMisses() + "\n"
                            + "render.sceneGeometryFlushes=" + performance.sceneGeometryFlushes() + "\n"
                            + "render.overlaysAfterSceneGeometry=true\n"
                            + "environment.sectionsCompiled="
                            + performance.environmentCompiledSections() + "\n"
                            + "environment.sectionDraws="
                            + performance.environmentRenderedSections() + "\n"
                            + "environment.sectionsFrustumCulled="
                            + performance.environmentFrustumCulledSections() + "\n"
                            + "environment.capturedSections="
                            + environmentPerformance.capturedSections() + "\n"
                            + "environment.captureNanos=" + environmentPerformance.captureNanos() + "\n"
                            + "environment.maxCaptureNanos=" + environmentPerformance.maxCaptureNanos() + "\n"
                            + "environment.maxCaptureSliceNanos="
                            + environmentPerformance.maxCaptureSliceNanos() + "\n"
                            + "environment.maxTickCaptureNanos="
                            + environmentPerformance.maxTickCaptureNanos() + "\n"
                            + "environment.maxAllowedCaptureSliceNanos="
                            + MAX_ENVIRONMENT_CAPTURE_SLICE_NANOS + "\n"
                            + "environment.maxAllowedTickCaptureNanos="
                            + MAX_ENVIRONMENT_CAPTURE_TICK_NANOS + "\n"
                            + "environment.dirtySectionsScheduled="
                            + environmentPerformance.dirtySectionsScheduled() + "\n"
                            + "environment.chunkInvalidations="
                            + environmentPerformance.chunkInvalidations() + "\n"
                            + "environment.eventDrivenInvalidation=true\n"
                            + "cache.verified=true\n");
            context.artifacts().write("yuushya-axis-scale-verification.txt", "text/plain",
                    "initial.position=" + INITIAL_POSITION + "\n"
                            + "applied.position=" + EXPECTED_POSITION + "\n"
                            + "applied.scale=" + EXPECTED_SCALE + "\n"
                            + "axisScale.inspector=x-only\n"
                            + "axisScale.gizmoAxes=x,y,z\n"
                            + "axisScale.otherAxesUnchanged=true\n"
                            + "axisScale.pivotPreserved=true\n"
                            + "axisScale.undoExact=true\n"
                            + "server=" + serverVerification.get() + "\n"
                            + "otherLayersUnchanged=true\nfixtureRemoved=true\n"
                            + "blockPicker.search=minecraft:glass\n"
                            + "blockAddition.serverLayers=4\n"
                            + "blockAddition.position=0.0,0.0,0.0\n"
                            + "blockStateSettings.waterlogged=true\n"
                            + "blockStateSettings.serverVerified=true\n"
                            + "zFightWarning.detected=true\n"
                            + "zFightWarning.optimizeAndSave=true\n"
                            + "itemPicker.preview=minecraft:diamond\n"
                            + "itemSettings.item=minecraft:oak_stairs\n"
                            + "itemSettings.renderMode=block\n"
                            + "itemSettings.blockStateComponent=true\n"
                            + "itemSettings.color=0x7F336699\n"
                            + "itemSettings.serverVerified=true\n"
                            + "itemWorldRender.cacheInvalidated=true\n"
                            + "selection.blankClear=true\n"
                            + "selection.blankDragPreserves=true\n"
                            + "selection.ctrlToggleThroughGizmo=true\n"
                            + "selection.modifierGizmoDrag=true\n"
                            + "gizmo.modifierSwitchDuringDrag=true\n"
                            + "gizmo.controlFinePriority=true\n"
                            + "textEditor.multiline=true\n"
                            + "textEditor.culled=true\n"
                            + "textEditor.mirror=true\n"
                            + "textEditor.addLayer=true\n"
                            + "textEditor.serverVerified=true\n"
                            + "collisionShape.initial=NONE\n"
                            + "collisionShape.applied=FENCE\n"
                            + "collisionShape.serverVerified=true\n"
                            + "environmentPreview.blocks=stone,gold_block\n"
                            + "environmentPreview.diameter=25\n"
                            + "environmentPreview.edgeDither=true\n"
                            + "environmentPreview.retainedBlocks=" + environmentRetainedBlocks + "\n"
                            + "environmentPreview.edgeRetained=" + environmentShellRetained + "/"
                            + environmentShellCandidates + "\n"
                            + "environmentPreview.liveUpdate=emerald_block\n"
                            + "environmentPreview.eventDrivenInvalidation=true\n"
                            + "environmentPreview.modeledBlocks=block,item,text\n"
                            + "camera.initialPosePreserved=true\n"
                            + "layerClipboard.copyPasteUndo=true\n"
                            + "nativeTransfer.blockItemText=true\n"
                            + "exitAutosave.verified=true\n"
                            + "lineWidth.near=" + initialLineWidthScale + "\n"
                            + "lineWidth.far=" + farLineWidthScale + "\n"
                            + "lineWidth.minimumPhysicalPixels=2.0\n"
                            + "render.overlaysAfterSceneGeometry=true\n"
                            + "gizmo.rotateAndScaleVerified=true\n"
                            + renderedLayerVerification);
        }

        @Override
        public void teardown(BenchClientContext context) {
            if (context.minecraft().screen instanceof YuushyaEditorScreen) {
                context.minecraft().setScreen(null);
            }
            if (guiSession != null && guiSession.active()) guiSession.close();
            if (server != null && fixturePos != null && !fixtureRemoved.get()) {
                server.execute(() -> {
                    ServerLevel level = serverLevel();
                    removeFixture(level);
                    fixtureRemoved.set(true);
                });
            }
        }

        private void createFixture(ServerLevel level) {
            try {
                level.setBlockAndUpdate(fixturePos, BlockRegistry.SHOW_BLOCK.get().defaultBlockState()
                        .setValue(YuushyaBlockStates.SHAPES, BlockShape.NONE));
                if (!(level.getBlockEntity(fixturePos) instanceof ShowBlockEntity entity)) {
                    throw new IllegalStateException("Yuushya SHOW_BLOCK did not create ShowBlockEntity at " + fixturePos);
                }
                List<TransformBlockData> layers = entity.getTransformData();
                layers.clear();
                layers.add(targetLayer());
                layers.add(cutoutLayer());
                layers.add(translucentLayer());
                entity.setSlot(0);
                entity.saveChanged();
                createStructuredFixtures(level);
                for (int x = -ENVIRONMENT_PLATFORM_RADIUS; x <= ENVIRONMENT_PLATFORM_RADIUS; x++) {
                    for (int z = -ENVIRONMENT_PLATFORM_RADIUS; z <= ENVIRONMENT_PLATFORM_RADIUS; z++) {
                        level.setBlockAndUpdate(fixturePos.offset(x, -1, z),
                                Blocks.STONE.defaultBlockState());
                    }
                }
                level.setBlockAndUpdate(fixturePos.offset(ENVIRONMENT_GOLD_OFFSET),
                        Blocks.GOLD_BLOCK.defaultBlockState());
                // The void preset leaves the player falling forever. Move the integrated
                // server player onto the fixture platform so the client can finish the
                // multi-phase editor acceptance flow without a death/disconnect reset.
                ServerPlayer player = playerId == null ? null : server.getPlayerList().getPlayer(playerId);
                if (player == null) {
                    throw new IllegalStateException("Integrated-server player is unavailable for fixture setup");
                }
                if (player.isDeadOrDying()) {
                    player.connection.handleClientCommand(new ServerboundClientCommandPacket(
                            ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                    player = server.getPlayerList().getPlayer(playerId);
                    if (player == null) {
                        throw new IllegalStateException("Integrated-server player disappeared during respawn");
                    }
                }
                player.setGameMode(GameType.CREATIVE);
                player.setNoGravity(true);
                player.setInvulnerable(true);
                player.teleportTo(fixturePos.getX() + 0.5D, fixturePos.getY() + 1.0D, fixturePos.getZ() + 0.5D);
                fixtureReady.set(true);
            } catch (Throwable throwable) {
                asyncFailure.compareAndSet(null, throwable);
            }
        }

        private void createStructuredFixtures(ServerLevel level) {
            BlockPos itemPos = fixturePos.offset(ITEM_FIXTURE_OFFSET);
            level.setBlockAndUpdate(itemPos, BlockRegistry.ITEM_BLOCK.get().defaultBlockState()
                    .setValue(YuushyaBlockStates.SHAPES, BlockShape.NONE));
            if (!(level.getBlockEntity(itemPos) instanceof ItemBlockEntity itemEntity)) {
                throw new IllegalStateException("Yuushya ITEM_BLOCK did not create ItemBlockEntity");
            }
            List<TransformItemData> items = itemEntity.getTransformData();
            items.clear();
            items.add(new TransformItemData(new Vector3d(), new Vector3f(), new Vector3f(1.0F),
                    Items.DIAMOND.getDefaultInstance(), 0xFFFFFFFF, true, false));
            items.add(new TransformItemData(new Vector3d(16.0D, 0.0D, 0.0D), new Vector3f(),
                    new Vector3f(1.0F), Items.GOLD_INGOT.getDefaultInstance(), 0xFFFFFFFF, true, false));
            itemEntity.setSlot(0);
            itemEntity.saveChanged();

            BlockPos textPos = fixturePos.offset(TEXT_FIXTURE_OFFSET);
            level.setBlockAndUpdate(textPos, BlockRegistry.TEXT_BLOCK.get().defaultBlockState()
                    .setValue(YuushyaBlockStates.SHAPES, BlockShape.NONE));
            if (!(level.getBlockEntity(textPos) instanceof TextBlockEntity textEntity)) {
                throw new IllegalStateException("Yuushya TEXT_BLOCK did not create TextBlockEntity");
            }
            List<TransformTextData> texts = textEntity.getTransformData();
            texts.clear();
            texts.add(new TransformTextData(new Vector3d(), new Vector3f(), new Vector3f(1.0F),
                    List.of("\"Bench Text\""), false, false, true));
            textEntity.setSlot(0);
            textEntity.saveChanged();

            BlockPos showPos = fixturePos.offset(SHOW_FIXTURE_OFFSET);
            level.setBlockAndUpdate(showPos, BlockRegistry.SHOW_BLOCK.get().defaultBlockState()
                    .setValue(YuushyaBlockStates.SHAPES, BlockShape.NONE));
            if (!(level.getBlockEntity(showPos) instanceof ShowBlockEntity showEntity)) {
                throw new IllegalStateException("Yuushya neighbor SHOW_BLOCK did not create ShowBlockEntity");
            }
            showEntity.getTransformData().clear();
            showEntity.getTransformData().add(new TransformBlockData(new Vector3d(), new Vector3f(),
                    new Vector3f(1.0F), Blocks.GLASS.defaultBlockState(), true));
            showEntity.setSlot(0);
            showEntity.saveChanged();
        }

        private void removeFixture(ServerLevel level) {
            level.removeBlock(fixturePos, false);
            level.removeBlock(fixturePos.offset(ITEM_FIXTURE_OFFSET), false);
            level.removeBlock(fixturePos.offset(TEXT_FIXTURE_OFFSET), false);
            level.removeBlock(fixturePos.offset(SHOW_FIXTURE_OFFSET), false);
            for (int x = -ENVIRONMENT_PLATFORM_RADIUS; x <= ENVIRONMENT_PLATFORM_RADIUS; x++) {
                for (int z = -ENVIRONMENT_PLATFORM_RADIUS; z <= ENVIRONMENT_PLATFORM_RADIUS; z++) {
                    level.removeBlock(fixturePos.offset(x, -1, z), false);
                }
            }
            level.removeBlock(fixturePos.offset(ENVIRONMENT_GOLD_OFFSET), false);
        }

        private void pollServerUpdate() {
            if (!serverCheckPending.compareAndSet(false, true)) return;
            server.execute(() -> {
                try {
                    ServerLevel level = serverLevel();
                    if (!(level.getBlockEntity(fixturePos) instanceof ShowBlockEntity entity)) {
                        throw new AssertionError("ShowBlockEntity disappeared before autosave verification");
                    }
                    List<TransformBlockData> layers = entity.getTransformData();
                    if (layers.size() != 5 || !isApplied(layers.getFirst())
                            || level.getBlockState(fixturePos).getValue(YuushyaBlockStates.SHAPES)
                                    != BlockShape.FENCE) return;
                    assertTargetLayer(layers.getFirst());
                    assertLayerEquals("cutout", editedCutoutLayer(), layers.get(1));
                    assertLayerEquals("translucent", translucentLayer(), layers.get(2));
                    TransformBlockData added = layers.get(3);
                    if (!added.blockState.is(Blocks.GLASS) || !added.isShown
                            || added.scales.distance(new Vector3f(1.0F)) > FLOAT_EPSILON
                            || added.pos.distance(new Vector3d()) > EPSILON) {
                        throw new AssertionError("Added glass layer did not reach the Yuushya server intact");
                    }
                    TransformBlockData optimized = layers.get(4);
                    double optimizedDistance = optimized.pos.distance(new Vector3d());
                    if (!optimized.blockState.is(Blocks.GLASS) || !optimized.isShown
                            || optimized.scales.distance(new Vector3f(1.0F)) > FLOAT_EPSILON
                            || optimizedDistance <= EPSILON || optimizedDistance > 5.0E-3D) {
                        throw new AssertionError("Z-fighting optimization offset is missing or too visible: "
                                + optimized.pos);
                    }
                    serverVerification.set("scaleXYZ=2.0,1.0,1.0;position=4.0,4.0,-2.0;layers=5;"
                            + "added=minecraft:glass@0,0,0;optimizedDuplicate=" + optimized.pos
                            + ";collisionShape=FENCE;autosave=onClose");
                    serverUpdateVerified.set(true);
                } catch (Throwable throwable) {
                    asyncFailure.compareAndSet(null, throwable);
                } finally {
                    serverCheckPending.set(false);
                }
            });
        }

        private void pollStructuredUpdate(boolean text) {
            if (!serverCheckPending.compareAndSet(false, true)) return;
            server.execute(() -> {
                try {
                    if (!text) {
                        if (!(serverLevel().getBlockEntity(fixturePos.offset(ITEM_FIXTURE_OFFSET))
                                instanceof ItemBlockEntity entity)) return;
                        List<TransformItemData> layers = entity.getTransformData();
                        if (layers.isEmpty()) return;
                        TransformItemData edited = layers.getFirst();
                        BlockState itemState = edited.itemStack.get(DataComponentRegistry.BLOCKSTATE.get());
                        if (!edited.itemStack.is(Items.OAK_STAIRS) || !edited.enableBlock
                                || edited.color != 0x7F336699 || itemState == null
                                || !itemState.is(Blocks.OAK_STAIRS)
                                || itemState.getValue(BlockStateProperties.HORIZONTAL_FACING)
                                        == Blocks.OAK_STAIRS.defaultBlockState()
                                                .getValue(BlockStateProperties.HORIZONTAL_FACING)) return;
                        if (layers.stream().skip(1).anyMatch(layer -> !layer.itemStack.isEmpty())) return;
                        itemServerVerified.set(true);
                    } else {
                        if (!(serverLevel().getBlockEntity(fixturePos.offset(TEXT_FIXTURE_OFFSET))
                                instanceof TextBlockEntity entity)) return;
                        List<TransformTextData> layers = entity.getTransformData();
                        if (layers.size() != 2
                                || !layers.getFirst().textLines.equals(
                                        List.of("\"Bench Updated\"", "\"Second Line\""))
                                || !layers.getFirst().isCulled || !layers.getFirst().isMirror
                                || !layers.get(1).textLines.equals(List.of("\"Bench Added\""))
                                || layers.get(1).isCulled || layers.get(1).isMirror
                                || !layers.get(1).isShown
                                || layers.get(1).pos.distance(new Vector3d()) > EPSILON
                                || layers.get(1).scales.distance(new Vector3f(1.0F)) > FLOAT_EPSILON) return;
                        textServerVerified.set(true);
                        removeFixture(serverLevel());
                        fixtureRemoved.set(true);
                    }
                } catch (Throwable throwable) {
                    asyncFailure.compareAndSet(null, throwable);
                } finally {
                    serverCheckPending.set(false);
                }
            });
        }

        private ServerLevel serverLevel() {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) throw new IllegalStateException("Integrated server dimension is unavailable: " + dimension);
            return level;
        }

        private void closeGuiSession() {
            if (guiSession == null) return;
            guiSession.close();
            guiSession = null;
        }

        private ShowBlockEntity clientFixture(BenchClientContext context) {
            return context.level().getBlockEntity(fixturePos) instanceof ShowBlockEntity entity ? entity : null;
        }

        private ItemBlockEntity clientItemFixture(BenchClientContext context) {
            return context.level().getBlockEntity(fixturePos.offset(ITEM_FIXTURE_OFFSET))
                    instanceof ItemBlockEntity entity ? entity : null;
        }

        private TextBlockEntity clientTextFixture(BenchClientContext context) {
            return context.level().getBlockEntity(fixturePos.offset(TEXT_FIXTURE_OFFSET))
                    instanceof TextBlockEntity entity ? entity : null;
        }

        private boolean environmentFixtureReachedClient(BenchClientContext context) {
            return context.level().getBlockState(fixturePos.offset(ENVIRONMENT_STONE_OFFSET)).is(Blocks.STONE)
                    && context.level().getBlockState(fixturePos.offset(ENVIRONMENT_GOLD_OFFSET))
                            .is(Blocks.GOLD_BLOCK)
                    && context.level().getBlockEntity(fixturePos.offset(SHOW_FIXTURE_OFFSET))
                            instanceof ShowBlockEntity
                    && context.level().getBlockEntity(fixturePos.offset(ITEM_FIXTURE_OFFSET))
                            instanceof ItemBlockEntity
                    && context.level().getBlockEntity(fixturePos.offset(TEXT_FIXTURE_OFFSET))
                            instanceof TextBlockEntity;
        }

        private static boolean fixtureReachedClient(ShowBlockEntity entity) {
            List<TransformBlockData> layers = entity.getTransformData();
            return layers.size() == 3
                    && layers.getFirst().blockState.is(Blocks.STONE)
                    && layers.get(1).blockState.is(Blocks.OAK_LEAVES)
                    && layers.get(2).blockState.is(Blocks.HONEY_BLOCK)
                    && entity.getBlockState().getValue(YuushyaBlockStates.SHAPES) == BlockShape.NONE;
        }

        private static TransformBlockData targetLayer() {
            return new TransformBlockData(new Vector3d(INITIAL_POSITION), new Vector3f(INITIAL_ROTATION),
                    new Vector3f(1.0F, 1.0F, 1.0F), Blocks.STONE.defaultBlockState(), true);
        }

        private static TransformBlockData cutoutLayer() {
            return new TransformBlockData(new Vector3d(-48.0D, 0.0D, 0.0D),
                    new Vector3f(0.0F, 45.0F, 0.0F), new Vector3f(0.75F, 1.25F, 0.5F),
                    Blocks.OAK_LEAVES.defaultBlockState(), true);
        }

        private static TransformBlockData editedCutoutLayer() {
            TransformBlockData layer = cutoutLayer();
            layer.blockState = layer.blockState.setValue(BlockStateProperties.WATERLOGGED, true);
            return layer;
        }

        private static TransformBlockData translucentLayer() {
            return new TransformBlockData(new Vector3d(32.0D, 0.0D, 0.0D),
                    new Vector3f(15.0F, 0.0F, -20.0F), new Vector3f(1.5F, 1.5F, 1.5F),
                    Blocks.HONEY_BLOCK.defaultBlockState(), true);
        }

        private static boolean isApplied(TransformBlockData actual) {
            return close(actual.pos, EXPECTED_POSITION)
                    && close(actual.scales, EXPECTED_SCALE);
        }

        private static void assertTargetLayer(TransformBlockData actual) {
            if (!close(actual.pos, EXPECTED_POSITION)) {
                throw new AssertionError("Position compensation mismatch: " + actual.pos);
            }
            if (!close(actual.scales, EXPECTED_SCALE)) {
                throw new AssertionError("Independent X scale changed Y/Z or was not saved: " + actual.scales);
            }
            if (!close(actual.rot, INITIAL_ROTATION) || !actual.blockState.is(Blocks.STONE) || !actual.isShown) {
                throw new AssertionError("Apply changed an unrelated target-layer property");
            }
        }

        private static void assertLayerEquals(String name, TransformBlockData expected, TransformBlockData actual) {
            if (!close(actual.pos, expected.pos) || !close(actual.rot, expected.rot)
                    || !close(actual.scales, expected.scales)
                    || !actual.blockState.equals(expected.blockState) || actual.isShown != expected.isShown) {
                throw new AssertionError(name + " layer changed unexpectedly");
            }
        }

        private void assertFieldValue(int index, double expected) {
            double actual = Double.parseDouble(transformBoxes[index].getValue());
            if (Math.abs(actual - expected) > EPSILON) {
                throw new AssertionError("Inspector field " + index + " expected " + expected + " but was " + actual);
            }
        }

        private static void verifySnapshot(BenchScreenSnapshot snapshot) {
            List<com.zhongbai233.bench.api.client.gui.BenchGuiNode> nodes = new ArrayList<>();
            for (com.zhongbai233.bench.api.client.gui.BenchGuiNode node : snapshot.flattened()) {
                if (node.visible()) nodes.add(node);
            }
            long editBoxes = nodes.stream().filter(node -> "edit-box".equals(node.role())).count();
            if (editBoxes != 9) throw new AssertionError("GUI snapshot expected 9 EditBoxes, found " + editBoxes);
            long ncpbThemeButtons = nodes.stream()
                    .filter(node -> BlackGoldButton.class.getName().equals(node.className())).count();
            if (ncpbThemeButtons < 10) {
                throw new AssertionError("NCPB editor theme unexpectedly missing: themed buttons="
                        + ncpbThemeButtons);
            }
            if (!snapshot.diagnostics().isEmpty()) {
                throw new AssertionError("GUI snapshot diagnostics: " + snapshot.diagnostics());
            }
            if (!SCALE_X.semanticName().equals(
                    com.zhongbai233.bench.api.client.gui.BenchGuiSelectors
                            .select(snapshot, SCALE_X).requireMatch().semanticName())) {
                throw new AssertionError("X-axis scale semantic selector did not resolve exactly");
            }
            for (var node : nodes) {
                var bounds = node.bounds();
                if (bounds.x() < 0 || bounds.y() < 0
                        || bounds.x() + bounds.width() > snapshot.guiWidth()
                        || bounds.y() + bounds.height() > snapshot.guiHeight()) {
                    throw new AssertionError("GUI node is outside the logical viewport: " + node.path()
                            + " " + bounds + " viewport=" + snapshot.guiWidth() + "x" + snapshot.guiHeight());
                }
            }
            for (int first = 0; first < nodes.size(); first++) {
                for (int second = first + 1; second < nodes.size(); second++) {
                    var a = nodes.get(first);
                    var b = nodes.get(second);
                    if (overlaps(a.bounds(), b.bounds())) {
                        throw new AssertionError("GUI nodes overlap: " + a.path() + " " + a.bounds()
                                + " and " + b.path() + " " + b.bounds());
                    }
                }
            }
        }

        private static boolean overlaps(com.zhongbai233.bench.api.client.gui.BenchGuiRectangle first,
                                        com.zhongbai233.bench.api.client.gui.BenchGuiRectangle second) {
            return first.x() < second.x() + second.width()
                    && second.x() < first.x() + first.width()
                    && first.y() < second.y() + second.height()
                    && second.y() < first.y() + first.height();
        }

        private static String formatSnapshot(BenchScreenSnapshot snapshot) {
            StringBuilder output = new StringBuilder();
            output.append("screen=").append(snapshot.screenClassName()).append('\n')
                    .append("title=").append(snapshot.title()).append('\n')
                    .append("gui=").append(snapshot.guiWidth()).append('x').append(snapshot.guiHeight()).append('\n')
                    .append("framebuffer=").append(snapshot.framebufferWidth()).append('x')
                    .append(snapshot.framebufferHeight()).append('\n');
            snapshot.flattened().forEach(node -> output.append(node.path()).append(" role=")
                    .append(node.role()).append(" class=").append(node.className())
                    .append(" semantic=").append(node.semanticName()).append(" text=")
                    .append(node.text()).append(" bounds=").append(node.bounds()).append('\n'));
            return output.toString();
        }

        private static EditBox[] transformBoxes(YuushyaEditorScreen screen) throws ReflectiveOperationException {
            Field field = YuushyaEditorScreen.class.getDeclaredField("transformBoxes");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot access editor inspector fields");
            return ((EditBox[]) field.get(screen)).clone();
        }

        private static void assertEditorType(YuushyaEditorScreen screen, EditorType expected)
                throws ReflectiveOperationException {
            Field field = YuushyaEditorScreen.class.getDeclaredField("host");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot inspect editor host");
            Object value = field.get(screen);
            Method method = value.getClass().getMethod("editorType");
            Object actual = method.invoke(value);
            if (actual != expected) {
                throw new AssertionError("Expected " + expected + " editor, found " + actual);
            }
        }

        private static BlackGoldButton collisionShapeButton(YuushyaEditorScreen screen)
                throws ReflectiveOperationException {
            Field field = YuushyaEditorScreen.class.getDeclaredField("collisionShapeButton");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot access collision-shape control");
            Object value = field.get(screen);
            if (!(value instanceof BlackGoldButton button)) {
                throw new IllegalStateException("Collision-shape control has an unexpected type");
            }
            return button;
        }

        private static void assertDraftCollisionShape(YuushyaEditorScreen screen,
                CollisionShape.Kind expected) throws ReflectiveOperationException {
            Field field = YuushyaEditorScreen.class.getDeclaredField("draft");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot access editor draft");
            Object value = field.get(screen);
            if (!(value instanceof com.zhongbai233.yuushya_editor.core.SceneDocument<?> document)) {
                throw new IllegalStateException("Editor draft has an unexpected type");
            }
            if (document.collisionShape().kind() != expected) {
                throw new AssertionError("Collision-shape control expected " + expected
                        + " but selected " + document.collisionShape().kind());
            }
        }

        private static EditorCameraState currentCamera(YuushyaEditorScreen screen) throws ReflectiveOperationException {
            Field field = YuushyaEditorScreen.class.getDeclaredField("camera");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot access editor camera");
            return (EditorCameraState) field.get(screen);
        }

        private static void setStableGizmoCamera(YuushyaEditorScreen screen)
                throws ReflectiveOperationException {
            EditorCameraState focused = currentCamera(screen);
            Vector3d focus = focused.focus();
            EditorCameraState stable = EditorCameraState.lookingAt(focused.mode(),
                    new Vector3d(focus).add(7.0D, 5.5D, 9.0D), focus,
                    new Vector3d(0.0D, 1.0D, 0.0D), focused.fovDegrees(), focused.orthoScale(),
                    focused.nearPlane(), focused.farPlane());
            Field field = YuushyaEditorScreen.class.getDeclaredField("camera");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot set editor test camera");
            field.set(screen, stable);
        }

        private static void assertInitialCamera(YuushyaEditorScreen screen, Vector3d expectedPosition,
                Quaternionf expectedRotation, float expectedFov) throws ReflectiveOperationException {
            EditorCameraState actual = currentCamera(screen);
            if (actual.position().distance(expectedPosition) > EPSILON) {
                throw new AssertionError("Editor did not preserve the entering camera position: expected "
                        + expectedPosition + ", found " + actual.position());
            }
            float rotationDot = Math.abs(actual.orientation().dot(expectedRotation));
            if (1.0F - rotationDot > 1.0E-5F) {
                throw new AssertionError("Editor did not preserve the entering camera direction: dot="
                        + rotationDot);
            }
            if (Math.abs(actual.fovDegrees() - Math.clamp(expectedFov, 2.0F, 178.0F)) > FLOAT_EPSILON) {
                throw new AssertionError("Editor did not preserve the entering camera FOV");
            }
        }

        private static void verifyLayerClipboard(YuushyaEditorScreen screen) throws Exception {
            SceneDocument<Object> before = draft(screen);
            SceneLayer<Object> source = before.selectedLayer().orElseThrow();
            invokePrivate(screen, "copySelectedLayer", new Class<?>[0]);
            invokePrivate(screen, "pasteLayer", new Class<?>[0]);
            SceneDocument<Object> pasted = draft(screen);
            if (pasted.layers().size() != before.layers().size() + 1) {
                throw new AssertionError("Layer paste did not append exactly one layer");
            }
            SceneLayer<Object> copy = pasted.selectedLayer().orElseThrow();
            if (copy.id().equals(source.id()) || !copy.hostData().equals(source.hostData())
                    || !copy.transform().equals(source.transform()) || copy.visible() != source.visible()) {
                throw new AssertionError("Layer paste did not preserve data with a fresh identity");
            }
            boolean hierarchyUpdated = screen.children().stream()
                    .filter(AbstractWidget.class::isInstance)
                    .map(AbstractWidget.class::cast)
                    .anyMatch(widget -> widget.getMessage().getString().contains(copy.name()));
            if (!hierarchyUpdated) {
                throw new AssertionError("Layer paste did not immediately rebuild the hierarchy rows");
            }
            invokePrivate(screen, "undo", new Class<?>[0]);
            if (!draft(screen).equals(before)) {
                throw new AssertionError("Undo did not exactly restore the document after layer paste");
            }
            boolean staleCopyRow = screen.children().stream()
                    .filter(AbstractWidget.class::isInstance)
                    .map(AbstractWidget.class::cast)
                    .anyMatch(widget -> widget.getMessage().getString().contains(copy.name()));
            boolean restoredSourceRow = screen.children().stream()
                    .filter(AbstractWidget.class::isInstance)
                    .map(AbstractWidget.class::cast)
                    .anyMatch(widget -> widget.getMessage().getString().contains(source.name()));
            if (staleCopyRow || !restoredSourceRow) {
                throw new AssertionError("Undo did not refresh the hierarchy rows and scroll position");
            }
        }

        @SuppressWarnings("unchecked")
        private static void verifyNativeRoundTrip(YuushyaEditorScreen screen) throws Exception {
            Field hostField = YuushyaEditorScreen.class.getDeclaredField("host");
            if (!hostField.trySetAccessible()) throw new IllegalStateException("Cannot inspect editor host");
            YuushyaEditorHost<Object> host = (YuushyaEditorHost<Object>) hostField.get(screen);
            SceneDocument<Object> before = draft(screen);
            String serialized = host.exportDocument(before);
            if (serialized.isBlank()) throw new AssertionError("Yuushya native export returned no data");
            SceneDocument<Object> imported = host.importDocument(serialized, before);
            if (imported.layers().size() != before.layers().size()
                    || !imported.collisionShape().equals(before.collisionShape())) {
                throw new AssertionError("Yuushya native round trip changed document structure");
            }
            for (int slot = 0; slot < before.layers().size(); slot++) {
                SceneLayer<Object> expected = before.layers().get(slot);
                SceneLayer<Object> actual = imported.layers().get(slot);
                if (!actual.hostData().equals(expected.hostData())
                        || !closeTransform(actual.transform(), expected.transform())
                        || actual.visible() != expected.visible()) {
                    throw new AssertionError("Yuushya native round trip changed layer " + slot);
                }
            }
        }

        private static boolean closeTransform(EditorTransform first, EditorTransform second) {
            return first.position().distance(second.position()) <= EPSILON
                    && first.scale().distance(second.scale()) <= FLOAT_EPSILON
                    && 1.0F - Math.abs(first.rotation().dot(second.rotation())) <= 1.0E-5F;
        }

        @SuppressWarnings("unchecked")
        private static SceneDocument<Object> draft(YuushyaEditorScreen screen)
                throws ReflectiveOperationException {
            Field field = YuushyaEditorScreen.class.getDeclaredField("draft");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot access editor draft");
            return (SceneDocument<Object>) field.get(screen);
        }

        private static void assertEditedBlockItem(YuushyaEditorScreen screen) throws Exception {
            SceneDocument<Object> document = draft(screen);
            if (document.layers().size() != 2
                    || !(document.layers().getFirst().hostData() instanceof ItemModelData data)) {
                throw new AssertionError("Item settings changed the document structure unexpectedly");
            }
            if (!data.itemStack().is(Items.OAK_STAIRS) || !data.enableBlock()
                    || data.color() != 0x7F336699 || data.blockState() == null
                    || !data.blockState().is(Blocks.OAK_STAIRS)
                    || data.blockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                            == Blocks.OAK_STAIRS.defaultBlockState()
                                    .getValue(BlockStateProperties.HORIZONTAL_FACING)) {
                throw new AssertionError("Item settings did not preserve item/block-state/colour edits: " + data);
            }
            Object content = invokePrivate(screen, "previewContent", new Class<?>[] {Object.class}, data);
            if (!(content instanceof com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewLayer.BlockContent block)
                    || block.centerOnPivot()) {
                throw new AssertionError("Block-mode item preview did not use native item-block coordinates");
            }
            BlockState stackState = data.itemStack().get(DataComponentRegistry.BLOCKSTATE.get());
            if (!data.blockState().equals(stackState)) {
                throw new AssertionError("Edited block state was not stored in Yuushya's item component");
            }
        }

        private static void assertEditedBlockState(YuushyaEditorScreen screen) throws Exception {
            SceneDocument<Object> document = draft(screen);
            if (document.layers().size() != 3
                    || !(document.layers().get(1).hostData() instanceof BlockState state)
                    || !state.is(Blocks.OAK_LEAVES)
                    || !state.getValue(BlockStateProperties.WATERLOGGED)) {
                throw new AssertionError("Block-state menu did not update waterlogged: "
                        + document.layers().get(1).hostData());
            }
        }

        private static void setTextEditorValue(TextContentEditorScreen screen, String value)
                throws ReflectiveOperationException {
            Field field = TextContentEditorScreen.class.getDeclaredField("editor");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot access text content field");
            ((MultiLineEditBox) field.get(screen)).setValue(value);
        }

        private static void pressTextOption(TextContentEditorScreen screen, String fieldName)
                throws ReflectiveOperationException {
            Field field = TextContentEditorScreen.class.getDeclaredField(fieldName);
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot access " + fieldName);
            BlackGoldButton button = (BlackGoldButton) field.get(screen);
            button.onPress(new MouseButtonEvent(button.getX() + button.getWidth() * 0.5D,
                    button.getY() + button.getHeight() * 0.5D,
                    new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)));
        }

        private static void assertEditedTextLayer(YuushyaEditorScreen screen) throws Exception {
            SceneDocument<Object> document = draft(screen);
            if (document.layers().size() != 1
                    || !(document.layers().getFirst().hostData() instanceof TextModelData data)
                    || !data.textLines().equals(List.of("\"Bench Updated\"", "\"Second Line\""))
                    || !data.culled() || !data.mirror()) {
                throw new AssertionError("Text editor did not retain multiline/culling/mirroring edits: "
                        + document.layers());
            }
        }

        private static void assertAddedTextLayer(YuushyaEditorScreen screen) throws Exception {
            SceneDocument<Object> document = draft(screen);
            if (document.layers().size() != 2
                    || !(document.layers().get(1).hostData() instanceof TextModelData data)
                    || !data.textLines().equals(List.of("\"Bench Added\""))
                    || data.culled() || data.mirror()
                    || !document.layers().get(1).transform().equals(EditorTransform.IDENTITY)
                    || !document.layers().get(1).visible()) {
                throw new AssertionError("New text layer did not use the expected defaults: "
                        + document.layers());
            }
        }

        private static CameraFrame currentCameraFrame(YuushyaEditorScreen screen) throws Exception {
            return (CameraFrame) invokePrivate(screen, "currentCameraFrame", new Class<?>[0]);
        }

        private static Object invokePrivate(Object target, String name, Class<?>[] parameterTypes,
                                            Object... arguments) throws Exception {
            Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
            if (!method.trySetAccessible()) throw new IllegalStateException("Cannot access editor method " + name);
            try {
                return method.invoke(target, arguments);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof Exception checked) throw checked;
                if (cause instanceof Error error) throw error;
                throw exception;
            }
        }

        private static void filterBlockPicker(BlockPickerScreen picker, String query) throws Exception {
            Field searchField = BlockPickerScreen.class.getDeclaredField("searchBox");
            if (!searchField.trySetAccessible()) throw new IllegalStateException("Cannot access block picker search");
            EditBox search = (EditBox) searchField.get(picker);
            search.setValue(query);

            Field filteredField = BlockPickerScreen.class.getDeclaredField("filteredBlocks");
            if (!filteredField.trySetAccessible()) {
                throw new IllegalStateException("Cannot access filtered block results");
            }
            List<?> filtered = (List<?>) filteredField.get(picker);
            if (filtered.isEmpty()) throw new AssertionError("Block picker search returned no glass block");
            Method id = filtered.getFirst().getClass().getDeclaredMethod("id");
            if (!id.trySetAccessible()) throw new IllegalStateException("Cannot inspect block picker entry");
            if (!"minecraft:glass".equals(id.invoke(filtered.getFirst()))) {
                throw new AssertionError("Block picker search did not prioritize exact minecraft:glass");
            }
        }

        private static void chooseFirstFilteredBlock(BlockPickerScreen picker) throws Exception {
            Field filteredField = BlockPickerScreen.class.getDeclaredField("filteredBlocks");
            if (!filteredField.trySetAccessible()) {
                throw new IllegalStateException("Cannot access filtered block results");
            }
            List<?> filtered = (List<?>) filteredField.get(picker);
            if (filtered.isEmpty()) throw new AssertionError("No filtered block is available to choose");
            Method choose = Arrays.stream(BlockPickerScreen.class.getDeclaredMethods())
                    .filter(method -> method.getName().equals("choose") && method.getParameterCount() == 1)
                    .findFirst().orElseThrow();
            if (!choose.trySetAccessible()) throw new IllegalStateException("Cannot invoke block picker choice");
            try {
                choose.invoke(picker, filtered.getFirst());
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof Exception checked) throw checked;
                if (cause instanceof Error error) throw error;
                throw exception;
            }
        }

        private static void assertGlassLayerAdded(YuushyaEditorScreen screen) throws Exception {
            Field field = YuushyaEditorScreen.class.getDeclaredField("draft");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot access editor draft");
            Object value = field.get(screen);
            if (!(value instanceof com.zhongbai233.yuushya_editor.core.SceneDocument<?> document)) {
                throw new IllegalStateException("Editor draft has an unexpected type");
            }
            if (document.layers().size() != 4 || document.selectedLayer().isEmpty()) {
                throw new AssertionError("Block picker did not append and select a fourth layer");
            }
            Object hostData = document.selectedLayer().orElseThrow().hostData();
            if (!(hostData instanceof BlockState state) || !state.is(Blocks.GLASS)) {
                throw new AssertionError("Block picker appended the wrong BlockState: " + hostData);
            }
            if (document.selectedLayer().orElseThrow().transform().position().distance(new Vector3d()) > EPSILON) {
                throw new AssertionError("Block picker did not create the new layer at model origin");
            }
        }

        private static void assertDuplicateGlassLayers(YuushyaEditorScreen screen) throws Exception {
            Field field = YuushyaEditorScreen.class.getDeclaredField("draft");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot access editor draft");
            Object value = field.get(screen);
            if (!(value instanceof com.zhongbai233.yuushya_editor.core.SceneDocument<?> document)
                    || document.layers().size() != 5) {
                throw new AssertionError("Z-fighting fixture did not append a fifth scene layer");
            }
            for (int index : new int[] {3, 4}) {
                var layer = document.layers().get(index);
                if (!(layer.hostData() instanceof BlockState state) || !state.is(Blocks.GLASS)
                        || layer.transform().position().distance(new Vector3d()) > EPSILON) {
                    throw new AssertionError("Z-fighting fixture layer " + index
                            + " is not an origin-aligned glass block");
                }
            }
        }

        private static void pressFirstWarningButton(ZFightWarningScreen warning) {
            BlackGoldButton optimize = null;
            for (var child : warning.children()) {
                if (child instanceof BlackGoldButton button
                        && (optimize == null || button.getX() < optimize.getX())) {
                    optimize = button;
                }
            }
            if (optimize == null) {
                throw new AssertionError("Z-fighting warning has no action buttons");
            }
            optimize.onPress(new MouseButtonEvent(optimize.getX() + optimize.getWidth() * 0.5D,
                    optimize.getY() + optimize.getHeight() * 0.5D,
                    new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)));
        }

        private static void filterItemPicker(ItemPickerScreen picker, String query) throws Exception {
            Field searchField = ItemPickerScreen.class.getDeclaredField("searchBox");
            if (!searchField.trySetAccessible()) throw new IllegalStateException("Cannot access item picker search");
            ((EditBox) searchField.get(picker)).setValue(query);

            List<?> filtered = filteredItemEntries(picker);
            if (filtered.isEmpty()) throw new AssertionError("Item picker search returned no diamond item");
            Method id = filtered.getFirst().getClass().getDeclaredMethod("id");
            if (!id.trySetAccessible()) throw new IllegalStateException("Cannot inspect item picker entry");
            if (!"minecraft:diamond".equals(id.invoke(filtered.getFirst()))) {
                throw new AssertionError("Item picker search did not prioritize exact minecraft:diamond");
            }
        }

        private static void assertItemPickerPreview(ItemPickerScreen picker) throws Exception {
            Object entry = invokePrivate(picker, "previewEntry",
                    new Class<?>[] {double.class, double.class}, -1.0D, -1.0D);
            if (entry == null) throw new AssertionError("Item picker preview has no candidate");
            Method stackMethod = entry.getClass().getDeclaredMethod("stack");
            if (!stackMethod.trySetAccessible()) {
                throw new IllegalStateException("Cannot inspect item picker preview candidate");
            }
            Object stack = stackMethod.invoke(entry);
            if (!(stack instanceof ItemStack itemStack) || !itemStack.is(Items.DIAMOND)) {
                throw new AssertionError("Item picker preview did not follow the filtered diamond candidate");
            }
        }

        private static List<?> filteredItemEntries(ItemPickerScreen picker) throws ReflectiveOperationException {
            Field filteredField = ItemPickerScreen.class.getDeclaredField("filteredItems");
            if (!filteredField.trySetAccessible()) {
                throw new IllegalStateException("Cannot access filtered item results");
            }
            return (List<?>) filteredField.get(picker);
        }

        private static EnvironmentPreviewFrame environmentPreview(YuushyaEditorScreen screen) throws Exception {
            Field field = YuushyaEditorScreen.class.getDeclaredField("environmentFrame");
            if (!field.trySetAccessible()) {
                throw new IllegalStateException("Cannot access editor environment preview frame");
            }
            return (EnvironmentPreviewFrame) field.get(screen);
        }

        private static EnvironmentPreviewManager environmentManager(YuushyaEditorScreen screen)
                throws ReflectiveOperationException {
            Field field = YuushyaEditorScreen.class.getDeclaredField("environmentPreview");
            if (!field.trySetAccessible()) {
                throw new IllegalStateException("Cannot access editor environment preview manager");
            }
            return (EnvironmentPreviewManager) field.get(screen);
        }

        private static boolean frameContains(EnvironmentPreviewFrame frame, BlockPos worldPos, Block block) {
            for (var section : frame.sections()) {
                for (var visible : section.blocks()) {
                    if (section.section().minBlockX() + visible.localX() == worldPos.getX()
                            && section.section().minBlockY() + visible.localY() == worldPos.getY()
                            && section.section().minBlockZ() + visible.localZ() == worldPos.getZ()) {
                        return visible.state().is(block);
                    }
                }
            }
            return false;
        }

        private static EnvironmentVerification assertEnvironmentPreview(EnvironmentPreviewFrame frame) {
            Set<BlockPos> retained = new HashSet<>();
            boolean stone = false;
            boolean gold = false;
            for (var section : frame.sections()) {
                for (var block : section.blocks()) {
                    BlockPos pos = new BlockPos(section.section().minBlockX() + block.localX(),
                            section.section().minBlockY() + block.localY(),
                            section.section().minBlockZ() + block.localZ());
                    retained.add(pos);
                    stone |= block.state().is(Blocks.STONE);
                    gold |= block.state().is(Blocks.GOLD_BLOCK);
                }
            }
            int shellCandidates = 0;
            int shellRetained = 0;
            for (int x = -ENVIRONMENT_PLATFORM_RADIUS; x <= ENVIRONMENT_PLATFORM_RADIUS; x++) {
                for (int z = -ENVIRONMENT_PLATFORM_RADIUS; z <= ENVIRONMENT_PLATFORM_RADIUS; z++) {
                    double distance = Math.sqrt(x * x + 1.0D + z * z);
                    if (distance <= 9.5D || distance >= 12.5D) continue;
                    shellCandidates++;
                    if (retained.contains(new BlockPos(frame.originX() + x,
                            frame.originY() - 1, frame.originZ() + z))) shellRetained++;
                }
            }
            if (!stone || !gold || frame.retainedBlocks() < 300) {
                throw new AssertionError("Editor did not capture the dense 25-block environment sphere: "
                        + frame.retainedBlocks());
            }
            if (shellRetained <= 0 || shellRetained >= shellCandidates) {
                throw new AssertionError("Environment edge was not stably dithered: retained="
                        + shellRetained + "/" + shellCandidates);
            }
            boolean modeledBlock = false;
            boolean modeledItem = false;
            boolean modeledText = false;
            for (var modeled : frame.modeledBlocks()) {
                for (SceneLayer<Object> layer : modeled.layers()) {
                    modeledBlock |= layer.hostData() instanceof BlockState;
                    modeledItem |= layer.hostData() instanceof ItemModelData;
                    modeledText |= layer.hostData() instanceof TextModelData;
                }
            }
            if (!modeledBlock || !modeledItem || !modeledText) {
                throw new AssertionError("Environment did not capture neighboring block/item/text models: "
                        + frame.modeledBlocks());
            }
            return new EnvironmentVerification(frame.retainedBlocks(), shellRetained, shellCandidates);
        }

        private record EnvironmentVerification(int retainedBlocks, int shellRetained,
                                               int shellCandidates) { }

        private static void assertBlockPickerPreview(BlockPickerScreen picker) throws Exception {
            Object entry = invokePrivate(picker, "previewEntry",
                    new Class<?>[] {double.class, double.class}, -1.0D, -1.0D);
            if (entry == null) throw new AssertionError("Block picker preview has no candidate");
            Method stateMethod = entry.getClass().getDeclaredMethod("state");
            if (!stateMethod.trySetAccessible()) {
                throw new IllegalStateException("Cannot inspect block picker preview candidate");
            }
            Object state = stateMethod.invoke(entry);
            if (!(state instanceof BlockState blockState) || !blockState.is(Blocks.GLASS)) {
                throw new AssertionError("Block picker preview did not follow the filtered glass candidate");
            }
        }

        private static YuushyaEditorScreen requireEnhancedScreen(BenchClientContext context) {
            if (context.minecraft().screen instanceof YuushyaEditorScreen screen) return screen;
            throw new AssertionError("Enhanced editor is no longer open");
        }

        private static BenchCaptureOptions guiCaptureOptions() {
            // A GUI session already pins the expected screen. World readiness and frame-pacing
            // gates add no image guarantee here and can time out in the void-world fixture.
            return BenchCaptureOptions.immediate();
        }

        private static Path requirePng(CompletableFuture<Path> future) throws Exception {
            Path path;
            try {
                path = future.join();
            } catch (java.util.concurrent.CompletionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof Exception checked) throw checked;
                throw exception;
            }
            if (!Files.isRegularFile(path) || Files.size(path) == 0) {
                throw new AssertionError("Screenshot is missing or empty: " + path);
            }
            return path;
        }

        private static String verifyRenderedLayers(Path screenshot, YuushyaEditorScreen screen,
                                                   BenchScreenSnapshot snapshot) throws Exception {
            if (snapshot == null) throw new AssertionError("GUI snapshot was not retained for visual verification");
            BufferedImage image;
            try {
                image = ImageIO.read(screenshot.toFile());
            } catch (IOException exception) {
                throw new AssertionError("Could not read editor screenshot: " + screenshot, exception);
            }
            if (image == null) throw new AssertionError("Editor screenshot is not a readable PNG: " + screenshot);

            CameraFrame frame = (CameraFrame) invokePrivate(screen, "currentCameraFrame", new Class<?>[0]);
            double opaque = matchingRatio(image, snapshot, frame, appliedTargetLayer(), LayerColor.OPAQUE);
            double cutout = matchingRatio(image, snapshot, frame, cutoutLayer(), LayerColor.CUTOUT);
            double translucent = matchingRatio(image, snapshot, frame, translucentLayer(), LayerColor.TRANSLUCENT);
            requireVisibleRatio("opaque stone", opaque, 0.025D);
            requireVisibleRatio("cutout oak leaves", cutout, 0.004D);
            requireVisibleRatio("translucent honey block", translucent, 0.025D);
            return "render.opaque.ratio=" + opaque + "\n"
                    + "render.cutout.ratio=" + cutout + "\n"
                    + "render.translucent.ratio=" + translucent + "\n"
                    + "render.layersVerified=true\n";
        }

        private void verifySelectionInteractions(YuushyaEditorScreen screen) throws Exception {
            SceneDocument<Object> initial = draft(screen);
            UUID originalPrimary = initial.selectedLayerId();
            UUID secondCandidate = null;
            for (SceneLayer<Object> layer : initial.layers()) {
                if (!layer.id().equals(originalPrimary)) {
                    secondCandidate = layer.id();
                    break;
                }
            }
            if (secondCandidate == null) throw new AssertionError("Selection interaction bench needs a second layer");
            UUID second = secondCandidate;
            String secondName = initial.layers().stream().filter(layer -> layer.id().equals(second))
                    .findFirst().orElseThrow().name();
            ProjectedPoint secondRow = hierarchyLayerPoint(screen, secondName);
            click(screen, secondRow, GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            if (!second.equals(draft(screen).selectedLayerId()) || selectedLayerIds(screen).size() != 2) {
                throw new AssertionError("Primary-button Ctrl-click did not create a two-layer selection");
            }
            click(screen, secondRow, GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            if (!originalPrimary.equals(draft(screen).selectedLayerId())
                    || !selectedLayerIds(screen).equals(Set.of(originalPrimary))) {
                throw new AssertionError("Primary-button Ctrl-click did not remove the second layer");
            }
            click(screen, secondRow, GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            if (!second.equals(draft(screen).selectedLayerId()) || selectedLayerIds(screen).size() != 2) {
                throw new AssertionError("macOS-style Ctrl-click did not create a two-layer selection");
            }

            click(screen, visibleMoveHandle(screen), GLFW.GLFW_MOD_CONTROL);
            if (!originalPrimary.equals(draft(screen).selectedLayerId())
                    || !selectedLayerIds(screen).equals(Set.of(originalPrimary))) {
                throw new AssertionError("Ctrl-click through the Gizmo did not remove the primary layer");
            }

            click(screen, visibleMoveHandle(screen), GLFW.GLFW_MOD_CONTROL);
            if (!originalPrimary.equals(draft(screen).selectedLayerId())
                    || !selectedLayerIds(screen).equals(Set.of(originalPrimary))) {
                throw new AssertionError("Ctrl-click incorrectly cleared the sole selected layer");
            }

            CameraFrame frame = (CameraFrame) invokePrivate(screen, "currentCameraFrame", new Class<?>[0]);
            EditorViewport viewport = frame.viewport();
            double[][] candidates = {
                    {viewport.x() + 2.0D, viewport.y() + 2.0D},
                    {viewport.x() + viewport.width() - 3.0D, viewport.y() + 2.0D},
                    {viewport.x() + 2.0D, viewport.y() + viewport.height() - 3.0D},
                    {viewport.x() + viewport.width() - 3.0D, viewport.y() + viewport.height() - 3.0D}
            };
            boolean cleared = false;
            double[] blankCandidate = null;
            for (double[] candidate : candidates) {
                click(screen, new ProjectedPoint(candidate[0], candidate[1], 0.0D, true, false), 0);
                if (draft(screen).selectedLayerId() == null) {
                    cleared = true;
                    blankCandidate = candidate;
                    break;
                }
            }
            if (!cleared || !selectedLayerIds(screen).isEmpty()) {
                throw new AssertionError("Clicking blank viewport space did not clear the selection");
            }
            invokePrivate(screen, "applySelectionClick",
                    new Class<?>[] {UUID.class, boolean.class, boolean.class}, originalPrimary, false, false);
            invokePrivate(screen, "syncInspector", new Class<?>[0]);

            EditorCameraState beforeBlankDragCamera = currentCamera(screen);
            Set<UUID> beforeBlankDragSelection = selectedLayerIds(screen);
            double towardCenterX = viewport.x() + viewport.width() * 0.5D - blankCandidate[0];
            double towardCenterY = viewport.y() + viewport.height() * 0.5D - blankCandidate[1];
            double towardCenterLength = Math.hypot(towardCenterX, towardCenterY);
            ProjectedPoint blankDragStart = new ProjectedPoint(
                    blankCandidate[0], blankCandidate[1], 0.0D, true, false);
            ProjectedPoint blankDragEnd = new ProjectedPoint(
                    blankCandidate[0] + towardCenterX / towardCenterLength * 24.0D,
                    blankCandidate[1] + towardCenterY / towardCenterLength * 24.0D,
                    0.0D, true, false);
            drag(screen, blankDragStart, blankDragEnd, 0, "blank viewport orbit");
            if (!beforeBlankDragSelection.equals(selectedLayerIds(screen))
                    || !originalPrimary.equals(draft(screen).selectedLayerId())) {
                throw new AssertionError("Dragging blank viewport space changed the layer selection");
            }
            if (beforeBlankDragCamera.equals(currentCamera(screen))) {
                throw new AssertionError("Dragging blank viewport space did not orbit the camera");
            }
            setStableGizmoCamera(screen);

            EditorTransform beforeModifiedDrag = selectedTransform(screen);
            ProjectedPoint modifiedHandle = visibleMoveHandle(screen);
            CameraFrame modifiedFrame = (CameraFrame) invokePrivate(
                    screen, "currentCameraFrame", new Class<?>[0]);
            ProjectedPoint modifiedCenter = EditorProjection.project(
                    BlockPreviewTransform.pivot(beforeModifiedDrag),
                    modifiedFrame.matrices(), modifiedFrame.viewport());
            double handleX = modifiedHandle.screenX() - modifiedCenter.screenX();
            double handleY = modifiedHandle.screenY() - modifiedCenter.screenY();
            double handleLength = Math.hypot(handleX, handleY);
            if (handleLength <= EPSILON) {
                throw new AssertionError("Move Gizmo handle collapsed onto its pivot");
            }
            double screenAxisX = handleX / handleLength;
            double screenAxisY = handleY / handleLength;
            verifyModifierSwitchDuringDrag(screen, modifiedHandle, screenAxisX, screenAxisY);
            if (selectedTransform(screen).equals(beforeModifiedDrag)) {
                throw new AssertionError("Modifier-switching Gizmo drag did not reach the transform logic");
            }
            invokePrivate(screen, "undo", new Class<?>[0]);
            if (!selectedTransform(screen).equals(beforeModifiedDrag)) {
                throw new AssertionError("Undo did not restore the modifier-assisted Gizmo drag");
            }
        }

        private static void verifyModifierSwitchDuringDrag(YuushyaEditorScreen screen,
                ProjectedPoint start, double screenAxisX, double screenAxisY) throws Exception {
            MouseButtonInfo primary = new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0);
            MouseButtonEvent press = new MouseButtonEvent(start.screenX(), start.screenY(), primary);
            if (!screen.mouseClicked(press, false)) {
                throw new AssertionError("Modifier-switching Gizmo did not accept the mouse press");
            }

            double currentDistance = 18.0D;
            ProjectedPoint current = pointAlong(start, screenAxisX, screenAxisY, currentDistance);
            dragStep(screen, start, current, 0, "unmodified move");
            EditorTransform unmodified = selectedTransform(screen);

            screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, GLFW.GLFW_MOD_SHIFT));
            assertBooleanField(screen, "gizmoDragShiftDown", true);
            currentDistance += 7.0D;
            ProjectedPoint shifted = pointAlong(start, screenAxisX, screenAxisY, currentDistance);
            // Deliberately keep the mouse event modifiers at zero: the key event must switch
            // precision even when the drag event retains its button-press modifier snapshot.
            dragStep(screen, current, shifted, 0, "mid-drag Shift move");
            EditorTransform shiftTransform = selectedTransform(screen);
            if (shiftTransform.equals(unmodified)) {
                throw new AssertionError("Pressing Shift during a Gizmo drag did not change its result");
            }

            screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_LEFT_CONTROL, 0,
                    GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL));
            assertBooleanField(screen, "gizmoDragShiftDown", true);
            assertBooleanField(screen, "gizmoDragControlDown", true);
            if (GizmoSnapPolicy.step(GizmoMode.MOVE, true, true)
                    != GizmoSnapPolicy.FINE_MOVE_STEP) {
                throw new AssertionError("Ctrl did not take fine-step priority over Shift");
            }
            currentDistance += 3.0D;
            ProjectedPoint controlled = pointAlong(start, screenAxisX, screenAxisY, currentDistance);
            dragStep(screen, shifted, controlled, 0, "mid-drag Ctrl+Shift move");
            if (selectedTransform(screen).equals(shiftTransform)) {
                throw new AssertionError("Pressing Ctrl during a Shift Gizmo drag did not change its result");
            }

            MouseButtonEvent release = new MouseButtonEvent(controlled.screenX(), controlled.screenY(), primary);
            if (!screen.mouseReleased(release)) {
                throw new AssertionError("Modifier-switching Gizmo did not accept the mouse release");
            }
            screen.keyReleased(new KeyEvent(GLFW.GLFW_KEY_LEFT_CONTROL, 0, GLFW.GLFW_MOD_SHIFT));
            screen.keyReleased(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, 0));
        }

        private static ProjectedPoint pointAlong(ProjectedPoint start, double axisX, double axisY,
                double distance) {
            return new ProjectedPoint(start.screenX() + axisX * distance,
                    start.screenY() + axisY * distance, start.depth(), true, false);
        }

        private static void dragStep(YuushyaEditorScreen screen, ProjectedPoint previous,
                ProjectedPoint next, int modifiers, String context) {
            MouseButtonEvent event = new MouseButtonEvent(next.screenX(), next.screenY(),
                    new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, modifiers));
            if (!screen.mouseDragged(event,
                    next.screenX() - previous.screenX(), next.screenY() - previous.screenY())) {
                throw new AssertionError(context + " was not accepted");
            }
        }

        private static void assertBooleanField(YuushyaEditorScreen screen, String name, boolean expected)
                throws ReflectiveOperationException {
            Field field = YuushyaEditorScreen.class.getDeclaredField(name);
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot inspect " + name);
            if (field.getBoolean(screen) != expected) {
                throw new AssertionError(name + " did not update during the active Gizmo drag");
            }
        }

        private static ProjectedPoint visibleMoveHandle(YuushyaEditorScreen screen) throws Exception {
            EditorTransform transform = selectedTransform(screen);
            CameraFrame frame = (CameraFrame) invokePrivate(screen, "currentCameraFrame", new Class<?>[0]);
            Vector3d pivot = BlockPreviewTransform.pivot(transform);
            double length = GizmoSizingPolicy.calculate(
                    GizmoSizingPolicy.worldBoundingRadius(transform.scale())).axisLength();
            for (GizmoHandle axis : new GizmoHandle[] {GizmoHandle.X, GizmoHandle.Y, GizmoHandle.Z}) {
                for (double direction : new double[] {-1.0D, 1.0D}) {
                    ProjectedPoint projected = EditorProjection.project(
                            GizmoHitTesting.axisEndpoint(pivot, axis, length * direction),
                            frame.matrices(), frame.viewport());
                    boolean coveredByWidget = screen.children().stream()
                            .filter(AbstractWidget.class::isInstance)
                            .map(AbstractWidget.class::cast)
                            .anyMatch(widget -> widget.isMouseOver(projected.screenX(), projected.screenY()));
                    if (projected.visible() && !coveredByWidget) return projected;
                }
            }
            throw new AssertionError("No visible move Gizmo handle is available");
        }

        private static Set<UUID> selectedLayerIds(YuushyaEditorScreen screen)
                throws ReflectiveOperationException {
            Field field = YuushyaEditorScreen.class.getDeclaredField("selectedLayerIds");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot inspect editor selection");
            @SuppressWarnings("unchecked") Set<UUID> value = (Set<UUID>) field.get(screen);
            return Set.copyOf(value);
        }

        private static ProjectedPoint hierarchyLayerPoint(YuushyaEditorScreen screen, String layerName) {
            AbstractWidget row = screen.children().stream()
                    .filter(AbstractWidget.class::isInstance)
                    .map(AbstractWidget.class::cast)
                    .filter(widget -> widget.getMessage().getString().contains(layerName))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No hierarchy row for " + layerName));
            return new ProjectedPoint(row.getX() + row.getWidth() * 0.5D,
                    row.getY() + row.getHeight() * 0.5D, 0.0D, true, false);
        }

        private static void click(YuushyaEditorScreen screen, ProjectedPoint point, int modifiers) {
            click(screen, point, modifiers, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        }

        private static void click(YuushyaEditorScreen screen, ProjectedPoint point, int modifiers,
                int button) {
            MouseButtonInfo primary = new MouseButtonInfo(button, modifiers);
            MouseButtonEvent event = new MouseButtonEvent(point.screenX(), point.screenY(), primary);
            if (!screen.mouseClicked(event, false) || !screen.mouseReleased(event)) {
                throw new AssertionError("Editor did not accept the scripted viewport click");
            }
        }

        private void verifyRotateAndScaleGizmos(YuushyaEditorScreen screen) throws Exception {
            EditorTransform baseline = selectedTransform(screen);
            CameraFrame rotationFrame = (CameraFrame) invokePrivate(
                    screen, "currentCameraFrame", new Class<?>[0]);
            Vector3d pivot = BlockPreviewTransform.pivot(baseline);
            double blockRadius = GizmoSizingPolicy.worldBoundingRadius(baseline.scale());
            GizmoSizingPolicy.Sizes rotationSizes = GizmoSizingPolicy.calculate(blockRadius);
            if (rotationSizes.axisLength() <= blockRadius || rotationSizes.rotationRadius() <= blockRadius) {
                throw new AssertionError("Adaptive Gizmo dimensions remain inside the selected block");
            }

            if (!screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_E, 0, 0))) {
                throw new AssertionError("E did not activate the rotate Gizmo");
            }
            Vector3d startRing = GizmoHitTesting.ringPoint(
                    pivot, GizmoHandle.X, rotationSizes.rotationRadius(), 0.65D);
            Vector3d endRing = GizmoHitTesting.ringPoint(
                    pivot, GizmoHandle.X, rotationSizes.rotationRadius(), 1.05D);
            ProjectedPoint startRotation = EditorProjection.project(
                    startRing, rotationFrame.matrices(), rotationFrame.viewport());
            ProjectedPoint endRotation = EditorProjection.project(
                    endRing, rotationFrame.matrices(), rotationFrame.viewport());
            drag(screen, startRotation, endRotation, 0, "X rotation");
            EditorTransform rotated = selectedTransform(screen);
            if (rotated.rotation().equals(baseline.rotation())) {
                throw new AssertionError("Rotate Gizmo drag did not change the selected rotation");
            }
            invokePrivate(screen, "undo", new Class<?>[0]);
            if (!selectedTransform(screen).equals(baseline)) {
                throw new AssertionError("Undo did not restore the rotate Gizmo transaction exactly");
            }

            if (!screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_R, 0, 0))) {
                throw new AssertionError("R did not activate the axis-scale Gizmo");
            }
            CameraFrame scaleFrame = (CameraFrame) invokePrivate(screen, "currentCameraFrame", new Class<?>[0]);
            ProjectedPoint center = EditorProjection.project(pivot, scaleFrame.matrices(), scaleFrame.viewport());
            if (!center.visible()) throw new AssertionError("Selected pivot is not visible for scale Gizmo test");
            GizmoSizingPolicy.Sizes scaleSizes = GizmoSizingPolicy.calculate(blockRadius);
            double scaleDragPixels = 12.0D;
            for (GizmoHandle axis : new GizmoHandle[] {GizmoHandle.X, GizmoHandle.Y, GizmoHandle.Z}) {
                Vector3d scaleEndpoint = null;
                for (double direction : new double[] {-1.0D, 1.0D}) {
                    Vector3d candidate = GizmoHitTesting.axisEndpoint(
                            pivot, axis, scaleSizes.scaleHandleLength() * direction);
                    if (EditorProjection.project(candidate, scaleFrame.matrices(), scaleFrame.viewport()).visible()) {
                        scaleEndpoint = candidate;
                        break;
                    }
                }
                if (scaleEndpoint == null) {
                    throw new AssertionError(axis + " scale handle is outside the viewport");
                }
                ProjectedPoint startScale = EditorProjection.project(
                        scaleEndpoint, scaleFrame.matrices(), scaleFrame.viewport());
                double screenAxisX = startScale.screenX() - center.screenX();
                double screenAxisY = startScale.screenY() - center.screenY();
                double screenAxisLength = Math.hypot(screenAxisX, screenAxisY);
                if (screenAxisLength <= EPSILON) {
                    throw new AssertionError(axis + " scale handle collapsed onto its pivot");
                }
                ProjectedPoint endScale = new ProjectedPoint(
                        startScale.screenX() + screenAxisX / screenAxisLength * scaleDragPixels,
                        startScale.screenY() + screenAxisY / screenAxisLength * scaleDragPixels,
                        center.depth(), true, false);
                drag(screen, startScale, endScale, 0, axis + " scale");
                EditorTransform scaled = selectedTransform(screen);
                assertOnlyAxisScaled(axis, baseline.scale(), scaled.scale());
                if (BlockPreviewTransform.pivot(scaled).distance(pivot) > EPSILON) {
                    throw new AssertionError(axis + "-axis scale Gizmo moved the visible pivot");
                }
                invokePrivate(screen, "undo", new Class<?>[0]);
                if (!selectedTransform(screen).equals(baseline)) {
                    throw new AssertionError("Undo did not exactly restore the " + axis + " scale transaction");
                }
            }
            assertFieldValue(0, EXPECTED_POSITION.x);
            assertFieldValue(1, EXPECTED_POSITION.y);
            assertFieldValue(2, EXPECTED_POSITION.z);
            assertFieldValue(6, EXPECTED_SCALE.x);
            assertFieldValue(7, EXPECTED_SCALE.y);
            assertFieldValue(8, EXPECTED_SCALE.z);
        }

        private static void assertOnlyAxisScaled(GizmoHandle axis, Vector3f before, Vector3f after) {
            boolean targetGrew = switch (axis) {
                case X -> after.x > before.x + 1.0E-4F;
                case Y -> after.y > before.y + 1.0E-4F;
                case Z -> after.z > before.z + 1.0E-4F;
                default -> false;
            };
            boolean otherAxesUnchanged = (axis == GizmoHandle.X || Math.abs(after.x - before.x) <= 1.0E-4F)
                    && (axis == GizmoHandle.Y || Math.abs(after.y - before.y) <= 1.0E-4F)
                    && (axis == GizmoHandle.Z || Math.abs(after.z - before.z) <= 1.0E-4F);
            if (!targetGrew || !otherAxesUnchanged) {
                throw new AssertionError(axis + "-axis scale changed another axis: before=" + before
                        + ", after=" + after);
            }
        }

        private static void drag(YuushyaEditorScreen screen, ProjectedPoint start, ProjectedPoint end) {
            drag(screen, start, end, 0, "Gizmo");
        }

        private static void drag(YuushyaEditorScreen screen, ProjectedPoint start, ProjectedPoint end,
                int modifiers) {
            drag(screen, start, end, modifiers, "Gizmo");
        }

        private static void drag(YuushyaEditorScreen screen, ProjectedPoint start, ProjectedPoint end,
                int modifiers, String context) {
            if (!start.visible() || !end.visible()) {
                throw new AssertionError(context + " drag endpoints are outside the editor viewport");
            }
            MouseButtonInfo primary = new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, modifiers);
            MouseButtonEvent press = new MouseButtonEvent(start.screenX(), start.screenY(), primary);
            MouseButtonEvent release = new MouseButtonEvent(end.screenX(), end.screenY(), primary);
            if (!screen.mouseClicked(press, false)) {
                throw new AssertionError(context + " Gizmo did not accept the scripted mouse press");
            }
            if (!screen.mouseDragged(release,
                    end.screenX() - start.screenX(), end.screenY() - start.screenY())) {
                throw new AssertionError(context + " Gizmo did not accept the scripted mouse drag");
            }
            if (!screen.mouseReleased(release)) {
                throw new AssertionError(context + " Gizmo did not accept the scripted mouse release");
            }
        }

        private static EditorTransform selectedTransform(YuushyaEditorScreen screen) throws Exception {
            Field field = YuushyaEditorScreen.class.getDeclaredField("draft");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot access editor draft");
            Object value = field.get(screen);
            if (!(value instanceof com.zhongbai233.yuushya_editor.core.SceneDocument<?> document)) {
                throw new IllegalStateException("Editor draft has an unexpected type");
            }
            return document.selectedLayer().orElseThrow().transform();
        }

        private static TransformBlockData appliedTargetLayer() {
            return new TransformBlockData(new Vector3d(EXPECTED_POSITION), new Vector3f(INITIAL_ROTATION),
                    new Vector3f(EXPECTED_SCALE), Blocks.STONE.defaultBlockState(), true);
        }

        private static double matchingRatio(BufferedImage image, BenchScreenSnapshot snapshot,
                                            CameraFrame frame, TransformBlockData layer,
                                            LayerColor expectedColor) {
            EditorTransform transform = new EditorTransform(layer.pos,
                    YuushyaTransformConversion.fromEulerDegrees(layer.rot), layer.scales);
            ProjectedPoint point = EditorProjection.project(BlockPreviewTransform.pivot(transform),
                    frame.matrices(), frame.viewport());
            if (!point.visible()) {
                throw new AssertionError(expectedColor.description + " pivot is outside the editor viewport");
            }

            double scaleX = (double) image.getWidth() / snapshot.guiWidth();
            double scaleY = (double) image.getHeight() / snapshot.guiHeight();
            int centerX = (int) Math.round(point.screenX() * scaleX);
            int centerY = (int) Math.round(point.screenY() * scaleY);
            int radiusX = Math.max(1, (int) Math.ceil(26.0D * scaleX));
            int radiusY = Math.max(1, (int) Math.ceil(26.0D * scaleY));
            int minX = Math.max(0, centerX - radiusX);
            int maxX = Math.min(image.getWidth() - 1, centerX + radiusX);
            int minY = Math.max(0, centerY - radiusY);
            int maxY = Math.min(image.getHeight() - 1, centerY + radiusY);
            long matching = 0L;
            long sampled = 0L;
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    int rgb = image.getRGB(x, y);
                    int red = (rgb >>> 16) & 0xFF;
                    int green = (rgb >>> 8) & 0xFF;
                    int blue = rgb & 0xFF;
                    if (expectedColor.matches(red, green, blue)) matching++;
                    sampled++;
                }
            }
            return sampled == 0L ? 0.0D : (double) matching / sampled;
        }

        private static void requireVisibleRatio(String layer, double actual, double minimum) {
            if (actual < minimum) {
                throw new AssertionError(layer + " was not visibly rendered: matching pixel ratio "
                        + actual + " is below " + minimum);
            }
        }

        private enum LayerColor {
            OPAQUE("opaque stone") {
                @Override boolean matches(int red, int green, int blue) {
                    int brightness = (red + green + blue) / 3;
                    return brightness >= 45 && brightness <= 190
                            && Math.abs(red - green) <= 18 && Math.abs(green - blue) <= 18;
                }
            },
            CUTOUT("cutout oak leaves") {
                @Override boolean matches(int red, int green, int blue) {
                    return green >= 42 && green >= red + 18 && green >= blue + 18;
                }
            },
            TRANSLUCENT("translucent honey block") {
                @Override boolean matches(int red, int green, int blue) {
                    return red >= 80 && green >= 35 && red >= green + 20 && green >= blue + 18;
                }
            };

            private final String description;

            LayerColor(String description) {
                this.description = description;
            }

            abstract boolean matches(int red, int green, int blue);
        }

        private void throwAsyncFailure() {
            Throwable failure = asyncFailure.get();
            if (failure == null) return;
            if (failure instanceof Error error) throw error;
            throw new IllegalStateException("Integrated-server fixture failed", failure);
        }

        private static boolean close(Vector3d actual, Vector3d expected) {
            return actual.distance(expected) <= EPSILON;
        }

        private static boolean close(Vector3f actual, Vector3f expected) {
            return actual.distance(expected) <= FLOAT_EPSILON;
        }
    }
}
