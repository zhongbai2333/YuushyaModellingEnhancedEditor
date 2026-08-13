package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhongbai233.yuushya_editor.core.camera.CameraMatrices;
import com.zhongbai233.yuushya_editor.core.camera.CameraMode;
import com.zhongbai233.yuushya_editor.core.camera.CameraState;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoHandle;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoHitTesting;
import com.zhongbai233.yuushya_editor.core.projection.ProjectedPoint;
import com.zhongbai233.yuushya_editor.core.projection.Projection;
import com.zhongbai233.yuushya_editor.core.projection.Viewport;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class GizmoHitTestingTest {
    @Test
    void projectedAxisSegmentsSelectTheirMoveHandle() {
        CameraState camera = CameraState.lookingAt(CameraMode.PERSPECTIVE,
                new Vector3d(4.0D, 3.0D, 6.0D), new Vector3d(), new Vector3d(0.0D, 1.0D, 0.0D),
                45.0F, 4.0F, 0.05F, 100.0F);
        Viewport viewport = new Viewport(20, 30, 800, 600);
        CameraMatrices matrices = CameraMatrices.create(camera, viewport);
        Vector3d origin = new Vector3d();

        for (GizmoHandle expected : new GizmoHandle[] {GizmoHandle.X, GizmoHandle.Y, GizmoHandle.Z}) {
            ProjectedPoint point = Projection.project(new Vector3d(origin).fma(0.85D, expected.axis()),
                    matrices, viewport);
            assertEquals(expected, GizmoHitTesting.moveHandleAt(point.screenX(), point.screenY(), origin,
                    matrices, viewport, 1.25D, 6.0D));
        }
    }

    @Test
    void centerIsReservedForNonAxisInteraction() {
        CameraState camera = CameraState.lookingAt(CameraMode.ORTHOGRAPHIC,
                new Vector3d(3.0D, 2.0D, 5.0D), new Vector3d(), new Vector3d(0.0D, 1.0D, 0.0D),
                45.0F, 4.0F, 0.05F, 100.0F);
        Viewport viewport = new Viewport(0, 0, 800, 600);
        CameraMatrices matrices = CameraMatrices.create(camera, viewport);
        ProjectedPoint center = Projection.project(new Vector3d(), matrices, viewport);

        assertEquals(GizmoHandle.NONE, GizmoHitTesting.moveHandleAt(center.screenX(), center.screenY(),
                new Vector3d(), matrices, viewport, 1.25D, 6.0D));
    }

    @Test
    void rotationRingsAndAllSixScaleHandlesAreHitInScreenSpace() {
        CameraState camera = CameraState.lookingAt(CameraMode.PERSPECTIVE,
                new Vector3d(4.0D, 3.0D, 6.0D), new Vector3d(), new Vector3d(0.0D, 1.0D, 0.0D),
                45.0F, 4.0F, 0.05F, 100.0F);
        Viewport viewport = new Viewport(20, 30, 800, 600);
        CameraMatrices matrices = CameraMatrices.create(camera, viewport);
        Vector3d origin = new Vector3d();
        Vector3d ringPoint = GizmoHitTesting.ringPoint(origin, GizmoHandle.X, 1.05D, 0.65D);
        ProjectedPoint projectedRing = Projection.project(ringPoint, matrices, viewport);
        assertEquals(GizmoHandle.X, GizmoHitTesting.rotateHandleAt(
                projectedRing.screenX(), projectedRing.screenY(), origin, matrices, viewport, 1.05D, 4.0D));

        for (GizmoHandle axis : new GizmoHandle[] {GizmoHandle.X, GizmoHandle.Y, GizmoHandle.Z}) {
            for (double direction : new double[] {-1.0D, 1.0D}) {
                ProjectedPoint endpoint = Projection.project(
                        GizmoHitTesting.axisEndpoint(origin, axis, 1.35D * direction), matrices, viewport);
                assertEquals(axis, GizmoHitTesting.scaleHandleAt(
                        endpoint.screenX(), endpoint.screenY(), origin, matrices, viewport, 1.35D, 4.0D));
            }
        }
    }

    @Test
    void moveAndScaleHandlesExposeBothDirections() {
        CameraState camera = CameraState.lookingAt(CameraMode.ORTHOGRAPHIC,
                new Vector3d(3.0D, 2.0D, 5.0D), new Vector3d(), new Vector3d(0.0D, 1.0D, 0.0D),
                45.0F, 4.0F, 0.05F, 100.0F);
        Viewport viewport = new Viewport(20, 30, 240, 160);
        CameraMatrices matrices = CameraMatrices.create(camera, viewport);

        Vector3d origin = new Vector3d();
        for (GizmoHandle axis : new GizmoHandle[] {GizmoHandle.X, GizmoHandle.Y, GizmoHandle.Z}) {
            for (double direction : new double[] {-1.0D, 1.0D}) {
                ProjectedPoint point = Projection.project(
                        GizmoHitTesting.axisEndpoint(origin, axis, 0.8D * direction), matrices, viewport);
                if (point.visible()) {
                    assertEquals(axis, GizmoHitTesting.moveHandleAt(point.screenX(), point.screenY(), origin,
                            matrices, viewport, 1.25D, 6.0D));
                }
            }
        }
        assertTrue(GizmoHitTesting.axisEndpoint(origin, GizmoHandle.X, -1.0D).x < 0.0D);
    }
}
