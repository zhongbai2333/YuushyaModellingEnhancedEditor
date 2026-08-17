package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhongbai233.yuushya_editor.core.preview.CollisionBoxOperations;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import java.util.List;
import org.junit.jupiter.api.Test;

class CollisionBoxOperationsTest {
    @Test
    void mergesAdjacentBoxesWithoutExpandingVolume() {
        List<CollisionShape.Box> result = CollisionBoxOperations.simplify(List.of(
                box(0, 0, 0, 0.5, 1, 1), box(0.5, 0, 0, 1, 1, 1)));
        assertEquals(List.of(box(0, 0, 0, 1, 1, 1)), result);
    }

    @Test
    void removesContainedAndDegenerateBoxes() {
        List<CollisionShape.Box> result = CollisionBoxOperations.simplify(List.of(
                box(0, 0, 0, 1, 1, 1), box(0.25, 0.25, 0.25, 0.75, 0.75, 0.75),
                box(2, 0, 0, 2, 1, 1)));
        assertEquals(List.of(box(0, 0, 0, 1, 1, 1)), result);
    }

    @Test
    void keepsBoxesThatCannotBeMergedExactly() {
        List<CollisionShape.Box> boxes = List.of(
                box(0, 0, 0, 0.5, 1, 1), box(0.5, 0.25, 0, 1, 1, 1));
        assertEquals(boxes, CollisionBoxOperations.simplify(boxes));
    }

    @Test
    void detectsAndClipsBoxesOutsideTheSourceCell() {
        List<CollisionShape.Box> boxes = List.of(
                box(-0.25, 0.25, 0.25, 0.5, 1.25, 0.75),
                box(1.1, 0, 0, 1.2, 1, 1));
        assertTrue(CollisionBoxOperations.extendsOutsideUnitCell(boxes));
        List<CollisionShape.Box> clipped = CollisionBoxOperations.clipToUnitCell(boxes);
        assertEquals(List.of(box(0, 0.25, 0.25, 0.5, 1, 0.75)), clipped);
        assertFalse(CollisionBoxOperations.extendsOutsideUnitCell(clipped));
    }

    @Test
    void snapsBoundsOutwardToSixteenths() {
        assertEquals(box(-0.0625, 0, 0, 1.0625, 0.5625, 1),
                CollisionBoxOperations.snapOutward(box(-0.001, 0.001, 0, 1.001, 0.501, 1), 16));
        assertEquals(box(0, 0, 0, 1, 0.5, 1),
                CollisionBoxOperations.snapOutward(box(0, 0, 0, 1, 0.5, 1), 16));
    }

    @Test
    void rotatesAsymmetricBoxAroundUnitCellAndRoundTrips() {
        List<CollisionShape.Box> source = List.of(box(0.125, 0.25, 0.5, 0.375, 0.75, 0.875));
        assertEquals(List.of(box(0.125, 0.25, 0.125, 0.5, 0.75, 0.375)),
                CollisionBoxOperations.rotateYClockwise(source, 1));
        assertEquals(List.of(box(0.625, 0.25, 0.125, 0.875, 0.75, 0.5)),
                CollisionBoxOperations.rotateYClockwise(source, 2));
        assertEquals(source, CollisionBoxOperations.rotateYClockwise(
                CollisionBoxOperations.rotateYClockwise(source, 1), -1));
    }

    private static CollisionShape.Box box(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        return new CollisionShape.Box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
