package com.zhongbai233.yuushya_editor.mixin;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.client.anvilcraft.rendering.CachedModeClient;
import com.zhongbai233.yuushya_editor.compat.YuushyaItemBlockOverlayPolicy;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets Yuushya's own live renderer draw transient frame/axis/text overlays for cached item blocks. */
@Mixin(value = CachedModeClient.class, remap = false)
public abstract class CachedModeClientOverlayMixin {
    @Inject(method = "isCachedModeEnabledOn", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void yuushya_editor$useLiveRendererForEditingOverlay(BlockEntity blockEntity,
            CallbackInfoReturnable<Boolean> callback) {
        if (blockEntity instanceof ItemBlockEntity itemBlock
                && YuushyaItemBlockOverlayPolicy.requiresLiveRenderer(itemBlock)) {
            callback.setReturnValue(false);
        }
    }
}
