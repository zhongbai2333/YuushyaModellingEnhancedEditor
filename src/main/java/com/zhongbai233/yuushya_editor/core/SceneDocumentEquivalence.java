package com.zhongbai233.yuushya_editor.core;

import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

/** Compares persisted model content while ignoring editor-only layer IDs, names, and selection. */
public final class SceneDocumentEquivalence {
    private static final double POSITION_EPSILON = 1.0E-7D;
    private static final float SCALE_EPSILON = 1.0E-6F;
    private static final float ROTATION_EPSILON = 1.0E-6F;

    private SceneDocumentEquivalence() { }

    public static boolean samePersistedContent(SceneDocument<?> first, SceneDocument<?> second) {
        if (!first.collisionShape().equals(second.collisionShape())) return false;
        List<? extends SceneLayer<?>> firstLayers = first.layers();
        List<? extends SceneLayer<?>> secondLayers = second.layers();
        if (firstLayers.size() != secondLayers.size()) return false;
        for (int index = 0; index < firstLayers.size(); index++) {
            SceneLayer<?> left = firstLayers.get(index);
            SceneLayer<?> right = secondLayers.get(index);
            if (left.visible() != right.visible() || !left.hostData().equals(right.hostData())
                    || !sameTransform(left.transform(), right.transform())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameTransform(EditorTransform first, EditorTransform second) {
        Vector3d firstPosition = first.position();
        Vector3d secondPosition = second.position();
        if (firstPosition.distanceSquared(secondPosition) > POSITION_EPSILON * POSITION_EPSILON) return false;
        Vector3f firstScale = first.scale();
        Vector3f secondScale = second.scale();
        if (firstScale.distanceSquared(secondScale) > SCALE_EPSILON * SCALE_EPSILON) return false;
        Quaternionf firstRotation = first.rotation();
        Quaternionf secondRotation = second.rotation();
        return 1.0F - Math.abs(firstRotation.dot(secondRotation)) <= ROTATION_EPSILON;
    }
}
