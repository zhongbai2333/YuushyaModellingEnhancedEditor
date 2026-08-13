package com.zhongbai233.yuushya_editor.mixin;

import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Typed bridge to the Yuushya text screen state needed when replacing its UI. */
@Mixin(TextBlockScreen.class)
public interface TextBlockScreenAccessor {
    @Accessor("blockEntity")
    TextBlockEntity yuushya_editor$getBlockEntity();

    @Accessor("newTextLines")
    List<String> yuushya_editor$getNewTextLines();
}
