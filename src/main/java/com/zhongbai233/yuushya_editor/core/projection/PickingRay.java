package com.zhongbai233.yuushya_editor.core.projection;

import java.util.Optional;
import java.util.OptionalDouble;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class PickingRay {
    private final Vector3d origin;
    private final Vector3d direction;

    public PickingRay(Vector3dc origin, Vector3dc direction) {
        this.origin = finite(origin, "origin");
        this.direction = finite(direction, "direction");
        if (this.direction.lengthSquared() <= 1.0e-12D) throw new IllegalArgumentException("direction is zero");
        this.direction.normalize();
    }

    public Vector3d origin() { return new Vector3d(origin); }
    public Vector3d direction() { return new Vector3d(direction); }
    public Vector3d pointAt(double distance) { return new Vector3d(direction).mul(distance).add(origin); }

    public Optional<RayHit> intersectRectangle(Vector3dc center, Vector3dc localX, Vector3dc localY,
            double halfWidth, double halfHeight) {
        Vector3d xAxis = normalized(localX, "localX");
        Vector3d yAxis = normalized(localY, "localY");
        if (!Double.isFinite(halfWidth) || !Double.isFinite(halfHeight) || halfWidth <= 0 || halfHeight <= 0) {
            throw new IllegalArgumentException("rectangle dimensions must be positive and finite");
        }
        Vector3d normal = xAxis.cross(yAxis, new Vector3d());
        if (normal.lengthSquared() <= 1.0e-12D) throw new IllegalArgumentException("rectangle axes are parallel");
        normal.normalize();
        double denominator = normal.dot(direction);
        if (Math.abs(denominator) <= 1.0e-9D) return Optional.empty();
        double distance = normal.dot(new Vector3d(center).sub(origin)) / denominator;
        if (!Double.isFinite(distance) || distance < 0) return Optional.empty();
        Vector3d point = pointAt(distance);
        Vector3d offset = new Vector3d(point).sub(center);
        double x = offset.dot(xAxis);
        double y = offset.dot(yAxis);
        return Math.abs(x) <= halfWidth + 1.0e-9D && Math.abs(y) <= halfHeight + 1.0e-9D
                ? Optional.of(new RayHit(distance, point, x, y)) : Optional.empty();
    }

    public OptionalDouble intersectAabb(Vector3dc minimum, Vector3dc maximum) {
        Vector3d min = finite(minimum, "minimum");
        Vector3d max = finite(maximum, "maximum");
        if (min.x > max.x || min.y > max.y || min.z > max.z) throw new IllegalArgumentException("invalid AABB");
        double near = 0.0D;
        double far = Double.POSITIVE_INFINITY;
        for (int axis = 0; axis < 3; axis++) {
            double d = direction.get(axis);
            double o = origin.get(axis);
            if (Math.abs(d) <= 1.0e-12D) {
                if (o < min.get(axis) || o > max.get(axis)) return OptionalDouble.empty();
                continue;
            }
            double first = (min.get(axis) - o) / d;
            double second = (max.get(axis) - o) / d;
            if (first > second) { double swap = first; first = second; second = swap; }
            near = Math.max(near, first);
            far = Math.min(far, second);
            if (near > far) return OptionalDouble.empty();
        }
        return far >= 0.0D ? OptionalDouble.of(near) : OptionalDouble.empty();
    }

    private static Vector3d normalized(Vector3dc value, String name) {
        Vector3d result = finite(value, name);
        if (result.lengthSquared() <= 1.0e-12D) throw new IllegalArgumentException(name + " is zero");
        return result.normalize();
    }

    private static Vector3d finite(Vector3dc value, String name) {
        java.util.Objects.requireNonNull(value, name);
        if (!Double.isFinite(value.x()) || !Double.isFinite(value.y()) || !Double.isFinite(value.z())) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return new Vector3d(value);
    }
}
