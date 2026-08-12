package com.zhongbai233.yuushya_editor.client.environment;

import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Keeps the editor's environment cache synchronized with client chunk lifecycle events. */
@EventBusSubscriber(value = Dist.CLIENT)
public final class EnvironmentPreviewEvents {
    private EnvironmentPreviewEvents() { }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ClientLevel level) {
            var pos = event.getChunk().getPos();
            EnvironmentPreviewManager.markChunkLoaded(level, pos.x(), pos.z());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level) {
            var pos = event.getChunk().getPos();
            EnvironmentPreviewManager.markChunkUnloaded(level, pos.x(), pos.z());
        }
    }
}
