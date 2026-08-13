package com.zhongbai233.yuushya_editor.core;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Pure selection-state transitions shared by hierarchy rows and viewport picking. */
public final class LayerSelection {
    private LayerSelection() { }

    public static Result click(List<UUID> orderedLayerIds, Collection<UUID> selectedLayerIds,
            UUID primaryLayerId, UUID clickedLayerId, boolean shiftDown, boolean controlDown) {
        Objects.requireNonNull(orderedLayerIds, "orderedLayerIds");
        Objects.requireNonNull(selectedLayerIds, "selectedLayerIds");
        Objects.requireNonNull(clickedLayerId, "clickedLayerId");
        if (!orderedLayerIds.contains(clickedLayerId)) {
            throw new IllegalArgumentException("clicked layer is not in the document");
        }

        LinkedHashSet<UUID> selected = new LinkedHashSet<>(selectedLayerIds);
        if (shiftDown && primaryLayerId != null) {
            int anchor = orderedLayerIds.indexOf(primaryLayerId);
            int target = orderedLayerIds.indexOf(clickedLayerId);
            if (anchor >= 0) {
                if (!controlDown) selected.clear();
                int from = Math.min(anchor, target);
                int to = Math.max(anchor, target);
                selected.addAll(orderedLayerIds.subList(from, to + 1));
                return new Result(List.copyOf(selected), clickedLayerId);
            }
        }

        if (controlDown) {
            if (selected.contains(clickedLayerId)) {
                // Keep one primary layer available for the inspector and gizmo. Clearing all
                // selection is an explicit blank-viewport action instead.
                if (selected.size() == 1) {
                    return new Result(List.copyOf(selected), primaryLayerId);
                }
                selected.remove(clickedLayerId);
                UUID primary = clickedLayerId.equals(primaryLayerId) ? last(selected) : primaryLayerId;
                return new Result(List.copyOf(selected), primary);
            }
            selected.add(clickedLayerId);
            return new Result(List.copyOf(selected), clickedLayerId);
        }

        return new Result(List.of(clickedLayerId), clickedLayerId);
    }

    private static UUID last(LinkedHashSet<UUID> values) {
        UUID last = null;
        for (UUID value : values) last = value;
        return last;
    }

    public record Result(List<UUID> selectedLayerIds, UUID primaryLayerId) {
        public Result {
            selectedLayerIds = List.copyOf(Objects.requireNonNull(selectedLayerIds, "selectedLayerIds"));
            if (primaryLayerId != null && !selectedLayerIds.contains(primaryLayerId)) {
                throw new IllegalArgumentException("primary layer must be selected");
            }
        }
    }
}
