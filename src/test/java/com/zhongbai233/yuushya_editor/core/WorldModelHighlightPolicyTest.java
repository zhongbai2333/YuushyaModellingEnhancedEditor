package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldModelHighlightPolicyTest {
    @Test
    void drawsOnlyWhileTheEditorIsHeldAndNoNativeOverlayOwnsTheBlock() {
        assertAll(
                () -> assertTrue(WorldModelHighlightPolicy.shouldDraw(true, false, false, false)),
                () -> assertFalse(WorldModelHighlightPolicy.shouldDraw(false, false, false, false)),
                () -> assertFalse(WorldModelHighlightPolicy.shouldDraw(true, true, false, false)),
                () -> assertFalse(WorldModelHighlightPolicy.shouldDraw(true, false, true, false)),
                () -> assertFalse(WorldModelHighlightPolicy.shouldDraw(true, false, false, true)));
    }
}
