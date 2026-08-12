package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.*;
import com.zhongbai233.yuushya_editor.core.command.CommandStack;
import com.zhongbai233.yuushya_editor.core.command.DragTransaction;
import org.junit.jupiter.api.Test;

class CommandStackTest {
    @Test
    void dragIsOneUndoStep() {
        CommandStack<Integer> history = new CommandStack<>(8);
        DragTransaction<Integer> drag = new DragTransaction<>(1, "move");
        drag.update(value -> value + 1);
        drag.update(value -> value + 3);
        int state = drag.commit(history);
        assertEquals(5, state);
        assertEquals(1, history.undo(state));
        assertEquals(5, history.redo(1));
    }

    @Test
    void unchangedDragDoesNotPolluteHistory() {
        CommandStack<Integer> history = new CommandStack<>(8);
        assertEquals(4, new DragTransaction<>(4, "noop").commit(history));
        assertFalse(history.canUndo());
    }
}
