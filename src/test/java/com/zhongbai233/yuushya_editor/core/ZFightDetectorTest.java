package com.zhongbai233.yuushya_editor.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhongbai233.yuushya_editor.core.geometry.ZFightDetector;
import com.zhongbai233.yuushya_editor.core.geometry.ZFightOptimizer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class ZFightDetectorTest {
    @Test
    void identicalCubesReportTheirSixOverlappingFaces() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        List<ZFightDetector.Conflict> conflicts = ZFightDetector.detect(List.of(
                candidate(first, EditorTransform.IDENTITY), candidate(second, EditorTransform.IDENTITY)));
        assertEquals(6, conflicts.size());
    }

    @Test
    void sharedFaceReportsButEdgeOnlyContactDoesNot() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        EditorTransform faceNeighbor = transform(new Vector3d(16.0D, 0.0D, 0.0D));
        assertFalse(ZFightDetector.detect(List.of(candidate(first, EditorTransform.IDENTITY),
                candidate(second, faceNeighbor))).isEmpty());

        EditorTransform edgeNeighbor = transform(new Vector3d(16.0D, 16.0D, 0.0D));
        assertTrue(ZFightDetector.detect(List.of(candidate(first, EditorTransform.IDENTITY),
                candidate(second, edgeNeighbor))).isEmpty());
    }

    @Test
    void tinyOptimizationSeparatesTheReportedPlanes() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        List<ZFightDetector.Candidate> candidates = List.of(candidate(first, EditorTransform.IDENTITY),
                candidate(second, EditorTransform.IDENTITY));
        List<ZFightDetector.Conflict> conflicts = ZFightDetector.detect(candidates);
        Map<UUID, Vector3d> offsets = ZFightOptimizer.worldOffsets(List.of(first, second), conflicts,
                ZFightOptimizer.DEFAULT_WORLD_EPSILON);
        Vector3d rawDelta = ZFightOptimizer.rawPositionDelta(EditorTransform.IDENTITY, offsets.get(second));
        EditorTransform adjusted = EditorTransform.IDENTITY.withPosition(rawDelta);

        assertTrue(ZFightDetector.detect(List.of(candidate(first, EditorTransform.IDENTITY),
                candidate(second, adjusted))).isEmpty());
    }

    @Test
    void disjointPartialBlockShapesAreNotTreatedAsFullCubes() {
        UUID lower = UUID.randomUUID();
        UUID upper = UUID.randomUUID();
        ZFightDetector.Candidate lowerSlab = new ZFightDetector.Candidate(lower, EditorTransform.IDENTITY,
                List.of(new ZFightDetector.Box(0.0D, 0.0D, 0.0D, 1.0D, 0.4D, 1.0D)));
        ZFightDetector.Candidate upperSlab = new ZFightDetector.Candidate(upper, EditorTransform.IDENTITY,
                List.of(new ZFightDetector.Box(0.0D, 0.6D, 0.0D, 1.0D, 1.0D, 1.0D)));

        assertTrue(ZFightDetector.detect(List.of(lowerSlab, upperSlab)).isEmpty());
    }

    @Test
    void itemBlockCandidatesUseTheNativeUncenteredOrigin() {
        UUID centered = UUID.randomUUID();
        UUID itemBlock = UUID.randomUUID();
        ZFightDetector.Candidate normal = new ZFightDetector.Candidate(centered, EditorTransform.IDENTITY);
        ZFightDetector.Candidate nativeItem = new ZFightDetector.Candidate(itemBlock,
                EditorTransform.IDENTITY,
                List.of(new ZFightDetector.Box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D)), false);

        assertTrue(ZFightDetector.detect(List.of(normal, nativeItem)).isEmpty());
        assertEquals(6, ZFightDetector.detect(List.of(nativeItem,
                new ZFightDetector.Candidate(UUID.randomUUID(), EditorTransform.IDENTITY,
                        nativeItem.boxes(), false))).size());
    }

    @Test
    void voxelUnionDoesNotExposeAnInternalStairFace() {
        UUID stair = UUID.randomUUID();
        UUID probe = UUID.randomUUID();
        ZFightDetector.Candidate stairShape = new ZFightDetector.Candidate(stair, EditorTransform.IDENTITY,
                List.of(new ZFightDetector.Box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D),
                        new ZFightDetector.Box(0.0D, 0.5D, 0.5D, 1.0D, 1.0D, 1.0D)));
        // This small box is wholly inside the upper step. Its bottom is coplanar with what would
        // be the lower slab's internal top face if the two shape boxes were checked independently.
        ZFightDetector.Candidate internalProbe = new ZFightDetector.Candidate(probe, EditorTransform.IDENTITY,
                List.of(new ZFightDetector.Box(0.2D, 0.5D, 0.6D, 0.8D, 0.75D, 0.9D)));

        assertTrue(ZFightDetector.detect(List.of(stairShape, internalProbe)).isEmpty());
    }

    @Test
    void optimizationUsesIncreasingOffsetsForThreeCoincidentLayers() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        List<ZFightDetector.Candidate> candidates = List.of(candidate(first, EditorTransform.IDENTITY),
                candidate(second, EditorTransform.IDENTITY), candidate(third, EditorTransform.IDENTITY));
        List<ZFightDetector.Conflict> conflicts = ZFightDetector.detect(candidates);
        Map<UUID, Vector3d> offsets = ZFightOptimizer.worldOffsets(List.of(first, second, third), conflicts,
                ZFightOptimizer.DEFAULT_WORLD_EPSILON);
        EditorTransform adjustedSecond = EditorTransform.IDENTITY.withPosition(
                ZFightOptimizer.rawPositionDelta(EditorTransform.IDENTITY, offsets.get(second)));
        EditorTransform adjustedThird = EditorTransform.IDENTITY.withPosition(
                ZFightOptimizer.rawPositionDelta(EditorTransform.IDENTITY, offsets.get(third)));

        assertTrue(ZFightDetector.detect(List.of(candidate(first, EditorTransform.IDENTITY),
                candidate(second, adjustedSecond), candidate(third, adjustedThird))).isEmpty());
    }

    @Test
    void rawOffsetCompensatesForNonUniformScale() {
        EditorTransform transform = new EditorTransform(new Vector3d(), new Quaternionf(),
                new Vector3f(2.0F, 4.0F, 8.0F));
        Vector3d raw = ZFightOptimizer.rawPositionDelta(transform, new Vector3d(1.0E-7D));
        assertEquals(8.0E-7D, raw.x, 1.0E-15D);
        assertEquals(4.0E-7D, raw.y, 1.0E-15D);
        assertEquals(2.0E-7D, raw.z, 1.0E-15D);
    }

    private static ZFightDetector.Candidate candidate(UUID id, EditorTransform transform) {
        return new ZFightDetector.Candidate(id, transform);
    }

    private static EditorTransform transform(Vector3d position) {
        return new EditorTransform(position, new Quaternionf(), new Vector3f(1.0F));
    }
}
