package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        LayerSelection.Result result = LayerSelection.click(
                ordered, List.of(second), second, second, false, true);

        assertEquals(List.of(second), result.selectedLayerIds());
        assertEquals(second, result.primaryLayerId());
    }

    @Test
    void controlClickRemovesPrimaryAndPromotesMostRecentlySelectedLayer() {
        LayerSelection.Result result = LayerSelection.click(
                ordered, List.of(first, second, third), third, third, false, true);

        assertEquals(List.of(first, second), result.selectedLayerIds());
        assertEquals(second, result.primaryLayerId());
    }

    @Test
    void controlClickOnUnselectedLayerAddsAndMakesItPrimary() {
        LayerSelection.Result result = LayerSelection.click(
                ordered, List.of(first), first, third, false, true);

        assertEquals(List.of(first, third), result.selectedLayerIds());
        assertEquals(third, result.primaryLayerId());
    }

    @Test
    void shiftClickReplacesSelectionWithContiguousRange() {
        LayerSelection.Result result = LayerSelection.click(
                ordered, List.of(first), first, third, true, false);

        assertEquals(ordered, result.selectedLayerIds());
        assertEquals(third, result.primaryLayerId());
    }
}
