package com.zhongbai233.yuushya_editor.core.gizmo;

import com.zhongbai233.yuushya_editor.core.camera.CameraMatrices;
import com.zhongbai233.yuushya_editor.core.projection.ProjectedPoint;
import com.zhongbai233.yuushya_editor.core.projection.Projection;
import com.zhongbai233.yuushya_editor.core.projection.Viewport;
import java.util.Objects;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/** Screen-space hit testing for six-way move/scale axes and full rotation rings. */
public final class GizmoHitTesting {
    private static final GizmoHandle[] AXIS_HANDLES = {GizmoHandle.X, GizmoHandle.Y, GizmoHandle.Z};
    private static final int RING_SEGMENTS = 48;

    private GizmoHitTesting() { }

    public static GizmoHandle moveHandleAt(double mouseX, double mouseY, Vector3dc origin,
            CameraMatrices matrices, Viewport viewport, double axisLength, double hitRadius) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(matrices, "matrices");
        Objects.requireNonNull(viewport, "viewport");
        if (!Double.isFinite(mouseX) || !Double.isFinite(mouseY)) {
            throw new IllegalArgumentException("pointer position must be finite");
        }
        if (!Double.isFinite(axisLength) || axisLength <= 0.0D
                || !Double.isFinite(hitRadius) || hitRadius <= 0.0D) {
            throw new IllegalArgumentException("Gizmo dimensions must be positive and finite");
        }

        ProjectedPoint center = Projection.project(origin, matrices, viewport);
        if (!center.visible()) return GizmoHandle.NONE;
        double centerDistance = Math.hypot(mouseX - center.screenX(), mouseY - center.screenY());
        if (centerDistance <= hitRadius) return GizmoHandle.NONE;

        GizmoHandle best = GizmoHandle.NONE;
        double bestDistance = hitRadius;
        for (GizmoHandle handle : AXIS_HANDLES) {
            for (double direction : new double[] {-1.0D, 1.0D}) {
                Vector3d endpoint = axisEndpoint(origin, handle, axisLength * direction);
                ProjectedPoint projectedEndpoint = Projection.project(endpoint, matrices, viewport);
                if (!projectedEndpoint.visible()) continue;
                double distance = distanceToSegment(mouseX, mouseY, center.screenX(), center.screenY(),
                        projectedEndpoint.screenX(), projectedEndpoint.screenY());
                if (distance <= bestDistance) {
                    bestDistance = distance;
                    best = handle;
                }
            }
        }
        return best;
    }

    public static GizmoHandle rotateHandleAt(double mouseX, double mouseY, Vector3dc origin,
            CameraMatrices matrices, Viewport viewport, double ringRadius, double hitRadius) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(matrices, "matrices");
        Objects.requireNonNull(viewport, "viewport");
        if (!Double.isFinite(ringRadius) || ringRadius <= 0.0D
                || !Double.isFinite(hitRadius) || hitRadius <= 0.0D) {
            throw new IllegalArgumentException("Gizmo dimensions must be positive and finite");
        }
        GizmoHandle best = GizmoHandle.NONE;
        double bestDistance = hitRadius;
        for (GizmoHandle handle : AXIS_HANDLES) {
            Vector3d previous = ringPoint(origin, handle, ringRadius, 0.0D);
            ProjectedPoint previousPoint = Projection.project(previous, matrices, viewport);
            for (int segment = 1; segment <= RING_SEGMENTS; segment++) {
                double angle = Math.PI * 2.0D * segment / RING_SEGMENTS;
                ProjectedPoint currentPoint = Projection.project(
                        ringPoint(origin, handle, ringRadius, angle), matrices, viewport);
                if (previousPoint.visible() && currentPoint.visible()) {
                    double distance = distanceToSegment(mouseX, mouseY,
                            previousPoint.screenX(), previousPoint.screenY(),
                            currentPoint.screenX(), currentPoint.screenY());
                    if (distance <= bestDistance) {
                        bestDistance = distance;
                        best = handle;
                    }
                }
                previousPoint = currentPoint;
            }
        }
        return best;
    }

    public static GizmoHandle scaleHandleAt(double mouseX, double mouseY, Vector3dc origin,
            CameraMatrices matrices, Viewport viewport, double handleLength, double hitRadius) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(matrices, "matrices");
        Objects.requireNonNull(viewport, "viewport");
        if (!Double.isFinite(mouseX) || !Double.isFinite(mouseY)
                || !Double.isFinite(handleLength) || handleLength <= 0.0D
                || !Double.isFinite(hitRadius) || hitRadius <= 0.0D) {
            throw new IllegalArgumentException("Scale Gizmo coordinates and dimensions must be finite");
        }
        GizmoHandle best = GizmoHandle.NONE;
        double bestDistance = hitRadius;
        for (GizmoHandle handle : AXIS_HANDLES) {
            for (double direction : new double[] {-1.0D, 1.0D}) {
                ProjectedPoint endpoint = Projection.project(
                        axisEndpoint(origin, handle, handleLength * direction), matrices, viewport);
                if (!endpoint.visible()) continue;
                double distance = Math.hypot(mouseX - endpoint.screenX(), mouseY - endpoint.screenY());
                if (distance <= bestDistance) {
                    bestDistance = distance;
                    best = handle;
                }
            }
        }
        return best;
    }

    /** Shared endpoint calculation used by rendering, tests, and hit testing. */
    public static Vector3d axisEndpoint(Vector3dc origin, GizmoHandle handle, double signedLength) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(handle, "handle");
        if (!Double.isFinite(signedLength) || signedLength == 0.0D) {
            throw new IllegalArgumentException("Axis length must be finite and non-zero");
        }
        return new Vector3d(origin).fma(signedLength, handle.axis());
    }

    public static Vector3d ringPoint(Vector3dc origin, GizmoHandle handle, double radius, double angle) {
        double first = Math.cos(angle) * radius;
        double second = Math.sin(angle) * radius;
        return switch (handle) {
            case X -> new Vector3d(origin).add(0.0D, first, second);
            case Y -> new Vector3d(origin).add(first, 0.0D, second);
            case Z -> new Vector3d(origin).add(first, second, 0.0D);
            case NONE, UNIFORM -> throw new IllegalArgumentException(handle + " does not define a rotation ring");
        };
    }

    static double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 1.0e-12D) return Math.hypot(px - x1, py - y1);
        double parameter = Math.clamp(((px - x1) * dx + (py - y1) * dy) / lengthSquared, 0.0D, 1.0D);
        return Math.hypot(px - (x1 + parameter * dx), py - (y1 + parameter * dy));
    }
}
