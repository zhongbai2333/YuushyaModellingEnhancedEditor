package com.zhongbai233.yuushya_editor.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Process-local modelling clipboard; pasted layers always receive a new host slot identity. */
public final class SceneLayerClipboard<T> {
    private List<SceneLayer<T>> copied = List.of();

    public void copy(SceneLayer<T> layer) {
        copied = List.of(Objects.requireNonNull(layer, "layer"));
    }

    public void copy(List<SceneLayer<T>> layers) {
        copied = List.copyOf(Objects.requireNonNull(layers, "layers"));
        if (copied.isEmpty()) throw new IllegalArgumentException("copied layers must not be empty");
    }

    public boolean hasValue() {
        return !copied.isEmpty();
    }

    public String copiedName() {
        if (copied.isEmpty()) throw new IllegalStateException("clipboard is empty");
        return copied.getFirst().name();
    }

    public SceneLayer<T> paste(String copiedName) {
        if (copied.isEmpty()) throw new IllegalStateException("clipboard is empty");
        SceneLayer<T> first = copied.getFirst();
        return new SceneLayer<>(UUID.randomUUID(), Objects.requireNonNull(copiedName, "copiedName"),
                first.hostData(), first.transform(), first.visible());
    }

    public List<SceneLayer<T>> pasteAll() {
        if (copied.isEmpty()) throw new IllegalStateException("clipboard is empty");
        List<SceneLayer<T>> result = new ArrayList<>(copied.size());
        for (SceneLayer<T> layer : copied) {
            result.add(new SceneLayer<>(UUID.randomUUID(), layer.name(), layer.hostData(),
                    layer.transform(), layer.visible()));
        }
        return List.copyOf(result);
    }

    public int size() {
        return copied.size();
    }
}
