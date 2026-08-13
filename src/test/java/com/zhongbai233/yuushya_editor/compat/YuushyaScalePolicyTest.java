package com.zhongbai233.yuushya_editor.compat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewTransform;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class YuushyaScalePolicyTest {
    @Test
    void independentlyChangesAxesAndPreservesVisiblePivot() {
        EditorTransform current = new EditorTransform(new Vector3d(12.0D, -8.0D, 4.0D),
                new Quaternionf(), new Vector3f(2.0F, 3.0F, 4.0F));

        EditorTransform changed = YuushyaScalePolicy.apply(current, current.position(),
                current.rotation(), new Vector3f(4.0F, 3.0F, 1.0F));

        assertEquals(new Vector3d(6.0D, -8.0D, 16.0D), changed.position());
        assertEquals(new Vector3f(4.0F, 3.0F, 1.0F), changed.scale());
        Vector3d expected = BlockPreviewTransform.pivot(current);
        Vector3d actual = BlockPreviewTransform.pivot(changed);
        assertAll(
                () -> assertEquals(expected.x, actual.x, 1.0e-12D),
                () -> assertEquals(expected.y, actual.y, 1.0e-12D),
                () -> assertEquals(expected.z, actual.z, 1.0e-12D));
    }

    @Test
    void rejectsInvalidAxisValues() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> apply(new Vector3f(0.0F, 1.0F, 1.0F))),
                () -> assertThrows(IllegalArgumentException.class, () -> apply(new Vector3f(1.0F, -1.0F, 1.0F))),
                () -> assertThrows(IllegalArgumentException.class, () -> apply(new Vector3f(1.0F, 1.0F, Float.NaN))));
    }

    private static EditorTransform apply(Vector3f scale) {
        return YuushyaScalePolicy.apply(EditorTransform.IDENTITY, new Vector3d(), new Quaternionf(), scale);
    }
}
