package com.zhongbai233.yuushya_editor.client.environment;

import com.zhongbai233.yuushya_editor.core.environment.EnvironmentPreviewPolicy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client-thread capture stage of NCPB's fixed terrain core. Sections are sampled incrementally,
 * masked through the stable 25-block sphere, and published as immutable compiler snapshots.
 */
public final class EnvironmentPreviewManager {
    private static final AtomicLong GENERATIONS = new AtomicLong();
    private static final long CAPTURE_BUDGET_NANOS = 4_000_000L;
    /** Bounds non-preemptible client-thread work; the deadline is checked between these batches. */
    private static final int CAPTURE_CELLS_PER_SLICE = 32;
    private static final int MAX_CAPTURES_PER_TICK = 4;
    private static final int NEIGHBORHOOD_HALO = -EnvironmentNeighborhoodIndex.MIN_LOCAL;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static volatile EnvironmentPreviewManager active;

    private final Map<EnvironmentSectionKey, EnvironmentSectionSnapshot> snapshots = new HashMap<>();
    private final ArrayDeque<EnvironmentSectionKey> pending = new ArrayDeque<>();
    private final Set<EnvironmentSectionKey> queued = new HashSet<>();
    private CaptureJob activeCapture;
    private ClientLevel level;
    private BlockPos origin;
    private List<EnvironmentSectionKey> expected = List.of();
    private Set<EnvironmentSectionKey> expectedSet = Set.of();
    private final Set<EnvironmentSectionKey> initialRemaining = new HashSet<>();
    private long generation;
    private long seed;
    private boolean frameDirty;
    private EnvironmentPreviewFrame cachedFrame = EnvironmentPreviewFrame.empty();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private long captureCount;
    private long captureNanos;
    private long maxCaptureNanos;
    private long maxCaptureSliceNanos;
    private long maxTickCaptureNanos;
    private long dirtySectionsScheduled;
    private long chunkInvalidations;

    public void update(ClientLevel newLevel, BlockPos newOrigin) {
        if (newLevel == null || newOrigin == null) {
            close();
            return;
        }
        if (level == newLevel && newOrigin.equals(origin)) return;
        EnvironmentPreviewManager previous = active;
        if (previous != null && previous != this) previous.close();
        active = this;
        level = newLevel;
        origin = newOrigin.immutable();
        generation = GENERATIONS.incrementAndGet();
        seed = EnvironmentPreviewPolicy.seed(origin.getX(), origin.getY(), origin.getZ());
        snapshots.clear();
        pending.clear();
        queued.clear();
        activeCapture = null;
        expected = expectedSections(origin);
        expectedSet = Set.copyOf(expected);
        initialRemaining.clear();
        initialRemaining.addAll(expected);
        expected.forEach(this::enqueue);
        frameDirty = true;
        cachedFrame = EnvironmentPreviewFrame.empty();
        captureCount = 0L;
        captureNanos = 0L;
        maxCaptureNanos = 0L;
        maxCaptureSliceNanos = 0L;
        maxTickCaptureNanos = 0L;
        dirtySectionsScheduled = 0L;
        chunkInvalidations = 0L;
    }

    public void tick() {
        if (level == null || origin == null) return;
        if (pending.isEmpty() && activeCapture == null) return;
        long tickStarted = System.nanoTime();
        long deadline = tickStarted + CAPTURE_BUDGET_NANOS;
        int captured = 0;
        while (captured < MAX_CAPTURES_PER_TICK) {
            if (activeCapture == null) {
                EnvironmentSectionKey key = pending.pollFirst();
                if (key == null) break;
                queued.remove(key);
                activeCapture = new CaptureJob(key);
            }
            if (activeCapture.captureNextSlice()) {
                publish(activeCapture);
                activeCapture = null;
                captured++;
            }
            if (System.nanoTime() >= deadline) break;
        }
        maxTickCaptureNanos = Math.max(maxTickCaptureNanos, System.nanoTime() - tickStarted);
    }

    public EnvironmentPreviewFrame frame() {
        if (!frameDirty) return cachedFrame;
        List<EnvironmentSectionSnapshot> ordered = new ArrayList<>(snapshots.values());
        ordered.sort(Comparator.comparingInt((EnvironmentSectionSnapshot value) -> value.section().y())
                .thenComparingInt(value -> value.section().z())
                .thenComparingInt(value -> value.section().x()));
        int blocks = ordered.stream().mapToInt(value -> value.blocks().size()).sum();
        cachedFrame = new EnvironmentPreviewFrame(generation,
                origin.getX(), origin.getY(), origin.getZ(), ordered,
                Set.copyOf(expected), blocks, initialRemaining.size());
        frameDirty = false;
        return cachedFrame;
    }

