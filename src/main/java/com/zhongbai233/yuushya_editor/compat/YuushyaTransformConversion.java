package com.zhongbai233.yuushya_editor.compat;

import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Converts Yuushya's Z-Y-X applied Euler degrees to the editor's immutable quaternion representation. */
public final class YuushyaTransformConversion {
    private static final float RADIANS_TO_DEGREES = (float) (180.0D / Math.PI);

    private YuushyaTransformConversion() { }

    public static Quaternionf fromEulerDegrees(Vector3fc xyzDegrees) {
        Objects.requireNonNull(xyzDegrees, "xyzDegrees");
        if (!finite(xyzDegrees)) throw new IllegalArgumentException("Euler angles must be finite");
        return new Quaternionf().rotationZYX(
                (float) Math.toRadians(xyzDegrees.z()),
                (float) Math.toRadians(xyzDegrees.y()),
                (float) Math.toRadians(xyzDegrees.x()));
    }

    public static Vector3f toEulerDegrees(Quaternionfc rotation) {
        Quaternionf normalized = new Quaternionf(Objects.requireNonNull(rotation, "rotation"));
        if (!finite(normalized) || normalized.lengthSquared() <= 1.0e-8F) {
            throw new IllegalArgumentException("rotation must be finite and non-zero");
        }
        Vector3f radians = normalized.normalize().getEulerAnglesZYX(new Vector3f());
        return radians.mul(RADIANS_TO_DEGREES);
    }

    private static boolean finite(Vector3fc value) {
        return Float.isFinite(value.x()) && Float.isFinite(value.y()) && Float.isFinite(value.z());
    }

    private static boolean finite(Quaternionfc value) {
        return Float.isFinite(value.x()) && Float.isFinite(value.y())
                && Float.isFinite(value.z()) && Float.isFinite(value.w());
    }
}
