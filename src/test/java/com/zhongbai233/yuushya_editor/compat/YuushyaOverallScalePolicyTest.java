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

class YuushyaOverallScalePolicyTest {
    @Test
    void changedScaleUniformizesAxesAndCompensatesPosition() {
        EditorTransform current = new EditorTransform(new Vector3d(12.0D, -8.0D, 4.0D),
                new Quaternionf(), new Vector3f(2.0F, 3.0F, 4.0F));

        EditorTransform changed = YuushyaOverallScalePolicy.apply(current, current.position(),
                current.rotation(), 4.0F);

        assertEquals(new Vector3d(6.0D, -4.0D, 2.0D), changed.position());
        assertEquals(new Vector3f(4.0F), changed.scale());
    }

    @Test
    void scaleCompensationPreservesYuushyaPivotForUniformData() {
        EditorTransform current = new EditorTransform(new Vector3d(12.0D, -8.0D, 4.0D),
                new Quaternionf(), new Vector3f(2.0F));

        EditorTransform changed = YuushyaOverallScalePolicy.apply(current, current.position(),
                current.rotation(), 5.0F);

        Vector3d expected = BlockPreviewTransform.pivot(current);
        Vector3d actual = BlockPreviewTransform.pivot(changed);
        assertAll(
                () -> assertEquals(expected.x, actual.x, 1.0e-12D),
                () -> assertEquals(expected.y, actual.y, 1.0e-12D),
                () -> assertEquals(expected.z, actual.z, 1.0e-12D));
    }

    @Test
    void unchangedScalePreservesLegacyNonUniformVector() {
        EditorTransform current = new EditorTransform(new Vector3d(), new Quaternionf(),
                new Vector3f(2.0F, 3.0F, 4.0F));

        EditorTransform committed = YuushyaOverallScalePolicy.apply(current,
                new Vector3d(1.0D, 2.0D, 3.0D), new Quaternionf().rotationX(0.25F), 2.0F);

        assertEquals(new Vector3f(2.0F, 3.0F, 4.0F), committed.scale());
        assertEquals(new Vector3d(1.0D, 2.0D, 3.0D), committed.position());
    }

    @Test
    void changedScaleRejectsNonPositiveOrNonFiniteValues() {
        EditorTransform current = EditorTransform.IDENTITY;
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> apply(current, 0.0F)),
                () -> assertThrows(IllegalArgumentException.class, () -> apply(current, -1.0F)),
                () -> assertThrows(IllegalArgumentException.class, () -> apply(current, Float.NaN)),
                () -> assertThrows(IllegalArgumentException.class, () -> apply(current, Float.POSITIVE_INFINITY)));
    }

    private static EditorTransform apply(EditorTransform current, float scale) {
        return YuushyaOverallScalePolicy.apply(current, current.position(), current.rotation(), scale);
    }
}
