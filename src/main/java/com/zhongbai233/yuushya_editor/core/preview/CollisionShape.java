package com.zhongbai233.yuushya_editor.core.preview;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Host-neutral snapshot of the ShowBlock collision shape. Coordinates are block-local. */
public record CollisionShape(Kind kind, List<Box> boxes) {
    public CollisionShape {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(boxes, "boxes");
        boxes = List.copyOf(boxes);
        if (boxes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("collision boxes must not contain null");
        }
        if (kind != Kind.CUSTOM && !boxes.equals(presetBoxes(kind))) {
            throw new IllegalArgumentException("preset collision shape boxes do not match its kind");
        }
    }

    public static CollisionShape forKind(Kind kind) {
        Objects.requireNonNull(kind, "kind");
        return new CollisionShape(kind, presetBoxes(kind));
    }

    private static List<Box> presetBoxes(Kind kind) {
        return switch (kind) {
            case NONE -> List.of();
            case FENCE -> List.of(new Box(0.4375D, 0.0D, 0.4375D, 0.5625D, 1.5D, 0.5625D));
            case BOTTOM_HALF -> List.of(new Box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D));
            case TOP_HALF -> List.of(new Box(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D));
            case BLOCK -> List.of(new Box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));
            case CUSTOM -> List.of();
        };
    }

    public static CollisionShape custom(List<Box> boxes) {
        return new CollisionShape(Kind.CUSTOM, new ArrayList<>(Objects.requireNonNull(boxes, "boxes")));
    }

    public static CollisionShape none() {
        return forKind(Kind.NONE);
    }

    public static CollisionShape block() {
        return forKind(Kind.BLOCK);
    }

    public boolean isEmpty() {
        return boxes.isEmpty();
    }

    public enum Kind {
        NONE,
        FENCE,
        BOTTOM_HALF,
        TOP_HALF,
        BLOCK,
        CUSTOM;

        public Kind nextEditablePreset() {
            return switch (this) {
                case NONE -> FENCE;
                case FENCE -> BOTTOM_HALF;
                case BOTTOM_HALF -> TOP_HALF;
                case TOP_HALF -> BLOCK;
                case BLOCK -> CUSTOM;
                case CUSTOM -> NONE;
            };
        }

        public String translationKey() {
            return "screen.yuushya_modelling_enhanced_editor.collision." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Box(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        public Box {
            double[] values = {minX, minY, minZ, maxX, maxY, maxZ};
            for (double value : values) {
                if (!Double.isFinite(value)) throw new IllegalArgumentException("collision box values must be finite");
            }
            if (minX > maxX || minY > maxY || minZ > maxZ) {
                throw new IllegalArgumentException("collision box minimum exceeds maximum");
            }
        }
    }
}
