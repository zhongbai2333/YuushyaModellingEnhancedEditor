package com.zhongbai233.yuushya_editor.client.renderer;

import com.zhongbai233.yuushya_editor.core.camera.CameraFrame;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewFrame;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

/** One immutable editor viewport snapshot submitted to Minecraft's GUI PIP pass. */
public record BlockPreviewPipRenderState(EnvironmentPreviewFrame environmentFrame,
        List<BlockPreviewLayer> layers, CameraFrame cameraFrame,
        BlockPreviewGizmo gizmo, CollisionShape collisionShape, float lineWidthScale, boolean showGrid,
        int x0, int y0, int x1, int y1, float scale,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds) implements PictureInPictureRenderState {
    public BlockPreviewPipRenderState {
        Objects.requireNonNull(environmentFrame, "environmentFrame");
        layers = List.copyOf(Objects.requireNonNull(layers, "layers"));
        Objects.requireNonNull(cameraFrame, "cameraFrame");
        Objects.requireNonNull(collisionShape, "collisionShape");
        if (x1 <= x0 || y1 <= y0) throw new IllegalArgumentException("preview bounds must be positive");
        if (!Float.isFinite(lineWidthScale) || lineWidthScale <= 0.0F) {
            throw new IllegalArgumentException("line-width scale must be positive and finite");
        }
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            throw new IllegalArgumentException("preview scale must be positive and finite");
        }
    }

    public BlockPreviewPipRenderState(EnvironmentPreviewFrame environmentFrame,
            List<BlockPreviewLayer> layers, CameraFrame cameraFrame,
            BlockPreviewGizmo gizmo, float lineWidthScale, boolean showGrid,
            int x0, int y0, int x1, int y1,
            ScreenRectangle scissorArea) {
        this(environmentFrame, layers, cameraFrame, gizmo, CollisionShape.none(), lineWidthScale, showGrid,
                x0, y0, x1, y1, 1.0F, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }

    public BlockPreviewPipRenderState(EnvironmentPreviewFrame environmentFrame,
            List<BlockPreviewLayer> layers, CameraFrame cameraFrame,
            BlockPreviewGizmo gizmo, CollisionShape collisionShape, float lineWidthScale, boolean showGrid,
            int x0, int y0, int x1, int y1,
            ScreenRectangle scissorArea) {
        this(environmentFrame, layers, cameraFrame, gizmo, collisionShape, lineWidthScale, showGrid,
                x0, y0, x1, y1, 1.0F, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }

    public BlockPreviewPipRenderState(List<BlockPreviewLayer> layers, CameraFrame cameraFrame,
            BlockPreviewGizmo gizmo, float lineWidthScale, boolean showGrid,
            int x0, int y0, int x1, int y1, ScreenRectangle scissorArea) {
        this(EnvironmentPreviewFrame.empty(), layers, cameraFrame, gizmo, CollisionShape.none(), lineWidthScale, showGrid,
                x0, y0, x1, y1, scissorArea);
    }
}
