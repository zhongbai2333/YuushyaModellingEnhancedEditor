package com.zhongbai233.yuushya_editor.core.geometry;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.joml.Quaternionf;
import org.joml.Vector3d;

/** Detects positive-area overlap between coplanar faces of transformed unit cubes. */
public final class ZFightDetector {
    private static final double NORMAL_EPSILON = 1.0E-7D;
    private static final double PLANE_EPSILON = 1.0E-8D;
    private static final double OVERLAP_EPSILON = 1.0E-10D;
    private static final double MODEL_UNITS_PER_WORLD_UNIT = 16.0D;
    private static final double[][][] LOCAL_FACES = {
        {{-.5, -.5, -.5}, {-.5, .5, -.5}, {-.5, .5, .5}, {-.5, -.5, .5}},
        {{.5, -.5, -.5}, {.5, -.5, .5}, {.5, .5, .5}, {.5, .5, -.5}},
        {{-.5, -.5, -.5}, {-.5, -.5, .5}, {.5, -.5, .5}, {.5, -.5, -.5}},
        {{-.5, .5, -.5}, {.5, .5, -.5}, {.5, .5, .5}, {-.5, .5, .5}},
        {{-.5, -.5, -.5}, {.5, -.5, -.5}, {.5, .5, -.5}, {-.5, .5, -.5}},
        {{-.5, -.5, .5}, {-.5, .5, .5}, {.5, .5, .5}, {.5, -.5, .5}}
    };

    private ZFightDetector() { }

    public static List<Conflict> detect(List<Candidate> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        List<List<Face>> faces = new ArrayList<>(candidates.size());
        for (Candidate candidate : candidates) faces.add(faces(candidate.transform()));
        List<Conflict> conflicts = new ArrayList<>();
        for (int first = 0; first < candidates.size(); first++) {
            for (int second = first + 1; second < candidates.size(); second++) {
                for (Face firstFace : faces.get(first)) {
                    for (Face secondFace : faces.get(second)) {
                        if (overlaps(firstFace, secondFace)) {
                            conflicts.add(new Conflict(candidates.get(first).layerId(),
                                    candidates.get(second).layerId(), canonical(firstFace.normal())));
                        }
                    }
                }
            }
        }
        return List.copyOf(conflicts);
    }

    private static List<Face> faces(EditorTransform transform) {
        Quaternionf rotation = transform.rotation();
        Vector3d position = transform.position().div(MODEL_UNITS_PER_WORLD_UNIT);
        org.joml.Vector3f scale = transform.scale();
        List<Face> result = new ArrayList<>(6);
        for (double[][] localFace : LOCAL_FACES) {
            Vector3d[] vertices = new Vector3d[4];
            for (int index = 0; index < vertices.length; index++) {
                Vector3d vertex = new Vector3d(localFace[index][0], localFace[index][1], localFace[index][2]);
                rotation.transform(vertex);
                vertex.add(position).mul(scale.x, scale.y, scale.z);
                vertices[index] = vertex;
            }
            Vector3d normal = new Vector3d(vertices[1]).sub(vertices[0])
                    .cross(new Vector3d(vertices[3]).sub(vertices[0])).normalize();
            result.add(new Face(vertices, normal));
        }
        return result;
    }

    private static boolean overlaps(Face first, Face second) {
        double normalDot = Math.abs(first.normal().dot(second.normal()));
        if (normalDot < 1.0D - NORMAL_EPSILON) return false;
        double plane = first.normal().dot(first.vertices()[0]);
        for (Vector3d point : second.vertices()) {
            if (Math.abs(first.normal().dot(point) - plane) > PLANE_EPSILON) return false;
        }
        int droppedAxis = dominantAxis(first.normal());
        Vec2[] a = project(first.vertices(), droppedAxis);
        Vec2[] b = project(second.vertices(), droppedAxis);
        return hasPositiveOverlap(a, b) && hasPositiveOverlap(b, a);
    }

    private static boolean hasPositiveOverlap(Vec2[] source, Vec2[] other) {
        for (int index = 0; index < source.length; index++) {
            Vec2 start = source[index];
            Vec2 end = source[(index + 1) % source.length];
            double axisX = -(end.y() - start.y());
            double axisY = end.x() - start.x();
            double length = Math.hypot(axisX, axisY);
            if (length <= OVERLAP_EPSILON) continue;
            axisX /= length;
            axisY /= length;
            double[] firstInterval = interval(source, axisX, axisY);
            double[] secondInterval = interval(other, axisX, axisY);
            double overlap = Math.min(firstInterval[1], secondInterval[1])
                    - Math.max(firstInterval[0], secondInterval[0]);
            if (overlap <= OVERLAP_EPSILON) return false;
        }
        return true;
    }

    private static double[] interval(Vec2[] vertices, double axisX, double axisY) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Vec2 vertex : vertices) {
            double value = vertex.x() * axisX + vertex.y() * axisY;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return new double[] {min, max};
    }

    private static Vec2[] project(Vector3d[] vertices, int droppedAxis) {
        Vec2[] result = new Vec2[vertices.length];
        for (int index = 0; index < vertices.length; index++) {
            Vector3d value = vertices[index];
            result[index] = switch (droppedAxis) {
                case 0 -> new Vec2(value.y, value.z);
                case 1 -> new Vec2(value.x, value.z);
                default -> new Vec2(value.x, value.y);
            };
        }
        return result;
    }

    private static int dominantAxis(Vector3d normal) {
        double x = Math.abs(normal.x);
        double y = Math.abs(normal.y);
        double z = Math.abs(normal.z);
        return x >= y && x >= z ? 0 : y >= z ? 1 : 2;
    }

    private static Vector3d canonical(Vector3d normal) {
        Vector3d result = new Vector3d(normal).normalize();
        int dominant = dominantAxis(result);
        double component = dominant == 0 ? result.x : dominant == 1 ? result.y : result.z;
        return component < 0.0D ? result.negate() : result;
    }

    public record Candidate(UUID layerId, EditorTransform transform) {
        public Candidate {
            Objects.requireNonNull(layerId, "layerId");
            Objects.requireNonNull(transform, "transform");
        }
    }

    public record Conflict(UUID firstLayerId, UUID secondLayerId, Vector3d normal) {
        public Conflict {
            Objects.requireNonNull(firstLayerId, "firstLayerId");
            Objects.requireNonNull(secondLayerId, "secondLayerId");
            normal = new Vector3d(Objects.requireNonNull(normal, "normal"));
        }

        @Override
        public Vector3d normal() {
            return new Vector3d(normal);
        }
    }

    private record Face(Vector3d[] vertices, Vector3d normal) { }
    private record Vec2(double x, double y) { }
}
