package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.*;
import com.zhongbai233.yuushya_editor.core.camera.CameraMatrices;
import com.zhongbai233.yuushya_editor.core.camera.CameraMode;
import com.zhongbai233.yuushya_editor.core.camera.CameraState;
import com.zhongbai233.yuushya_editor.core.projection.PickingRay;
import com.zhongbai233.yuushya_editor.core.projection.Projection;
import com.zhongbai233.yuushya_editor.core.projection.Viewport;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class ProjectionAndPickingTest {
    @Test
    void centerScreenRayHitsOriginBox() {
        CameraState camera = CameraState.lookingAt(CameraMode.PERSPECTIVE,
                new Vector3d(0, 0, 5), new Vector3d(), new Vector3d(0, 1, 0),
                60, 4, 0.05F, 1000);
        Viewport viewport = new Viewport(0, 0, 800, 600);
        PickingRay ray = Projection.rayFromScreen(400, 300, CameraMatrices.create(camera, viewport), viewport);
        assertTrue(ray.intersectAabb(new Vector3d(-0.5), new Vector3d(0.5)).isPresent());
        assertTrue(ray.intersectRectangle(new Vector3d(), new Vector3d(1, 0, 0),
                new Vector3d(0, 1, 0), 1, 1).isPresent());
    }

    @Test
    void originProjectsToViewportCenter() {
        CameraState camera = CameraState.lookingAt(CameraMode.PERSPECTIVE,
                new Vector3d(0, 0, 5), new Vector3d(), new Vector3d(0, 1, 0),
                60, 4, 0.05F, 1000);
        Viewport viewport = new Viewport(20, 30, 800, 600);
        var point = Projection.project(new Vector3d(), CameraMatrices.create(camera, viewport), viewport);
        assertTrue(point.visible());
        assertEquals(420, point.screenX(), 0.001);
        assertEquals(330, point.screenY(), 0.001);
    }
}
