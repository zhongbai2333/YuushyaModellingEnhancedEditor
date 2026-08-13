package com.zhongbai233.yuushya_editor.core;

import java.util.Objects;

/** Parses Yuushya item-layer colours without dropping their alpha channel. */
public final class ItemColorCodec {
    private ItemColorCodec() { }

    public static int parse(String input) {
        String value = Objects.requireNonNull(input, "input").strip();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.length() == 6) value = "FF" + value;
        if (value.length() != 8 || !value.matches("[0-9a-fA-F]{8}")) {
            throw new IllegalArgumentException("Expected #RRGGBB or #AARRGGBB");
        }
        return (int) Long.parseLong(value, 16);
    }
}
