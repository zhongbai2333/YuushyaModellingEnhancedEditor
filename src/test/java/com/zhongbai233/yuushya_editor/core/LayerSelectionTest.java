package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zhongbai233.scene_editor.core.selection.MultiSelectionPolicy;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LayerSelectionTest {
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final UUID third = UUID.randomUUID();
    private final List<UUID> ordered = List.of(first, second, third);

    @Test
    void controlClickKeepsTheOnlySelectedLayer() {
        MultiSelectionPolicy.Result result = MultiSelectionPolicy.click(
                ordered, List.of(second), second, second, false, true);

        assertEquals(List.of(second), result.selectedElementIds());
        assertEquals(second, result.primaryElementId());
    }

    @Test
    void controlClickRemovesPrimaryAndPromotesMostRecentlySelectedLayer() {
        MultiSelectionPolicy.Result result = MultiSelectionPolicy.click(
                ordered, List.of(first, second, third), third, third, false, true);

        assertEquals(List.of(first, second), result.selectedElementIds());
        assertEquals(second, result.primaryElementId());
    }

    @Test
    void controlClickOnUnselectedLayerAddsAndMakesItPrimary() {
        MultiSelectionPolicy.Result result = MultiSelectionPolicy.click(
                ordered, List.of(first), first, third, false, true);

        assertEquals(List.of(first, third), result.selectedElementIds());
        assertEquals(third, result.primaryElementId());
    }

    @Test
    void shiftClickReplacesSelectionWithContiguousRange() {
        MultiSelectionPolicy.Result result = MultiSelectionPolicy.click(
                ordered, List.of(first), first, third, true, false);

        assertEquals(ordered, result.selectedElementIds());
        assertEquals(third, result.primaryElementId());
    }
}
