package com.zhongbai233.yuushya_editor.core.geometry;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import java.util.HashMap;
import java.util.ArrayList;
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
    /** Large enough to survive the float matrix used by the renderer, still visually negligible. */
    public static final double DEFAULT_WORLD_EPSILON = 1.0E-4D;

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
        Map<UUID, Vector3d> result = new LinkedHashMap<>();
        for (UUID id : layerOrder) result.put(id, new Vector3d());
        List<Constraint> constraints = new ArrayList<>();
        for (ZFightDetector.Conflict conflict : conflicts) {
            Integer firstSlot = slots.get(conflict.firstLayerId());
            Integer secondSlot = slots.get(conflict.secondLayerId());
            if (firstSlot == null || secondSlot == null) continue;
            UUID earlier = firstSlot < secondSlot ? conflict.firstLayerId() : conflict.secondLayerId();
            UUID later = firstSlot < secondSlot ? conflict.secondLayerId() : conflict.firstLayerId();
            Vector3d direction = conflict.normal().normalize();
            if (later.equals(conflict.firstLayerId())) direction.negate();
            constraints.add(new Constraint(earlier, later, direction));
        }
        int passes = Math.max(1, layerOrder.size() * 2);
        for (int pass = 0; pass < passes; pass++) {
            boolean changed = false;
            for (Constraint constraint : constraints) {
                Vector3d earlierOffset = result.get(constraint.earlier());
                Vector3d laterOffset = result.get(constraint.later());
                double separation = new Vector3d(laterOffset).sub(earlierOffset)
                        .dot(constraint.direction());
                double missing = epsilon - separation;
                if (missing > epsilon * 1.0E-9D) {
                    laterOffset.fma(missing, constraint.direction());
                    changed = true;
                }
            }
            if (!changed) break;
        }
        result.entrySet().removeIf(entry -> entry.getValue().lengthSquared() == 0.0D);
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

    private record Constraint(UUID earlier, UUID later, Vector3d direction) { }
}
