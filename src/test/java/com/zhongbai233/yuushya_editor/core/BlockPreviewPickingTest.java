package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewPicking;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewTransform;
import com.zhongbai233.scene_editor.core.projection.PickingRay;
import java.util.OptionalDouble;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class BlockPreviewPickingTest {
    @Test
    void transformedUnitCubeReportsComparableWorldRayDistances() {
        PickingRay ray = new PickingRay(new Vector3d(0.0D, 0.0D, 5.0D),
                new Vector3d(0.0D, 0.0D, -1.0D));
        EditorTransform far = new EditorTransform(new Vector3d(), new Quaternionf(), new Vector3f(1.0F));
        EditorTransform near = new EditorTransform(new Vector3d(0.0D, 0.0D, 16.0D),
                new Quaternionf().rotationY(0.35F), new Vector3f(2.0F));

        double nearDistance = BlockPreviewPicking.hitDistance(ray, near).orElseThrow();
        double farDistance = BlockPreviewPicking.hitDistance(ray, far).orElseThrow();

        assertTrue(nearDistance < farDistance);
        assertEquals(4.5D, farDistance, 1.0E-6D);
    }

    @Test
    void pickingUsesRenderedVolumeInsteadOfPivotDistance() {
        PickingRay ray = new PickingRay(new Vector3d(1.25D, 0.0D, 5.0D),
                new Vector3d(0.0D, 0.0D, -1.0D));
        EditorTransform wide = new EditorTransform(new Vector3d(), new Quaternionf(),
                new Vector3f(3.0F, 1.0F, 1.0F));
        EditorTransform unit = EditorTransform.IDENTITY;

        assertTrue(BlockPreviewPicking.hitDistance(ray, wide).isPresent());
        assertEquals(OptionalDouble.empty(), BlockPreviewPicking.hitDistance(ray, unit));
    }

    @Test
    void textPickingUsesTheRenderedTextPlaneInsteadOfAUnitCube() {
        var textMatrix = BlockPreviewTransform.textMatrix(EditorTransform.IDENTITY);
        PickingRay throughVisibleText = new PickingRay(new Vector3d(2.5D, 0.05D, 3.0D),
                new Vector3d(0.0D, 0.0D, -1.0D));
        PickingRay beyondTextWidth = new PickingRay(new Vector3d(5.7D, 0.05D, 3.0D),
                new Vector3d(0.0D, 0.0D, -1.0D));

        assertTrue(BlockPreviewPicking.hitDistance(throughVisibleText, textMatrix,
                0.0D, 0.0D, -0.08D, 60.0D, 9.0D, 0.08D).isPresent());
        assertEquals(OptionalDouble.empty(), BlockPreviewPicking.hitDistance(beyondTextWidth, textMatrix,
                0.0D, 0.0D, -0.08D, 60.0D, 9.0D, 0.08D));
    }
}
