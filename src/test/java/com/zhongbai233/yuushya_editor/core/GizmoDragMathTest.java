package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhongbai233.scene_editor.core.gizmo.GizmoConstraint;
import com.zhongbai233.scene_editor.core.gizmo.GizmoDragMath;
import com.zhongbai233.scene_editor.core.gizmo.GizmoSizingPolicy;
import com.zhongbai233.scene_editor.core.command.CommandStack;
import com.zhongbai233.scene_editor.core.transaction.DragTransaction;
import com.zhongbai233.scene_editor.core.projection.PickingRay;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class GizmoDragMathTest {
    @Test
    void axisAndPlaneConstraintsFollowPickingRay() {
        PickingRay ray = new PickingRay(new Vector3d(2.0D, 3.0D, 5.0D),
                new Vector3d(-0.2D, -0.3D, -1.0D));
        Vector3d axisHit = GizmoDragMath.intersectConstraint(ray, new Vector3d(),
                new Vector3d(1.0D, 0.0D, 0.0D), GizmoConstraint.X_AXIS).orElseThrow();
        assertEquals(0.0D, axisHit.y, 1.0e-5D);
        assertEquals(0.0D, axisHit.z, 1.0e-5D);

        Vector3d planeHit = GizmoDragMath.intersectConstraint(ray, new Vector3d(),
                new Vector3d(0.0D, 1.0D, 0.0D), GizmoConstraint.XZ_PLANE).orElseThrow();
        assertEquals(0.0D, planeHit.y, 1.0e-5D);
    }

    @Test
    void rotationConstraintReturnsSignedDegrees() {
        float degrees = GizmoDragMath.rotationDeltaDegrees(new Vector3d(),
                new Vector3d(0.0D, 1.0D, 0.0D),
                new Vector3d(1.0D, 0.0D, 0.0D), new Vector3d(0.0D, 0.0D, -1.0D));
        assertEquals(90.0F, degrees, 1.0e-5F);
    }

    @Test
    void rotationSensitivityCompensatesForAHandleGrowingWithTheModel() {
        assertEquals(10.0F, GizmoDragMath.scaleIndependentRotationDegrees(
                10.0F, GizmoSizingPolicy.MIN_ROTATION_RADIUS), 1.0e-5F);
        assertEquals(40.0F, GizmoDragMath.scaleIndependentRotationDegrees(
                10.0F, GizmoSizingPolicy.MIN_ROTATION_RADIUS * 4.0D), 1.0e-5F);
        assertEquals(180.0F, GizmoDragMath.scaleIndependentRotationDegrees(
                90.0F, GizmoSizingPolicy.MIN_ROTATION_RADIUS * 100.0D), 1.0e-5F);
    }

    @Test
    void worldDragConvertsToModelUnitsAndCommitsAsOneUndoStep() {
        Vector3d startPosition = new Vector3d(32.0D, 4.0D, -8.0D);
        Vector3d axis = new Vector3d(1.0D, 0.0D, 0.0D);
        Vector3d startHit = new Vector3d();
        DragTransaction<Vector3d> drag = new DragTransaction<>(startPosition, "move X");
        drag.update(ignored -> GizmoDragMath.translatedModelPosition(startPosition, axis, startHit,
                new Vector3d(0.25D, 0.0D, 0.0D), 16.0D));
        drag.update(ignored -> GizmoDragMath.translatedModelPosition(startPosition, axis, startHit,
                new Vector3d(0.5D, 0.0D, 0.0D), 16.0D));

        CommandStack<Vector3d> history = new CommandStack<>(8);
        Vector3d moved = drag.commit(history);
        assertEquals(new Vector3d(40.0D, 4.0D, -8.0D), moved);
        assertEquals(startPosition, history.undo(moved));
        assertEquals(false, history.canUndo());
        assertEquals(moved, history.redo(startPosition));
    }

    @Test
    void scaledWorldDragUsesInverseScaleConversion() {
        Vector3d moved = GizmoDragMath.translatedModelPosition(new Vector3d(),
                new Vector3d(1.0D, 0.0D, 0.0D), new Vector3d(),
                new Vector3d(0.5D, 0.0D, 0.0D), 16.0D / 2.0D);

        assertEquals(new Vector3d(4.0D, 0.0D, 0.0D), moved);
    }

    @Test
    void uniformScaleDragUsesClampedScreenDistanceRatio() {
        assertEquals(2.0D, GizmoDragMath.uniformScaleFactor(20.0D, 40.0D), 1.0E-9D);
        assertEquals(0.01D, GizmoDragMath.uniformScaleFactor(20.0D, 0.0D), 1.0E-9D);
        assertEquals(100.0D, GizmoDragMath.uniformScaleFactor(1.0D, 1000.0D), 1.0E-9D);
    }
}
