package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CollisionShapeTest {
    @Test
    void presetsMatchYuushyaVoxelBoxes() {
        assertEquals(List.of(new CollisionShape.Box(0.0D, 0.0D, 0.0D,
                1.0D, 1.0D, 1.0D)), CollisionShape.block().boxes());
        assertEquals(1, CollisionShape.forKind(CollisionShape.Kind.FENCE).boxes().size());
        assertEquals(1.5D, CollisionShape.forKind(CollisionShape.Kind.FENCE).boxes().getFirst().maxY());
        assertEquals(0, CollisionShape.none().boxes().size());
    }

    @Test
    void presetCannotBeConstructedWithDifferentBoxes() {
        assertThrows(IllegalArgumentException.class, () -> new CollisionShape(
                CollisionShape.Kind.BLOCK, List.of()));
    }

    @Test
    void editablePresetCycleNeverCreatesHostCustomPlaceholder() {
        assertEquals(CollisionShape.Kind.FENCE, CollisionShape.Kind.NONE.nextEditablePreset());
        assertEquals(CollisionShape.Kind.BOTTOM_HALF, CollisionShape.Kind.FENCE.nextEditablePreset());
        assertEquals(CollisionShape.Kind.TOP_HALF, CollisionShape.Kind.BOTTOM_HALF.nextEditablePreset());
        assertEquals(CollisionShape.Kind.BLOCK, CollisionShape.Kind.TOP_HALF.nextEditablePreset());
        assertEquals(CollisionShape.Kind.NONE, CollisionShape.Kind.BLOCK.nextEditablePreset());
        assertEquals(CollisionShape.Kind.NONE, CollisionShape.Kind.CUSTOM.nextEditablePreset());
    }

    @Test
    void documentOperationsPreserveCollisionShape() {
        SceneLayer<String> layer = new SceneLayer<>(UUID.randomUUID(), "block", "stone",
                EditorTransform.IDENTITY, true);
        SceneDocument<String> document = new SceneDocument<>(List.of(layer), layer.id(),
                CollisionShape.forKind(CollisionShape.Kind.TOP_HALF));
        assertEquals(document.collisionShape(), document.select(layer.id()).collisionShape());
        assertEquals(document.collisionShape(), document.replace(layer.withVisible(false)).collisionShape());
        assertEquals(document.collisionShape(), document.append(new SceneLayer<>(UUID.randomUUID(),
                "second", "glass", EditorTransform.IDENTITY, true)).collisionShape());
    }
}
