package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zhongbai233.scene_editor.core.camera.CursorWrapPolicy;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class CursorWrapPolicyTest {
    @Test
    void wrapsRightEdgeToLeftMargin() {
        OptionalDouble target = CursorWrapPolicy.horizontalTarget(799.0D, 800.0D, 2.0D);

        assertEquals(2.0D, target.orElseThrow());
    }

    @Test
    void wrapsLeftEdgeToRightMargin() {
        OptionalDouble target = CursorWrapPolicy.horizontalTarget(1.0D, 800.0D, 2.0D);

        assertEquals(798.0D, target.orElseThrow());
    }

    @Test
    void leavesInteriorCursorAlone() {
        assertFalse(CursorWrapPolicy.horizontalTarget(400.0D, 800.0D, 2.0D).isPresent());
    }

    @Test
    void wrapsBottomEdgeToTopMargin() {
        assertEquals(2.0D, CursorWrapPolicy.verticalTarget(599.0D, 600.0D, 2.0D).orElseThrow());
    }

    @Test
    void wrapsTopEdgeToBottomMargin() {
        assertEquals(598.0D, CursorWrapPolicy.verticalTarget(1.0D, 600.0D, 2.0D).orElseThrow());
    }

    @Test
    void leavesInteriorVerticalCursorAlone() {
        assertFalse(CursorWrapPolicy.verticalTarget(300.0D, 600.0D, 2.0D).isPresent());
    }

    @Test
    void rejectsMarginThatConsumesWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> CursorWrapPolicy.horizontalTarget(5.0D, 10.0D, 5.0D));
    }
}
