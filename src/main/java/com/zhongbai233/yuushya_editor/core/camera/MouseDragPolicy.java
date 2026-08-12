package com.zhongbai233.yuushya_editor.core.camera;

/** Maps preview mouse drags to editor responsibilities without depending on a GUI toolkit. */
public final class MouseDragPolicy {
    private MouseDragPolicy() { }

    public static Action action(int mouseButton, boolean firstPerson, boolean gizmoHandleActive) {
        if (mouseButton == 1 && !firstPerson) return Action.PAN;
        if (mouseButton == 0 && gizmoHandleActive && !firstPerson) return Action.GIZMO;
        return Action.ORBIT;
    }

    public enum Action {
        ORBIT,
        PAN,
        GIZMO
    }
}
