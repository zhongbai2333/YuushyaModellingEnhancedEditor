package com.zhongbai233.yuushya_editor.core.geometry;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.preview.BlockPreviewTransform;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

/** Detects positive-area overlap between coplanar exterior faces of transformed voxel shapes. */
public final class ZFightDetector {
    private static final double NORMAL_EPSILON = 1.0E-6D;
    private static final double PLANE_EPSILON = 2.0E-7D;
    private static final double OVERLAP_EPSILON = 1.0E-9D;
    private static final Box UNIT_CUBE = new Box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    private ZFightDetector() { }

    public static List<Conflict> detect(List<Candidate> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        List<List<Face>> faces = new ArrayList<>(candidates.size());
        List<Vector3d> centers = new ArrayList<>(candidates.size());
        for (Candidate candidate : candidates) {
            faces.add(faces(candidate));
            Vector3f center = transform(candidate).transformPosition(new Vector3f(0.5F));
            centers.add(new Vector3d(center));
        }
        List<Conflict> conflicts = new ArrayList<>();
        Set<ConflictKey> seen = new LinkedHashSet<>();
        for (int first = 0; first < candidates.size(); first++) {
            for (int second = first + 1; second < candidates.size(); second++) {
                for (Face firstFace : faces.get(first)) {
                    for (Face secondFace : faces.get(second)) {
                        if (!overlaps(firstFace, secondFace)) continue;
                        Vector3d direction = separationForSecond(firstFace.normal(),
                                centers.get(first), centers.get(second));
                        double plane = direction.dot(firstFace.vertices()[0]);
                        ConflictKey key = new ConflictKey(first, second, axisKey(direction),
                                quantizePlane(plane));
                        if (seen.add(key)) {
                            conflicts.add(new Conflict(candidates.get(first).layerId(),
                                    candidates.get(second).layerId(), direction));
                        }
                    }
                }
            }
        }
        return List.copyOf(conflicts);
    }

    private static List<Face> faces(Candidate candidate) {
        List<Box> boxes = candidate.boxes();
        if (boxes.isEmpty()) return List.of();
        double[] xs = boundaries(boxes, Axis.X);
        double[] ys = boundaries(boxes, Axis.Y);
        double[] zs = boundaries(boxes, Axis.Z);
        boolean[][][] occupied = new boolean[xs.length - 1][ys.length - 1][zs.length - 1];
        for (int x = 0; x < xs.length - 1; x++) {
            for (int y = 0; y < ys.length - 1; y++) {
                for (int z = 0; z < zs.length - 1; z++) {
                    double px = (xs[x] + xs[x + 1]) * 0.5D;
                    double py = (ys[y] + ys[y + 1]) * 0.5D;
                    double pz = (zs[z] + zs[z + 1]) * 0.5D;
                    occupied[x][y][z] = boxes.stream().anyMatch(box -> box.contains(px, py, pz));
                }
            }
        }
        Matrix4f matrix = transform(candidate);
        List<Face> result = new ArrayList<>();
        for (int x = 0; x < xs.length - 1; x++) {
            for (int y = 0; y < ys.length - 1; y++) {
                for (int z = 0; z < zs.length - 1; z++) {
                    if (!occupied[x][y][z]) continue;
                    if (x == 0 || !occupied[x - 1][y][z]) addFace(result, matrix,
                            quadX(xs[x], ys[y], ys[y + 1], zs[z], zs[z + 1], false));
                    if (x == xs.length - 2 || !occupied[x + 1][y][z]) addFace(result, matrix,
                            quadX(xs[x + 1], ys[y], ys[y + 1], zs[z], zs[z + 1], true));
                    if (y == 0 || !occupied[x][y - 1][z]) addFace(result, matrix,
                            quadY(ys[y], xs[x], xs[x + 1], zs[z], zs[z + 1], false));
                    if (y == ys.length - 2 || !occupied[x][y + 1][z]) addFace(result, matrix,
                            quadY(ys[y + 1], xs[x], xs[x + 1], zs[z], zs[z + 1], true));
                    if (z == 0 || !occupied[x][y][z - 1]) addFace(result, matrix,
                            quadZ(zs[z], xs[x], xs[x + 1], ys[y], ys[y + 1], false));
                    if (z == zs.length - 2 || !occupied[x][y][z + 1]) addFace(result, matrix,
                            quadZ(zs[z + 1], xs[x], xs[x + 1], ys[y], ys[y + 1], true));
                }
            }
        }
        return result;
    }

    private static void addFace(List<Face> result, Matrix4f matrix, double[][] local) {
        Vector3d[] vertices = new Vector3d[4];
        for (int index = 0; index < vertices.length; index++) {
            Vector3f transformed = matrix.transformPosition(new Vector3f(
                    (float) local[index][0], (float) local[index][1], (float) local[index][2]));
            vertices[index] = new Vector3d(transformed);
        }
        Vector3d normal = new Vector3d(vertices[1]).sub(vertices[0])
                .cross(new Vector3d(vertices[3]).sub(vertices[0]));
        if (normal.lengthSquared() > OVERLAP_EPSILON * OVERLAP_EPSILON) {
            result.add(new Face(vertices, normal.normalize()));
        }
    }

