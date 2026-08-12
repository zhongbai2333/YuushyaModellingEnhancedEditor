package com.zhongbai233.yuushya_editor.bench;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import com.yuushya.modelling.registries.BlockRegistry;
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
import com.zhongbai233.yuushya_editor.client.ItemPickerScreen;
import com.zhongbai233.yuushya_editor.client.ZFightWarningScreen;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewPipRenderer;
import com.zhongbai233.yuushya_editor.client.renderer.PreviewLineWidthPolicy;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewFrame;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewManager;
import com.zhongbai233.yuushya_editor.client.widget.BlackGoldButton;
import com.zhongbai233.yuushya_editor.compat.YuushyaTransformConversion;
import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.EditorType;
import com.zhongbai233.yuushya_editor.core.TextModelData;
import com.zhongbai233.yuushya_editor.core.camera.CameraFrame;
import com.zhongbai233.yuushya_editor.core.camera.CameraState;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoHandle;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoHitTesting;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoSizingPolicy;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewTransform;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import com.zhongbai233.yuushya_editor.core.projection.ProjectedPoint;
import com.zhongbai233.yuushya_editor.core.projection.Projection;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/** Unattended integrated-client acceptance test for the real Yuushya show-block editor path. */
public final class YuushyaEditorBenchProvider implements BenchClientProvider {
    private static final String SCENARIO_ID = "yuushya-editor.overall-scale-apply";

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
                "Yuushya editor overall-scale apply",
                Set.of("client", "gui", "yuushya", "editor"),
                Duration.ofSeconds(60)),
                ignored -> new OverallScaleScenario());
    }

    private static final class OverallScaleScenario implements BenchClientScenario {
        private static final double EPSILON = 1.0E-6D;
        private static final float FLOAT_EPSILON = 1.0E-6F;
        private static final Vector3d INITIAL_POSITION = new Vector3d(8.0D, 4.0D, -2.0D);
        private static final Vector3d EXPECTED_POSITION = new Vector3d(4.0D, 2.0D, -1.0D);
        private static final Vector3f INITIAL_ROTATION = new Vector3f(10.0F, 20.0F, 30.0F);
        private static final Vector3f EXPECTED_SCALE = new Vector3f(2.0F, 2.0F, 2.0F);
        private static final BenchGuiSelector OVERALL_SCALE = BenchGuiSelector.semanticName("overall-scale");
        private static final BlockPos ENVIRONMENT_STONE_OFFSET = new BlockPos(3, -1, 2);
        private static final BlockPos ENVIRONMENT_GOLD_OFFSET = new BlockPos(-3, 0, -2);
        private static final int ENVIRONMENT_PLATFORM_RADIUS = 12;
        private static final int PIP_CACHE_STABILITY_TICKS = 8;
        private static final long MAX_ENVIRONMENT_CAPTURE_SLICE_NANOS = 10_000_000L;
        private static final long MAX_ENVIRONMENT_CAPTURE_TICK_NANOS = 12_000_000L;
        private static final BlockPos ITEM_FIXTURE_OFFSET = new BlockPos(5, 0, 0);
        private static final BlockPos TEXT_FIXTURE_OFFSET = new BlockPos(7, 0, 0);

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
        private CompletableFuture<Path> zFightWarningScreenshot;
        private CompletableFuture<Path> itemPickerScreenshot;
        private boolean screenOpened;
        private boolean inspectorCommitted;
        private boolean gizmoInteractionsVerified;
        private boolean collisionShapeChanged;
        private boolean renderedLayersVerified;
        private boolean farCameraRestored;
        private boolean blockPickerOpened;
        private boolean blockAdditionVerified;
        private boolean zFightDuplicateAdded;
        private boolean zFightWarningOpened;
        private boolean autosaveSent;
        private boolean itemEditorOpened;
        private boolean itemPickerOpened;
        private boolean itemPickerReturned;
        private boolean itemDeleteSent;
        private boolean textEditorOpened;
        private boolean textUpdateSent;
        private boolean environmentPreviewVerified;
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
                context.minecraft().setScreen(new ShowBlockScreen(clientEntity, null));
                if (!(context.minecraft().screen instanceof YuushyaEditorScreen enhanced)) {
                    throw new AssertionError("ShowBlockScreen was not intercepted by YuushyaEditorScreen: "
                            + (context.minecraft().screen == null ? "null"
                            : context.minecraft().screen.getClass().getName()));
                }
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                transformBoxes = transformBoxes(enhanced);
                if (transformBoxes.length != 7) {
                    throw new AssertionError("Expected seven inspector fields, found " + transformBoxes.length);
                }
                guiSession.name(transformBoxes[6], "overall-scale");
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
                transformBoxes[6].setValue("2.0");
                Object committed = invokePrivate(screen, "commitInspector",
                        new Class<?>[] {String.class}, "ModBench overall scale");
                if (!Boolean.TRUE.equals(committed)) {
                    throw new AssertionError("Editor rejected the valid overall scale value");
                }
                assertFieldValue(0, EXPECTED_POSITION.x);
                assertFieldValue(1, EXPECTED_POSITION.y);
                assertFieldValue(2, EXPECTED_POSITION.z);
                assertFieldValue(6, EXPECTED_SCALE.x);
                verifySnapshot(guiSession.snapshot());
                inspectorCommitted = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!gizmoInteractionsVerified) {
                verifyRotateAndScaleGizmos(requireEnhancedScreen(context));
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
                BenchCaptureOptions fullOptions = BenchCaptureOptions.defaults()
                        .withHiddenHud(false).withStableFrames(4);
                editorScreenshot = context.automation().captureScreenshot(
                        "yuushya-editor-overall-scale", fullOptions);
                scaleScreenshot = guiSession.captureWidget(
                        "yuushya-editor-overall-scale-control", OVERALL_SCALE,
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
                throw new AssertionError("Rotate and overall-scale Gizmo interactions were not verified");
            }

            if (farLineWidthScreenshot == null) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                CameraFrame frame = currentCameraFrame(screen);
                initialLineWidthScale = PreviewLineWidthPolicy.forCamera(currentCamera(screen));
                screen.mouseScrolled(frame.viewport().x() + frame.viewport().width() * 0.5D,
                        frame.viewport().y() + frame.viewport().height() * 0.5D,
                        0.0D, -8.0D);
                farLineWidthScale = PreviewLineWidthPolicy.forCamera(currentCamera(screen));
                if (!(farLineWidthScale > initialLineWidthScale * 1.5F)) {
                    throw new AssertionError("Pulling the camera away did not visibly increase line width: "
                            + initialLineWidthScale + " -> " + farLineWidthScale);
                }
                farLineWidthScreenshot = context.automation().captureScreenshot(
                        "yuushya-editor-far-line-width", BenchCaptureOptions.defaults()
                                .withHiddenHud(false).withStableFrames(4));
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
                float restored = PreviewLineWidthPolicy.forCamera(currentCamera(screen));
                if (Math.abs(restored - initialLineWidthScale) > 0.02F) {
                    throw new AssertionError("Line-width zoom verification did not restore the camera: "
                            + initialLineWidthScale + " -> " + restored);
                }
                farCameraRestored = true;
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
                        "yuushya-editor-block-picker", BenchCaptureOptions.defaults()
                                .withHiddenHud(false).withStableFrames(4));
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
                        "yuushya-editor-added-glass-selected", BenchCaptureOptions.defaults()
                                .withHiddenHud(false).withStableFrames(4));
                blockAdditionVerified = true;
                return BenchClientStepResult.CONTINUE;
            }

            if (!addedBlockScreenshot.isDone()) return BenchClientStepResult.CONTINUE;
            requirePng(addedBlockScreenshot);

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
                        "yuushya-editor-zfight-warning", BenchCaptureOptions.defaults()
                                .withHiddenHud(false).withStableFrames(4));
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
                        "yuushya-editor-item-picker", BenchCaptureOptions.defaults()
                                .withHiddenHud(false).withStableFrames(4));
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
            if (!itemDeleteSent) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                invokePrivate(screen, "removeSelectedLayer", new Class<?>[0]);
                screen.onClose();
                if (context.minecraft().screen != null) {
                    throw new AssertionError("Item editor delete/autosave did not close");
                }
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
                guiSession = context.automation().beginGuiSession(YuushyaEditorScreen.class);
                textEditorOpened = true;
                return BenchClientStepResult.CONTINUE;
            }
            if (!textUpdateSent) {
                YuushyaEditorScreen screen = requireEnhancedScreen(context);
                invokePrivate(screen, "replaceSelectedContent",
                        new Class<?>[] {Object.class, String.class},
                        new TextModelData(List.of("\"Bench Updated\""), true, true), "ModBench text update");
                screen.onClose();
                if (context.minecraft().screen != null) {
                    throw new AssertionError("Text editor content/autosave did not close");
                }
                closeGuiSession();
                textUpdateSent = true;
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
            requirePng(zFightWarningScreenshot);
            requirePng(itemPickerScreenshot);
            if (!renderedLayersVerified) {
                throw new AssertionError("Opaque/cutout/translucent preview layers were not visually verified");
            }
            if (!blockAdditionVerified) throw new AssertionError("Registry block addition was not verified");
            if (!zFightDuplicateAdded || !zFightWarningOpened) {
                throw new AssertionError("Z-fighting warning and optimization were not verified");
            }
            if (!itemPickerOpened || !itemPickerReturned) {
                throw new AssertionError("Item picker preview flow was not verified");
            }
            if (!environmentPreviewVerified) throw new AssertionError("World environment preview was not verified");
            if (!environmentMutationVerified || environmentPerformance == null) {
                throw new AssertionError("Event-driven environment invalidation was not verified");
            }
            if (!collisionShapeChanged) throw new AssertionError("Collision-shape control was not verified");
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
            context.artifacts().write("yuushya-overall-scale-verification.txt", "text/plain",
                    "initial.position=" + INITIAL_POSITION + "\n"
                            + "applied.position=" + EXPECTED_POSITION + "\n"
                            + "applied.scale=" + EXPECTED_SCALE + "\n"
                            + "server=" + serverVerification.get() + "\n"
                            + "otherLayersUnchanged=true\nfixtureRemoved=true\n"
                            + "blockPicker.search=minecraft:glass\n"
                            + "blockAddition.serverLayers=4\n"
                            + "blockAddition.position=0.0,0.0,0.0\n"
                            + "zFightWarning.detected=true\n"
                            + "zFightWarning.optimizeAndSave=true\n"
                            + "itemPicker.preview=minecraft:diamond\n"
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
                            + "exitAutosave.verified=true\n"
                            + "lineWidth.near=" + initialLineWidthScale + "\n"
                            + "lineWidth.far=" + farLineWidthScale + "\n"
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
        }

        private void removeFixture(ServerLevel level) {
            level.removeBlock(fixturePos, false);
            level.removeBlock(fixturePos.offset(ITEM_FIXTURE_OFFSET), false);
            level.removeBlock(fixturePos.offset(TEXT_FIXTURE_OFFSET), false);
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
                    assertLayerEquals("cutout", cutoutLayer(), layers.get(1));
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
                            || optimizedDistance <= EPSILON || optimizedDistance > 1.0E-4D) {
                        throw new AssertionError("Z-fighting optimization offset is missing or too visible: "
                                + optimized.pos);
                    }
                    serverVerification.set("scaleXYZ=2.0;position=4.0,2.0,-1.0;layers=5;"
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
                        if (layers.isEmpty() || !layers.getFirst().itemStack.is(Items.GOLD_INGOT)) return;
                        if (layers.stream().skip(1).anyMatch(layer -> !layer.itemStack.isEmpty())) return;
                        itemServerVerified.set(true);
                    } else {
                        if (!(serverLevel().getBlockEntity(fixturePos.offset(TEXT_FIXTURE_OFFSET))
                                instanceof TextBlockEntity entity)) return;
                        List<TransformTextData> layers = entity.getTransformData();
                        if (layers.size() != 1 || !layers.getFirst().textLines.equals(List.of("\"Bench Updated\""))
                                || !layers.getFirst().isCulled || !layers.getFirst().isMirror) return;
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
                            .is(Blocks.GOLD_BLOCK);
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
                throw new AssertionError("Scale axes were not synchronized: " + actual.scales);
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
            if (editBoxes != 7) throw new AssertionError("GUI snapshot expected 7 EditBoxes, found " + editBoxes);
            long ncpbThemeButtons = nodes.stream()
                    .filter(node -> BlackGoldButton.class.getName().equals(node.className())).count();
            if (ncpbThemeButtons < 10) {
                throw new AssertionError("NCPB editor theme unexpectedly missing: themed buttons="
                        + ncpbThemeButtons);
            }
            if (!snapshot.diagnostics().isEmpty()) {
                throw new AssertionError("GUI snapshot diagnostics: " + snapshot.diagnostics());
            }
            if (!OVERALL_SCALE.semanticName().equals(
                    com.zhongbai233.bench.api.client.gui.BenchGuiSelectors
                            .select(snapshot, OVERALL_SCALE).requireMatch().semanticName())) {
                throw new AssertionError("Overall-scale semantic selector did not resolve exactly");
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

        private static CameraState currentCamera(YuushyaEditorScreen screen) throws ReflectiveOperationException {
            Field field = YuushyaEditorScreen.class.getDeclaredField("camera");
            if (!field.trySetAccessible()) throw new IllegalStateException("Cannot access editor camera");
            return (CameraState) field.get(screen);
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

        private void verifyRotateAndScaleGizmos(YuushyaEditorScreen screen) throws Exception {
            EditorTransform baseline = selectedTransform(screen);
            CameraFrame rotationFrame = (CameraFrame) invokePrivate(
                    screen, "currentCameraFrame", new Class<?>[0]);
            Vector3d pivot = BlockPreviewTransform.pivot(baseline);
            GizmoSizingPolicy.Sizes rotationSizes = GizmoSizingPolicy.calculate(baseline, rotationFrame);
            double blockRadius = GizmoSizingPolicy.worldBoundingRadius(baseline);
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
            ProjectedPoint startRotation = Projection.project(
                    startRing, rotationFrame.matrices(), rotationFrame.viewport());
            ProjectedPoint endRotation = Projection.project(
                    endRing, rotationFrame.matrices(), rotationFrame.viewport());
            drag(screen, startRotation, endRotation);
            EditorTransform rotated = selectedTransform(screen);
            if (rotated.rotation().equals(baseline.rotation())) {
                throw new AssertionError("Rotate Gizmo drag did not change the selected rotation");
            }
            invokePrivate(screen, "undo", new Class<?>[0]);
            if (!selectedTransform(screen).equals(baseline)) {
                throw new AssertionError("Undo did not restore the rotate Gizmo transaction exactly");
            }

            if (!screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_R, 0, 0))) {
                throw new AssertionError("R did not activate the overall-scale Gizmo");
            }
            CameraFrame scaleFrame = (CameraFrame) invokePrivate(screen, "currentCameraFrame", new Class<?>[0]);
            ProjectedPoint center = Projection.project(pivot, scaleFrame.matrices(), scaleFrame.viewport());
            if (!center.visible()) throw new AssertionError("Selected pivot is not visible for scale Gizmo test");
            GizmoSizingPolicy.Sizes scaleSizes = GizmoSizingPolicy.calculate(baseline, scaleFrame);
            Vector3d scaleEndpoint = null;
            for (GizmoHandle axis : new GizmoHandle[] {GizmoHandle.X, GizmoHandle.Y, GizmoHandle.Z}) {
                for (double direction : new double[] {-1.0D, 1.0D}) {
                    Vector3d candidate = GizmoHitTesting.axisEndpoint(
                            pivot, axis, scaleSizes.scaleHandleLength() * direction);
                    if (Projection.project(candidate, scaleFrame.matrices(), scaleFrame.viewport()).visible()) {
                        scaleEndpoint = candidate;
                        break;
                    }
                }
                if (scaleEndpoint != null) break;
            }
            if (scaleEndpoint == null) {
                throw new AssertionError("Adaptive scale handles are outside the viewport");
            }
            ProjectedPoint startScale = Projection.project(
                    scaleEndpoint, scaleFrame.matrices(), scaleFrame.viewport());
            double scaleDragFactor = 1.15D;
            ProjectedPoint endScale = new ProjectedPoint(
                    center.screenX() + (startScale.screenX() - center.screenX()) * scaleDragFactor,
                    center.screenY() + (startScale.screenY() - center.screenY()) * scaleDragFactor,
                    center.depth(), true, false);
            drag(screen, startScale, endScale);
            EditorTransform scaled = selectedTransform(screen);
            if (Math.abs(scaled.scale().x - baseline.scale().x * (float) scaleDragFactor) > 1.0E-4F
                    || BlockPreviewTransform.pivot(scaled).distance(pivot) > EPSILON) {
                throw new AssertionError("Overall-scale Gizmo did not scale uniformly around the preserved pivot");
            }
            invokePrivate(screen, "undo", new Class<?>[0]);
            if (!selectedTransform(screen).equals(baseline)) {
                throw new AssertionError("Undo did not restore the scale Gizmo transaction exactly");
            }
            assertFieldValue(0, EXPECTED_POSITION.x);
            assertFieldValue(1, EXPECTED_POSITION.y);
            assertFieldValue(2, EXPECTED_POSITION.z);
            assertFieldValue(6, EXPECTED_SCALE.x);
        }

        private static void drag(YuushyaEditorScreen screen, ProjectedPoint start, ProjectedPoint end) {
            if (!start.visible() || !end.visible()) {
                throw new AssertionError("Gizmo drag endpoints are outside the editor viewport");
            }
            MouseButtonInfo primary = new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0);
            MouseButtonEvent press = new MouseButtonEvent(start.screenX(), start.screenY(), primary);
            MouseButtonEvent release = new MouseButtonEvent(end.screenX(), end.screenY(), primary);
            if (!screen.mouseClicked(press, false)) {
                throw new AssertionError("Gizmo did not accept the scripted mouse press");
            }
            if (!screen.mouseDragged(release,
                    end.screenX() - start.screenX(), end.screenY() - start.screenY())) {
                throw new AssertionError("Gizmo did not accept the scripted mouse drag");
            }
            if (!screen.mouseReleased(release)) {
                throw new AssertionError("Gizmo did not accept the scripted mouse release");
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
            ProjectedPoint point = Projection.project(BlockPreviewTransform.pivot(transform),
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
