package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhongbai233.yuushya_editor.core.camera.CameraFrame;
import com.zhongbai233.yuushya_editor.core.camera.CameraMatrices;
import com.zhongbai233.yuushya_editor.core.camera.CameraMode;
import com.zhongbai233.yuushya_editor.core.camera.CameraState;
import com.zhongbai233.yuushya_editor.core.gizmo.GizmoSizingPolicy;
import com.zhongbai233.yuushya_editor.core.projection.Viewport;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class GizmoSizingPolicyTest {
    @Test
    void largeBlockMovesAllHandlesOutsideItsBoundingSphere() {
        Viewport viewport = new Viewport(0, 0, 800, 600);
        CameraState camera = CameraState.lookingAt(CameraMode.PERSPECTIVE,
                new Vector3d(10.0D, 8.0D, 14.0D), new Vector3d(), new Vector3d(0.0D, 1.0D, 0.0D),
                45.0F, 6.0F, 0.05F, 100.0F);
        CameraFrame frame = new CameraFrame(CameraMatrices.create(camera, viewport), viewport, camera.mode());
        EditorTransform transform = EditorTransform.IDENTITY.withScale(new Vector3f(8.0F));

        double blockRadius = GizmoSizingPolicy.worldBoundingRadius(transform);
        GizmoSizingPolicy.Sizes sizes = GizmoSizingPolicy.calculate(transform, frame);

        assertTrue(sizes.axisLength() > blockRadius);
        assertTrue(sizes.rotationRadius() > blockRadius);
        assertTrue(sizes.scaleHandleLength() > blockRadius);
    }
}
