package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SceneDocumentTest {
    @Test
    void appendPreservesOrderAndSelectsTheNewLayer() {
        SceneLayer<String> first = new SceneLayer<>(UUID.randomUUID(), "first", "stone",
                EditorTransform.IDENTITY, true);
        SceneLayer<String> added = new SceneLayer<>(UUID.randomUUID(), "added", "glass",
                EditorTransform.IDENTITY, true);

        SceneDocument<String> result = new SceneDocument<>(List.of(first), first.id()).append(added);

        assertEquals(List.of(first, added), result.layers());
        assertEquals(added.id(), result.selectedLayerId());
    }

    @Test
    void appendRejectsDuplicateIdentity() {
        SceneLayer<String> first = new SceneLayer<>(UUID.randomUUID(), "first", "stone",
                EditorTransform.IDENTITY, true);
        SceneDocument<String> document = new SceneDocument<>(List.of(first), first.id());

        assertThrows(IllegalArgumentException.class, () -> document.append(
                new SceneLayer<>(first.id(), "duplicate", "glass", EditorTransform.IDENTITY, true)));
    }

    @Test
    void removeSelectsTheNextLayerThenThePreviousAtTheEnd() {
        SceneLayer<String> first = layer("first");
        SceneLayer<String> middle = layer("middle");
        SceneLayer<String> last = layer("last");
        SceneDocument<String> document = new SceneDocument<>(List.of(first, middle, last), middle.id());

        SceneDocument<String> withoutMiddle = document.remove(middle.id());
        assertEquals(List.of(first, last), withoutMiddle.layers());
        assertEquals(last.id(), withoutMiddle.selectedLayerId());

        SceneDocument<String> withoutLast = withoutMiddle.remove(last.id());
        assertEquals(first.id(), withoutLast.selectedLayerId());
    }

    @Test
    void removeLastLayerClearsSelection() {
        SceneLayer<String> only = layer("only");
        SceneDocument<String> result = new SceneDocument<>(List.of(only), only.id()).remove(only.id());
        assertTrue(result.layers().isEmpty());
        assertNull(result.selectedLayerId());
    }

    private static SceneLayer<String> layer(String name) {
        return new SceneLayer<>(UUID.randomUUID(), name, name, EditorTransform.IDENTITY, true);
    }
}
