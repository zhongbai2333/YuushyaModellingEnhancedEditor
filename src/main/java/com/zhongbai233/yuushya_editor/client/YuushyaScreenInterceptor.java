package com.zhongbai233.yuushya_editor.client;

import com.mojang.logging.LogUtils;
import com.zhongbai233.yuushya_editor.compat.Yuushya26EditorHost;
import com.zhongbai233.yuushya_editor.compat.Yuushya26StructuredEditorHost;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;

/** Replaces the audited Yuushya block/item/text editors and retains a one-shot original-screen bypass. */
public final class YuushyaScreenInterceptor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Screen> ORIGINAL_BYPASS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private YuushyaScreenInterceptor() { }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        Screen screen = event.getNewScreen();
        if (screen == null || ORIGINAL_BYPASS.remove(screen)) {
            return;
        }
        String className = screen.getClass().getName();
        boolean showBlock = Yuushya26EditorHost.SCREEN_CLASS.equals(className);
        boolean structured = Yuushya26StructuredEditorHost.ITEM_SCREEN_CLASS.equals(className)
                || Yuushya26StructuredEditorHost.TEXT_SCREEN_CLASS.equals(className);
        if (!showBlock && !structured) return;
        try {
            com.zhongbai233.yuushya_editor.compat.YuushyaEditorHost<Object> host = showBlock
                    ? Yuushya26EditorHost.fromOriginalScreen(screen)
                    : Yuushya26StructuredEditorHost.fromOriginalScreen(screen);
            event.setNewScreen(new YuushyaEditorScreen(host, screen));
            LOGGER.info("Opened enhanced Yuushya {} editor", host.editorType().name().toLowerCase());
        } catch (RuntimeException | LinkageError exception) {
            LOGGER.error("Enhanced editor binding failed; keeping the original Yuushya screen", exception);
        }
    }

    public static void openOriginal(Screen screen) {
        ORIGINAL_BYPASS.add(screen);
        Minecraft.getInstance().setScreen(screen);
    }
}
