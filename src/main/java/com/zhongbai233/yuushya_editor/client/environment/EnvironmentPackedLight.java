package com.zhongbai233.yuushya_editor.client.environment;

/** Packs vanilla block and sky light levels into one byte in an immutable section snapshot. */
public final class EnvironmentPackedLight {
    private EnvironmentPackedLight() { }

    public static byte pack(int block, int sky) {
        if (block < 0 || block > 15 || sky < 0 || sky > 15) {
            throw new IllegalArgumentException("light level must be within [0, 15]");
        }
        return (byte) ((sky << 4) | block);
    }

    public static int block(byte packed) { return packed & 0x0F; }
    public static int sky(byte packed) { return packed >>> 4 & 0x0F; }
}
