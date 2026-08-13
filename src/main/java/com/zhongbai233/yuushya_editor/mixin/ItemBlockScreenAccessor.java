package com.zhongbai233.yuushya_editor.mixin;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Typed bridge to the Yuushya item screen state needed when replacing its UI. */
@Mixin(ItemBlockScreen.class)
public interface ItemBlockScreenAccessor {
    @Accessor("blockEntity")
    ItemBlockEntity yuushya_editor$getBlockEntity();

    @Accessor("newItemStack")
    ItemStack yuushya_editor$getNewItemStack();
}
