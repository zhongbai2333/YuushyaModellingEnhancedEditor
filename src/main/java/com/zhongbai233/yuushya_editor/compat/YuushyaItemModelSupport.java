package com.zhongbai233.yuushya_editor.compat;

import com.yuushya.modelling.registries.DataComponentRegistry;
import com.zhongbai233.yuushya_editor.core.ItemModelData;
import java.util.Objects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Keeps Yuushya's block-state item component intact through its compile-time host API. */
public final class YuushyaItemModelSupport {
    private YuushyaItemModelSupport() { }

    /** Creates the same initial item data as Yuushya's native item editor. */
    public static ItemModelData create(ItemStack value) {
        ItemStack stack = Objects.requireNonNull(value, "value").copy();
        BlockState state = resolveBlockState(stack);
        if (state != null) stack = withBlockStateComponent(stack, state);
        return new ItemModelData(stack, 0xFFFFFFFF, false, state);
    }

    /** Replaces an item while retaining colour and block mode only when the replacement supports it. */
    public static ItemModelData replace(ItemModelData current, ItemStack value) {
        Objects.requireNonNull(current, "current");
        ItemStack stack = Objects.requireNonNull(value, "value").copy();
        BlockState state = resolveBlockState(stack);
        if (state != null) stack = withBlockStateComponent(stack, state);
        return new ItemModelData(stack, current.color(), current.enableBlock() && state != null, state);
    }

    /** Returns the component state, or a BlockItem's default state when the native editor has not stored one yet. */
    public static BlockState resolveBlockState(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        BlockState componentState = stack.get(DataComponentRegistry.BLOCKSTATE.get());
        if (componentState != null) return componentState;
        return stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState() : null;
    }

    /** Updates both the editor-side state and the stack component consumed by Yuushya's renderer/server. */
    public static ItemModelData withBlockState(ItemModelData current, BlockState state) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(state, "state");
        return current.withBlockState(withBlockStateComponent(current.itemStack(), state), state);
    }

    /** Normalizes data immediately before native export or network submission. */
    public static ItemStack stackForHost(ItemModelData data) {
        Objects.requireNonNull(data, "data");
        return data.blockState() == null
                ? data.itemStack() : withBlockStateComponent(data.itemStack(), data.blockState());
    }

    private static ItemStack withBlockStateComponent(ItemStack original, BlockState state) {
        ItemStack copy = original.copy();
        copy.set(DataComponentRegistry.BLOCKSTATE.get(), state);
        return copy;
    }
}
