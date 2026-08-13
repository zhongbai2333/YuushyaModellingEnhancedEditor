package com.zhongbai233.yuushya_editor.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Shared, deterministic editing operations for Minecraft block-state properties. */
public final class BlockStatePropertySupport {
    private BlockStatePropertySupport() { }

    public static List<Property<?>> properties(BlockState state) {
        Objects.requireNonNull(state, "state");
        return List.copyOf(state.getProperties());
    }

    public static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property,
            int direction) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(property, "property");
        if (!state.hasProperty(property)) {
            throw new IllegalArgumentException("Property " + property.getName()
                    + " does not belong to " + state);
        }
        List<T> values = new ArrayList<>(property.getPossibleValues());
        if (values.isEmpty() || direction == 0) return state;
        int current = values.indexOf(state.getValue(property));
        int next = Math.floorMod(current + direction, values.size());
        return state.setValue(property, values.get(next));
    }

    public static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(property, "property");
        return property.getName(state.getValue(property));
    }
}
