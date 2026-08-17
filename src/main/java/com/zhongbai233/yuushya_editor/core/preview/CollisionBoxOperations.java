package com.zhongbai233.yuushya_editor.core.preview;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Deterministic cleanup operations shared by the collision editor and tests. */
public final class CollisionBoxOperations {
    private static final double EPSILON = 1.0e-7D;

    private CollisionBoxOperations() { }

    /**
     * Removes contained boxes and combines boxes that have equal spans on two axes and
     * overlap or touch on the third. This never expands the occupied volume.
     */
    public static List<CollisionShape.Box> simplify(List<CollisionShape.Box> input) {
        Objects.requireNonNull(input, "input");
        List<CollisionShape.Box> boxes = new ArrayList<>(input);
        boxes.removeIf(CollisionBoxOperations::degenerate);
        removeContained(boxes);
        boolean changed;
        do {
            changed = false;
            outer:
            for (int first = 0; first < boxes.size(); first++) {
                for (int second = first + 1; second < boxes.size(); second++) {
                    CollisionShape.Box merged = merge(boxes.get(first), boxes.get(second));
                    if (merged == null) continue;
                    boxes.set(first, merged);
                    boxes.remove(second);
                    removeContained(boxes);
                    changed = true;
                    break outer;
                }
            }
        } while (changed);
        boxes.sort(Comparator.comparingDouble(CollisionShape.Box::minY)
                .thenComparingDouble(CollisionShape.Box::minX)
                .thenComparingDouble(CollisionShape.Box::minZ)
                .thenComparingDouble(CollisionShape.Box::maxY)
                .thenComparingDouble(CollisionShape.Box::maxX)
                .thenComparingDouble(CollisionShape.Box::maxZ));
        return List.copyOf(boxes);
    }

    /** True when at least one box reaches outside the source block's vanilla query cell. */
    public static boolean extendsOutsideUnitCell(List<CollisionShape.Box> boxes) {
        Objects.requireNonNull(boxes, "boxes");
        return boxes.stream().anyMatch(box -> box.minX() < -EPSILON || box.minY() < -EPSILON
                || box.minZ() < -EPSILON || box.maxX() > 1.0D + EPSILON
                || box.maxY() > 1.0D + EPSILON || box.maxZ() > 1.0D + EPSILON);
    }

    /** Clips boxes to one block cell and removes any empty remnants. */
    public static List<CollisionShape.Box> clipToUnitCell(List<CollisionShape.Box> input) {
        Objects.requireNonNull(input, "input");
        return simplify(input.stream().map(box -> new CollisionShape.Box(
                Math.clamp(box.minX(), 0.0D, 1.0D), Math.clamp(box.minY(), 0.0D, 1.0D),
                Math.clamp(box.minZ(), 0.0D, 1.0D), Math.clamp(box.maxX(), 0.0D, 1.0D),
                Math.clamp(box.maxY(), 0.0D, 1.0D), Math.clamp(box.maxZ(), 0.0D, 1.0D)))
                .toList());
    }

    /** Expands bounds outward to the requested grid without shrinking occupied volume. */
    public static CollisionShape.Box snapOutward(CollisionShape.Box box, double divisionsPerBlock) {
        Objects.requireNonNull(box, "box");
        if (!Double.isFinite(divisionsPerBlock) || divisionsPerBlock <= 0.0D) {
            throw new IllegalArgumentException("divisionsPerBlock must be finite and positive");
        }
        return new CollisionShape.Box(floorGrid(box.minX(), divisionsPerBlock),
                floorGrid(box.minY(), divisionsPerBlock), floorGrid(box.minZ(), divisionsPerBlock),
                ceilGrid(box.maxX(), divisionsPerBlock), ceilGrid(box.maxY(), divisionsPerBlock),
                ceilGrid(box.maxZ(), divisionsPerBlock));
    }

