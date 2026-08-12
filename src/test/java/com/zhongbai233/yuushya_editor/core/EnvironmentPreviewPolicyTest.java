package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhongbai233.yuushya_editor.core.environment.EnvironmentPreviewPolicy;
import org.junit.jupiter.api.Test;

class EnvironmentPreviewPolicyTest {
    @Test
    void usesNcpbDiameterAndThreeBlockDitherShell() {
        assertEquals(25.0D, EnvironmentPreviewPolicy.DIAMETER);
        assertEquals(1.0D, EnvironmentPreviewPolicy.retention(0.5D, 0.5D, 0.5D, 9, 0, 0));
        assertEquals(0.0D, EnvironmentPreviewPolicy.retention(0.5D, 0.5D, 0.5D, 13, 0, 0));
        double shell = EnvironmentPreviewPolicy.retention(0.5D, 0.5D, 0.5D, 11, 0, 0);
        assertTrue(shell > 0.0D && shell < 1.0D);
    }

    @Test
    void edgeScatterIsStableAndBounded() {
        long seed = EnvironmentPreviewPolicy.seed(4, 64, -7);
        boolean first = EnvironmentPreviewPolicy.rendersBlock(seed,
                4.5D, 64.5D, -6.5D, 15, 66, -8);
        assertEquals(first, EnvironmentPreviewPolicy.rendersBlock(seed,
                4.5D, 64.5D, -6.5D, 15, 66, -8));
        assertTrue(EnvironmentPreviewPolicy.rendersBlock(seed,
                4.5D, 64.5D, -6.5D, 4, 64, -7));
        assertFalse(EnvironmentPreviewPolicy.rendersBlock(seed,
                4.5D, 64.5D, -6.5D, 24, 64, -7));
        assertTrue(EnvironmentPreviewPolicy.sectionMayContainDetail(
                0.5D, 0.5D, 0.5D, 0, 0, 0));
        assertFalse(EnvironmentPreviewPolicy.sectionMayContainDetail(
                0.5D, 0.5D, 0.5D, 32, 0, 0));
    }
}