    private static Matrix4f transform(Candidate candidate) {
        return candidate.centerOnPivot()
                ? BlockPreviewTransform.matrix(candidate.transform())
                : BlockPreviewTransform.itemMatrix(candidate.transform());
    }

    private static double[] boundaries(List<Box> boxes, Axis axis) {
        return boxes.stream().flatMapToDouble(box -> java.util.stream.DoubleStream.of(
                        axis.min(box), axis.max(box)))
                .distinct().sorted().toArray();
    }

    private static double[][] quadX(double x, double y0, double y1, double z0, double z1, boolean positive) {
        return positive
                ? new double[][] {{x, y0, z0}, {x, y0, z1}, {x, y1, z1}, {x, y1, z0}}
                : new double[][] {{x, y0, z0}, {x, y1, z0}, {x, y1, z1}, {x, y0, z1}};
    }

    private static double[][] quadY(double y, double x0, double x1, double z0, double z1, boolean positive) {
        return positive
                ? new double[][] {{x0, y, z0}, {x1, y, z0}, {x1, y, z1}, {x0, y, z1}}
                : new double[][] {{x0, y, z0}, {x0, y, z1}, {x1, y, z1}, {x1, y, z0}};
    }

    private static double[][] quadZ(double z, double x0, double x1, double y0, double y1, boolean positive) {
        return positive
                ? new double[][] {{x0, y0, z}, {x0, y1, z}, {x1, y1, z}, {x1, y0, z}}
                : new double[][] {{x0, y0, z}, {x1, y0, z}, {x1, y1, z}, {x0, y1, z}};
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

    private static Vector3d separationForSecond(Vector3d normal, Vector3d firstCenter, Vector3d secondCenter) {
        Vector3d result = canonical(normal);
        Vector3d centers = new Vector3d(secondCenter).sub(firstCenter);
        if (Math.abs(centers.dot(result)) > PLANE_EPSILON && centers.dot(result) < 0.0D) result.negate();
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

    private static String axisKey(Vector3d normal) {
        return Math.round(normal.x * 1.0E6D) + ":" + Math.round(normal.y * 1.0E6D)
                + ":" + Math.round(normal.z * 1.0E6D);
    }

    private static long quantizePlane(double plane) {
        return Math.round(plane / PLANE_EPSILON);
    }

    public record Box(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        public Box {
            if (!finite(minX) || !finite(minY) || !finite(minZ) || !finite(maxX)
                    || !finite(maxY) || !finite(maxZ)
                    || minX >= maxX || minY >= maxY || minZ >= maxZ) {
                throw new IllegalArgumentException("shape box must be finite and have positive volume");
            }
        }

        private boolean contains(double x, double y, double z) {
            return x > minX - OVERLAP_EPSILON && x < maxX + OVERLAP_EPSILON
                    && y > minY - OVERLAP_EPSILON && y < maxY + OVERLAP_EPSILON
                    && z > minZ - OVERLAP_EPSILON && z < maxZ + OVERLAP_EPSILON;
        }

        private static boolean finite(double value) {
            return Double.isFinite(value);
        }
    }

    public record Candidate(UUID layerId, EditorTransform transform, List<Box> boxes,
            boolean centerOnPivot) {
        public Candidate {
            Objects.requireNonNull(layerId, "layerId");
            Objects.requireNonNull(transform, "transform");
            boxes = List.copyOf(Objects.requireNonNull(boxes, "boxes"));
        }

        public Candidate(UUID layerId, EditorTransform transform, List<Box> boxes) {
            this(layerId, transform, boxes, true);
        }

        /** Compatibility/default geometry for tests and callers that explicitly mean a full block. */
        public Candidate(UUID layerId, EditorTransform transform) {
            this(layerId, transform, List.of(UNIT_CUBE), true);
        }
    }

    /** normal is the world-space direction in which the second layer should be separated. */
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

    private enum Axis {
        X { @Override double min(Box box) { return box.minX; } @Override double max(Box box) { return box.maxX; } },
        Y { @Override double min(Box box) { return box.minY; } @Override double max(Box box) { return box.maxY; } },
        Z { @Override double min(Box box) { return box.minZ; } @Override double max(Box box) { return box.maxZ; } };

        abstract double min(Box box);
        abstract double max(Box box);
    }

    private record ConflictKey(int firstCandidate, int secondCandidate, String axis, long plane) { }
    private record Face(Vector3d[] vertices, Vector3d normal) { }
    private record Vec2(double x, double y) { }
}
