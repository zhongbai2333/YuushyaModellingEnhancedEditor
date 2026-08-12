package com.zhongbai233.yuushya_editor.client.renderer;

import com.zhongbai233.yuushya_editor.core.gizmo.GizmoHandle;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoMode;
import java.util.Objects;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/** Immutable gizmo geometry submitted alongside one block-preview frame. */
public record BlockPreviewGizmo(Vector3d origin, GizmoMode mode, GizmoHandle activeHandle,
        double axisLength, double rotationRadius, double scaleHandleLength) {
    public BlockPreviewGizmo {
        origin = new Vector3d(Objects.requireNonNull(origin, "origin"));
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(activeHandle, "activeHandle");
        if (!positiveFinite(axisLength) || !positiveFinite(rotationRadius)
                || !positiveFinite(scaleHandleLength)) {
            throw new IllegalArgumentException("gizmo dimensions must be positive and finite");
        }
    }

    public BlockPreviewGizmo(Vector3dc origin, GizmoMode mode, GizmoHandle activeHandle,
            double axisLength, double rotationRadius, double scaleHandleLength) {
        this(new Vector3d(origin), mode, activeHandle, axisLength, rotationRadius, scaleHandleLength);
    }

    @Override
    public Vector3d origin() {
        return new Vector3d(origin);
    }

    private static boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0D;
    }
}
