package com.zhongbai233.yuushya_editor.compat;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import java.util.Objects;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Applies Yuushya's independent scale fields while keeping the visible pivot stationary. */
public final class YuushyaScalePolicy {
    private YuushyaScalePolicy() { }

    public static EditorTransform apply(EditorTransform current, Vector3dc enteredPosition,
            Quaternionfc enteredRotation, Vector3fc requestedScale) {
        EditorTransform required = Objects.requireNonNull(current, "current");
        Objects.requireNonNull(enteredPosition, "enteredPosition");
        Objects.requireNonNull(enteredRotation, "enteredRotation");
        Vector3f nextScale = new Vector3f(Objects.requireNonNull(requestedScale, "requestedScale"));
        if (!positiveFinite(nextScale.x) || !positiveFinite(nextScale.y) || !positiveFinite(nextScale.z)) {
            throw new IllegalArgumentException("Scale must be positive and finite on every axis");
        }

        Vector3f previousScale = required.scale();
        Vector3d compensatedPosition = new Vector3d(enteredPosition)
                .mul(previousScale.x / (double) nextScale.x,
                        previousScale.y / (double) nextScale.y,
                        previousScale.z / (double) nextScale.z);
        return new EditorTransform(compensatedPosition, enteredRotation, nextScale);
    }

    private static boolean positiveFinite(float value) {
        return Float.isFinite(value) && value > 0.0F;
    }
}
