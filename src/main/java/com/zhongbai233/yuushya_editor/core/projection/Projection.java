package com.zhongbai233.yuushya_editor.core.projection;

import com.zhongbai233.yuushya_editor.core.camera.CameraMatrices;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4f;

public final class Projection {
    private Projection() { }

    public static ProjectedPoint project(Vector3dc point, CameraMatrices matrices, Viewport viewport) {
        Vector4f clip = new Vector4f((float) point.x(), (float) point.y(), (float) point.z(), 1.0F)
                .mul(matrices.viewProjection());
        boolean behind = clip.w <= 0.0F;
        if (!Float.isFinite(clip.w) || Math.abs(clip.w) <= 1.0e-7F) {
            return new ProjectedPoint(Double.NaN, Double.NaN, Double.NaN, false, behind);
        }
        double x = clip.x / clip.w;
        double y = clip.y / clip.w;
        double z = clip.z / clip.w;
        double screenX = viewport.x() + (x + 1.0D) * 0.5D * viewport.width();
        double screenY = viewport.y() + (1.0D - y) * 0.5D * viewport.height();
        boolean visible = !behind && Math.abs(x) <= 1.0D && Math.abs(y) <= 1.0D && Math.abs(z) <= 1.0D;
        return new ProjectedPoint(screenX, screenY, z, visible, behind);
    }

    public static PickingRay rayFromScreen(double mouseX, double mouseY, CameraMatrices matrices, Viewport viewport) {
        double x = ((mouseX - viewport.x()) / viewport.width()) * 2.0D - 1.0D;
        double y = 1.0D - ((mouseY - viewport.y()) / viewport.height()) * 2.0D;
        Vector3d near = unproject(x, y, -1.0D, matrices);
        Vector3d interior = unproject(x, y, 0.0D, matrices);
        return new PickingRay(near, interior.sub(near));
    }

    /** Converts a GUI-space point and an NDC depth back to its world-space position. */
    public static Vector3d worldPointAtScreenDepth(double screenX, double screenY, double depth,
            CameraMatrices matrices, Viewport viewport) {
        java.util.Objects.requireNonNull(matrices, "matrices");
        java.util.Objects.requireNonNull(viewport, "viewport");
        if (!Double.isFinite(screenX) || !Double.isFinite(screenY) || !Double.isFinite(depth)) {
            throw new IllegalArgumentException("screen point and depth must be finite");
        }
        double x = ((screenX - viewport.x()) / viewport.width()) * 2.0D - 1.0D;
        double y = 1.0D - ((screenY - viewport.y()) / viewport.height()) * 2.0D;
        return unproject(x, y, depth, matrices);
    }

    private static Vector3d unproject(double x, double y, double z, CameraMatrices matrices) {
        Vector4f world = new Vector4f((float) x, (float) y, (float) z, 1.0F)
                .mul(matrices.inverseViewProjection());
        if (!Float.isFinite(world.w) || Math.abs(world.w) <= 1.0e-7F) {
            throw new IllegalStateException("camera matrix cannot unproject point");
        }
        return new Vector3d(world.x / world.w, world.y / world.w, world.z / world.w);
    }
}
