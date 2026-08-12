package com.zhongbai233.yuushya_editor.client.environment;

import java.util.Arrays;
import java.util.List;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable Minecraft-free-lifecycle section data captured on the client thread. */
public record EnvironmentSectionSnapshot(EnvironmentSectionKey section,
        List<VisibleBlock> blocks, List<BlockState> neighborhoodStates,
        byte[] neighborhoodLight, long fingerprint) {
    public EnvironmentSectionSnapshot {
        java.util.Objects.requireNonNull(section, "section");
        blocks = List.copyOf(java.util.Objects.requireNonNull(blocks, "blocks"));
        neighborhoodStates = List.copyOf(java.util.Objects.requireNonNull(
                neighborhoodStates, "neighborhoodStates"));
        neighborhoodLight = java.util.Objects.requireNonNull(
                neighborhoodLight, "neighborhoodLight").clone();
        if (neighborhoodStates.size() != EnvironmentNeighborhoodIndex.CELL_COUNT) {
            throw new IllegalArgumentException("environment neighborhood must contain exactly 20^3 states");
        }
        if (neighborhoodLight.length != EnvironmentNeighborhoodIndex.CELL_COUNT) {
            throw new IllegalArgumentException("environment light must contain exactly 20^3 values");
        }
    }

    public BlockState neighborhoodState(int localX, int localY, int localZ) {
        if (!EnvironmentNeighborhoodIndex.contains(localX, localY, localZ)) {
            return Blocks.AIR.defaultBlockState();
        }
        return neighborhoodStates.get(EnvironmentNeighborhoodIndex.index(localX, localY, localZ));
    }

    @Override
    public byte[] neighborhoodLight() {
        return neighborhoodLight.clone();
    }

    public byte neighborhoodLight(int localX, int localY, int localZ) {
        if (!EnvironmentNeighborhoodIndex.contains(localX, localY, localZ)) return 0;
        return neighborhoodLight[EnvironmentNeighborhoodIndex.index(localX, localY, localZ)];
    }

    public boolean sameContent(EnvironmentSectionSnapshot other) {
        return other != null && fingerprint == other.fingerprint
                && blocks.equals(other.blocks)
                && neighborhoodStates.equals(other.neighborhoodStates)
                && Arrays.equals(neighborhoodLight, other.neighborhoodLight);
    }

    public record VisibleBlock(int localX, int localY, int localZ,
            BlockState state, EnvironmentTintColors tintColors) {
        public VisibleBlock {
            if (localX < 0 || localX >= EnvironmentSectionKey.SIZE
                    || localY < 0 || localY >= EnvironmentSectionKey.SIZE
                    || localZ < 0 || localZ >= EnvironmentSectionKey.SIZE) {
                throw new IllegalArgumentException("visible block coordinates must be within [0, 15]");
            }
            java.util.Objects.requireNonNull(state, "state");
            java.util.Objects.requireNonNull(tintColors, "tintColors");
        }
    }
}
