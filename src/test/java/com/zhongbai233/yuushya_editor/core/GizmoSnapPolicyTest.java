package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhongbai233.yuushya_editor.core.gizmo.GizmoMode;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoSnapPolicy;
import org.junit.jupiter.api.Test;

class GizmoSnapPolicyTest {
    @Test
    void moveAndScaleUseRequestedModifierLevels() {
        assertEquals(0.1D, GizmoSnapPolicy.step(GizmoMode.MOVE, false, false));
        assertEquals(0.05D, GizmoSnapPolicy.step(GizmoMode.MOVE, true, false));
        assertEquals(0.001D, GizmoSnapPolicy.step(GizmoMode.MOVE, true, true));
        assertEquals(0.05D, GizmoSnapPolicy.step(GizmoMode.SCALE, true, false));
    }

    @Test
    void rotationUsesCoarseNormalAndFineLevels() {
        assertEquals(15.0D, GizmoSnapPolicy.step(GizmoMode.ROTATE, false, false));
        assertEquals(5.0D, GizmoSnapPolicy.step(GizmoMode.ROTATE, true, false));
        assertEquals(0.001D, GizmoSnapPolicy.step(GizmoMode.ROTATE, false, true));
    }

    @Test
    void controlFineStepWinsWhenBothModifiersAreDown() {
        assertEquals(GizmoSnapPolicy.FINE_MOVE_STEP,
                GizmoSnapPolicy.step(GizmoMode.MOVE, true, true));
        assertEquals(GizmoSnapPolicy.FINE_MOVE_STEP,
                GizmoSnapPolicy.step(GizmoMode.SCALE, true, true));
        assertEquals(GizmoSnapPolicy.FINE_ROTATE_STEP,
                GizmoSnapPolicy.step(GizmoMode.ROTATE, true, true));
    }

    @Test
    void snappingIsRelativeAndSymmetric() {
        assertEquals(0.1D, GizmoSnapPolicy.snapDelta(0.149D, 0.1D), 1.0E-12D);
        assertEquals(-0.1D, GizmoSnapPolicy.snapDelta(-0.149D, 0.1D), 1.0E-12D);
        assertEquals(0.0D, GizmoSnapPolicy.snapDelta(0.024D, 0.05D));
    }
}
