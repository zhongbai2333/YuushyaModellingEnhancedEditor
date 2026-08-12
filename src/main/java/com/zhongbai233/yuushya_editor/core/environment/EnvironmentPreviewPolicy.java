package com.zhongbai233.yuushya_editor.core.environment;

/** NCPB-compatible fixed terrain core: solid sphere with a stable dithered outer shell. */
public final class EnvironmentPreviewPolicy {
    public static final double DIAMETER = 25.0D;
    public static final double RADIUS = DIAMETER * 0.5D;
    public static final double FADE_WIDTH = 3.0D;
    public static final double SOLID_RADIUS = RADIUS - FADE_WIDTH;
    public static final int SECTION_SIZE = 16;

    private EnvironmentPreviewPolicy() { }

    public static double retention(double centerX, double centerY, double centerZ,
            int blockX, int blockY, int blockZ) {
        double dx = blockX + 0.5D - centerX;
        double dy = blockY + 0.5D - centerY;
        double dz = blockZ + 0.5D - centerZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= SOLID_RADIUS) return 1.0D;
        if (distance >= RADIUS) return 0.0D;
        double t = (distance - SOLID_RADIUS) / FADE_WIDTH;
        double smooth = t * t * (3.0D - 2.0D * t);
        return 1.0D - smooth;
    }

    public static boolean rendersBlock(long seed, double centerX, double centerY, double centerZ,
            int blockX, int blockY, int blockZ) {
        double retention = retention(centerX, centerY, centerZ, blockX, blockY, blockZ);
        return retention >= 1.0D
                || retention > 0.0D && unitHash(seed, blockX, blockY, blockZ) < retention;
    }

    public static boolean sectionMayContainDetail(double centerX, double centerY, double centerZ,
            int minBlockX, int minBlockY, int minBlockZ) {
        double nearestX = Math.clamp(centerX, minBlockX + 0.5D,
                minBlockX + SECTION_SIZE - 0.5D);
        double nearestY = Math.clamp(centerY, minBlockY + 0.5D,
                minBlockY + SECTION_SIZE - 0.5D);
        double nearestZ = Math.clamp(centerZ, minBlockZ + 0.5D,
                minBlockZ + SECTION_SIZE - 0.5D);
        double dx = nearestX - centerX;
        double dy = nearestY - centerY;
        double dz = nearestZ - centerZ;
        return dx * dx + dy * dy + dz * dz < RADIUS * RADIUS;
    }

    public static long seed(int originX, int originY, int originZ) {
        long value = ((long) originX * 341873128712L)
                ^ ((long) originY * 132897987541L)
                ^ ((long) originZ * 42317861L);
        return value ^ value >>> 29;
    }

    private static double unitHash(long seed, int x, int y, int z) {
        long value = seed;
        value ^= mix((long) x * 0x9E3779B97F4A7C15L);
        value ^= mix((long) y * 0xC2B2AE3D27D4EB4FL);
        value ^= mix((long) z * 0x165667B19E3779F9L);
        return (mix(value) >>> 11) * 0x1.0p-53;
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
