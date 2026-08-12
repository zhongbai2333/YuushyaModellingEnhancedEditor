package com.zhongbai233.yuushya_editor.core.selection;

/** Distinguishes a blank click from a camera drag before clearing selection. */
public final class BlankClickSelectionPolicy {
    private static final double CLICK_DISTANCE_SQUARED = 16.0D;

    private BlankClickSelectionPolicy() { }

    public static boolean shouldDeselect(boolean blankCandidate, int mouseButton,
            double pressX, double pressY, double releaseX, double releaseY) {
        if (!blankCandidate || mouseButton != 0) return false;
        double dx = releaseX - pressX;
        double dy = releaseY - pressY;
        return dx * dx + dy * dy < CLICK_DISTANCE_SQUARED;
    }
}
