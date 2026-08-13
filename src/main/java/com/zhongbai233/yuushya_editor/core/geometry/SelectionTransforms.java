package com.zhongbai233.yuushya_editor.core.geometry;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.SceneDocument;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

/** Applies one world-space manipulation consistently to a group of selected layers. */
public final class SelectionTransforms {
    private SelectionTransforms() { }

    public static <T> Vector3d pivot(SceneDocument<T> document, Collection<UUID> selection) {
        Objects.requireNonNull(document, "document");
        Set<UUID> ids = Set.copyOf(Objects.requireNonNull(selection, "selection"));
        Vector3d result = new Vector3d();
        int count = 0;
        for (SceneLayer<T> layer : document.layers()) {
            if (ids.contains(layer.id())) {
                result.add(BlockPreviewTransform.pivot(layer.transform()));
                count++;
            }
        }
        return count == 0 ? result : result.div(count);
    }

    public static <T> SceneDocument<T> translate(SceneDocument<T> document,
            Collection<UUID> selection, Vector3dc worldDelta) {
        Objects.requireNonNull(worldDelta, "worldDelta");
        return map(document, selection, transform -> {
            Vector3d rawDelta = ZFightOptimizer.rawPositionDelta(transform, worldDelta);
            return transform.withPosition(transform.position().add(rawDelta));
        });
    }

    public static <T> SceneDocument<T> rotate(SceneDocument<T> document,
            Collection<UUID> selection, Vector3dc groupPivot, Quaternionfc worldDelta) {
        Objects.requireNonNull(groupPivot, "groupPivot");
        Quaternionf delta = new Quaternionf(Objects.requireNonNull(worldDelta, "worldDelta")).normalize();
        return map(document, selection, transform -> {
            Vector3d oldPivot = BlockPreviewTransform.pivot(transform);
            Vector3d newPivot = oldPivot.sub(groupPivot);
            delta.transform(newPivot);
            newPivot.add(groupPivot);
            Quaternionf rotation = delta.mul(transform.rotation(), new Quaternionf()).normalize();
            return new EditorTransform(rawPosition(newPivot, transform.scale()), rotation, transform.scale());
        });
    }

    public static <T> SceneDocument<T> scale(SceneDocument<T> document,
            Collection<UUID> selection, Vector3dc groupPivot, double factor) {
        if (!Double.isFinite(factor) || factor <= 0.0D) {
            throw new IllegalArgumentException("scale factor must be positive and finite");
        }
        return scale(document, selection, groupPivot, new Vector3d(factor));
    }

    public static <T> SceneDocument<T> scale(SceneDocument<T> document,
            Collection<UUID> selection, Vector3dc groupPivot, Vector3dc factors) {
        Objects.requireNonNull(groupPivot, "groupPivot");
        Objects.requireNonNull(factors, "factors");
        if (!positiveFinite(factors.x()) || !positiveFinite(factors.y()) || !positiveFinite(factors.z())) {
            throw new IllegalArgumentException("scale factors must be positive and finite");
        }
        return map(document, selection, transform -> {
            Vector3f oldScale = transform.scale();
            Vector3f newScale = new Vector3f((float) (oldScale.x * factors.x()),
                    (float) (oldScale.y * factors.y()), (float) (oldScale.z * factors.z()));
            if (!Float.isFinite(newScale.x) || !Float.isFinite(newScale.y) || !Float.isFinite(newScale.z)
                    || newScale.x <= 0.0F || newScale.y <= 0.0F || newScale.z <= 0.0F) {
                throw new IllegalArgumentException("scaled layer is outside the supported range");
            }
            Vector3d newPivot = BlockPreviewTransform.pivot(transform).sub(groupPivot)
                    .mul(factors).add(groupPivot);
            return new EditorTransform(rawPosition(newPivot, newScale), transform.rotation(), newScale);
        });
    }

    private static boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0D;
    }

    private static <T> SceneDocument<T> map(SceneDocument<T> document,
            Collection<UUID> selection, java.util.function.UnaryOperator<EditorTransform> operation) {
        Objects.requireNonNull(document, "document");
        Set<UUID> ids = new HashSet<>(Objects.requireNonNull(selection, "selection"));
        if (ids.isEmpty()) return document;
        List<SceneLayer<T>> result = new ArrayList<>(document.layers().size());
        for (SceneLayer<T> layer : document.layers()) {
            result.add(ids.contains(layer.id()) ? layer.withTransform(operation.apply(layer.transform())) : layer);
        }
        return new SceneDocument<>(result, document.selectedLayerId(), document.collisionShape());
    }

    private static Vector3d rawPosition(Vector3dc worldPivot, Vector3f scale) {
        return new Vector3d(worldPivot.x() * BlockPreviewTransform.MODEL_UNITS_PER_WORLD_UNIT / scale.x,
                worldPivot.y() * BlockPreviewTransform.MODEL_UNITS_PER_WORLD_UNIT / scale.y,
                worldPivot.z() * BlockPreviewTransform.MODEL_UNITS_PER_WORLD_UNIT / scale.z);
    }
}
