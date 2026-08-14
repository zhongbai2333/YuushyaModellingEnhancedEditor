package com.zhongbai233.yuushya_editor.core.geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/** Deterministically places spatially close elements next to each other in a linear hierarchy. */
public final class SpatialOrdering {
    private SpatialOrdering() {
    }

    public static <T> List<T> nearestNeighbor(List<T> values,
            Function<? super T, ? extends Vector3dc> centerProvider) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(centerProvider, "centerProvider");
        if (values.size() < 2) return List.copyOf(values);

        List<Candidate<T>> remaining = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            T value = Objects.requireNonNull(values.get(index), "value");
            Vector3dc center = Objects.requireNonNull(centerProvider.apply(value), "center");
            remaining.add(new Candidate<>(value, new Vector3d(center), index));
        }

        Comparator<Candidate<T>> stableSpatialOrder = Comparator
                .comparingDouble((Candidate<T> candidate) -> candidate.center().x)
                .thenComparingDouble(candidate -> candidate.center().y)
                .thenComparingDouble(candidate -> candidate.center().z)
                .thenComparingInt((Candidate<T> candidate) -> candidate.originalIndex());
        List<T> result = new ArrayList<>(values.size());
        Candidate<T> current = remaining.stream().min(stableSpatialOrder).orElseThrow();
        remaining.remove(current);
        result.add(current.value());

        while (!remaining.isEmpty()) {
            Vector3d anchor = current.center();
            current = remaining.stream().min(Comparator
                    .comparingDouble((Candidate<T> candidate) -> candidate.center().distanceSquared(anchor))
                    .thenComparing(stableSpatialOrder)).orElseThrow();
            remaining.remove(current);
            result.add(current.value());
        }
        return List.copyOf(result);
    }

    private record Candidate<T>(T value, Vector3d center, int originalIndex) {
    }
}