    /** Rotates boxes clockwise around the center of a block cell when viewed from above. */
    public static List<CollisionShape.Box> rotateYClockwise(
            List<CollisionShape.Box> input, int quarterTurns) {
        Objects.requireNonNull(input, "input");
        int turns = Math.floorMod(quarterTurns, 4);
        if (turns == 0) return List.copyOf(input);
        return input.stream().map(box -> switch (turns) {
            case 1 -> new CollisionShape.Box(1.0D - box.maxZ(), box.minY(), box.minX(),
                    1.0D - box.minZ(), box.maxY(), box.maxX());
            case 2 -> new CollisionShape.Box(1.0D - box.maxX(), box.minY(), 1.0D - box.maxZ(),
                    1.0D - box.minX(), box.maxY(), 1.0D - box.minZ());
            case 3 -> new CollisionShape.Box(box.minZ(), box.minY(), 1.0D - box.maxX(),
                    box.maxZ(), box.maxY(), 1.0D - box.minX());
            default -> throw new AssertionError("normalized quarter turn is outside 1..3");
        }).toList();
    }

    private static double floorGrid(double value, double divisions) {
        return Math.floor(value * divisions + 1.0e-6D) / divisions;
    }

    private static double ceilGrid(double value, double divisions) {
        return Math.ceil(value * divisions - 1.0e-6D) / divisions;
    }

    private static void removeContained(List<CollisionShape.Box> boxes) {
        for (int candidate = boxes.size() - 1; candidate >= 0; candidate--) {
            for (int container = 0; container < boxes.size(); container++) {
                if (candidate == container) continue;
                if (contains(boxes.get(container), boxes.get(candidate))) {
                    boxes.remove(candidate);
                    break;
                }
            }
        }
    }

    private static CollisionShape.Box merge(CollisionShape.Box left, CollisionShape.Box right) {
        if (same(left.minY(), right.minY()) && same(left.maxY(), right.maxY())
                && same(left.minZ(), right.minZ()) && same(left.maxZ(), right.maxZ())
                && overlaps(left.minX(), left.maxX(), right.minX(), right.maxX())) {
            return new CollisionShape.Box(Math.min(left.minX(), right.minX()), left.minY(), left.minZ(),
                    Math.max(left.maxX(), right.maxX()), left.maxY(), left.maxZ());
        }
        if (same(left.minX(), right.minX()) && same(left.maxX(), right.maxX())
                && same(left.minZ(), right.minZ()) && same(left.maxZ(), right.maxZ())
                && overlaps(left.minY(), left.maxY(), right.minY(), right.maxY())) {
            return new CollisionShape.Box(left.minX(), Math.min(left.minY(), right.minY()), left.minZ(),
                    left.maxX(), Math.max(left.maxY(), right.maxY()), left.maxZ());
        }
        if (same(left.minX(), right.minX()) && same(left.maxX(), right.maxX())
                && same(left.minY(), right.minY()) && same(left.maxY(), right.maxY())
                && overlaps(left.minZ(), left.maxZ(), right.minZ(), right.maxZ())) {
            return new CollisionShape.Box(left.minX(), left.minY(), Math.min(left.minZ(), right.minZ()),
                    left.maxX(), left.maxY(), Math.max(left.maxZ(), right.maxZ()));
        }
        return null;
    }

    private static boolean contains(CollisionShape.Box outer, CollisionShape.Box inner) {
        return outer.minX() <= inner.minX() + EPSILON && outer.minY() <= inner.minY() + EPSILON
                && outer.minZ() <= inner.minZ() + EPSILON && outer.maxX() + EPSILON >= inner.maxX()
                && outer.maxY() + EPSILON >= inner.maxY() && outer.maxZ() + EPSILON >= inner.maxZ();
    }

    private static boolean overlaps(double firstMin, double firstMax, double secondMin, double secondMax) {
        return firstMax + EPSILON >= secondMin && secondMax + EPSILON >= firstMin;
    }

    private static boolean same(double left, double right) {
        return Math.abs(left - right) <= EPSILON;
    }

    private static boolean degenerate(CollisionShape.Box box) {
        return box.maxX() - box.minX() <= EPSILON || box.maxY() - box.minY() <= EPSILON
                || box.maxZ() - box.minZ() <= EPSILON;
    }
}
