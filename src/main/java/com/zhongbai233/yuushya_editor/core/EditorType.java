package com.zhongbai233.yuushya_editor.core;

/** Yuushya modelling block family currently hosted by the shared scene editor. */
public enum EditorType {
    BLOCK("screen.yuushya_modelling_enhanced_editor.type.block",
            "screen.yuushya_modelling_enhanced_editor.add_block"),
    ITEM("screen.yuushya_modelling_enhanced_editor.type.item",
            "screen.yuushya_modelling_enhanced_editor.add_item"),
    TEXT("screen.yuushya_modelling_enhanced_editor.type.text",
            "screen.yuushya_modelling_enhanced_editor.add_text");

    private final String translationKey;
    private final String addTranslationKey;

    EditorType(String translationKey, String addTranslationKey) {
        this.translationKey = translationKey;
        this.addTranslationKey = addTranslationKey;
    }

    public String translationKey() { return translationKey; }
    public String addTranslationKey() { return addTranslationKey; }
}
