package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewTransform;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

class BlockPreviewTransformTest {
    @Test
    void blockCenterUsesYuushyasScaleThenTranslatePivot() {
        EditorTransform transform = new EditorTransform(new Vector3d(16.0D, 32.0D, -16.0D),
                new Quaternionf().rotationY((float) (Math.PI * 0.5D)), new Vector3f(2.0F, 3.0F, 4.0F));

        Vector4f center = BlockPreviewTransform.matrix(transform)
                .transform(new Vector4f(0.5F, 0.5F, 0.5F, 1.0F));

        assertEquals(2.0F, center.x, 1.0e-5F);
        assertEquals(6.0F, center.y, 1.0e-5F);
        assertEquals(-4.0F, center.z, 1.0e-5F);
        assertEquals(new Vector3d(2.0D, 6.0D, -4.0D), BlockPreviewTransform.pivot(transform));
    }

    @Test
    void matrixMatchesScaleTranslateRotateComposition() {
        EditorTransform transform = new EditorTransform(new Vector3d(16.0D, 0.0D, 0.0D),
                new Quaternionf().rotationZ((float) (Math.PI * 0.5D)), new Vector3f(2.0F));

        Vector4f point = BlockPreviewTransform.matrix(transform)
                .transform(new Vector4f(1.0F, 0.5F, 0.5F, 1.0F));

        assertEquals(2.0F, point.x, 1.0e-5F);
        assertEquals(1.0F, point.y, 1.0e-5F);
        assertEquals(0.0F, point.z, 1.0e-5F);
    }
}
