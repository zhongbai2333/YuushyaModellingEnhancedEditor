package com.zhongbai233.yuushya_editor.client.environment;

import com.zhongbai233.yuushya_editor.core.SceneLayer;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;

/** A neighboring Yuushya modelling block and the immutable model layers captured from it. */
public record EnvironmentModeledBlock(BlockPos position, List<SceneLayer<Object>> layers) {
    public EnvironmentModeledBlock {
        position = Objects.requireNonNull(position, "position").immutable();
        layers = List.copyOf(Objects.requireNonNull(layers, "layers"));
    }
}
