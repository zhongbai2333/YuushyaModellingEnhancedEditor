package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhongbai233.scene_editor.core.camera.EditorCameraController;
import com.zhongbai233.scene_editor.core.camera.CameraMatrices;
import com.zhongbai233.scene_editor.core.camera.EditorCameraMode;
import com.zhongbai233.scene_editor.core.camera.EditorCameraState;
import com.zhongbai233.scene_editor.core.projection.EditorProjection;
import com.zhongbai233.scene_editor.core.projection.EditorViewport;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class CameraControllerTest {
    private static final double EPSILON = 1.0e-5D;
    private static final Vector3d UP = new Vector3d(0.0D, 1.0D, 0.0D);
    private final EditorCameraController controller = new EditorCameraController();

    @Test
    void orbitKeepsFocusAndDistance() {
        EditorCameraState initial = camera(EditorCameraMode.ORBIT, new Vector3d(0.0D, 0.0D, 10.0D));
        EditorCameraState result = controller.orbit(initial, Math.PI * 0.5D, 0.0D, UP);

        assertVector(result.focus(), 0.0D, 0.0D, 0.0D);
        assertEquals(10.0D, result.position().distance(result.focus()), EPSILON);
        assertVector(result.position(), -10.0D, 0.0D, 0.0D);
        Vector3f forward = result.orientation().transform(new Vector3f(0.0F, 0.0F, -1.0F));
        assertEquals(1.0D, forward.x, EPSILON);
    }

    @Test
    void largeVerticalImpulseStopsBeforeCrossingThePole() {
        EditorCameraState initial = camera(EditorCameraMode.ORBIT, new Vector3d(0.0D, 0.0D, 10.0D));
        EditorCameraState upward = controller.orbit(initial, 0.0D, Math.toRadians(200.0D), UP);
        EditorCameraState downward = controller.orbit(initial, 0.0D, Math.toRadians(-200.0D), UP);

        assertEquals(10.0D, upward.position().distance(upward.focus()), EPSILON);
        assertEquals(10.0D, downward.position().distance(downward.focus()), EPSILON);
        assertTrue(upward.position().z > 0.0D, "large upward impulse crossed the camera pole");
        assertTrue(downward.position().z > 0.0D, "large downward impulse crossed the camera pole");
        assertTrue(upward.position().y < 0.0D);
        assertTrue(downward.position().y > 0.0D);
    }

    @Test
    void repeatedVerticalImpulseRemainsStableAtThePoleLimit() {
        EditorCameraState state = camera(EditorCameraMode.ORBIT, new Vector3d(0.0D, 0.0D, 10.0D));
        for (int index = 0; index < 100; index++) {
            state = controller.orbit(state, 0.0D, Math.toRadians(1000.0D), UP);
        }

        assertEquals(10.0D, state.position().distance(state.focus()), EPSILON);
        assertTrue(state.position().z > 0.0D);
        assertTrue(Double.isFinite(state.position().x));
        assertTrue(Double.isFinite(state.position().y));
        assertTrue(Double.isFinite(state.position().z));
    }

    @Test
    void panMovesPositionAndFocusTogether() {
        EditorCameraState initial = camera(EditorCameraMode.ORBIT, new Vector3d(0.0D, 0.0D, 10.0D));
        EditorCameraState result = controller.panPixels(initial, 100.0D, 50.0D, new EditorViewport(0, 0, 800, 400));
        Vector3d positionDelta = result.position().sub(initial.position());
        Vector3d focusDelta = result.focus().sub(initial.focus());
        assertVector(positionDelta, focusDelta.x, focusDelta.y, focusDelta.z);
        assertTrue(positionDelta.x < 0.0D);
        assertTrue(positionDelta.y > 0.0D);
    }

    @Test
    void dollyAndProjectionSwitchPreserveFraming() {
        EditorViewport viewport = new EditorViewport(0, 0, 800, 400);
        EditorCameraState perspective = camera(EditorCameraMode.ORBIT, new Vector3d(0.0D, 0.0D, 10.0D));
        assertTrue(controller.dolly(perspective, 1.0D).position().z < perspective.position().z);

        Vector3d point = new Vector3d(1.0D, 0.0D, 0.0D);
        double perspectiveX = EditorProjection.project(point, CameraMatrices.create(perspective, viewport), viewport)
                .screenX();
        EditorCameraState orthographic = controller.switchProjection(perspective, EditorCameraMode.ORTHOGRAPHIC);
        double orthographicX = EditorProjection.project(point, CameraMatrices.create(orthographic, viewport), viewport)
                .screenX();
        assertEquals(perspectiveX, orthographicX, EPSILON);
        assertEquals(10.0D, controller.switchProjection(orthographic, EditorCameraMode.ORBIT)
                .position().distance(orthographic.focus()), EPSILON);
    }

    private static EditorCameraState camera(EditorCameraMode mode, Vector3d position) {
        return EditorCameraState.lookingAt(mode, position, new Vector3d(), UP, 45.0F, 4.0F, 0.05F, 100.0F);
    }

    private static void assertVector(Vector3d actual, double x, double y, double z) {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);
    }
}
