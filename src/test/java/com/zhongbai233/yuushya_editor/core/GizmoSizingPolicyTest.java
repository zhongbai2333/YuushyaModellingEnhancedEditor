package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhongbai233.scene_editor.core.gizmo.GizmoSizingPolicy;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class GizmoSizingPolicyTest {
    @Test
    void largeBlockMovesAllHandlesOutsideItsBoundingSphere() {
        EditorTransform transform = EditorTransform.IDENTITY.withScale(new Vector3f(8.0F));

        double blockRadius = GizmoSizingPolicy.worldBoundingRadius(transform.scale());
        GizmoSizingPolicy.Sizes sizes = GizmoSizingPolicy.calculate(blockRadius);

        assertTrue(sizes.axisLength() > blockRadius);
        assertTrue(sizes.rotationRadius() > blockRadius);
        assertTrue(sizes.scaleHandleLength() > blockRadius);
    }
}
