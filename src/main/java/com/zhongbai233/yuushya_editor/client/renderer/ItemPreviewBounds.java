package com.zhongbai233.yuushya_editor.client.renderer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** Resolves the bounds of the same item model that the preview renderer submits. */
final class ItemPreviewBounds {
    private static final int MAX_CACHE_SIZE = 256;
    private static final double MIN_INTERACTION_THICKNESS = 0.04D;
    private static final AABB FALLBACK = new AABB(-0.5D, -0.5D, -0.5D,
            0.5D, 0.5D, 0.5D);
    private static final List<CacheEntry> CACHE = new ArrayList<>();
    private static BlockStateModelSet cachedModelSet;

    private ItemPreviewBounds() { }

    static AABB resolve(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        synchronizeCache(minecraft);
        for (int index = CACHE.size() - 1; index >= 0; index--) {
            CacheEntry entry = CACHE.get(index);
            if (entry.matches(stack)) return entry.bounds();
        }

        try {
            ItemStackRenderState renderState = new ItemStackRenderState();
            minecraft.getItemModelResolver().updateForTopItem(renderState, stack,
                    ItemDisplayContext.NONE, minecraft.level, null, 0);
            AABB bounds = normalize(renderState.getModelBoundingBox());
            // Animated model dispatch can choose another geometry on the next frame.
            if (!renderState.isAnimated()) cache(stack, bounds);
            return bounds;
        } catch (RuntimeException ignored) {
            // A broken resource-pack model should not make the editor impossible to use.
            return FALLBACK;
        }
    }

    private static void synchronizeCache(Minecraft minecraft) {
        BlockStateModelSet current = minecraft.getModelManager().getBlockStateModelSet();
        if (current == cachedModelSet) return;
        cachedModelSet = current;
        CACHE.clear();
    }

    private static void cache(ItemStack stack, AABB bounds) {
        if (CACHE.size() >= MAX_CACHE_SIZE) CACHE.remove(0);
        CACHE.add(new CacheEntry(stack.copy(), bounds));
    }

    private static AABB normalize(AABB bounds) {
        if (!finite(bounds) || bounds.maxX < bounds.minX || bounds.maxY < bounds.minY
                || bounds.maxZ < bounds.minZ) {
            return FALLBACK;
        }
        double sizeX = bounds.maxX - bounds.minX;
        double sizeY = bounds.maxY - bounds.minY;
        double sizeZ = bounds.maxZ - bounds.minZ;
        if (sizeX == 0.0D && sizeY == 0.0D && sizeZ == 0.0D) return FALLBACK;
        return new AABB(
                expandedMin(bounds.minX, bounds.maxX),
                expandedMin(bounds.minY, bounds.maxY),
                expandedMin(bounds.minZ, bounds.maxZ),
                expandedMax(bounds.minX, bounds.maxX),
                expandedMax(bounds.minY, bounds.maxY),
                expandedMax(bounds.minZ, bounds.maxZ));
    }

    private static double expandedMin(double min, double max) {
        double missing = MIN_INTERACTION_THICKNESS - (max - min);
        return missing > 0.0D ? min - missing * 0.5D : min;
    }

    private static double expandedMax(double min, double max) {
        double missing = MIN_INTERACTION_THICKNESS - (max - min);
        return missing > 0.0D ? max + missing * 0.5D : max;
    }

    private static boolean finite(AABB bounds) {
        return Double.isFinite(bounds.minX) && Double.isFinite(bounds.minY)
                && Double.isFinite(bounds.minZ) && Double.isFinite(bounds.maxX)
                && Double.isFinite(bounds.maxY) && Double.isFinite(bounds.maxZ);
    }

    private record CacheEntry(ItemStack stack, AABB bounds) {
        private boolean matches(ItemStack candidate) {
            return stack.getCount() == candidate.getCount()
                    && ItemStack.isSameItemSameComponents(stack, candidate);
        }
    }
}
