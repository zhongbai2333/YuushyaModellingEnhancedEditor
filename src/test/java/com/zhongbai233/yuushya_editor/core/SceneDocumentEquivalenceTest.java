package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class SceneDocumentEquivalenceTest {
    @Test
    void ignoresEditorOnlyIdentityNameAndSelection() {
        SceneDocument<String> first = document(UUID.randomUUID(), "first", true, EditorTransform.IDENTITY);
        SceneDocument<String> second = document(UUID.randomUUID(), "second", false, EditorTransform.IDENTITY);

        assertTrue(SceneDocumentEquivalence.samePersistedContent(first, second));
    }

    @Test
    void acceptsEquivalentQuaternionSignAndTinyRoundTripNoise() {
        EditorTransform first = new EditorTransform(new Vector3d(1.0D, 2.0D, 3.0D),
                new Quaternionf().rotationXYZ(0.2F, 0.4F, 0.6F), new Vector3f(2.0F));
        Quaternionf negated = first.rotation().mul(-1.0F);
        EditorTransform second = new EditorTransform(new Vector3d(1.0D + 1.0E-8D, 2.0D, 3.0D),
                negated, new Vector3f(2.0F));

        assertTrue(SceneDocumentEquivalence.samePersistedContent(
                document(UUID.randomUUID(), "first", true, first),
                document(UUID.randomUUID(), "second", true, second)));
    }

    @Test
    void rejectsPersistedContentChanges() {
        SceneDocument<String> first = document(UUID.randomUUID(), "first", true, EditorTransform.IDENTITY);
        EditorTransform moved = EditorTransform.IDENTITY.withPosition(new Vector3d(1.0D, 0.0D, 0.0D));
        SceneDocument<String> second = document(UUID.randomUUID(), "second", true, moved);

        assertFalse(SceneDocumentEquivalence.samePersistedContent(first, second));
    }

    private static SceneDocument<String> document(UUID id, String name, boolean selected,
            EditorTransform transform) {
        SceneLayer<String> layer = new SceneLayer<>(id, name, "stone", transform, true);
        return new SceneDocument<>(List.of(layer), selected ? id : null);
    }
}
