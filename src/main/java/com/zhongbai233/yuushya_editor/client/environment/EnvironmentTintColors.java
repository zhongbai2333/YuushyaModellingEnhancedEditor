package com.zhongbai233.yuushya_editor.client.environment;

/** Immutable biome tint snapshot used by off-thread terrain compilation. */
public record EnvironmentTintColors(int grass, int foliage, int dryFoliage, int water) {
    public static final EnvironmentTintColors UNTINTED = new EnvironmentTintColors(-1, -1, -1, -1);

    public int color(TintType type) {
        return switch (java.util.Objects.requireNonNull(type, "type")) {
            case GRASS -> grass;
            case FOLIAGE -> foliage;
            case DRY_FOLIAGE -> dryFoliage;
            case WATER -> water;
        };
    }

    public enum TintType { GRASS, FOLIAGE, DRY_FOLIAGE, WATER }
}
