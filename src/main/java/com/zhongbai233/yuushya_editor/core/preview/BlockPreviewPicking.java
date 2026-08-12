package com.zhongbai233.yuushya_editor.core.preview;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.projection.PickingRay;
import java.util.Objects;
import java.util.OptionalDouble;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/** Ray intersection against the transformed unit cube used by the block preview renderer. */
public final class BlockPreviewPicking {
    private static final double EPSILON = 1.0E-12D;

    private BlockPreviewPicking() { }

    /** Returns the nearest non-negative world-ray distance, or empty when the layer is missed. */
    public static OptionalDouble hitDistance(PickingRay ray, EditorTransform transform) {
        Objects.requireNonNull(ray, "ray");
        Matrix4f inverse = BlockPreviewTransform.matrix(Objects.requireNonNull(transform, "transform")).invert();
        Vector3d origin = ray.origin().mulPosition(inverse);
        // Do not normalize after the inverse transform: preserving the original ray parameter makes
        // distances from differently scaled layers directly comparable.
        Vector3d direction = ray.direction().mulDirection(inverse);

        double near = 0.0D;
        double far = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            double axisOrigin = origin.get(axis);
            double axisDirection = direction.get(axis);
            if (Math.abs(axisDirection) <= EPSILON) {
                if (axisOrigin < 0.0D || axisOrigin > 1.0D) return OptionalDouble.empty();
                continue;
            }
            double first = -axisOrigin / axisDirection;
            double second = (1.0D - axisOrigin) / axisDirection;
            if (first > second) {
                double swap = first;
                first = second;
                second = swap;
            }
            near = Math.max(near, first);
            far = Math.min(far, second);
            if (near > far) return OptionalDouble.empty();
        }
        return far >= 0.0D && Double.isFinite(near) ? OptionalDouble.of(near) : OptionalDouble.empty();
    }
}
