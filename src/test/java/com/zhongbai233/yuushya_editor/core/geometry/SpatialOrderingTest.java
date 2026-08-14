package com.zhongbai233.yuushya_editor.core.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class SpatialOrderingTest {
    @Test
    void placesMembersOfSeparatedGroupsTogether() {
        Point farSecond = new Point("far-second", 101.0D, 0.0D, 0.0D);
        Point nearFirst = new Point("near-first", 0.0D, 0.0D, 0.0D);
        Point farFirst = new Point("far-first", 100.0D, 0.0D, 0.0D);
        Point nearSecond = new Point("near-second", 1.0D, 0.0D, 0.0D);

        List<Point> ordered = SpatialOrdering.nearestNeighbor(
                List.of(farSecond, nearFirst, farFirst, nearSecond), (Point point) -> point.center());

        assertEquals(List.of(nearFirst, nearSecond, farFirst, farSecond), ordered);
    }

    @Test
    void resolvesEqualPositionsUsingTheOriginalOrder() {
        Point first = new Point("first", 0.0D, 0.0D, 0.0D);
        Point second = new Point("second", 0.0D, 0.0D, 0.0D);
        Point third = new Point("third", 0.0D, 0.0D, 0.0D);

        assertEquals(List.of(first, second, third), SpatialOrdering.nearestNeighbor(
                List.of(first, second, third), (Point point) -> point.center()));
    }

    @Test
    void emptyAndSingleElementInputsRemainStable() {
        Point only = new Point("only", 2.0D, 3.0D, 4.0D);

        assertEquals(List.of(), SpatialOrdering.nearestNeighbor(
                List.<Point>of(), (Point point) -> point.center()));
        assertEquals(List.of(only), SpatialOrdering.nearestNeighbor(
                List.of(only), (Point point) -> point.center()));
    }

    private record Point(String name, Vector3d center) {
        Point(String name, double x, double y, double z) {
            this(name, new Vector3d(x, y, z));
        }
    }
}
