package com.zhongbai233.yuushya_editor.client.renderer;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoHandle;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoHitTesting;
import com.zhongbai233.yuushya_editor.core.projection.ProjectedPoint;
import com.zhongbai233.yuushya_editor.core.projection.Projection;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewTransform;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.slf4j.Logger;

/** Minimal current-resource-pack BlockState renderer for the editor viewport. */
public final class BlockPreviewPipRenderer extends PictureInPictureRenderer<BlockPreviewPipRenderState> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final int GIZMO_RING_SEGMENTS = 48;
    /**
     * The preview transform centers a unit block around its pivot, so the block's
     * bottom face (and the editor origin ground plane) is half a unit below the
     * pivot. Keep the grid on that face instead of at the pivot height.
     */
    private static final float GROUND_GRID_Y = -0.5F;
    private static final ThreadLocal<Float> ACTIVE_LINE_WIDTH_SCALE = ThreadLocal.withInitial(() -> 1.0F);
    private static long renderedTextures;
    private static long reusedTextures;
    private static long submittedBlocks;
    private static long frustumCulledBlocks;
    private static long modelCacheMisses;
    private static long sceneGeometryFlushes;
    private final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer(
            "yuushya_enhanced_editor_preview");
    private final EnvironmentPreviewGpuCache environmentGpuCache = new EnvironmentPreviewGpuCache();
    private final Map<BlockState, BlockModelRenderState> blockModelCache = new HashMap<>();
    private BlockStateModelSet cachedModelSet;
    private BlockPreviewPipRenderState lastRenderedState;
    private boolean failureLogged;

    public BlockPreviewPipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }

    @Override
    public Class<BlockPreviewPipRenderState> getRenderStateClass() {
        return BlockPreviewPipRenderState.class;
    }

    @Override
    protected boolean textureIsReadyToBlit(BlockPreviewPipRenderState state) {
        BlockStateModelSet currentModels = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        boolean ready = currentModels == cachedModelSet && state.equals(lastRenderedState)
                && environmentGpuCache.textureIsCurrent(state.environmentFrame());
        if (ready) reusedTextures++;
        return ready;
    }

    @Override
    protected void renderToTexture(BlockPreviewPipRenderState state, PoseStack ignored) {
        renderedTextures++;
        Minecraft minecraft = Minecraft.getInstance();
        FeatureRenderDispatcher dispatcher = minecraft.gameRenderer.getFeatureRenderDispatcher();
        float previousLineWidthScale = ACTIVE_LINE_WIDTH_SCALE.get();
        ACTIVE_LINE_WIDTH_SCALE.set(state.lineWidthScale());
        RenderSystem.backupProjectionMatrix();
        ProjectionType projectionType = state.cameraFrame().mode()
                == com.zhongbai233.yuushya_editor.core.camera.CameraMode.ORTHOGRAPHIC
                ? ProjectionType.ORTHOGRAPHIC : ProjectionType.PERSPECTIVE;
        RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(
                state.cameraFrame().matrices().projection()), projectionType);
        try {
            minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
            synchronizeModelCache(minecraft);
            renderScene(minecraft, dispatcher, state);
            lastRenderedState = state;
            failureLogged = false;
        } catch (RuntimeException failure) {
            lastRenderedState = null;
            dispatcher.clearSubmitNodes();
            if (!failureLogged) {
                LOGGER.warn("BlockState editor preview failed; keeping the editor usable", failure);
                failureLogged = true;
            }
        } finally {
            // PictureInPictureRenderer normally flushes after renderToTexture returns. This
            // renderer temporarily installs its own 3D projection, so every render type
            // (especially the sorted translucent sheet) must be drawn before that projection
            // is restored. The base-class flush that follows is harmless once this is empty.
            try {
                bufferSource.endBatch();
            } finally {
                RenderSystem.restoreProjectionMatrix();
                ACTIVE_LINE_WIDTH_SCALE.set(previousLineWidthScale);
            }
        }
    }

    private void renderScene(Minecraft minecraft, FeatureRenderDispatcher dispatcher,
            BlockPreviewPipRenderState state) {
        SubmitNodeStorage nodeStorage = dispatcher.getSubmitNodeStorage();
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(state.cameraFrame().matrices().view());
        Matrix4f viewProjection = state.cameraFrame().matrices().viewProjection();
        environmentGpuCache.updateAndRender(state.environmentFrame(), poseStack.last().pose(), viewProjection);
        submitLayers(minecraft, nodeStorage, poseStack, viewProjection, state.layers());
        dispatcher.renderAllFeatures();
        // Item/text/block feature nodes can leave vertices in RenderType-specific deferred
        // buffers. Flush every scene batch before submitting overlays so a later item batch
        // cannot paint over grid, cell, selection, collision, or Gizmo lines that are closer
        // to the camera. The overlays still use normal depth testing against scene geometry.
        bufferSource.endBatch();
        sceneGeometryFlushes++;
        if (state.showGrid()) drawGrid(poseStack);
        drawModelingCellOutline(poseStack);
        drawCollisionShape(poseStack, state.collisionShape());
        drawSelectionOutlines(poseStack, state);
        if (state.gizmo() != null) drawGizmo(poseStack, state);
    }

    private void submitLayers(Minecraft minecraft, SubmitNodeStorage nodeStorage,
            PoseStack poseStack, Matrix4fc viewProjection, List<BlockPreviewLayer> layers) {
        for (BlockPreviewLayer layer : layers) {
            Matrix4f modelTransform = previewMatrix(layer);
            if (layer.content() instanceof BlockPreviewLayer.BlockContent
                    && !intersectsClip(viewProjection, modelTransform)) {
                frustumCulledBlocks++;
                continue;
            }
            poseStack.pushPose();
            poseStack.mulPose(modelTransform);
            switch (layer.content()) {
                case BlockPreviewLayer.BlockContent block -> {
                    BlockState blockState = block.blockState();
                    BlockModelRenderState blockModel = blockModelCache.get(blockState);
                    if (blockModel == null) {
                        modelCacheMisses++;
                        BlockModelRenderState resolved = new BlockModelRenderState();
                        minecraft.getBlockModelResolver().update(resolved, blockState, DISPLAY_CONTEXT);
                        blockModelCache.put(blockState, resolved);
                        blockModel = resolved;
                    }
                    submittedBlocks++;
                    blockModel.submitMultiLayer(poseStack, nodeStorage, LightCoordsUtil.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY, 0);
                }
                case BlockPreviewLayer.ItemContent item -> {
                    ItemStackRenderState itemState = new ItemStackRenderState();
                    minecraft.getItemModelResolver().updateForTopItem(itemState, item.itemStack(),
                            ItemDisplayContext.NONE, minecraft.level, null, 0);
                    itemState.submit(poseStack, nodeStorage, LightCoordsUtil.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY, 0);
                }
                case BlockPreviewLayer.TextContent text -> submitText(minecraft, nodeStorage, poseStack, text);
            }
            poseStack.popPose();
        }
    }

    private static Matrix4f previewMatrix(BlockPreviewLayer layer) {
        Matrix4f contentTransform = switch (layer.content()) {
            case BlockPreviewLayer.BlockContent block -> block.centerOnPivot()
                    ? BlockPreviewTransform.matrix(layer.transform())
                    : BlockPreviewTransform.itemMatrix(layer.transform());
            case BlockPreviewLayer.ItemContent _ -> BlockPreviewTransform.itemMatrix(layer.transform());
            case BlockPreviewLayer.TextContent _ -> BlockPreviewTransform.textMatrix(layer.transform());
        };
        Vector3d offset = layer.worldOffset();
        return new Matrix4f().translate((float) offset.x, (float) offset.y, (float) offset.z)
                .mul(contentTransform);
    }

    private static void submitText(Minecraft minecraft, SubmitNodeStorage nodeStorage, PoseStack poseStack,
            BlockPreviewLayer.TextContent text) {
        nodeStorage.submitText(poseStack, 0.0F, 0.0F, text.component().getVisualOrderText(), false,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT,
                0xFFFFFFFF, 0, 0);
        if (text.culled()) return;
        poseStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.PI));
        poseStack.translate(-minecraft.font.width(text.component()), 0.0D, 0.0D);
        nodeStorage.submitText(poseStack, 0.0F, 0.0F, text.component().getVisualOrderText(), false,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT,
                0xFFFFFFFF, 0, 0);
    }

    private void synchronizeModelCache(Minecraft minecraft) {
        BlockStateModelSet current = minecraft.getModelManager().getBlockStateModelSet();
        if (current == cachedModelSet) return;
        cachedModelSet = current;
        blockModelCache.clear();
        lastRenderedState = null;
    }

    /** Conservative unit-cube frustum test used while the camera is moving and texture reuse is unavailable. */
    private static boolean intersectsClip(Matrix4fc viewProjection, Matrix4fc modelTransform) {
        Matrix4f clip = new Matrix4f(viewProjection).mul(modelTransform);
        int outsidePlanes = 0x3F;
        for (int corner = 0; corner < 8 && outsidePlanes != 0; corner++) {
            float x = (corner & 4) == 0 ? 0.0F : 1.0F;
            float y = (corner & 2) == 0 ? 0.0F : 1.0F;
            float z = (corner & 1) == 0 ? 0.0F : 1.0F;
            float clipX = clip.m00() * x + clip.m10() * y + clip.m20() * z + clip.m30();
            float clipY = clip.m01() * x + clip.m11() * y + clip.m21() * z + clip.m31();
            float clipZ = clip.m02() * x + clip.m12() * y + clip.m22() * z + clip.m32();
            float clipW = clip.m03() * x + clip.m13() * y + clip.m23() * z + clip.m33();
            int cornerOutside = 0;
            if (clipX < -clipW) cornerOutside |= 1;
            if (clipX > clipW) cornerOutside |= 2;
            if (clipY < -clipW) cornerOutside |= 4;
            if (clipY > clipW) cornerOutside |= 8;
            if (clipZ < -clipW) cornerOutside |= 16;
            if (clipZ > clipW) cornerOutside |= 32;
            outsidePlanes &= cornerOutside;
        }
        return outsidePlanes == 0;
    }

    public static void resetPerformanceCounters() {
        renderedTextures = 0L;
        reusedTextures = 0L;
        submittedBlocks = 0L;
        frustumCulledBlocks = 0L;
        modelCacheMisses = 0L;
        sceneGeometryFlushes = 0L;
        EnvironmentPreviewGpuCache.resetCounters();
    }

    public static PerformanceSnapshot performanceSnapshot() {
        EnvironmentPreviewGpuCache.Performance environment = EnvironmentPreviewGpuCache.performance();
        return new PerformanceSnapshot(renderedTextures, reusedTextures, submittedBlocks,
                frustumCulledBlocks, modelCacheMisses, sceneGeometryFlushes, environment.compiledSections(),
                environment.renderedSections(), environment.frustumCulledSections());
    }

    public record PerformanceSnapshot(long renderedTextures, long reusedTextures,
            long submittedBlocks, long frustumCulledBlocks, long modelCacheMisses,
            long sceneGeometryFlushes,
            long environmentCompiledSections, long environmentRenderedSections,
            long environmentFrustumCulledSections) { }

    private void drawGrid(PoseStack poseStack) {
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.lines());
        for (int i = -8; i <= 8; i++) {
            boolean major = i % 4 == 0;
            // Preserve NCPB's thin physical-pixel grid, but lift the luminance enough for the
            // much darker standalone editor viewport. Alpha alone was not sufficient on Retina.
            int gridColor = major ? 0xA09A7428 : 0x786E5620;
            int xColor = i == 0 ? 0xD0FFB347 : gridColor;
            int zColor = i == 0 ? 0xD045E7FF : gridColor;
            float width = major ? 1.20F : 1.0F;
            line(buffer, pose, i, GROUND_GRID_Y, -8.0F, i, GROUND_GRID_Y, 8.0F, xColor, width);
            line(buffer, pose, -8.0F, GROUND_GRID_Y, i, 8.0F, GROUND_GRID_Y, i, zColor, width);
        }
    }

    private void drawModelingCellOutline(PoseStack poseStack) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.linesTranslucent());
        box(buffer, poseStack.last(), -0.5F, -0.5F, -0.5F,
                0.5F, 0.5F, 0.5F, 0xB845E7FF, 1.25F);
    }

    private void drawSelectionOutlines(PoseStack poseStack, BlockPreviewPipRenderState state) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.linesTranslucent());
        for (BlockPreviewLayer layer : state.layers()) {
            if (!layer.selected()) continue;
            poseStack.pushPose();
            poseStack.mulPose(previewMatrix(layer));
            if (layer.content() instanceof BlockPreviewLayer.BlockContent) {
                box(buffer, poseStack.last(), -0.018F, -0.018F, -0.018F,
                        1.018F, 1.018F, 1.018F, 0xF0FFD769, 1.65F);
            } else if (layer.content() instanceof BlockPreviewLayer.TextContent text) {
                float width = Math.max(1.0F, Minecraft.getInstance().font.width(text.component()));
                box(buffer, poseStack.last(), -0.18F, -0.18F, -0.08F,
                        width + 0.18F, 9.18F, 0.08F, 0xF0FFD769, 1.65F);
            } else {
                box(buffer, poseStack.last(), -0.518F, -0.518F, -0.518F,
                        0.518F, 0.518F, 0.518F, 0xF0FFD769, 1.65F);
            }
            poseStack.popPose();
        }
    }

    private void drawCollisionShape(PoseStack poseStack, CollisionShape collisionShape) {
        if (collisionShape.isEmpty()) return;
        poseStack.pushPose();
        // Yuushya's voxel shapes use the physical ShowBlock cell [0, 1], while the
        // modelling preview centers that cell around the editor pivot.
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.linesTranslucent());
        for (CollisionShape.Box collisionBox : collisionShape.boxes()) {
            box(buffer, pose, (float) collisionBox.minX(), (float) collisionBox.minY(),
                    (float) collisionBox.minZ(), (float) collisionBox.maxX(),
                    (float) collisionBox.maxY(), (float) collisionBox.maxZ(),
                    0xC0FFB347, 1.5F);
        }
        poseStack.popPose();
    }

    private void drawGizmo(PoseStack poseStack, BlockPreviewPipRenderState state) {
        BlockPreviewGizmo gizmo = state.gizmo();
        Vector3d origin = gizmo.origin();
        poseStack.pushPose();
        poseStack.translate(origin.x, origin.y, origin.z);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.linesTranslucent());
        switch (gizmo.mode()) {
            case MOVE -> drawMoveGizmo(buffer, pose, gizmo);
            case ROTATE -> drawRotateGizmo(buffer, pose, gizmo);
            case SCALE -> drawScaleGizmo(buffer, pose, state, gizmo);
        }
        poseStack.popPose();
    }

    private static void drawMoveGizmo(VertexConsumer buffer, PoseStack.Pose pose, BlockPreviewGizmo gizmo) {
        float length = (float) gizmo.axisLength();
        drawMoveAxis(buffer, pose, 'x', length, gizmo.activeHandle() == GizmoHandle.X,
                0xFFE65A46, 0xFFFF9B91);
        drawMoveAxis(buffer, pose, 'y', length, gizmo.activeHandle() == GizmoHandle.Y,
                0xFFA0DC5A, 0xFFCAFF9F);
        drawMoveAxis(buffer, pose, 'z', length, gizmo.activeHandle() == GizmoHandle.Z,
                0xFF5AB4DC, 0xFF9DD9FF);
        box(buffer, pose, -0.045F, -0.045F, -0.045F, 0.045F, 0.045F, 0.045F,
                0xFFE8E8E8, 1.2F);
    }

    private static void drawMoveAxis(VertexConsumer buffer, PoseStack.Pose pose, char axis, float length,
            boolean selected, int color, int selectedColor) {
        int visibleColor = selected ? selectedColor : color;
        float width = selected ? 2.4F : 1.5F;
        float arrowLength = length * 0.10F;
        float wing = length * 0.052F;
        Vector3d direction = switch (axis) {
            case 'x' -> new Vector3d(1.0D, 0.0D, 0.0D);
            case 'y' -> new Vector3d(0.0D, 1.0D, 0.0D);
            default -> new Vector3d(0.0D, 0.0D, 1.0D);
        };
        Vector3d firstWing = axis == 'x'
                ? new Vector3d(0.0D, wing, 0.0D) : new Vector3d(wing, 0.0D, 0.0D);
        Vector3d secondWing = axis == 'z'
                ? new Vector3d(0.0D, wing, 0.0D) : new Vector3d(0.0D, 0.0D, wing);
        line(buffer, pose, new Vector3d(direction).mul(-length), new Vector3d(direction).mul(length),
                visibleColor, width);
        for (double sign : new double[] {-1.0D, 1.0D}) {
            Vector3d tip = new Vector3d(direction).mul(length * sign);
            Vector3d base = new Vector3d(direction).mul((length - arrowLength) * sign);
            line(buffer, pose, tip, new Vector3d(base).add(firstWing), visibleColor, width);
            line(buffer, pose, tip, new Vector3d(base).sub(firstWing), visibleColor, width);
            line(buffer, pose, tip, new Vector3d(base).add(secondWing), visibleColor, width);
            line(buffer, pose, tip, new Vector3d(base).sub(secondWing), visibleColor, width);
        }
    }

    private static void drawRotateGizmo(VertexConsumer buffer, PoseStack.Pose pose, BlockPreviewGizmo gizmo) {
        drawRing(buffer, pose, GizmoHandle.X, (float) gizmo.rotationRadius(),
                gizmo.activeHandle() == GizmoHandle.X, 0xFFE65A46, 0xFFFF9B91);
        drawRing(buffer, pose, GizmoHandle.Y, (float) gizmo.rotationRadius(),
                gizmo.activeHandle() == GizmoHandle.Y, 0xFFA0DC5A, 0xFFCAFF9F);
        drawRing(buffer, pose, GizmoHandle.Z, (float) gizmo.rotationRadius(),
                gizmo.activeHandle() == GizmoHandle.Z, 0xFF5AB4DC, 0xFF9DD9FF);
    }

    private static void drawRing(VertexConsumer buffer, PoseStack.Pose pose, GizmoHandle axis, float radius,
            boolean selected, int color, int selectedColor) {
        int visibleColor = selected ? selectedColor : color;
        float width = selected ? 2.2F : 1.2F;
        Vector3d previous = GizmoHitTesting.ringPoint(new Vector3d(), axis, radius, 0.0D);
        for (int segment = 1; segment <= GIZMO_RING_SEGMENTS; segment++) {
            double angle = Math.PI * 2.0D * segment / GIZMO_RING_SEGMENTS;
            Vector3d current = GizmoHitTesting.ringPoint(new Vector3d(), axis, radius, angle);
            line(buffer, pose, (float) previous.x, (float) previous.y, (float) previous.z,
                    (float) current.x, (float) current.y, (float) current.z,
                    visibleColor, width);
            previous = current;
        }
        for (int quarter = 0; quarter < 4; quarter++) {
            double angle = Math.PI * 0.5D * quarter;
            Vector3d marker = GizmoHitTesting.ringPoint(new Vector3d(), axis, radius, angle);
            Vector3d radial = new Vector3d(marker).normalize().mul(radius * 0.035D);
            Vector3d tangent = GizmoHitTesting.ringPoint(new Vector3d(), axis, radius, angle + 0.01D)
                    .sub(marker).normalize().mul(radius * 0.035D);
            line(buffer, pose, new Vector3d(marker).add(radial), new Vector3d(marker).add(tangent),
                    visibleColor, width);
            line(buffer, pose, new Vector3d(marker).add(tangent), new Vector3d(marker).sub(radial),
                    visibleColor, width);
            line(buffer, pose, new Vector3d(marker).sub(radial), new Vector3d(marker).sub(tangent),
                    visibleColor, width);
            line(buffer, pose, new Vector3d(marker).sub(tangent), new Vector3d(marker).add(radial),
                    visibleColor, width);
        }
    }

    private static void drawScaleGizmo(VertexConsumer buffer, PoseStack.Pose pose,
            BlockPreviewPipRenderState state, BlockPreviewGizmo gizmo) {
        for (GizmoHandle axis : new GizmoHandle[] {GizmoHandle.X, GizmoHandle.Y, GizmoHandle.Z}) {
            boolean selected = gizmo.activeHandle() == axis;
            int color = switch (axis) {
                case X -> selected ? 0xFFFF9B91 : 0xFFE65A46;
                case Y -> selected ? 0xFFCAFF9F : 0xFFA0DC5A;
                case Z -> selected ? 0xFF9DD9FF : 0xFF5AB4DC;
                default -> throw new IllegalStateException("Unexpected scale axis " + axis);
            };
            float width = selected ? 2.4F : 1.5F;
            Vector3d negative = new Vector3d(axis.axis()).mul(-gizmo.scaleHandleLength());
            Vector3d positive = new Vector3d(axis.axis()).mul(gizmo.scaleHandleLength());
            line(buffer, pose, negative, positive, color, width);
            drawScaleMarker(buffer, pose, state, gizmo, negative, selected, color, width);
            drawScaleMarker(buffer, pose, state, gizmo, positive, selected, color, width);
        }
        box(buffer, pose, -0.055F, -0.055F, -0.055F, 0.055F, 0.055F, 0.055F,
                0xFFE8E8E8, 1.2F);
    }

    private static void drawScaleMarker(VertexConsumer buffer, PoseStack.Pose pose,
            BlockPreviewPipRenderState state, BlockPreviewGizmo gizmo, Vector3d localEndpoint,
            boolean selected, int color, float width) {
        Vector3d endpoint = new Vector3d(gizmo.origin()).add(localEndpoint);
        ProjectedPoint projected = Projection.project(endpoint, state.cameraFrame().matrices(),
                state.cameraFrame().viewport());
        if (!projected.visible()) return;
        double radius = selected ? 4.5D : 3.5D;
        Vector3d top = markerPoint(projected, 0.0D, -radius, state).sub(gizmo.origin());
        Vector3d right = markerPoint(projected, radius, 0.0D, state).sub(gizmo.origin());
        Vector3d bottom = markerPoint(projected, 0.0D, radius, state).sub(gizmo.origin());
        Vector3d left = markerPoint(projected, -radius, 0.0D, state).sub(gizmo.origin());
        line(buffer, pose, top, right, color, width);
        line(buffer, pose, right, bottom, color, width);
        line(buffer, pose, bottom, left, color, width);
        line(buffer, pose, left, top, color, width);
    }

    private static Vector3d markerPoint(ProjectedPoint center, double offsetX, double offsetY,
            BlockPreviewPipRenderState state) {
        return Projection.worldPointAtScreenDepth(center.screenX() + offsetX, center.screenY() + offsetY,
                center.depth(), state.cameraFrame().matrices(), state.cameraFrame().viewport());
    }

    private static void box(VertexConsumer buffer, PoseStack.Pose pose, float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ, int color, float width) {
        line(buffer, pose, minX, minY, minZ, maxX, minY, minZ, color, width);
        line(buffer, pose, maxX, minY, minZ, maxX, minY, maxZ, color, width);
        line(buffer, pose, maxX, minY, maxZ, minX, minY, maxZ, color, width);
        line(buffer, pose, minX, minY, maxZ, minX, minY, minZ, color, width);
        line(buffer, pose, minX, maxY, minZ, maxX, maxY, minZ, color, width);
        line(buffer, pose, maxX, maxY, minZ, maxX, maxY, maxZ, color, width);
        line(buffer, pose, maxX, maxY, maxZ, minX, maxY, maxZ, color, width);
        line(buffer, pose, minX, maxY, maxZ, minX, maxY, minZ, color, width);
        line(buffer, pose, minX, minY, minZ, minX, maxY, minZ, color, width);
        line(buffer, pose, maxX, minY, minZ, maxX, maxY, minZ, color, width);
        line(buffer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, color, width);
        line(buffer, pose, minX, minY, maxZ, minX, maxY, maxZ, color, width);
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose, Vector3d from, Vector3d to,
            int color, float width) {
        line(buffer, pose, (float) from.x, (float) from.y, (float) from.z,
                (float) to.x, (float) to.y, (float) to.z, color, width);
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose, float x1, float y1, float z1,
            float x2, float y2, float z2, int color, float width) {
        // Keep camera-distance compensation while preventing the thinnest tier from
        // collapsing into a Retina hairline.
        float visibleWidth = PreviewLineWidthPolicy.visibleWidth(width, ACTIVE_LINE_WIDTH_SCALE.get());
        buffer.addVertex(pose, x1, y1, z1).setColor(color).setNormal(0.0F, 1.0F, 0.0F)
                .setLineWidth(visibleWidth);
        buffer.addVertex(pose, x2, y2, z2).setColor(color).setNormal(0.0F, 1.0F, 0.0F)
                .setLineWidth(visibleWidth);
    }

    @Override
    protected String getTextureLabel() {
        return "yuushya_enhanced_editor_block_preview";
    }

    @Override
    public void close() {
        environmentGpuCache.close();
        blockModelCache.clear();
        cachedModelSet = null;
        lastRenderedState = null;
        super.close();
        projectionBuffer.close();
    }
}
