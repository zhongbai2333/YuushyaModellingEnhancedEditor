package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SceneLayerClipboardTest {
    @Test
    void pastePreservesLayerDataButCreatesANewIdentity() {
        SceneLayerClipboard<String> clipboard = new SceneLayerClipboard<>();
        SceneLayer<String> source = new SceneLayer<>(UUID.randomUUID(), "source", "payload",
                EditorTransform.IDENTITY, false);

        clipboard.copy(source);
        SceneLayer<String> pasted = clipboard.paste("source copy");

        assertNotEquals(source.id(), pasted.id());
        assertEquals("source copy", pasted.name());
        assertEquals(source.hostData(), pasted.hostData());
        assertEquals(source.transform(), pasted.transform());
        assertEquals(source.visible(), pasted.visible());
    }

    @Test
    void emptyClipboardCannotPaste() {
        SceneLayerClipboard<String> clipboard = new SceneLayerClipboard<>();
        assertThrows(IllegalStateException.class, () -> clipboard.paste("copy"));
    }

    @Test
    void batchPastePreservesOrderAndCreatesFreshIdentities() {
        SceneLayerClipboard<String> clipboard = new SceneLayerClipboard<>();
        SceneLayer<String> first = new SceneLayer<>(UUID.randomUUID(), "first", "a",
                EditorTransform.IDENTITY, true);
        SceneLayer<String> second = new SceneLayer<>(UUID.randomUUID(), "second", "b",
                EditorTransform.IDENTITY, false);

        clipboard.copy(List.of(first, second));
        List<SceneLayer<String>> pasted = clipboard.pasteAll();

        assertEquals(2, pasted.size());
        assertEquals("a", pasted.get(0).hostData());
        assertEquals("b", pasted.get(1).hostData());
        assertNotEquals(first.id(), pasted.get(0).id());
        assertNotEquals(second.id(), pasted.get(1).id());
    }
}
