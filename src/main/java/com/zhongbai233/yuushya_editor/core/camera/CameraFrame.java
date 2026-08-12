package com.zhongbai233.yuushya_editor.core.camera;

import com.zhongbai233.yuushya_editor.core.projection.Viewport;
import java.util.Objects;

/** One immutable camera snapshot shared by rendering, projection, and picking for a viewport frame. */
public record CameraFrame(CameraMatrices matrices, Viewport viewport, CameraMode mode) {
    public CameraFrame {
        Objects.requireNonNull(matrices, "matrices");
        Objects.requireNonNull(viewport, "viewport");
        Objects.requireNonNull(mode, "mode");
    }
}
