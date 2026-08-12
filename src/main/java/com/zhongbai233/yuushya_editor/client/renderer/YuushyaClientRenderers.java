package com.zhongbai233.yuushya_editor.client.renderer;

import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

/** Client render registrations kept separate from the reflective Yuushya adapter. */
public final class YuushyaClientRenderers {
    private YuushyaClientRenderers() { }

    public static void registerPictureInPictureRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(BlockPreviewPipRenderState.class, BlockPreviewPipRenderer::new);
    }
}
