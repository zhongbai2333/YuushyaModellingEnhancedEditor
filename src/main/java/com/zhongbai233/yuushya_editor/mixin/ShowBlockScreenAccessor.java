package com.zhongbai233.yuushya_editor.mixin;

import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Typed bridge to the Yuushya screen state needed when replacing its UI. */
@Mixin(ShowBlockScreen.class)
public interface ShowBlockScreenAccessor {
    @Accessor("blockEntity")
    ShowBlockEntity yuushya_editor$getBlockEntity();
}
