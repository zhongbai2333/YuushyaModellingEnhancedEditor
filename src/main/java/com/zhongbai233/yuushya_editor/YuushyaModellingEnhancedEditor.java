package com.zhongbai233.yuushya_editor;

import com.mojang.logging.LogUtils;
import com.zhongbai233.yuushya_editor.client.YuushyaScreenInterceptor;
import com.zhongbai233.yuushya_editor.client.renderer.YuushyaClientRenderers;
import com.zhongbai233.yuushya_editor.compat.YuushyaCompatibility;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/** Client-only bootstrap. Yuushya remains authoritative for world data and networking. */
@Mod(value = YuushyaModellingEnhancedEditor.MOD_ID, dist = Dist.CLIENT)
public final class YuushyaModellingEnhancedEditor {
    public static final String MOD_ID = "yuushya_modelling_enhanced_editor";
    private static final Logger LOGGER = LogUtils.getLogger();

    public YuushyaModellingEnhancedEditor(IEventBus modBus, ModContainer container) {
        modBus.addListener(YuushyaClientRenderers::registerPictureInPictureRenderers);
        NeoForge.EVENT_BUS.addListener(YuushyaScreenInterceptor::onScreenOpening);
        YuushyaCompatibility.ProbeResult result = YuushyaCompatibility.probe(
                Thread.currentThread().getContextClassLoader());
        if (result.available()) {
            LOGGER.info("Yuushya Modelling compatibility target detected: {}", result.variant());
        } else {
            LOGGER.warn("Enhanced editor is inactive: {}", result.message());
        }
    }
}
