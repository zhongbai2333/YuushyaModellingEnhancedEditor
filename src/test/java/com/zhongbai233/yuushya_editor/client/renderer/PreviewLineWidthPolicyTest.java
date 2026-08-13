package com.zhongbai233.yuushya_editor.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhongbai233.scene_editor.core.render.LineWidthPolicy;
import org.junit.jupiter.api.Test;

class PreviewLineWidthPolicyTest {
    @Test
    void perspectiveLinesGrowWithDistanceAndRemainBounded() {
        float near = LineWidthPolicy.perspective(2.0F);
        float medium = LineWidthPolicy.perspective(12.0F);
        float far = LineWidthPolicy.perspective(10_000.0F);

        assertEquals(1.30F, near, 0.001F);
        assertTrue(medium > near);
        assertEquals(6.0F, far, 0.001F);
    }

    @Test
    void orthographicLinesGrowWhenMoreWorldSpaceIsVisible() {
        float close = LineWidthPolicy.orthographic(2.0F);
        float zoomedOut = LineWidthPolicy.orthographic(12.0F);

        assertEquals(1.30F, close, 0.001F);
        assertTrue(zoomedOut > close);
    }

    @Test
    void invalidInputsUseSafeBaseline() {
        assertEquals(1.30F, LineWidthPolicy.perspective(Float.NaN), 0.001F);
        assertEquals(1.30F, LineWidthPolicy.orthographic(0.0F), 0.001F);
    }

    @Test
    void thinnestRenderedTierIsRaisedToTwoPhysicalPixels() {
        assertEquals(2.0F, LineWidthPolicy.visibleWidth(1.0F, 1.30F), 0.001F);
        assertTrue(LineWidthPolicy.visibleWidth(1.5F, 1.30F) > 2.0F);
        assertEquals(8.0F, LineWidthPolicy.visibleWidth(4.0F, 6.0F), 0.001F);
    }
}
