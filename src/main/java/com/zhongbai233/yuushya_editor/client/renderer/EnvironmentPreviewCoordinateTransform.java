package com.zhongbai233.yuushya_editor.client.renderer;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Compensates the main-world camera-relative coordinate encoding used by vanilla terrain shaders. */
final class EnvironmentPreviewCoordinateTransform {
    private EnvironmentPreviewCoordinateTransform() { }

    static Frame create(Matrix4fc editorModelView, double cameraX, double cameraY, double cameraZ) {
        int blockX = floorToInt(cameraX);
        int blockY = floorToInt(cameraY);
        int blockZ = floorToInt(cameraZ);
        Matrix4f compensated = new Matrix4f(editorModelView).translate(
                (float) (cameraX - blockX), (float) (cameraY - blockY), (float) (cameraZ - blockZ));
        return new Frame(compensated, blockX, blockY, blockZ);
    }

    private static int floorToInt(double value) {
        if (!Double.isFinite(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("camera coordinate must be finite and within int range");
        }
        return (int) Math.floor(value);
    }

    record Frame(Matrix4f modelView, int cameraBlockX, int cameraBlockY, int cameraBlockZ) {
        Frame {
            modelView = new Matrix4f(java.util.Objects.requireNonNull(modelView, "modelView"));
        }

        int encodedSectionX(int local) { return Math.addExact(cameraBlockX, local); }
        int encodedSectionY(int local) { return Math.addExact(cameraBlockY, local); }
        int encodedSectionZ(int local) { return Math.addExact(cameraBlockZ, local); }
    }
}
