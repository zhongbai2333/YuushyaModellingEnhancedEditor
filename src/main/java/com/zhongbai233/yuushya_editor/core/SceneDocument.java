package com.zhongbai233.yuushya_editor.core;

import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Immutable snapshot edited locally before the host adapter submits changes. */
public record SceneDocument<T>(List<SceneLayer<T>> layers, UUID selectedLayerId,
        CollisionShape collisionShape) {
    public SceneDocument {
        Objects.requireNonNull(layers, "layers");
        Objects.requireNonNull(collisionShape, "collisionShape");
        Set<UUID> layerIds = new HashSet<>(layers.size());
        for (SceneLayer<T> layer : layers) {
            if (layer == null) throw new IllegalArgumentException("layers must not contain null");
            if (!layerIds.add(layer.id())) {
                throw new IllegalArgumentException("layer ids must be unique");
            }
        }
        if (selectedLayerId != null && !layerIds.contains(selectedLayerId)) {
            throw new IllegalArgumentException("selected layer must exist in the document");
        }
        layers = List.copyOf(layers);
    }

    /** Compatibility constructor for host-neutral documents that do not expose a shape. */
    public SceneDocument(List<SceneLayer<T>> layers, UUID selectedLayerId) {
        this(layers, selectedLayerId, CollisionShape.none());
    }

    public static <T> SceneDocument<T> empty() {
        return new SceneDocument<>(List.of(), null, CollisionShape.none());
    }

    public Optional<SceneLayer<T>> selectedLayer() {
        return selectedLayerId == null ? Optional.empty()
                : layers.stream().filter(layer -> layer.id().equals(selectedLayerId)).findFirst();
    }

    public SceneDocument<T> select(UUID id) {
        return new SceneDocument<>(layers, id, collisionShape);
    }

    public SceneDocument<T> replace(SceneLayer<T> replacement) {
        Objects.requireNonNull(replacement, "replacement");
        boolean found = false;
        var result = new java.util.ArrayList<SceneLayer<T>>(layers.size());
        for (SceneLayer<T> layer : layers) {
            if (layer.id().equals(replacement.id())) {
                result.add(replacement);
                found = true;
            } else {
                result.add(layer);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("replacement layer is not part of the document");
        }
        return new SceneDocument<>(result, selectedLayerId, collisionShape);
    }

    /** Appends a newly created layer and selects it. Existing slot order remains unchanged. */
    public SceneDocument<T> append(SceneLayer<T> layer) {
        Objects.requireNonNull(layer, "layer");
        if (layers.stream().anyMatch(existing -> existing.id().equals(layer.id()))) {
            throw new IllegalArgumentException("appended layer id must be unique");
        }
        var result = new java.util.ArrayList<SceneLayer<T>>(layers.size() + 1);
        result.addAll(layers);
        result.add(layer);
        return new SceneDocument<>(result, layer.id(), collisionShape);
    }

    /** Removes one layer and keeps selection on the nearest surviving row. */
    public SceneDocument<T> remove(UUID id) {
        Objects.requireNonNull(id, "id");
        int removedIndex = -1;
        var result = new java.util.ArrayList<SceneLayer<T>>(Math.max(0, layers.size() - 1));
        for (int index = 0; index < layers.size(); index++) {
            SceneLayer<T> layer = layers.get(index);
            if (layer.id().equals(id)) {
                removedIndex = index;
            } else {
                result.add(layer);
            }
        }
        if (removedIndex < 0) {
            throw new IllegalArgumentException("removed layer is not part of the document");
        }
        UUID nextSelection = selectedLayerId;
        if (id.equals(selectedLayerId)) {
            nextSelection = result.isEmpty() ? null
                    : result.get(Math.min(removedIndex, result.size() - 1)).id();
        }
        return new SceneDocument<>(result, nextSelection, collisionShape);
    }

    public SceneDocument<T> withCollisionShape(CollisionShape value) {
        return new SceneDocument<>(layers, selectedLayerId, value);
    }
}
