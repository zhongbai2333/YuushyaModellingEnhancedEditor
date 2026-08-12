package com.zhongbai233.yuushya_editor.core.geometry;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Executes the three explicit outcomes offered by the Z-fighting save gate. */
public final class ZFightSaveCoordinator {
    private ZFightSaveCoordinator() { }

    public static Result execute(Decision decision, Runnable optimize, BooleanSupplier submit) {
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(optimize, "optimize");
        Objects.requireNonNull(submit, "submit");
        if (decision == Decision.RETURN_TO_EDITOR) return Result.RETURNED_TO_EDITOR;
        if (decision == Decision.OPTIMIZE_AND_SAVE) optimize.run();
        return submit.getAsBoolean() ? Result.SAVED : Result.SAVE_FAILED;
    }

    public enum Decision {
        OPTIMIZE_AND_SAVE,
        SAVE_UNCHANGED,
        RETURN_TO_EDITOR
    }

    public enum Result {
        SAVED,
        SAVE_FAILED,
        RETURNED_TO_EDITOR
    }
}
