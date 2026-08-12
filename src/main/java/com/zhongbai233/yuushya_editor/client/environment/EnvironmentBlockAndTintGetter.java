package com.zhongbai233.yuushya_editor.client.environment;

import java.util.Arrays;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/** Read-only 20-cube model, tint and light view for off-thread vanilla terrain tessellation. */
final class EnvironmentBlockAndTintGetter implements BlockAndTintGetter {
    private static final int SECTION_CELLS = 16 * 16 * 16;
    private final int[] grass = tintArray();
    private final int[] foliage = tintArray();
    private final int[] dryFoliage = tintArray();
    private final int[] water = tintArray();
    private final EnvironmentSectionSnapshot snapshot;

    EnvironmentBlockAndTintGetter(EnvironmentSectionSnapshot snapshot) {
        this.snapshot = java.util.Objects.requireNonNull(snapshot, "snapshot");
        for (EnvironmentSectionSnapshot.VisibleBlock block : snapshot.blocks()) {
            int index = sectionIndex(block.localX(), block.localY(), block.localZ());
            grass[index] = block.tintColors().color(EnvironmentTintColors.TintType.GRASS);
            foliage[index] = block.tintColors().color(EnvironmentTintColors.TintType.FOLIAGE);
            dryFoliage[index] = block.tintColors().color(EnvironmentTintColors.TintType.DRY_FOLIAGE);
            water[index] = block.tintColors().color(EnvironmentTintColors.TintType.WATER);
        }
    }

    @Override public CardinalLighting cardinalLighting() { return CardinalLighting.DEFAULT; }
    @Override public LevelLightEngine getLightEngine() { return LevelLightEngine.EMPTY; }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        int x = pos.getX() - snapshot.section().minBlockX();
        int y = pos.getY() - snapshot.section().minBlockY();
        int z = pos.getZ() - snapshot.section().minBlockZ();
        byte packed = snapshot.neighborhoodLight(x, y, z);
        return layer == LightLayer.SKY ? EnvironmentPackedLight.sky(packed)
                : EnvironmentPackedLight.block(packed);
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        int x = pos.getX() - snapshot.section().minBlockX();
        int y = pos.getY() - snapshot.section().minBlockY();
        int z = pos.getZ() - snapshot.section().minBlockZ();
        if (x < 0 || x >= 16 || y < 0 || y >= 16 || z < 0 || z >= 16) return -1;
        int index = sectionIndex(x, y, z);
        if (resolver == BiomeColors.GRASS_COLOR_RESOLVER) return grass[index];
        if (resolver == BiomeColors.FOLIAGE_COLOR_RESOLVER) return foliage[index];
        if (resolver == BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER) return dryFoliage[index];
        if (resolver == BiomeColors.WATER_COLOR_RESOLVER) return water[index];
        return -1;
    }

    @Override public @Nullable BlockEntity getBlockEntity(BlockPos pos) { return null; }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return snapshot.neighborhoodState(
                pos.getX() - snapshot.section().minBlockX(),
                pos.getY() - snapshot.section().minBlockY(),
                pos.getZ() - snapshot.section().minBlockZ());
    }

    @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
    @Override public int getHeight() { return EnvironmentNeighborhoodIndex.SIZE; }
    @Override public int getMinY() {
        return snapshot.section().minBlockY() + EnvironmentNeighborhoodIndex.MIN_LOCAL;
    }

    private static int[] tintArray() {
        int[] values = new int[SECTION_CELLS];
        Arrays.fill(values, -1);
        return values;
    }

    private static int sectionIndex(int x, int y, int z) { return (y * 16 + z) * 16 + x; }
}
