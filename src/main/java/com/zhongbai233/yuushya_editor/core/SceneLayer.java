package com.zhongbai233.yuushya_editor.core;

import java.util.Objects;
import java.util.UUID;

/** A host-neutral editable layer. Host data may be a Minecraft BlockState adapter value. */
public record SceneLayer<T>(UUID id, String name, T hostData, EditorTransform transform, boolean visible) {
    public SceneLayer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(hostData, "hostData");
        Objects.requireNonNull(transform, "transform");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    public SceneLayer<T> withTransform(EditorTransform value) {
        return new SceneLayer<>(id, name, hostData, value, visible);
    }

    public SceneLayer<T> withVisible(boolean value) {
        return new SceneLayer<>(id, name, hostData, transform, value);
    }
}
