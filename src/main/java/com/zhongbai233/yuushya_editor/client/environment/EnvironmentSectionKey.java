package com.zhongbai233.yuushya_editor.client.environment;

/** Immutable 16x16x16 world-section coordinate used by the environment preview. */
public record EnvironmentSectionKey(int x, int y, int z) {
    public static final int SIZE = 16;

    public static EnvironmentSectionKey fromBlock(int blockX, int blockY, int blockZ) {
        return new EnvironmentSectionKey(Math.floorDiv(blockX, SIZE),
                Math.floorDiv(blockY, SIZE), Math.floorDiv(blockZ, SIZE));
    }

    public int minBlockX() { return x * SIZE; }
    public int minBlockY() { return y * SIZE; }
    public int minBlockZ() { return z * SIZE; }
}
