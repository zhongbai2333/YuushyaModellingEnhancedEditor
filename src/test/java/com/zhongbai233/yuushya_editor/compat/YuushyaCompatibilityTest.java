package com.zhongbai233.yuushya_editor.compat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class YuushyaCompatibilityTest {
    @Test
    void missingIntegrationFailsClosed() {
        ClassLoader empty = new ClassLoader(null) { };
        var result = YuushyaCompatibility.probe(empty);
        assertFalse(result.available());
        assertEquals("none", result.variant());
    }
}
