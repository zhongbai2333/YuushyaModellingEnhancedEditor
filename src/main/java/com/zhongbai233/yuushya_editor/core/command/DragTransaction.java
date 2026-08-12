package com.zhongbai233.yuushya_editor.core.command;

import java.util.Objects;
import java.util.function.UnaryOperator;

/** Coalesces a continuous pointer drag into one undo step. */
public final class DragTransaction<S> {
    private final S before;
    private final String description;
    private S current;

    public DragTransaction(S before, String description) {
        this.before = Objects.requireNonNull(before, "before");
        this.current = before;
        this.description = Objects.requireNonNull(description, "description");
    }

    public S update(UnaryOperator<S> operation) {
        current = Objects.requireNonNull(operation.apply(current), "operation result");
        return current;
    }

    public boolean changed() { return !before.equals(current); }
    public S before() { return before; }
    public S current() { return current; }

    public S commit(CommandStack<S> stack) {
        Objects.requireNonNull(stack, "stack");
        if (!changed()) return current;
        S after = current;
        return stack.execute(before, new EditorCommand<>() {
            public S apply(S state) { return after; }
            public S undo(S state) { return before; }
            public String description() { return description; }
        });
    }
}
