package com.zhongbai233.yuushya_editor.compat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class YuushyaSlotRewritePlanTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    @Test
    void compactsAnAirTombstoneWithoutMovingTheLeadingLayer() {
        YuushyaSlotRewritePlan.Plan plan = YuushyaSlotRewritePlan.create(
                List.of(A, B), Map.of(A, 0, B, 2), 3);

        assertAll(
                () -> assertFalse(plan.writes().get(0).fullRewrite()),
                () -> assertTrue(plan.writes().get(1).fullRewrite()),
                () -> assertEquals(2, plan.writes().get(1).sourceSlot()),
                () -> assertEquals(List.of(2), plan.resetSlots()));
    }

    @Test
    void deletingTheFirstLayerRewritesEveryShiftedSurvivor() {
        YuushyaSlotRewritePlan.Plan plan = YuushyaSlotRewritePlan.create(
                List.of(B, C), Map.of(A, 0, B, 1, C, 2), 3);

        assertAll(
                () -> assertTrue(plan.writes().get(0).fullRewrite()),
                () -> assertTrue(plan.writes().get(1).fullRewrite()),
                () -> assertEquals(List.of(2), plan.resetSlots()));
    }

    @Test
    void deletingTheLastLayerOnlyResetsItsPhysicalSlot() {
        YuushyaSlotRewritePlan.Plan plan = YuushyaSlotRewritePlan.create(
                List.of(A, B), Map.of(A, 0, B, 1, C, 2), 3);

        assertAll(
                () -> assertFalse(plan.writes().get(0).fullRewrite()),
                () -> assertFalse(plan.writes().get(1).fullRewrite()),
                () -> assertEquals(List.of(2), plan.resetSlots()));
    }

    @Test
    void newAndReorderedLayersAreFullyWritten() {
        YuushyaSlotRewritePlan.Plan plan = YuushyaSlotRewritePlan.create(
                List.of(B, A, C), Map.of(A, 0, B, 1), 2);

        assertAll(
                () -> assertTrue(plan.writes().get(0).fullRewrite()),
                () -> assertTrue(plan.writes().get(1).fullRewrite()),
                () -> assertTrue(plan.writes().get(2).fullRewrite()),
                () -> assertEquals(List.of(), plan.resetSlots()));
    }

    @Test
    void staleOutOfRangeSourceSlotsNeverUseDifferentialWrites() {
        YuushyaSlotRewritePlan.Plan plan = YuushyaSlotRewritePlan.create(
                List.of(A, B), Map.of(A, 0, B, 1), 1);

        assertAll(
                () -> assertFalse(plan.writes().get(0).fullRewrite()),
                () -> assertTrue(plan.writes().get(1).fullRewrite()));
    }
}
