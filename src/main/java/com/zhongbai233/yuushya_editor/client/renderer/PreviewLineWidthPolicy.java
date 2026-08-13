package com.zhongbai233.yuushya_editor.client.renderer;

import com.zhongbai233.yuushya_editor.core.camera.CameraMode;
import com.zhongbai233.yuushya_editor.core.camera.CameraState;
import java.util.Objects;

/** NCPB's physical-pixel line-width compensation for the modelling preview. */
public final class PreviewLineWidthPolicy {
    private static final float BASE_SCALE = 1.30F;
    private static final float REFERENCE_DISTANCE = 4.0F;
    private static final float REFERENCE_ORTHO_HALF_HEIGHT = 3.0F;
    private static final float MIN_VISIBLE_WIDTH = 2.0F;
    private static final float MAX_VISIBLE_WIDTH = 8.0F;
    // PIP runs in physical pixels. A slightly higher ceiling than the original NCPB preview
    // keeps the grid readable on Retina displays when the camera is pulled very far away.
    private static final float MAX_SCALE = 6.0F;

    private PreviewLineWidthPolicy() { }

    public static float forCamera(CameraState camera) {
        Objects.requireNonNull(camera, "camera");
        return camera.mode() == CameraMode.ORTHOGRAPHIC
                ? orthographic(camera.orthoScale())
                : perspective((float) camera.position().distance(camera.focus()));
    }

    public static float perspective(float cameraDistance) {
        float distanceRatio = Math.max(1.0F, finitePositive(cameraDistance) / REFERENCE_DISTANCE);
        return clamp(BASE_SCALE * (float) Math.sqrt(distanceRatio));
    }

    public static float orthographic(float halfHeight) {
        float viewRatio = Math.max(1.0F, finitePositive(halfHeight) / REFERENCE_ORTHO_HALF_HEIGHT);
        return clamp(BASE_SCALE * (float) Math.sqrt(viewRatio));
    }

    static float visibleWidth(float logicalWidth, float cameraScale) {
        float safeLogicalWidth = finitePositive(logicalWidth);
        float safeCameraScale = finitePositive(cameraScale);
        return Math.clamp(safeLogicalWidth * safeCameraScale * 1.15F,
                MIN_VISIBLE_WIDTH, MAX_VISIBLE_WIDTH);
    }

    private static float finitePositive(float value) {
        return Float.isFinite(value) && value > 0.0F ? value : 1.0F;
    }

    private static float clamp(float value) {
        return Math.clamp(value, BASE_SCALE, MAX_SCALE);
    }
}
