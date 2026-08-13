package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhongbai233.scene_editor.core.camera.CameraMatrices;
import com.zhongbai233.scene_editor.core.camera.EditorCameraMode;
import com.zhongbai233.scene_editor.core.camera.EditorCameraState;
import com.zhongbai233.scene_editor.core.projection.EditorViewport;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class CameraMatricesTest {
    @Test
    void equivalentCameraSnapshotsHaveValueSemantics() {
        EditorCameraState camera = EditorCameraState.lookingAt(EditorCameraMode.ORBIT,
                new Vector3d(7.5D, 4.0D, 9.25D), new Vector3d(0.5D, 0.75D, -0.25D),
                new Vector3d(0.0D, 1.0D, 0.0D), 55.0F, 4.0F, 0.05F, 1000.0F);
        EditorViewport viewport = new EditorViewport(24, 32, 960, 540);

        CameraMatrices first = CameraMatrices.create(camera, viewport);
        CameraMatrices second = CameraMatrices.create(camera, viewport);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
