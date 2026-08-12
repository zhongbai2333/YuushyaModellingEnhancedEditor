package com.zhongbai233.yuushya_editor.core.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Bounded undo/redo history. */
public final class CommandStack<S> {
    private final int capacity;
    private final Deque<EditorCommand<S>> undo = new ArrayDeque<>();
    private final Deque<EditorCommand<S>> redo = new ArrayDeque<>();

    public CommandStack(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
    }

    public S execute(S state, EditorCommand<S> command) {
        EditorCommand<S> required = Objects.requireNonNull(command, "command");
        S result = Objects.requireNonNull(required.apply(state), "command result");
        undo.addLast(required);
        while (undo.size() > capacity) undo.removeFirst();
        redo.clear();
        return result;
    }

    public S undo(S state) {
        if (undo.isEmpty()) return state;
        EditorCommand<S> command = undo.removeLast();
        S result = Objects.requireNonNull(command.undo(state), "undo result");
        redo.addLast(command);
        return result;
    }

    public S redo(S state) {
        if (redo.isEmpty()) return state;
        EditorCommand<S> command = redo.removeLast();
        S result = Objects.requireNonNull(command.apply(state), "redo result");
        undo.addLast(command);
        return result;
    }

    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }
    public void clear() { undo.clear(); redo.clear(); }
}
