package com.zhongbai233.yuushya_editor.core.geometry;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

/** Computes deterministic micro-offsets for the later layer in each conflicting pair. */
public final class ZFightOptimizer {
    public static final double DEFAULT_WORLD_EPSILON = 1.0E-7D;

    private ZFightOptimizer() { }

    public static Map<UUID, Vector3d> worldOffsets(List<UUID> layerOrder,
            List<ZFightDetector.Conflict> conflicts, double epsilon) {
        Objects.requireNonNull(layerOrder, "layerOrder");
        Objects.requireNonNull(conflicts, "conflicts");
        if (!Double.isFinite(epsilon) || epsilon <= 0.0D) {
            throw new IllegalArgumentException("epsilon must be positive and finite");
        }
        Map<UUID, Integer> slots = new HashMap<>();
        for (int slot = 0; slot < layerOrder.size(); slot++) slots.put(layerOrder.get(slot), slot);
        Map<UUID, List<Vector3d>> directions = new LinkedHashMap<>();
        for (ZFightDetector.Conflict conflict : conflicts) {
            Integer firstSlot = slots.get(conflict.firstLayerId());
            Integer secondSlot = slots.get(conflict.secondLayerId());
            if (firstSlot == null || secondSlot == null) continue;
            UUID target = firstSlot > secondSlot ? conflict.firstLayerId() : conflict.secondLayerId();
            Vector3d normal = conflict.normal().normalize();
            List<Vector3d> unique = directions.computeIfAbsent(target, ignored -> new ArrayList<>());
            boolean duplicate = unique.stream().anyMatch(value -> Math.abs(value.dot(normal)) > 1.0D - 1.0E-6D);
            if (!duplicate) unique.add(normal);
        }
        Map<UUID, Vector3d> result = new LinkedHashMap<>();
        directions.forEach((id, normals) -> {
            Vector3d offset = new Vector3d();
            normals.forEach(normal -> offset.fma(epsilon, normal));
            if (offset.lengthSquared() > 0.0D) result.put(id, offset);
        });
        return Map.copyOf(result);
    }

    /** Converts a desired post-scale world displacement back to Yuushya's raw position units. */
    public static Vector3d rawPositionDelta(EditorTransform transform, Vector3dc worldOffset) {
        Objects.requireNonNull(transform, "transform");
        Objects.requireNonNull(worldOffset, "worldOffset");
        Vector3f scale = transform.scale();
        return new Vector3d(worldOffset.x() * 16.0D / scale.x,
                worldOffset.y() * 16.0D / scale.y,
                worldOffset.z() * 16.0D / scale.z);
    }
}
