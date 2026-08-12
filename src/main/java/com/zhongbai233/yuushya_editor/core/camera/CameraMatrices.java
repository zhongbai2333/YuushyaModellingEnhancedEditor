package com.zhongbai233.yuushya_editor.core.camera;

import com.zhongbai233.yuushya_editor.core.projection.Viewport;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/** Defensive matrix snapshot shared by rendering, projection, and picking. */
public final class CameraMatrices {
    private final Matrix4f view;
    private final Matrix4f projection;
    private final Matrix4f viewProjection;
    private final Matrix4f inverseViewProjection;

    private CameraMatrices(Matrix4f view, Matrix4f projection) {
        this.view = new Matrix4f(view);
        this.projection = new Matrix4f(projection);
        this.viewProjection = new Matrix4f(projection).mul(view);
        this.inverseViewProjection = new Matrix4f(viewProjection).invert();
    }

    public static CameraMatrices create(CameraState camera, Viewport viewport) {
        Vector3d position = camera.position();
        Matrix4f view = new Matrix4f().rotate(camera.orientation().conjugate())
                .translate((float) -position.x, (float) -position.y, (float) -position.z);
        Matrix4f projection;
        if (camera.mode() == CameraMode.ORTHOGRAPHIC) {
            float halfHeight = camera.orthoScale();
            float halfWidth = halfHeight * (float) viewport.aspectRatio();
            projection = new Matrix4f().ortho(-halfWidth, halfWidth, -halfHeight, halfHeight,
                    camera.nearPlane(), camera.farPlane());
        } else {
            projection = new Matrix4f().perspective((float) Math.toRadians(camera.fovDegrees()),
                    (float) viewport.aspectRatio(), camera.nearPlane(), camera.farPlane());
        }
        return new CameraMatrices(view, projection);
    }

    public Matrix4f view() { return new Matrix4f(view); }
    public Matrix4f projection() { return new Matrix4f(projection); }
    public Matrix4f viewProjection() { return new Matrix4f(viewProjection); }
    public Matrix4f inverseViewProjection() { return new Matrix4f(inverseViewProjection); }

    @Override
    public boolean equals(Object other) {
        return other instanceof CameraMatrices value
                && view.equals(value.view) && projection.equals(value.projection);
    }

    @Override
    public int hashCode() {
        return 31 * view.hashCode() + projection.hashCode();
    }
}
