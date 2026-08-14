package com.zhongbai233.yuushya_editor.compat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YuushyaItemBlockOverlayPolicyTest {
    @Test
    void bypassesTheStaticCacheForEveryTransientEditingOverlay() {
        assertAll(
                () -> assertTrue(YuushyaItemBlockOverlayPolicy.requiresLiveRenderer(true, false, false)),
                () -> assertTrue(YuushyaItemBlockOverlayPolicy.requiresLiveRenderer(false, true, false)),
                () -> assertTrue(YuushyaItemBlockOverlayPolicy.requiresLiveRenderer(false, false, true)),
                () -> assertTrue(YuushyaItemBlockOverlayPolicy.requiresLiveRenderer(true, true, true)));
    }

    @Test
    void preservesCachedRenderingWhenNoOverlayIsVisible() {
        assertFalse(YuushyaItemBlockOverlayPolicy.requiresLiveRenderer(false, false, false));
    }
}
