package com.zhongbai233.yuushya_editor.core.camera;

import com.zhongbai233.yuushya_editor.core.projection.Viewport;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

/**
 * Host-neutral editor camera navigation extracted from NCPB. Every operation returns a new immutable state.
 */
public final class CameraController {
    private static final double EPSILON = 1.0e-9D;
    private final Settings settings;

    public CameraController() {
        this(Settings.defaults());
    }

    public CameraController(Settings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public CameraState orbit(CameraState state, double yawRadians, double pitchRadians, Vector3dc worldUp) {
        Objects.requireNonNull(state, "state");
        requireFinite(yawRadians, "yawRadians");
        requireFinite(pitchRadians, "pitchRadians");
        Vector3f up = normalized(worldUp, "worldUp");
        Vector3d focus = state.focus();
        Vector3d offset = state.position().sub(focus);
        double distance = offset.length();
        if (distance <= EPSILON) throw new IllegalArgumentException("camera position and focus must differ");

        Vector3f offsetF = new Vector3f((float) offset.x, (float) offset.y, (float) offset.z);
        new Quaternionf().rotateAxis((float) -yawRadians, up.x, up.y, up.z).transform(offsetF);
        Vector3f forward = new Vector3f(offsetF).negate().normalize();
        Vector3f right = forward.cross(up, new Vector3f());
        if (right.lengthSquared() > 1.0e-8F && pitchRadians != 0.0D) {
            right.normalize();
            Vector3f normalizedOffset = new Vector3f(offsetF).normalize();
            double currentElevation = Math.asin(clamp(normalizedOffset.dot(up), -1.0D, 1.0D));
            double maximumElevation = Math.toRadians(90.0D - settings.minimumPoleAngleDegrees());
            // Positive pitch rotates the camera offset toward -worldUp. Clamp the requested
            // elevation before rotating so a large mouse impulse cannot jump across a pole.
            double targetElevation = clamp(currentElevation - pitchRadians,
                    -maximumElevation, maximumElevation);
            double safePitch = currentElevation - targetElevation;
            new Quaternionf().rotateAxis((float) safePitch, right.x, right.y, right.z).transform(offsetF);
        }
        offsetF.normalize((float) distance);
        Vector3d position = new Vector3d(focus).add(offsetF.x, offsetF.y, offsetF.z);
        return lookingAtLike(state, position, focus, new Vector3d(up));
    }

    /** Mouse deltas use a grab-the-scene convention. */
    public CameraState panPixels(CameraState state, double deltaPixelsX, double deltaPixelsY, Viewport viewport) {
        requireFinite(deltaPixelsX, "deltaPixelsX");
        requireFinite(deltaPixelsY, "deltaPixelsY");
        Objects.requireNonNull(viewport, "viewport");
        double unitsPerPixel = worldUnitsPerPixel(state, viewport);
        Vector3f right = state.orientation().transform(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f up = state.orientation().transform(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3d translation = new Vector3d(right).mul(-deltaPixelsX * unitsPerPixel)
                .add(new Vector3d(up).mul(deltaPixelsY * unitsPerPixel));
        return state.withPose(state.position().add(translation), state.orientation(), state.focus().add(translation));
    }

    /** Positive wheel steps zoom toward the focus. */
    public CameraState dolly(CameraState state, double wheelSteps) {
        requireFinite(wheelSteps, "wheelSteps");
        double factor = Math.exp(-settings.dollyExponent() * wheelSteps);
        if (state.mode() == CameraMode.ORTHOGRAPHIC) {
            double scale = clamp(state.orthoScale() * factor, settings.minimumOrthoScale(),
                    settings.maximumOrthoScale());
            return state.withOrthoScale((float) scale);
        }
        Vector3d focus = state.focus();
        Vector3d offset = state.position().sub(focus);
        double distance = offset.length();
        if (distance <= EPSILON) throw new IllegalArgumentException("camera position and focus must differ");
        double targetDistance = clamp(distance * factor, settings.minimumDistance(), settings.maximumDistance());
        Vector3d position = new Vector3d(focus).add(offset.mul(targetDistance / distance));
        return state.withPose(position, state.orientation(), focus);
    }

    public CameraState focus(CameraState state, Vector3dc center, double boundingRadius,
            Viewport viewport, Vector3dc worldUp) {
        Objects.requireNonNull(viewport, "viewport");
        requireFinite(boundingRadius, "boundingRadius");
        if (boundingRadius < 0.0D) throw new IllegalArgumentException("boundingRadius must be non-negative");
        double radius = Math.max(settings.minimumFocusRadius(), boundingRadius) * settings.focusMargin();
        Vector3f backward = state.orientation().transform(new Vector3f(0.0F, 0.0F, 1.0F)).normalize();
        if (state.mode() == CameraMode.ORTHOGRAPHIC) {
            float scale = (float) clamp(radius, settings.minimumOrthoScale(), settings.maximumOrthoScale());
            Vector3d position = new Vector3d(center).add(new Vector3d(backward)
                    .mul(Math.max(settings.minimumDistance(), state.position().distance(state.focus()))));
            return lookingAtLike(state.withOrthoScale(scale), position, center, worldUp);
        }
        double verticalHalfFov = Math.toRadians(state.fovDegrees()) * 0.5D;
        double horizontalHalfFov = Math.atan(Math.tan(verticalHalfFov) * viewport.aspectRatio());
        double limitingHalfFov = Math.min(verticalHalfFov, horizontalHalfFov);
        double distance = clamp(radius / Math.sin(limitingHalfFov), settings.minimumDistance(),
                settings.maximumDistance());
        Vector3d position = new Vector3d(center).add(new Vector3d(backward).mul(distance));
        return lookingAtLike(state, position, center, worldUp);
    }

    public CameraState standardView(CameraState state, StandardCameraView view, Vector3dc worldUp) {
        Objects.requireNonNull(view, "view");
        Vector3d backward = switch (view) {
            case FRONT -> new Vector3d(0.0D, 0.0D, 1.0D);
            case BACK -> new Vector3d(0.0D, 0.0D, -1.0D);
            case LEFT -> new Vector3d(-1.0D, 0.0D, 0.0D);
            case RIGHT -> new Vector3d(1.0D, 0.0D, 0.0D);
            case TOP -> new Vector3d(0.0D, 1.0D, 0.0D);
            case BOTTOM -> new Vector3d(0.0D, -1.0D, 0.0D);
        };
        Vector3d focus = state.focus();
        double distance = Math.max(settings.minimumDistance(), state.position().distance(focus));
        Vector3d position = new Vector3d(focus).add(backward.mul(distance));
        Vector3dc effectiveUp = (view == StandardCameraView.TOP || view == StandardCameraView.BOTTOM)
                ? new Vector3d(0.0D, 0.0D, view == StandardCameraView.TOP ? -1.0D : 1.0D)
                : worldUp;
        return lookingAtLike(state, position, focus, effectiveUp);
    }

    /** Switches projection while preserving the focus-plane vertical framing. */
    public CameraState switchProjection(CameraState state, CameraMode targetMode) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(targetMode, "targetMode");
        if (state.mode() == targetMode) return state;
        double halfFov = Math.toRadians(state.fovDegrees()) * 0.5D;
        if (targetMode == CameraMode.ORTHOGRAPHIC) {
            double distance = state.position().distance(state.focus());
            double scale = clamp(distance * Math.tan(halfFov), settings.minimumOrthoScale(),
                    settings.maximumOrthoScale());
            return state.withOrthoScale((float) scale).withMode(CameraMode.ORTHOGRAPHIC);
        }
        if (targetMode != CameraMode.PERSPECTIVE) {
            throw new IllegalArgumentException("targetMode must be PERSPECTIVE or ORTHOGRAPHIC");
        }
        Vector3f backward = state.orientation().transform(new Vector3f(0.0F, 0.0F, 1.0F));
        double distance = clamp(state.orthoScale() / Math.tan(halfFov), settings.minimumDistance(),
                settings.maximumDistance());
        Vector3d focus = state.focus();
        Vector3d position = new Vector3d(focus).add(new Vector3d(backward).mul(distance));
        return state.withPose(position, state.orientation(), focus).withMode(CameraMode.PERSPECTIVE);
    }

    private static double worldUnitsPerPixel(CameraState state, Viewport viewport) {
        if (state.mode() == CameraMode.ORTHOGRAPHIC) return 2.0D * state.orthoScale() / viewport.height();
        double distance = state.position().distance(state.focus());
        return 2.0D * distance * Math.tan(Math.toRadians(state.fovDegrees()) * 0.5D) / viewport.height();
    }

    private static CameraState lookingAtLike(CameraState source, Vector3dc position, Vector3dc focus,
            Vector3dc worldUp) {
        return CameraState.lookingAt(source.mode(), position, focus, worldUp, source.fovDegrees(),
                source.orthoScale(), source.nearPlane(), source.farPlane());
    }

    private static Vector3f normalized(Vector3dc value, String name) {
        Objects.requireNonNull(value, name);
        if (!Double.isFinite(value.x()) || !Double.isFinite(value.y()) || !Double.isFinite(value.z())) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        Vector3f result = new Vector3f((float) value.x(), (float) value.y(), (float) value.z());
        if (result.lengthSquared() <= 1.0e-8F) throw new IllegalArgumentException(name + " must be non-zero");
        return result.normalize();
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Settings(double dollyExponent, double minimumDistance, double maximumDistance,
            double minimumOrthoScale, double maximumOrthoScale, double minimumPoleAngleDegrees,
            double minimumFocusRadius, double focusMargin) {
        public Settings {
            if (!positiveFinite(dollyExponent) || !positiveFinite(minimumDistance)
                    || !positiveFinite(maximumDistance) || maximumDistance < minimumDistance
                    || !positiveFinite(minimumOrthoScale) || !positiveFinite(maximumOrthoScale)
                    || maximumOrthoScale < minimumOrthoScale
                    || !Double.isFinite(minimumPoleAngleDegrees) || minimumPoleAngleDegrees <= 0.0D
                    || minimumPoleAngleDegrees >= 45.0D || !positiveFinite(minimumFocusRadius)
                    || !Double.isFinite(focusMargin) || focusMargin < 1.0D) {
                throw new IllegalArgumentException("invalid camera controller settings");
            }
        }

        public static Settings defaults() {
            return new Settings(0.18D, 0.05D, 4096.0D, 0.01D, 4096.0D, 2.0D, 0.05D, 1.15D);
        }

        private static boolean positiveFinite(double value) {
            return Double.isFinite(value) && value > 0.0D;
        }
    }
}
