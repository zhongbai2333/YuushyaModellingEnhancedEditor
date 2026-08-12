package com.zhongbai233.yuushya_editor.core.projection;

public record Viewport(int x, int y, int width, int height) {
    public Viewport {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("viewport dimensions must be positive");
    }

    public double aspectRatio() { return (double) width / height; }
}
