package com.zhongbai233.yuushya_editor.core;

/** Pure policy for deciding when the passive modeled-block locator is visible. */
public final class WorldModelHighlightPolicy {
    private WorldModelHighlightPolicy() {
    }

    public static boolean shouldDraw(
            boolean editorHeld, boolean showFrame, boolean showAxis, boolean showText) {
        return editorHeld && !showFrame && !showAxis && !showText;
    }
}
