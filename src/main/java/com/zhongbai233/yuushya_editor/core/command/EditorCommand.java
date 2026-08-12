package com.zhongbai233.yuushya_editor.core.command;

public interface EditorCommand<S> {
    S apply(S state);
    S undo(S state);
    String description();
}
