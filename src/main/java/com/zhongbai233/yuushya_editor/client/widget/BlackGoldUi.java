package com.zhongbai233.yuushya_editor.client.widget;

import net.minecraft.client.gui.Font;

/** NCPB editor theme primitives kept local so the enhanced editor has no runtime NCPB dependency. */
public final class BlackGoldUi {
    public static final int GOLD = 0xFFD4A843;
    public static final int GOLD_DIM = 0xFF6B4F12;
    public static final int CYAN = 0xFF45E7FF;
    public static final int PANEL_TOP = 0xE0080A0D;
    public static final int PANEL_BOTTOM = 0xE0111318;
    public static final int VIEW_TOP = 0xFF080A0D;
    public static final int VIEW_BOTTOM = 0xFF111318;
    public static final int TEXT_PRIMARY = 0xFFE0D8C8;
    public static final int TEXT_SECONDARY = 0xFFA09888;
    public static final int TEXT_DIM = 0xFF605848;

    private BlackGoldUi() { }

    /** Clips dynamic block-state names using the same pixel-accurate rule as NCPB. */
    public static String ellipsize(Font font, String text, int maxWidth) {
        String safe = text == null ? "" : text;
        if (maxWidth <= 0) return "";
        if (font.width(safe) <= maxWidth) return safe;
        String ellipsis = "…";
        int ellipsisWidth = font.width(ellipsis);
        if (ellipsisWidth > maxWidth) return "";
        StringBuilder result = new StringBuilder();
        int width = 0;
        for (int offset = 0; offset < safe.length();) {
            int codePoint = safe.codePointAt(offset);
            String glyph = new String(Character.toChars(codePoint));
            int glyphWidth = font.width(glyph);
            if (width + glyphWidth + ellipsisWidth > maxWidth) break;
            result.append(glyph);
            width += glyphWidth;
            offset += Character.charCount(codePoint);
        }
        return result.append(ellipsis).toString();
    }
}
