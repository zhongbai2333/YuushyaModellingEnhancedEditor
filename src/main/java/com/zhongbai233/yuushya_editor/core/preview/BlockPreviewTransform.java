package com.zhongbai233.yuushya_editor.core.preview;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

/** Builds Yuushya's editor-space transform for a unit block. */
public final class BlockPreviewTransform {
    public static final double MODEL_UNITS_PER_WORLD_UNIT = 16.0D;

    private BlockPreviewTransform() { }

    public static Matrix4f matrix(EditorTransform transform) {
        EditorTransform required = Objects.requireNonNull(transform, "transform");
        Vector3d position = required.position().div(MODEL_UNITS_PER_WORLD_UNIT);
        Vector3f scale = required.scale();
        return new Matrix4f()
                .scale(scale)
                .translate((float) position.x, (float) position.y, (float) position.z)
                .rotate(required.rotation())
                .translate(-0.5F, -0.5F, -0.5F);
    }

    /** Yuushya item layers are centered item models and therefore have no unit-cube centering translation. */
    public static Matrix4f itemMatrix(EditorTransform transform) {
        EditorTransform required = Objects.requireNonNull(transform, "transform");
        Vector3d position = required.position().div(MODEL_UNITS_PER_WORLD_UNIT);
        return new Matrix4f().scale(required.scale())
                .translate((float) position.x, (float) position.y, (float) position.z)
                .rotate(required.rotation());
    }

    /** Matches Yuushya's text anchor after moving the edited block cell to editor-centered coordinates. */
    public static Matrix4f textMatrix(EditorTransform transform) {
        EditorTransform required = Objects.requireNonNull(transform, "transform");
        Vector3d position = required.position().div(MODEL_UNITS_PER_WORLD_UNIT);
        return new Matrix4f().translate(-0.5F, 0.5F, -0.5F)
                .scale(required.scale())
                .translate((float) position.x, (float) position.y, (float) position.z)
                .rotate(required.rotation())
                .rotateX((float) Math.PI)
                .scale(0.1F);
    }

    /** Center/pivot visible after Yuushya applies scale before its position translation. */
    public static Vector3d pivot(EditorTransform transform) {
        EditorTransform required = Objects.requireNonNull(transform, "transform");
        Vector3d position = required.position().div(MODEL_UNITS_PER_WORLD_UNIT);
        Vector3f scale = required.scale();
        return position.mul(scale.x, scale.y, scale.z);
    }
}
