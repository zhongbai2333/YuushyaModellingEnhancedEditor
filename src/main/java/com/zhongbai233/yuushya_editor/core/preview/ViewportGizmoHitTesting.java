package com.zhongbai233.yuushya_editor.core.preview;

import com.zhongbai233.scene_editor.core.camera.CameraMatrices;
import com.zhongbai233.scene_editor.core.gizmo.GizmoHandle;
import com.zhongbai233.scene_editor.core.gizmo.GizmoHitTesting;
import com.zhongbai233.scene_editor.core.projection.EditorProjection;
import com.zhongbai233.scene_editor.core.projection.EditorViewport;
import com.zhongbai233.scene_editor.core.projection.ProjectedPoint;
import java.util.Objects;
import org.joml.Vector3dc;

/** Screen-clipped axis picking for handles whose endpoints may lie outside the viewport. */
public final class ViewportGizmoHitTesting {
    private static final GizmoHandle[] AXES = {GizmoHandle.X, GizmoHandle.Y, GizmoHandle.Z};
    private static final double EPSILON = 1.0E-12D;
    private static final int RING_SEGMENTS = 48;

    private ViewportGizmoHitTesting() {
    }

    public static GizmoHandle axisHandleAt(double mouseX, double mouseY, Vector3dc origin,
            CameraMatrices matrices, EditorViewport viewport, double axisLength, double hitRadius) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(matrices, "matrices");
        Objects.requireNonNull(viewport, "viewport");
        if (!Double.isFinite(mouseX) || !Double.isFinite(mouseY)
                || !Double.isFinite(axisLength) || axisLength <= 0.0D
                || !Double.isFinite(hitRadius) || hitRadius <= 0.0D) {
            throw new IllegalArgumentException("gizmo coordinates and dimensions must be finite");
        }

        ProjectedPoint center = EditorProjection.project(origin, matrices, viewport);
        if (!projectable(center)
                || Math.hypot(mouseX - center.screenX(), mouseY - center.screenY()) <= hitRadius) {
            return GizmoHandle.NONE;
        }

        GizmoHandle best = GizmoHandle.NONE;
        double bestDistance = hitRadius;
        for (GizmoHandle axis : AXES) {
            for (double direction : new double[] {-1.0D, 1.0D}) {
                ProjectedPoint endpoint = EditorProjection.project(
                        GizmoHitTesting.axisEndpoint(origin, axis, axisLength * direction),
                        matrices, viewport);
                double distance = distanceToVisibleSegment(
                        mouseX, mouseY, center, endpoint, viewport);
                if (distance <= bestDistance) {
                    bestDistance = distance;
                    best = axis;
                }
            }
        }
        return best;
    }

    public static GizmoHandle rotationHandleAt(double mouseX, double mouseY, Vector3dc origin,
            CameraMatrices matrices, EditorViewport viewport, double ringRadius, double hitRadius) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(matrices, "matrices");
        Objects.requireNonNull(viewport, "viewport");
        if (!Double.isFinite(mouseX) || !Double.isFinite(mouseY)
                || !Double.isFinite(ringRadius) || ringRadius <= 0.0D
                || !Double.isFinite(hitRadius) || hitRadius <= 0.0D) {
            throw new IllegalArgumentException("gizmo coordinates and dimensions must be finite");
        }

        GizmoHandle best = GizmoHandle.NONE;
        double bestDistance = hitRadius;
        for (GizmoHandle axis : AXES) {
            ProjectedPoint previous = EditorProjection.project(
                    GizmoHitTesting.ringPoint(origin, axis, ringRadius, 0.0D), matrices, viewport);
            for (int segment = 1; segment <= RING_SEGMENTS; segment++) {
                double angle = Math.PI * 2.0D * segment / RING_SEGMENTS;
                ProjectedPoint current = EditorProjection.project(
                        GizmoHitTesting.ringPoint(origin, axis, ringRadius, angle), matrices, viewport);
                double distance = distanceToVisibleSegment(mouseX, mouseY, previous, current, viewport);
                if (distance <= bestDistance) {
                    bestDistance = distance;
                    best = axis;
                }
                previous = current;
            }
        }
        return best;
    }

    private static double distanceToVisibleSegment(double mouseX, double mouseY,
            ProjectedPoint start, ProjectedPoint end, EditorViewport viewport) {
        if (!projectable(start) || !projectable(end)) return Double.POSITIVE_INFINITY;
        if ((start.depth() < -1.0D && end.depth() < -1.0D)
                || (start.depth() > 1.0D && end.depth() > 1.0D)) {
            return Double.POSITIVE_INFINITY;
        }

        double dx = end.screenX() - start.screenX();
        double dy = end.screenY() - start.screenY();
        ClipInterval interval = new ClipInterval(0.0D, 1.0D);
        interval = clip(interval, start.screenX(), dx, viewport.x(), viewport.x() + viewport.width());
        if (interval == null) return Double.POSITIVE_INFINITY;
        interval = clip(interval, start.screenY(), dy, viewport.y(), viewport.y() + viewport.height());
        if (interval == null) return Double.POSITIVE_INFINITY;

        double x1 = start.screenX() + dx * interval.minimum();
        double y1 = start.screenY() + dy * interval.minimum();
        double x2 = start.screenX() + dx * interval.maximum();
        double y2 = start.screenY() + dy * interval.maximum();
        return distanceToSegment(mouseX, mouseY, x1, y1, x2, y2);
    }

    private static ClipInterval clip(ClipInterval interval, double start, double delta,
            double minimum, double maximum) {
        if (Math.abs(delta) <= EPSILON) {
            return start >= minimum && start <= maximum ? interval : null;
        }
        double first = (minimum - start) / delta;
        double second = (maximum - start) / delta;
        if (first > second) {
            double swap = first;
            first = second;
            second = swap;
        }
        double clippedMinimum = Math.max(interval.minimum(), first);
        double clippedMaximum = Math.min(interval.maximum(), second);
        return clippedMinimum <= clippedMaximum
                ? new ClipInterval(clippedMinimum, clippedMaximum) : null;
    }

    private static double distanceToSegment(double px, double py,
            double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= EPSILON) return Math.hypot(px - x1, py - y1);
        double parameter = Math.clamp(((px - x1) * dx + (py - y1) * dy) / lengthSquared, 0.0D, 1.0D);
        return Math.hypot(px - (x1 + parameter * dx), py - (y1 + parameter * dy));
    }

    private static boolean projectable(ProjectedPoint point) {
        return !point.behindCamera()
                && Double.isFinite(point.screenX())
                && Double.isFinite(point.screenY())
                && Double.isFinite(point.depth());
    }

    private record ClipInterval(double minimum, double maximum) {
    }
}
