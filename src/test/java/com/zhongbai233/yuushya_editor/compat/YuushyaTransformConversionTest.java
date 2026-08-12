package com.zhongbai233.yuushya_editor.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class YuushyaTransformConversionTest {
    @Test
    void eulerQuaternionRoundTripPreservesOrientation() {
        Vector3f euler = new Vector3f(20.0F, -35.0F, 70.0F);
        Quaternionf expected = YuushyaTransformConversion.fromEulerDegrees(euler);
        Vector3f roundTripEuler = YuushyaTransformConversion.toEulerDegrees(expected);
        Quaternionf actual = YuushyaTransformConversion.fromEulerDegrees(roundTripEuler);
        assertEquals(1.0F, Math.abs(expected.dot(actual)), 1.0e-5F);
    }

    @Test
    void rejectsInvalidAnglesAndRotations() {
        assertThrows(IllegalArgumentException.class, () -> YuushyaTransformConversion.fromEulerDegrees(
                new Vector3f(Float.NaN, 0.0F, 0.0F)));
        assertThrows(IllegalArgumentException.class, () -> YuushyaTransformConversion.toEulerDegrees(
                new Quaternionf(0.0F, 0.0F, 0.0F, 0.0F)));
    }
}
