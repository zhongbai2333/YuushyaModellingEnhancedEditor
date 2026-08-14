package com.zhongbai233.yuushya_editor.compat;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;

/** Keeps Yuushya's transient editing overlays on its live renderer instead of its static item cache. */
public final class YuushyaItemBlockOverlayPolicy {
    private YuushyaItemBlockOverlayPolicy() {
    }

    public static boolean requiresLiveRenderer(ItemBlockEntity blockEntity) {
        return requiresLiveRenderer(
                blockEntity.isShowFrame(), blockEntity.isShowAxis(), blockEntity.isShowText());
    }

    static boolean requiresLiveRenderer(boolean showFrame, boolean showAxis, boolean showText) {
        return showFrame || showAxis || showText;
    }
}
