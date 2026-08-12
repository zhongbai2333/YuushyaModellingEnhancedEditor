package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhongbai233.yuushya_editor.core.geometry.ZFightSaveCoordinator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ZFightSaveCoordinatorTest {
    @Test
    void optimizeAndSaveRunsBothActionsInOrder() {
        StringBuilder order = new StringBuilder();
        ZFightSaveCoordinator.Result result = ZFightSaveCoordinator.execute(
                ZFightSaveCoordinator.Decision.OPTIMIZE_AND_SAVE,
                () -> order.append("optimize>"), () -> {
                    order.append("save");
                    return true;
                });

        assertEquals("optimize>save", order.toString());
        assertEquals(ZFightSaveCoordinator.Result.SAVED, result);
    }

    @Test
    void saveUnchangedDoesNotOptimize() {
        AtomicInteger optimizations = new AtomicInteger();
        AtomicInteger submissions = new AtomicInteger();
        ZFightSaveCoordinator.Result result = ZFightSaveCoordinator.execute(
                ZFightSaveCoordinator.Decision.SAVE_UNCHANGED,
                optimizations::incrementAndGet, () -> {
                    submissions.incrementAndGet();
                    return true;
                });

        assertEquals(0, optimizations.get());
        assertEquals(1, submissions.get());
        assertEquals(ZFightSaveCoordinator.Result.SAVED, result);
    }

    @Test
    void returnToEditorDoesNotModifyOrSubmit() {
        AtomicInteger optimizations = new AtomicInteger();
        AtomicInteger submissions = new AtomicInteger();
        ZFightSaveCoordinator.Result result = ZFightSaveCoordinator.execute(
                ZFightSaveCoordinator.Decision.RETURN_TO_EDITOR,
                optimizations::incrementAndGet, () -> {
                    submissions.incrementAndGet();
                    return true;
                });

        assertEquals(0, optimizations.get());
        assertEquals(0, submissions.get());
        assertEquals(ZFightSaveCoordinator.Result.RETURNED_TO_EDITOR, result);
    }

    @Test
    void failedSubmissionIsReportedAfterOptimization() {
        AtomicInteger optimizations = new AtomicInteger();
        ZFightSaveCoordinator.Result result = ZFightSaveCoordinator.execute(
                ZFightSaveCoordinator.Decision.OPTIMIZE_AND_SAVE,
                optimizations::incrementAndGet, () -> false);

        assertEquals(1, optimizations.get());
        assertEquals(ZFightSaveCoordinator.Result.SAVE_FAILED, result);
    }
}
