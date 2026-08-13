package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ItemColorCodecTest {
    @Test
    void parsesRgbAndArgbWithoutLosingAlpha() {
        assertEquals(0xFF336699, ItemColorCodec.parse("#336699"));
        assertEquals(0x7F336699, ItemColorCodec.parse("7f336699"));
    }

    @Test
    void rejectsAmbiguousColourValues() {
        assertThrows(IllegalArgumentException.class, () -> ItemColorCodec.parse("#12345"));
        assertThrows(IllegalArgumentException.class, () -> ItemColorCodec.parse("#GG336699"));
    }
}
