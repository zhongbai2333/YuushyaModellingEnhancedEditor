package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhongbai233.yuushya_editor.core.geometry.SelectionTransforms;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewTransform;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class SelectionTransformsTest {
    @Test
    void translationMovesEverySelectedLayerAndLeavesOthersAlone() {
        SceneLayer<String> first = layer(new Vector3d(0.0D, 0.0D, 0.0D));
        SceneLayer<String> second = layer(new Vector3d(16.0D, 0.0D, 0.0D));
        SceneLayer<String> other = layer(new Vector3d(32.0D, 0.0D, 0.0D));
        SceneDocument<String> document = new SceneDocument<>(List.of(first, second, other), first.id());

        SceneDocument<String> moved = SelectionTransforms.translate(document, Set.of(first.id(), second.id()),
                new Vector3d(0.0D, 2.0D, 0.0D));

        assertEquals(new Vector3d(0.0D, 2.0D, 0.0D), pivot(moved.layers().get(0)));
        assertEquals(new Vector3d(1.0D, 2.0D, 0.0D), pivot(moved.layers().get(1)));
        assertEquals(new Vector3d(2.0D, 0.0D, 0.0D), pivot(moved.layers().get(2)));
    }

    @Test
    void groupRotationUsesTheSharedPivot() {
        SceneLayer<String> first = layer(new Vector3d(-16.0D, 0.0D, 0.0D));
        SceneLayer<String> second = layer(new Vector3d(16.0D, 0.0D, 0.0D));
        SceneDocument<String> document = new SceneDocument<>(List.of(first, second), first.id());

        SceneDocument<String> rotated = SelectionTransforms.rotate(document, Set.of(first.id(), second.id()),
                new Vector3d(), new Quaternionf().rotationZ((float) (Math.PI * 0.5D)));

        assertVector(new Vector3d(0.0D, -1.0D, 0.0D), pivot(rotated.layers().get(0)));
        assertVector(new Vector3d(0.0D, 1.0D, 0.0D), pivot(rotated.layers().get(1)));
    }

    @Test
    void groupScaleChangesSpacingAndLayerScale() {
        SceneLayer<String> first = layer(new Vector3d(-16.0D, 0.0D, 0.0D));
        SceneLayer<String> second = layer(new Vector3d(16.0D, 0.0D, 0.0D));
        SceneDocument<String> document = new SceneDocument<>(List.of(first, second), first.id());

        SceneDocument<String> scaled = SelectionTransforms.scale(document, Set.of(first.id(), second.id()),
                new Vector3d(), 2.0D);

        assertVector(new Vector3d(-2.0D, 0.0D, 0.0D), pivot(scaled.layers().get(0)));
        assertVector(new Vector3d(2.0D, 0.0D, 0.0D), pivot(scaled.layers().get(1)));
        assertEquals(new Vector3f(2.0F), scaled.layers().get(0).transform().scale());
        assertEquals(new Vector3f(2.0F), scaled.layers().get(1).transform().scale());
    }

    @Test
    void axisScaleOnlyChangesMatchingSpacingAndScaleComponents() {
        SceneLayer<String> first = layer(new Vector3d(-16.0D, -16.0D, 0.0D));
        SceneLayer<String> second = layer(new Vector3d(16.0D, 16.0D, 0.0D));
        SceneDocument<String> document = new SceneDocument<>(List.of(first, second), first.id());

        SceneDocument<String> scaled = SelectionTransforms.scale(document, Set.of(first.id(), second.id()),
                new Vector3d(), new Vector3d(2.0D, 1.0D, 1.0D));

        assertVector(new Vector3d(-2.0D, -1.0D, 0.0D), pivot(scaled.layers().get(0)));
        assertVector(new Vector3d(2.0D, 1.0D, 0.0D), pivot(scaled.layers().get(1)));
        assertEquals(new Vector3f(2.0F, 1.0F, 1.0F), scaled.layers().get(0).transform().scale());
        assertEquals(new Vector3f(2.0F, 1.0F, 1.0F), scaled.layers().get(1).transform().scale());
    }

    private static SceneLayer<String> layer(Vector3d rawPosition) {
        return new SceneLayer<>(UUID.randomUUID(), "layer", "content",
                new EditorTransform(rawPosition, new Quaternionf(), new Vector3f(1.0F)), true);
    }

    private static Vector3d pivot(SceneLayer<String> layer) {
        return BlockPreviewTransform.pivot(layer.transform());
    }

    private static void assertVector(Vector3d expected, Vector3d actual) {
        assertEquals(expected.x, actual.x, 1.0E-5D);
        assertEquals(expected.y, actual.y, 1.0E-5D);
        assertEquals(expected.z, actual.z, 1.0E-5D);
    }
}
