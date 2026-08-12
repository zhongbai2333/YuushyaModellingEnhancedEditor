package com.zhongbai233.yuushya_editor.core.gizmo;

import org.joml.Vector3d;

/** World-space move handle exposed by the first editor Gizmo slice. */
public enum GizmoHandle {
    NONE,
    X,
    Y,
    Z,
    UNIFORM;

    public Vector3d axis() {
        return switch (this) {
            case X -> new Vector3d(1.0D, 0.0D, 0.0D);
            case Y -> new Vector3d(0.0D, 1.0D, 0.0D);
            case Z -> new Vector3d(0.0D, 0.0D, 1.0D);
            case NONE, UNIFORM -> throw new IllegalStateException(this + " has no axis");
        };
    }

    public GizmoConstraint constraint() {
        return switch (this) {
            case X -> GizmoConstraint.X_AXIS;
            case Y -> GizmoConstraint.Y_AXIS;
            case Z -> GizmoConstraint.Z_AXIS;
            case NONE, UNIFORM -> throw new IllegalStateException(this + " has no movement constraint");
        };
    }

    public GizmoConstraint rotationConstraint() {
        return switch (this) {
            case X -> GizmoConstraint.YZ_PLANE;
            case Y -> GizmoConstraint.XZ_PLANE;
            case Z -> GizmoConstraint.XY_PLANE;
            case NONE, UNIFORM -> throw new IllegalStateException(this + " has no rotation constraint");
        };
    }
}
