package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.*;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class EditorTransformTest {
    @Test
    void defensivelyCopiesValues() {
        Vector3d position = new Vector3d(1, 2, 3);
        Vector3f scale = new Vector3f(2, 3, 4);
        EditorTransform transform = new EditorTransform(position, new Quaternionf(), scale);
        position.zero();
        scale.set(1);
        assertEquals(new Vector3d(1, 2, 3), transform.position());
        assertEquals(new Vector3f(2, 3, 4), transform.scale());
        transform.position().zero();
        assertEquals(new Vector3d(1, 2, 3), transform.position());
    }

    @Test
    void rejectsInvalidComponents() {
        assertThrows(IllegalArgumentException.class, () -> new EditorTransform(
                new Vector3d(Double.NaN, 0, 0), new Quaternionf(), new Vector3f(1)));
        assertThrows(IllegalArgumentException.class, () -> new EditorTransform(
                new Vector3d(), new Quaternionf(), new Vector3f(1, 0, 1)));
        assertThrows(IllegalArgumentException.class, () -> new EditorTransform(
                new Vector3d(), new Quaternionf(0, 0, 0, 0), new Vector3f(1)));
    }
}
