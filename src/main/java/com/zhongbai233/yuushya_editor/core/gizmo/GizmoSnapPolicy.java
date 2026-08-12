package com.zhongbai233.yuushya_editor.core.gizmo;

/** Modifier-aware, drag-relative snapping shared by the editor gizmos. */
public final class GizmoSnapPolicy {
    public static final double DEFAULT_MOVE_STEP = 0.1D;
    public static final double SHIFT_MOVE_STEP = 0.05D;
    public static final double FINE_MOVE_STEP = 0.001D;
    public static final double DEFAULT_ROTATE_STEP = 15.0D;
    public static final double SHIFT_ROTATE_STEP = 5.0D;
    public static final double FINE_ROTATE_STEP = 0.001D;

    private GizmoSnapPolicy() { }

    public static double step(GizmoMode mode, boolean shiftDown, boolean controlDown) {
        if (mode == GizmoMode.ROTATE) {
            if (controlDown) return FINE_ROTATE_STEP;
            return shiftDown ? SHIFT_ROTATE_STEP : DEFAULT_ROTATE_STEP;
        }
        if (controlDown) return FINE_MOVE_STEP;
        return shiftDown ? SHIFT_MOVE_STEP : DEFAULT_MOVE_STEP;
    }

    /** Quantizes a delta from the start of the current drag, never the legacy absolute value. */
    public static double snapDelta(double delta, double step) {
        if (!Double.isFinite(delta)) throw new IllegalArgumentException("delta must be finite");
        if (!Double.isFinite(step) || step <= 0.0D) {
            throw new IllegalArgumentException("step must be positive and finite");
        }
        double snapped = Math.rint(delta / step) * step;
        return snapped == -0.0D ? 0.0D : snapped;
    }
}
