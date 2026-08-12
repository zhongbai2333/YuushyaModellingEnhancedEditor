package com.zhongbai233.yuushya_editor.compat;

import com.zhongbai233.yuushya_editor.core.SceneDocument;
import com.zhongbai233.yuushya_editor.core.EditorType;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import java.util.Optional;
import net.minecraft.core.BlockPos;

/** Boundary implemented by a version-specific Yuushya adapter; core code never imports Yuushya classes. */
public interface YuushyaEditorHost<T> {
    SceneDocument<T> loadDocument();

    default EditorType editorType() {
        return EditorType.BLOCK;
    }

    /** Payload passed by Yuushya while opening an add-item/add-text screen. */
    default Optional<SceneLayer<T>> initialLayer() {
        return Optional.empty();
    }

    ValidationResult validate(SceneDocument<T> draft);

    void submit(SceneDocument<T> original, SceneDocument<T> draft);

    /** World-space anchor used only for the read-only terrain preview around the edited ShowBlock. */
    default Optional<BlockPos> worldOrigin() {
        return Optional.empty();
    }

    record ValidationResult(boolean valid, String message, Object[] arguments) {
        public ValidationResult {
            arguments = arguments == null ? new Object[0] : arguments.clone();
        }

        public static ValidationResult ok() { return new ValidationResult(true, "", new Object[0]); }
        public static ValidationResult rejected(String message, Object... arguments) {
            return new ValidationResult(false, message, arguments);
        }

        @Override
        public Object[] arguments() { return arguments.clone(); }
    }
}
