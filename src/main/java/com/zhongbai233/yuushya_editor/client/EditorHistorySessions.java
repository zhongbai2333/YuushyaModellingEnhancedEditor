package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.yuushya_editor.compat.YuushyaEditorHost;
import com.zhongbai233.yuushya_editor.core.EditorType;
import com.zhongbai233.yuushya_editor.core.SceneDocument;
import com.zhongbai233.yuushya_editor.core.SceneDocumentEquivalence;
import com.zhongbai233.yuushya_editor.core.command.CommandStack;
import com.zhongbai233.yuushya_editor.core.command.EditorSessionPool;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Owns bounded undo histories for editors that are closed but may be reopened this game session. */
public final class EditorHistorySessions {
    static final int MAXIMUM_SESSIONS = 8;
    static final int MAXIMUM_COMMANDS = 512;
    static final Duration TIME_TO_LIVE = Duration.ofMinutes(30L);

    private static final EditorSessionPool<Key, SceneDocument<Object>> POOL =
            new EditorSessionPool<>(MAXIMUM_SESSIONS, MAXIMUM_COMMANDS, TIME_TO_LIVE.toNanos());

    private EditorHistorySessions() { }

    static Optional<Key> key(YuushyaEditorHost<?> host) {
        Objects.requireNonNull(host, "host");
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return Optional.empty();
        Optional<BlockPos> origin = host.worldOrigin();
        if (origin.isEmpty()) return Optional.empty();
        String dimension = minecraft.level.dimension().identifier().toString();
        return Optional.of(new Key(host.editorType(), dimension, origin.orElseThrow().asLong()));
    }

    static Optional<EditorSessionPool.Session<SceneDocument<Object>>> take(
            Key key, SceneDocument<Object> currentDocument) {
        return POOL.take(key, currentDocument, SceneDocumentEquivalence::samePersistedContent);
    }

    static void put(Key key, SceneDocument<Object> document,
            CommandStack<SceneDocument<Object>> history) {
        POOL.put(key, document, history);
    }

    static void discard(Key key) {
        POOL.discard(key);
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        POOL.clear();
    }

    record Key(EditorType editorType, String dimension, long blockPosition) {
        Key {
            Objects.requireNonNull(editorType, "editorType");
            Objects.requireNonNull(dimension, "dimension");
        }
    }
}
