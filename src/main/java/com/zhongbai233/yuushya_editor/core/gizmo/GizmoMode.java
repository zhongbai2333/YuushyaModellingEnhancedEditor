package com.zhongbai233.yuushya_editor.core.gizmo;

/** Active transform tool, following the conventional W/E/R editor shortcuts. */
public enum GizmoMode {
    MOVE("Move"),
    ROTATE("Rotate"),
    SCALE("Scale");

    private final String label;

    GizmoMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