    public void close() {
        if (active == this) active = null;
        level = null;
        origin = null;
        expected = List.of();
        expectedSet = Set.of();
        initialRemaining.clear();
        snapshots.clear();
        pending.clear();
        queued.clear();
        activeCapture = null;
        cachedFrame = EnvironmentPreviewFrame.empty();
        frameDirty = false;
    }

    public PerformanceSnapshot performanceSnapshot() {
        return new PerformanceSnapshot(captureCount, captureNanos, maxCaptureNanos,
                maxCaptureSliceNanos, maxTickCaptureNanos, dirtySectionsScheduled,
                chunkInvalidations, pending.size() + (activeCapture == null ? 0 : 1));
    }

    public static void markBlockDirty(ClientLevel level, BlockPos pos) {
        EnvironmentPreviewManager manager = active;
        if (manager != null && manager.level == level && pos != null) manager.invalidateBlock(pos);
    }

    public static void markChunkLoaded(ClientLevel level, int chunkX, int chunkZ) {
        EnvironmentPreviewManager manager = active;
        if (manager != null && manager.level == level) manager.invalidateChunk(chunkX, chunkZ, true);
    }

    public static void markChunkUnloaded(ClientLevel level, int chunkX, int chunkZ) {
        EnvironmentPreviewManager manager = active;
        if (manager != null && manager.level == level) manager.invalidateChunk(chunkX, chunkZ, false);
    }

    private void enqueue(EnvironmentSectionKey key) {
        if (queued.add(key)) pending.addLast(key);
    }

    private void enqueueFirst(EnvironmentSectionKey key) {
        if (!expectedSet.contains(key)) return;
        if (queued.add(key)) {
            pending.addFirst(key);
        } else {
            pending.remove(key);
            pending.addFirst(key);
        }
    }

    private void invalidateBlock(BlockPos pos) {
        EnvironmentSectionKey center = EnvironmentSectionKey.fromBlock(pos.getX(), pos.getY(), pos.getZ());
        int localX = Math.floorMod(pos.getX(), EnvironmentSectionKey.SIZE);
        int localY = Math.floorMod(pos.getY(), EnvironmentSectionKey.SIZE);
        int localZ = Math.floorMod(pos.getZ(), EnvironmentSectionKey.SIZE);
        int minOffsetX = localX < NEIGHBORHOOD_HALO ? -1 : 0;
        int maxOffsetX = localX >= EnvironmentSectionKey.SIZE - NEIGHBORHOOD_HALO ? 1 : 0;
        int minOffsetY = localY < NEIGHBORHOOD_HALO ? -1 : 0;
        int maxOffsetY = localY >= EnvironmentSectionKey.SIZE - NEIGHBORHOOD_HALO ? 1 : 0;
        int minOffsetZ = localZ < NEIGHBORHOOD_HALO ? -1 : 0;
        int maxOffsetZ = localZ >= EnvironmentSectionKey.SIZE - NEIGHBORHOOD_HALO ? 1 : 0;
        for (int y = minOffsetY; y <= maxOffsetY; y++) {
            for (int z = minOffsetZ; z <= maxOffsetZ; z++) {
                for (int x = minOffsetX; x <= maxOffsetX; x++) {
                    EnvironmentSectionKey key = new EnvironmentSectionKey(
                            center.x() + x, center.y() + y, center.z() + z);
                    if (expectedSet.contains(key)) {
                        enqueueFirst(key);
                        dirtySectionsScheduled++;
                    }
                }
            }
        }
    }

    private void invalidateChunk(int chunkX, int chunkZ, boolean loaded) {
        boolean removed = false;
        for (EnvironmentSectionKey key : expected) {
            if (Math.abs(key.x() - chunkX) > 1 || Math.abs(key.z() - chunkZ) > 1) continue;
            if (!loaded && key.x() == chunkX && key.z() == chunkZ) {
                removed |= snapshots.remove(key) != null;
            }
            enqueueFirst(key);
            chunkInvalidations++;
        }
        frameDirty |= removed;
    }

