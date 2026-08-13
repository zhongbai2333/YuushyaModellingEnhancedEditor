package com.zhongbai233.yuushya_editor.core.camera;

import java.util.OptionalDouble;

/** Decides when a viewport drag cursor should wrap across a window boundary. */
public final class CursorWrapPolicy {
    private CursorWrapPolicy() { }

    public static OptionalDouble horizontalTarget(double cursorX, double width, double margin) {
        return target(cursorX, width, margin);
    }

    public static OptionalDouble verticalTarget(double cursorY, double height, double margin) {
        return target(cursorY, height, margin);
    }

    private static OptionalDouble target(double cursor, double extent, double margin) {
        if (!Double.isFinite(cursor) || !Double.isFinite(extent) || !Double.isFinite(margin)
                || extent <= 0.0D || margin < 0.0D || margin * 2.0D >= extent) {
            throw new IllegalArgumentException("cursor wrap dimensions must be finite and usable");
        }
        if (cursor <= margin) return OptionalDouble.of(extent - margin);
        if (cursor >= extent - margin) return OptionalDouble.of(margin);
        return OptionalDouble.empty();
    }
}
