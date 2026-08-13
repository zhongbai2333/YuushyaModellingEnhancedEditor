package com.zhongbai233.yuushya_editor.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class EditorSessionPoolTest {
    @Test
    void takeRestoresMatchingSessionAndRemovesItFromPool() {
        EditorSessionPool<String, Integer> pool = new EditorSessionPool<>(8, 512, 100L, () -> 0L);
        CommandStack<Integer> history = historyWithCommands(2);
        pool.put("model", 2, history);

        EditorSessionPool.Session<Integer> restored = pool.take("model", 2,
                java.util.Objects::equals).orElseThrow();

        assertEquals(2, restored.document());
        assertEquals(2, restored.history().size());
        assertEquals(1, restored.history().undo(restored.document()));
        assertEquals(0, pool.sessionCount());
    }

    @Test
    void changedDocumentInvalidatesCachedHistory() {
        EditorSessionPool<String, Integer> pool = new EditorSessionPool<>(8, 512, 100L, () -> 0L);
        pool.put("model", 2, historyWithCommands(1));

        assertTrue(pool.take("model", 3, java.util.Objects::equals).isEmpty());
        assertEquals(0, pool.sessionCount());
    }

    @Test
    void evictsLeastRecentlyUsedSessionsToMeetBothLimits() {
        EditorSessionPool<String, Integer> pool = new EditorSessionPool<>(2, 3, 100L, () -> 0L);
        pool.put("first", 2, historyWithCommands(2));
        pool.put("second", 2, historyWithCommands(2));

        assertTrue(pool.take("first", 2, java.util.Objects::equals).isEmpty());
        assertTrue(pool.take("second", 2, java.util.Objects::equals).isPresent());
    }

    @Test
    void evictsOldestSessionWhenSessionLimitIsExceeded() {
        EditorSessionPool<String, Integer> pool = new EditorSessionPool<>(2, 512, 100L, () -> 0L);
        pool.put("first", 1, historyWithCommands(1));
        pool.put("second", 1, historyWithCommands(1));
        pool.put("third", 1, historyWithCommands(1));

        assertTrue(pool.take("first", 1, java.util.Objects::equals).isEmpty());
        assertTrue(pool.take("second", 1, java.util.Objects::equals).isPresent());
        assertTrue(pool.take("third", 1, java.util.Objects::equals).isPresent());
    }

    @Test
    void expiresInactiveSessions() {
        AtomicLong time = new AtomicLong();
        EditorSessionPool<String, Integer> pool = new EditorSessionPool<>(8, 512, 30L, time::get);
        pool.put("model", 1, historyWithCommands(1));

        time.set(30L);

        assertEquals(0, pool.sessionCount());
    }

    private static CommandStack<Integer> historyWithCommands(int count) {
        CommandStack<Integer> history = new CommandStack<>(128);
        int state = 0;
        for (int index = 0; index < count; index++) {
            int before = state;
            int after = before + 1;
            state = history.execute(state, new EditorCommand<>() {
                @Override public Integer apply(Integer ignored) { return after; }
                @Override public Integer undo(Integer ignored) { return before; }
                @Override public String description() { return "increment"; }
            });
        }
        return history;
    }
}