    private void publish(CaptureJob completed) {
        EnvironmentSectionSnapshot refreshed = completed.snapshot();
        captureCount++;
        captureNanos += completed.workNanos;
        maxCaptureNanos = Math.max(maxCaptureNanos, completed.workNanos);
        EnvironmentSectionSnapshot previous = snapshots.get(completed.key);
        if (previous == null || !previous.sameContent(refreshed)) {
            snapshots.put(completed.key, refreshed);
            frameDirty = true;
        }
        if (initialRemaining.remove(completed.key)) {
            frameDirty = true;
        }
    }

    private final class CaptureJob {
        private final EnvironmentSectionKey key;
        private final List<BlockState> states = new ArrayList<>(EnvironmentNeighborhoodIndex.CELL_COUNT);
        private final byte[] light = new byte[EnvironmentNeighborhoodIndex.CELL_COUNT];
        private final List<EnvironmentSectionSnapshot.VisibleBlock> visible = new ArrayList<>();
        private final boolean[] loadedChunks;
        private int index;
        private long fingerprint = 0xCBF29CE484222325L;
        private long workNanos;

        private CaptureJob(EnvironmentSectionKey key) {
            this.key = key;
            loadedChunks = loadedChunks(key);
        }

        private boolean captureNextSlice() {
            long sliceStarted = System.nanoTime();
            int batchEnd = Math.min(index + CAPTURE_CELLS_PER_SLICE,
                    EnvironmentNeighborhoodIndex.CELL_COUNT);
            while (index < batchEnd) {
                int flatIndex = index;
                int planeSize = EnvironmentNeighborhoodIndex.SIZE * EnvironmentNeighborhoodIndex.SIZE;
                int localY = flatIndex / planeSize + EnvironmentNeighborhoodIndex.MIN_LOCAL;
                int inPlane = flatIndex % planeSize;
                int localZ = inPlane / EnvironmentNeighborhoodIndex.SIZE
                        + EnvironmentNeighborhoodIndex.MIN_LOCAL;
                int localX = inPlane % EnvironmentNeighborhoodIndex.SIZE
                        + EnvironmentNeighborhoodIndex.MIN_LOCAL;
                captureCell(localX, localY, localZ);
            }
            long elapsed = System.nanoTime() - sliceStarted;
            workNanos += elapsed;
            maxCaptureSliceNanos = Math.max(maxCaptureSliceNanos, elapsed);
            return index >= EnvironmentNeighborhoodIndex.CELL_COUNT;
        }

        private void captureCell(int localX, int localY, int localZ) {
            int worldX = key.minBlockX() + localX;
            int worldY = key.minBlockY() + localY;
            int worldZ = key.minBlockZ() + localZ;
            boolean validY = worldY >= level.getMinY() && worldY < level.getMaxY();
            int chunkOffsetX = Math.floorDiv(localX, EnvironmentSectionKey.SIZE);
            int chunkOffsetZ = Math.floorDiv(localZ, EnvironmentSectionKey.SIZE);
            boolean loaded = chunkOffsetX >= -1 && chunkOffsetX <= 1
                    && chunkOffsetZ >= -1 && chunkOffsetZ <= 1
                    && loadedChunks[(chunkOffsetZ + 1) * 3 + chunkOffsetX + 1];
            boolean retained = validY && loaded
                    && !(worldX == origin.getX() && worldY == origin.getY() && worldZ == origin.getZ())
                    && EnvironmentPreviewPolicy.rendersBlock(seed,
                            origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D,
                            worldX, worldY, worldZ);
            BlockState state = retained ? safeState(worldX, worldY, worldZ) : AIR;
            if (state == null) state = AIR;
            states.add(state);
            byte packedLight = state.isAir() ? 0 : safeLight(worldX, worldY, worldZ);
            light[index++] = packedLight;
            fingerprint = mixFingerprint(fingerprint, state.hashCode());
            fingerprint = mixFingerprint(fingerprint, packedLight & 0xFF);

            if (localX >= 0 && localX < EnvironmentSectionKey.SIZE
                    && localY >= 0 && localY < EnvironmentSectionKey.SIZE
                    && localZ >= 0 && localZ < EnvironmentSectionKey.SIZE
                    && isRenderable(state)) {
                cursor.set(worldX, worldY, worldZ);
                EnvironmentTintColors tint = new EnvironmentTintColors(
                        safeTint(BiomeColors.GRASS_COLOR_RESOLVER),
                        safeTint(BiomeColors.FOLIAGE_COLOR_RESOLVER),
                        safeTint(BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER),
                        safeTint(BiomeColors.WATER_COLOR_RESOLVER));
                visible.add(new EnvironmentSectionSnapshot.VisibleBlock(localX, localY, localZ, state, tint));
                fingerprint = mixFingerprint(fingerprint, tint.hashCode());
            }
        }

