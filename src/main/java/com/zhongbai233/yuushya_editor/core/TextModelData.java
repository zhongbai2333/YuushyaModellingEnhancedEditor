package com.zhongbai233.yuushya_editor.core;

import java.util.List;
import java.util.Objects;

/** Host-neutral copy of Yuushya's JSON text lines and rendering flags. */
public record TextModelData(List<String> textLines, boolean culled, boolean mirror) {
    public TextModelData {
        Objects.requireNonNull(textLines, "textLines");
        textLines = List.copyOf(textLines);
        if (textLines.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("textLines must not contain null");
        }
    }

    public TextModelData withTextLines(List<String> value) {
        return new TextModelData(value, culled, mirror);
    }
}
