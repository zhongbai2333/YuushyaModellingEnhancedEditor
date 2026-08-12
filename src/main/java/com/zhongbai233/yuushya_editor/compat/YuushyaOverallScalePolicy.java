package com.zhongbai233.yuushya_editor.compat;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import java.util.Objects;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

/** Reproduces the single overall-scale control exposed by Yuushya's show-block screen. */
public final class YuushyaOverallScalePolicy {
    private YuushyaOverallScalePolicy() { }

    public static EditorTransform apply(EditorTransform current, Vector3dc enteredPosition,
            Quaternionfc enteredRotation, float requestedScale) {
        EditorTransform required = Objects.requireNonNull(current, "current");
        Objects.requireNonNull(enteredPosition, "enteredPosition");
        Objects.requireNonNull(enteredRotation, "enteredRotation");

        Vector3f previousScale = required.scale();
        if (Float.compare(requestedScale, previousScale.x) == 0) {
            // Yuushya's UI reads SCALE_X, but old or externally-authored data can still contain a
            // non-uniform vector. Merely committing other fields must not normalize that data.
            return new EditorTransform(enteredPosition, enteredRotation, previousScale);
        }
        if (!Float.isFinite(requestedScale) || requestedScale <= 0.0F) {
            throw new IllegalArgumentException("Overall scale must be positive and finite");
        }

        double ratio = previousScale.x / (double) requestedScale;
        Vector3d compensatedPosition = new Vector3d(enteredPosition).mul(ratio);
        return new EditorTransform(compensatedPosition, enteredRotation, new Vector3f(requestedScale));
    }
}
