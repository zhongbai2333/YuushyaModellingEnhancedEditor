package com.zhongbai233.yuushya_editor.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Plans a compact logical document over Yuushya's non-compacting physical transform slots. */
final class YuushyaSlotRewritePlan {
    private YuushyaSlotRewritePlan() {
    }

    static Plan create(List<UUID> layerIds, Map<UUID, Integer> sourceSlots, int physicalSlotCount) {
        Objects.requireNonNull(layerIds, "layerIds");
        Objects.requireNonNull(sourceSlots, "sourceSlots");
        if (physicalSlotCount < 0) throw new IllegalArgumentException("physicalSlotCount must be non-negative");

        List<Write> writes = new ArrayList<>(layerIds.size());
        for (int targetSlot = 0; targetSlot < layerIds.size(); targetSlot++) {
            UUID layerId = Objects.requireNonNull(layerIds.get(targetSlot), "layer id");
            Integer sourceSlot = sourceSlots.get(layerId);
            writes.add(new Write(layerId, targetSlot, sourceSlot,
                    sourceSlot == null || sourceSlot < 0 || sourceSlot >= physicalSlotCount
                            || sourceSlot != targetSlot));
        }

        List<Integer> resetSlots = new ArrayList<>(Math.max(0, physicalSlotCount - layerIds.size()));
        for (int slot = physicalSlotCount - 1; slot >= layerIds.size(); slot--) resetSlots.add(slot);
        return new Plan(writes, resetSlots);
    }

    record Write(UUID layerId, int targetSlot, Integer sourceSlot, boolean fullRewrite) {
    }

    record Plan(List<Write> writes, List<Integer> resetSlots) {
        Plan {
            writes = List.copyOf(writes);
            resetSlots = List.copyOf(resetSlots);
        }
    }
}