        private EnvironmentSectionSnapshot snapshot() {
            if (index < EnvironmentNeighborhoodIndex.CELL_COUNT) {
                throw new IllegalStateException("Environment section capture is incomplete: " + key);
            }
            return new EnvironmentSectionSnapshot(key, visible, states, light, fingerprint);
        }
    }

    private boolean[] loadedChunks(EnvironmentSectionKey key) {
        boolean[] loaded = new boolean[9];
        for (int z = -1; z <= 1; z++) {
            for (int x = -1; x <= 1; x++) {
                loaded[(z + 1) * 3 + x + 1] = level.hasChunk(key.x() + x, key.z() + z);
            }
        }
        return loaded;
    }

    private BlockState safeState(int x, int y, int z) {
        try {
            cursor.set(x, y, z);
            return level.getBlockState(cursor);
        } catch (Throwable failure) {
            if (failure instanceof VirtualMachineError fatal) throw fatal;
            return AIR;
        }
    }

    private byte safeLight(int x, int y, int z) {
        try {
            cursor.set(x, y, z);
            return EnvironmentPackedLight.pack(level.getBrightness(LightLayer.BLOCK, cursor),
                    level.getBrightness(LightLayer.SKY, cursor));
        } catch (Throwable failure) {
            if (failure instanceof VirtualMachineError fatal) throw fatal;
            return 0;
        }
    }

    private int safeTint(ColorResolver resolver) {
        try {
            return level.getBlockTint(cursor, resolver);
        } catch (Throwable failure) {
            if (failure instanceof VirtualMachineError fatal) throw fatal;
            return -1;
        }
    }

    private static boolean isRenderable(BlockState state) {
        if (state.isAir()) return false;
        try {
            return state.getRenderShape() == RenderShape.MODEL || !state.getFluidState().isEmpty();
        } catch (Throwable failure) {
            if (failure instanceof VirtualMachineError fatal) throw fatal;
            return false;
        }
    }

    private static List<EnvironmentSectionKey> expectedSections(BlockPos origin) {
        int minSectionX = Math.floorDiv(origin.getX() - 12, EnvironmentSectionKey.SIZE);
        int maxSectionX = Math.floorDiv(origin.getX() + 12, EnvironmentSectionKey.SIZE);
        int minSectionY = Math.floorDiv(origin.getY() - 12, EnvironmentSectionKey.SIZE);
        int maxSectionY = Math.floorDiv(origin.getY() + 12, EnvironmentSectionKey.SIZE);
        int minSectionZ = Math.floorDiv(origin.getZ() - 12, EnvironmentSectionKey.SIZE);
        int maxSectionZ = Math.floorDiv(origin.getZ() + 12, EnvironmentSectionKey.SIZE);
        List<EnvironmentSectionKey> keys = new ArrayList<>();
        double centerX = origin.getX() + 0.5D;
        double centerY = origin.getY() + 0.5D;
        double centerZ = origin.getZ() + 0.5D;
        for (int y = minSectionY; y <= maxSectionY; y++) {
            for (int z = minSectionZ; z <= maxSectionZ; z++) {
                for (int x = minSectionX; x <= maxSectionX; x++) {
                    EnvironmentSectionKey key = new EnvironmentSectionKey(x, y, z);
                    if (EnvironmentPreviewPolicy.sectionMayContainDetail(centerX, centerY, centerZ,
                            key.minBlockX(), key.minBlockY(), key.minBlockZ())) keys.add(key);
                }
            }
        }
        keys.sort(Comparator.comparingDouble(key -> sectionDistanceSquared(key, centerX, centerY, centerZ)));
        return List.copyOf(keys);
    }

    private static double sectionDistanceSquared(EnvironmentSectionKey key,
            double centerX, double centerY, double centerZ) {
        double dx = key.minBlockX() + 8.0D - centerX;
        double dy = key.minBlockY() + 8.0D - centerY;
        double dz = key.minBlockZ() + 8.0D - centerZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private static long mixFingerprint(long value, long input) {
        value ^= input;
        value *= 0x100000001B3L;
        return value;
    }

    public record PerformanceSnapshot(long capturedSections, long captureNanos,
            long maxCaptureNanos, long maxCaptureSliceNanos, long maxTickCaptureNanos,
            long dirtySectionsScheduled, long chunkInvalidations, int pendingSections) { }
}
