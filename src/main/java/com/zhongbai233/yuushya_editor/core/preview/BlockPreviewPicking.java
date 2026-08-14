package com.zhongbai233.yuushya_editor.core.preview;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.scene_editor.core.projection.PickingRay;
import java.util.Objects;
import java.util.OptionalDouble;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;

/** Ray intersection against preview content in its rendered local coordinate system. */
public final class BlockPreviewPicking {
    private static final double EPSILON = 1.0E-12D;

    private BlockPreviewPicking() { }

    /** Returns the nearest non-negative world-ray distance, or empty when the layer is missed. */
    public static OptionalDouble hitDistance(PickingRay ray, EditorTransform transform) {
        return hitDistance(ray, BlockPreviewTransform.matrix(Objects.requireNonNull(transform, "transform")),
                0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    }

    /** Intersects a rendered local-space box while preserving comparable world-ray parameters. */
    public static OptionalDouble hitDistance(PickingRay ray, Matrix4fc localToWorld,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        Objects.requireNonNull(ray, "ray");
        Matrix4f inverse = new Matrix4f(Objects.requireNonNull(localToWorld, "localToWorld")).invert();
        Vector3d origin = ray.origin().mulPosition(inverse);
        // Do not normalize after the inverse transform: preserving the original ray parameter makes
        // distances from differently scaled layers directly comparable.
        Vector3d direction = ray.direction().mulDirection(inverse);

        double near = 0.0D;
        double far = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            double axisOrigin = origin.get(axis);
            double axisDirection = direction.get(axis);
            double axisMinimum = axis == 0 ? minX : axis == 1 ? minY : minZ;
            double axisMaximum = axis == 0 ? maxX : axis == 1 ? maxY : maxZ;
            if (Math.abs(axisDirection) <= EPSILON) {
                if (axisOrigin < axisMinimum || axisOrigin > axisMaximum) {
                    return OptionalDouble.empty();
                }
                continue;
            }
            double first = (axisMinimum - axisOrigin) / axisDirection;
            double second = (axisMaximum - axisOrigin) / axisDirection;
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
