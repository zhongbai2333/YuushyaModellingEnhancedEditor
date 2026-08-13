package com.zhongbai233.yuushya_editor.core;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Host-neutral copy of Yuushya's item-specific per-layer fields. */
public final class ItemModelData {
    private final ItemStack itemStack;
    private final int color;
    private final boolean enableBlock;
    private final BlockState blockState;

    public ItemModelData(ItemStack itemStack, int color, boolean enableBlock) {
        this(itemStack, color, enableBlock, null);
    }

    public ItemModelData(ItemStack itemStack, int color, boolean enableBlock, BlockState blockState) {
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack").copy();
        this.color = color;
        this.enableBlock = enableBlock && blockState != null;
        this.blockState = blockState;
    }

    public ItemStack itemStack() { return itemStack.copy(); }
    public int color() { return color; }
    public boolean enableBlock() { return enableBlock; }
    public BlockState blockState() { return blockState; }

    public ItemModelData withItemStack(ItemStack value) {
        return new ItemModelData(value, color, false, null);
    }

    public ItemModelData withItemStack(ItemStack value, BlockState valueBlockState) {
        return new ItemModelData(value, color, enableBlock && valueBlockState != null, valueBlockState);
    }

    public ItemModelData withColor(int value) {
        return new ItemModelData(itemStack, value, enableBlock, blockState);
    }

    public ItemModelData withBlockRendering(boolean value) {
        return new ItemModelData(itemStack, color, value && blockState != null, blockState);
    }

    public ItemModelData withBlockState(ItemStack value, BlockState valueBlockState) {
        return new ItemModelData(value, color, enableBlock && valueBlockState != null, valueBlockState);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ItemModelData value && color == value.color
                && enableBlock == value.enableBlock && Objects.equals(blockState, value.blockState)
                && ItemStack.matches(itemStack, value.itemStack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ItemStack.hashItemAndComponents(itemStack), itemStack.getCount(), color,
                enableBlock, blockState);
    }
}
