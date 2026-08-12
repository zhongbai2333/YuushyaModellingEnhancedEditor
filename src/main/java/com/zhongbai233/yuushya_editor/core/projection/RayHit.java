package com.zhongbai233.yuushya_editor.core.projection;

import org.joml.Vector3d;

public record RayHit(double distance, Vector3d point, double localX, double localY) {
    public RayHit {
        point = new Vector3d(point);
    }

    @Override
    public Vector3d point() { return new Vector3d(point); }
}
