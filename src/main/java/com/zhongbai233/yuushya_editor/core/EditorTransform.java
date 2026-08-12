package com.zhongbai233.yuushya_editor.core;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable scene transform independent of Minecraft and Yuushya internals. */
public final class EditorTransform {
    public static final EditorTransform IDENTITY = new EditorTransform(
            new Vector3d(), new Quaternionf(), new Vector3f(1.0F));

    private final Vector3d position;
    private final Quaternionf rotation;
    private final Vector3f scale;

    public EditorTransform(Vector3dc position, Quaternionfc rotation, Vector3fc scale) {
        this.position = finite(position, "position");
        this.rotation = new Quaternionf(java.util.Objects.requireNonNull(rotation, "rotation"));
        if (!finite(this.rotation) || this.rotation.lengthSquared() <= 1.0e-8F) {
            throw new IllegalArgumentException("rotation must be finite and non-zero");
        }
        this.rotation.normalize();
        this.scale = new Vector3f(java.util.Objects.requireNonNull(scale, "scale"));
        if (!finite(this.scale) || this.scale.x == 0.0F || this.scale.y == 0.0F || this.scale.z == 0.0F) {
            throw new IllegalArgumentException("scale must be finite and non-zero on every axis");
        }
    }

    public Vector3d position() { return new Vector3d(position); }
    public Quaternionf rotation() { return new Quaternionf(rotation); }
    public Vector3f scale() { return new Vector3f(scale); }

    public EditorTransform withPosition(Vector3dc value) {
        return new EditorTransform(value, rotation, scale);
    }

    public EditorTransform withRotation(Quaternionfc value) {
        return new EditorTransform(position, value, scale);
    }

    public EditorTransform withScale(Vector3fc value) {
        return new EditorTransform(position, rotation, value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EditorTransform value
                && position.equals(value.position)
                && rotation.equals(value.rotation)
                && scale.equals(value.scale);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(position, rotation, scale);
    }

    private static Vector3d finite(Vector3dc value, String name) {
        java.util.Objects.requireNonNull(value, name);
        if (!Double.isFinite(value.x()) || !Double.isFinite(value.y()) || !Double.isFinite(value.z())) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return new Vector3d(value);
    }

    private static boolean finite(Quaternionfc value) {
        return Float.isFinite(value.x()) && Float.isFinite(value.y())
                && Float.isFinite(value.z()) && Float.isFinite(value.w());
    }

    private static boolean finite(Vector3fc value) {
        return Float.isFinite(value.x()) && Float.isFinite(value.y()) && Float.isFinite(value.z());
    }
}
