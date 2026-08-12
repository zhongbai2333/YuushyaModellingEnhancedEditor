package com.zhongbai233.yuushya_editor.mixin;

import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Invalidates only preview sections touched by incremental server block updates. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerBlockUpdateMixin {
    @Shadow
    private ClientLevel level;

    @Inject(method = "handleBlockUpdate", at = @At("TAIL"), require = 0)
    private void yuushya_editor$markSingleBlockDirty(ClientboundBlockUpdatePacket packet, CallbackInfo callback) {
        BlockPos pos = packet.getPos();
        if (yuushya_editor$isLoaded(pos)) {
            EnvironmentPreviewManager.markBlockDirty(level, pos);
        }
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"), require = 0)
    private void yuushya_editor$markSectionBlocksDirty(ClientboundSectionBlocksUpdatePacket packet,
            CallbackInfo callback) {
        packet.runUpdates((pos, state) -> {
            if (yuushya_editor$isLoaded(pos)) {
                EnvironmentPreviewManager.markBlockDirty(level, pos);
            }
        });
    }

    private boolean yuushya_editor$isLoaded(BlockPos pos) {
        return level != null && level.getChunkSource().hasChunk(
                Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
    }
}
