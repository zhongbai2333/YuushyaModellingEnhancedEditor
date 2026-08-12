package com.zhongbai233.yuushya_editor.client.renderer;

import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.TlsfAllocator;
import com.mojang.blaze3d.vertex.UberGpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentPreviewFrame;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentSectionCompiler;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentSectionKey;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentSectionSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** NCPB-derived persistent section mesh cache for the editor's 25-block environment sphere. */
final class EnvironmentPreviewGpuCache implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentPreviewGpuCache.class);
    private static final Identifier BLOCK_ATLAS =
            Identifier.withDefaultNamespace("textures/atlas/blocks.png");
    private static final List<String> CHUNK_SECTION_UNIFORM = List.of("ChunkSection");

    private final Map<EnvironmentSectionKey, GpuSection> sections = new HashMap<>();
    private final Map<EnvironmentSectionKey, EnvironmentSectionSnapshot> failedSources = new HashMap<>();
    private final ConcurrentLinkedQueue<CompilationOutcome> completed = new ConcurrentLinkedQueue<>();
    private final ExecutorService compiler = Executors.newSingleThreadExecutor(compilerThreadFactory());
    private CompilationRequest activeCompilation;
    private long compilationEpoch;
    private long generation;
    private BlockStateModelSet modelSet;
    private SharedBuffers sharedBuffers;
    private long residentRevision;
    private RenderPlan cachedRenderPlan;
    private UniformPlan cachedUniformPlan;
    private boolean closed;
    private boolean disabledForSession;
    private boolean failureLogged;
    private static long compiledSections;
    private static long renderedSections;
    private static long frustumCulledSections;

    /** True means the current PIP texture can be reused while no completed mesh is waiting to be uploaded. */
    boolean textureIsCurrent(EnvironmentPreviewFrame frame) {
        if (frame == null || frame.generation() == 0L) return sections.isEmpty();
        BlockStateModelSet current = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        if (disabledForSession || frame.generation() != generation || current != modelSet) return false;
        if (!completed.isEmpty()) return false;
        // A worker may continue while the last complete texture is reused. Its queue becoming non-empty
        // invalidates the next frame without forcing busy redraws during tessellation.
        if (activeCompilation != null) return true;
        for (EnvironmentSectionSnapshot source : frame.sections()) {
            GpuSection resident = sections.get(source.section());
            if ((resident == null || resident.source != source)
                    && failedSources.get(source.section()) != source) return false;
        }
        return true;
    }

    void updateAndRender(EnvironmentPreviewFrame frame, Matrix4fc modelView, Matrix4fc viewProjection) {
        if (frame == null || frame.generation() == 0L) {
            releaseSession();
            return;
        }
        if (disabledForSession) return;
        try {
            updateAndRenderInternal(frame, modelView, viewProjection);
        } catch (Throwable failure) {
            if (failure instanceof VirtualMachineError fatal) throw fatal;
            if (failure instanceof Error fatal) throw fatal;
            disableForSession(failure);
        }
    }

    private void updateAndRenderInternal(EnvironmentPreviewFrame frame,
            Matrix4fc modelView, Matrix4fc viewProjection) {
        BlockStateModelSet currentModels = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        if (frame.generation() != generation || currentModels != modelSet) {
            clear();
            generation = frame.generation();
            modelSet = currentModels;
        }
        if (sharedBuffers == null) sharedBuffers = new SharedBuffers();
        removeStale(frame);
        consumeCompleted(frame);
        scheduleCompilation(frame, modelView, viewProjection);
        render(frame, modelView, viewProjection);
    }

    private void removeStale(EnvironmentPreviewFrame frame) {
        boolean removed = sections.entrySet().removeIf(entry -> {
            if (frame.expectedSections().contains(entry.getKey())) return false;
            entry.getValue().close();
            failedSources.remove(entry.getKey());
            return true;
        });
        if (removed) invalidatePlans();
    }

    private void scheduleCompilation(EnvironmentPreviewFrame frame,
            Matrix4fc modelView, Matrix4fc viewProjection) {
        if (activeCompilation != null || closed) return;
        EnvironmentSectionSnapshot best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        EnvironmentSectionSnapshot outside = null;
        double outsideDistance = Double.POSITIVE_INFINITY;
        for (EnvironmentSectionSnapshot source : frame.sections()) {
            GpuSection resident = sections.get(source.section());
            if (resident != null && resident.source == source) continue;
            if (failedSources.get(source.section()) == source) continue;
            double distance = viewDistanceSquared(modelView, frame, source.section());
            if (intersectsFrustum(frame, viewProjection, source.section())) {
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = source;
                }
            } else if (distance < outsideDistance) {
                outsideDistance = distance;
                outside = source;
            }
        }
        if (best == null) best = outside;
        if (best != null) submitCompilation(frame, best);
    }

    private void submitCompilation(EnvironmentPreviewFrame frame, EnvironmentSectionSnapshot source) {
        Minecraft minecraft = Minecraft.getInstance();
        CompilationRequest request = new CompilationRequest(compilationEpoch, frame, source,
                modelSet, minecraft.getModelManager().getFluidStateModelSet(), minecraft.getBlockColors());
        activeCompilation = request;
        compiler.execute(() -> {
            EnvironmentSectionCompiler.CompiledSection result = null;
            Throwable failure = null;
            try {
                result = EnvironmentSectionCompiler.compile(request.frame, request.source,
                        request.models, request.fluidModels, request.blockColors);
            } catch (Throwable caught) {
                failure = caught;
            }
            if (closed) {
                if (result != null) result.close();
                return;
            }
            CompilationOutcome outcome = new CompilationOutcome(request, result, failure);
            completed.add(outcome);
            if (closed && completed.remove(outcome)) outcome.closeCompiled();
        });
    }

    private void consumeCompleted(EnvironmentPreviewFrame frame) {
        CompilationOutcome outcome = completed.poll();
        if (outcome == null) return;
        if (activeCompilation == outcome.request) activeCompilation = null;
        boolean current = outcome.request.epoch == compilationEpoch
                && frame.generation() == outcome.request.frame.generation()
                && frame.sections().stream().anyMatch(value -> value == outcome.request.source);
        if (!current) {
            outcome.closeCompiled();
            return;
        }
        if (outcome.failure != null) {
            if (outcome.failure instanceof VirtualMachineError fatal) throw fatal;
            failedSources.put(outcome.request.source.section(), outcome.request.source);
            LOGGER.warn("Unable to compile environment preview section {}; retaining the old mesh",
                    outcome.request.source.section(), outcome.failure);
            return;
        }
        try (EnvironmentSectionCompiler.CompiledSection compiled = outcome.compiled) {
            GpuSection replacement = GpuSection.upload(compiled, sharedBuffers);
            GpuSection old = sections.put(outcome.request.source.section(), replacement);
            if (old != null) old.close();
            failedSources.remove(outcome.request.source.section());
            compiledSections++;
            invalidatePlans();
        }
    }

    private void render(EnvironmentPreviewFrame frame, Matrix4fc modelView, Matrix4fc viewProjection) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractTexture atlas = minecraft.getTextureManager().getTexture(BLOCK_ATLAS);
        Vec3 camera = minecraft.gameRenderer.getMainCamera().position();
        EnvironmentPreviewCoordinateTransform.Frame transform =
                EnvironmentPreviewCoordinateTransform.create(modelView, camera.x, camera.y, camera.z);
        RenderPlan plan = renderPlan(frame, modelView, viewProjection);
        renderedSections += plan.visible.size();
        GpuBufferSlice[] chunkInfos = chunkInfos(frame, transform,
                atlas.getTexture().getWidth(0), atlas.getTexture().getHeight(0), plan);
        drawLayers(minecraft, atlas, plan, chunkInfos);
    }

    private RenderPlan renderPlan(EnvironmentPreviewFrame frame,
            Matrix4fc modelView, Matrix4fc viewProjection) {
        RenderPlan cached = cachedRenderPlan;
        if (cached != null && cached.residentRevision == residentRevision && cached.frame == frame
                && cached.modelView.equals(modelView) && cached.viewProjection.equals(viewProjection)) {
            return cached;
        }
        List<GpuSection> visible = new ArrayList<>();
        for (GpuSection section : sections.values()) {
            if (intersectsFrustum(frame, viewProjection, section.key)) visible.add(section);
            else frustumCulledSections++;
        }
        visible.sort(Comparator.comparingDouble(value -> viewDepth(modelView, frame, value.key)));
        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> draws =
                new EnumMap<>(ChunkSectionLayer.class);
        EnumMap<ChunkSectionLayer, Integer> sequentialCounts = new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            List<RenderPass.Draw<GpuBufferSlice[]>> layerDraws = new ArrayList<>();
            int largestSequential = 0;
            for (int i = 0; i < visible.size(); i++) {
                GpuLayer mesh = visible.get(i).layers.get(layer);
                if (mesh == null) continue;
                LayerBufferSlice slice = sharedBuffers.slice(layer, mesh);
                if (slice == null) continue;
                VertexFormat format = layer.pipeline().getVertexFormat();
                int baseVertex = Math.toIntExact(slice.vertexOffset / format.getVertexSize());
                int firstIndex = mesh.customIndices
                        ? Math.toIntExact(slice.indexOffset / mesh.indexType.bytes) : 0;
                if (!mesh.customIndices) largestSequential = Math.max(largestSequential, mesh.indexCount);
                int uniformIndex = i;
                layerDraws.add(new RenderPass.Draw<>(0, slice.vertexBuffer, slice.indexBuffer,
                        mesh.customIndices ? mesh.indexType : null, firstIndex, mesh.indexCount,
                        baseVertex, (ubos, uploader) -> uploader.upload("ChunkSection", ubos[uniformIndex])));
            }
            draws.put(layer, List.copyOf(layerDraws));
            sequentialCounts.put(layer, largestSequential);
        }
        cached = new RenderPlan(frame, residentRevision, new Matrix4f(modelView),
                new Matrix4f(viewProjection), List.copyOf(visible), draws, sequentialCounts);
        cachedRenderPlan = cached;
        return cached;
    }

    private GpuBufferSlice[] chunkInfos(EnvironmentPreviewFrame frame,
            EnvironmentPreviewCoordinateTransform.Frame transform,
            int atlasWidth, int atlasHeight, RenderPlan plan) {
        if (plan.visible.isEmpty()) return new GpuBufferSlice[0];
        UniformPlan uniforms = cachedUniformPlan;
        if (uniforms == null || uniforms.renderPlan != plan || uniforms.atlasWidth != atlasWidth
                || uniforms.atlasHeight != atlasHeight
                || uniforms.cameraBlockX != transform.cameraBlockX()
                || uniforms.cameraBlockY != transform.cameraBlockY()
                || uniforms.cameraBlockZ != transform.cameraBlockZ()
                || !uniforms.modelView.equals(transform.modelView())) {
            DynamicUniforms.ChunkSectionInfo[] infos =
                    new DynamicUniforms.ChunkSectionInfo[plan.visible.size()];
            for (int i = 0; i < plan.visible.size(); i++) {
                EnvironmentSectionKey key = plan.visible.get(i).key;
                infos[i] = new DynamicUniforms.ChunkSectionInfo(transform.modelView(),
                        transform.encodedSectionX(key.minBlockX() - frame.originX()),
                        transform.encodedSectionY(key.minBlockY() - frame.originY()),
                        transform.encodedSectionZ(key.minBlockZ() - frame.originZ()),
                        1.0F, atlasWidth, atlasHeight);
            }
            uniforms = new UniformPlan(plan, new Matrix4f(transform.modelView()),
                    transform.cameraBlockX(), transform.cameraBlockY(), transform.cameraBlockZ(),
                    atlasWidth, atlasHeight, infos);
            cachedUniformPlan = uniforms;
        }
        return RenderSystem.getDynamicUniforms().writeChunkSections(uniforms.infos);
    }

    private static void drawLayers(Minecraft minecraft, AbstractTexture atlas,
            RenderPlan plan, GpuBufferSlice[] chunkInfos) {
        var color = RenderSystem.outputColorTextureOverride;
        var depth = RenderSystem.outputDepthTextureOverride;
        if (color == null || depth == null) return;
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Yuushya editor persistent environment sections", color, OptionalInt.empty(),
                depth, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture("Sampler0", atlas.getTextureView(), atlas.getSampler());
            pass.bindTexture("Sampler2", minecraft.gameRenderer.lightmap(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                List<RenderPass.Draw<GpuBufferSlice[]>> draws = plan.draws.get(layer);
                if (draws.isEmpty()) continue;
                int count = plan.largestSequentialIndices.get(layer);
                RenderSystem.AutoStorageIndexBuffer sequential =
                        RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                GpuBuffer defaultIndices = count == 0 ? null : sequential.getBuffer(count);
                VertexFormat.IndexType defaultType = count == 0 ? null : sequential.type();
                pass.setPipeline(layer.pipeline());
                pass.drawMultipleIndexed(draws, defaultIndices, defaultType,
                        CHUNK_SECTION_UNIFORM, chunkInfos);
            }
        }
    }

    private static boolean intersectsFrustum(EnvironmentPreviewFrame frame,
            Matrix4fc matrix, EnvironmentSectionKey key) {
        float minX = key.minBlockX() - frame.originX() - 0.5F;
        float minY = key.minBlockY() - frame.originY() - 0.5F;
        float minZ = key.minBlockZ() - frame.originZ() - 0.5F;
        return intersectsClip(matrix, minX, minY, minZ,
                minX + EnvironmentSectionKey.SIZE,
                minY + EnvironmentSectionKey.SIZE,
                minZ + EnvironmentSectionKey.SIZE);
    }

    private static boolean intersectsClip(Matrix4fc matrix,
            float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        int outsidePlanes = 0x3F;
        for (int corner = 0; corner < 8 && outsidePlanes != 0; corner++) {
            float x = (corner & 4) == 0 ? minX : maxX;
            float y = (corner & 2) == 0 ? minY : maxY;
            float z = (corner & 1) == 0 ? minZ : maxZ;
            float clipX = matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30();
            float clipY = matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31();
            float clipZ = matrix.m02() * x + matrix.m12() * y + matrix.m22() * z + matrix.m32();
            float clipW = matrix.m03() * x + matrix.m13() * y + matrix.m23() * z + matrix.m33();
            int outside = 0;
            if (clipX < -clipW) outside |= 1;
            if (clipX > clipW) outside |= 2;
            if (clipY < -clipW) outside |= 4;
            if (clipY > clipW) outside |= 8;
            if (clipZ < -clipW) outside |= 16;
            if (clipZ > clipW) outside |= 32;
            outsidePlanes &= outside;
        }
        return outsidePlanes == 0;
    }

    private static double viewDistanceSquared(Matrix4fc view,
            EnvironmentPreviewFrame frame, EnvironmentSectionKey key) {
        float x = key.minBlockX() - frame.originX() + 8.0F;
        float y = key.minBlockY() - frame.originY() + 8.0F;
        float z = key.minBlockZ() - frame.originZ() + 8.0F;
        double vx = view.m00() * x + view.m10() * y + view.m20() * z + view.m30();
        double vy = view.m01() * x + view.m11() * y + view.m21() * z + view.m31();
        double vz = view.m02() * x + view.m12() * y + view.m22() * z + view.m32();
        return vx * vx + vy * vy + vz * vz;
    }

    private static double viewDepth(Matrix4fc view,
            EnvironmentPreviewFrame frame, EnvironmentSectionKey key) {
        float x = key.minBlockX() - frame.originX() + 8.0F;
        float y = key.minBlockY() - frame.originY() + 8.0F;
        float z = key.minBlockZ() - frame.originZ() + 8.0F;
        return view.m02() * x + view.m12() * y + view.m22() * z + view.m32();
    }

    private void invalidatePlans() {
        residentRevision++;
        cachedRenderPlan = null;
        cachedUniformPlan = null;
    }

    private void disableForSession(Throwable failure) {
        disabledForSession = true;
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.warn("Environment GPU cache failed; disabling it for the current editor session", failure);
        }
        clear();
        if (sharedBuffers != null) {
            sharedBuffers.close();
            sharedBuffers = null;
        }
    }

    static void resetCounters() {
        compiledSections = 0L;
        renderedSections = 0L;
        frustumCulledSections = 0L;
    }

    static Performance performance() {
        return new Performance(compiledSections, renderedSections, frustumCulledSections);
    }

    void clear() {
        compilationEpoch++;
        activeCompilation = null;
        sections.values().forEach(GpuSection::close);
        sections.clear();
        failedSources.clear();
        generation = 0L;
        modelSet = null;
        invalidatePlans();
        CompilationOutcome outcome;
        while ((outcome = completed.poll()) != null) outcome.closeCompiled();
    }

    void releaseSession() {
        if (sharedBuffers == null && sections.isEmpty() && activeCompilation == null
                && completed.isEmpty()) return;
        clear();
        if (sharedBuffers != null) {
            sharedBuffers.close();
            sharedBuffers = null;
        }
        disabledForSession = false;
        failureLogged = false;
    }

    @Override public void close() {
        closed = true;
        releaseSession();
        compiler.shutdownNow();
    }

    private static ThreadFactory compilerThreadFactory() {
        return task -> {
            Thread thread = new Thread(task, "Yuushya editor environment compiler");
            thread.setDaemon(true);
            thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
            return thread;
        };
    }

    record Performance(long compiledSections, long renderedSections,
            long frustumCulledSections) { }

    private record CompilationRequest(long epoch, EnvironmentPreviewFrame frame,
            EnvironmentSectionSnapshot source, BlockStateModelSet models,
            FluidStateModelSet fluidModels, BlockColors blockColors) { }

    private record CompilationOutcome(CompilationRequest request,
            EnvironmentSectionCompiler.CompiledSection compiled, Throwable failure) {
        private CompilationOutcome {
            if ((compiled == null) == (failure == null)) {
                throw new IllegalArgumentException("compilation outcome must contain exactly one result");
            }
        }
        private void closeCompiled() { if (compiled != null) compiled.close(); }
    }

    private static final class GpuSection implements AutoCloseable {
        private final EnvironmentSectionKey key;
        private final EnvironmentSectionSnapshot source;
        private final Map<ChunkSectionLayer, GpuLayer> layers;

        private GpuSection(EnvironmentSectionKey key, EnvironmentSectionSnapshot source,
                Map<ChunkSectionLayer, GpuLayer> layers) {
            this.key = key;
            this.source = source;
            this.layers = layers;
        }

        private static GpuSection upload(EnvironmentSectionCompiler.CompiledSection compiled,
                SharedBuffers buffers) {
            Map<ChunkSectionLayer, GpuLayer> layers = new EnumMap<>(ChunkSectionLayer.class);
            try {
                for (Map.Entry<ChunkSectionLayer, MeshData> entry : compiled.layers().entrySet()) {
                    GpuLayer layer = GpuLayer.create(entry.getValue());
                    buffers.upload(entry.getKey(), layer, entry.getValue());
                    layers.put(entry.getKey(), layer);
                }
                return new GpuSection(compiled.source().section(), compiled.source(), layers);
            } catch (Throwable failure) {
                layers.values().forEach(GpuLayer::close);
                throw failure;
            }
        }

        @Override public void close() {
            layers.values().forEach(GpuLayer::close);
            layers.clear();
        }
    }

    private static final class GpuLayer implements AutoCloseable {
        private final int indexCount;
        private final VertexFormat.IndexType indexType;
        private final boolean customIndices;
        private SharedBuffers owner;
        private ChunkSectionLayer ownerLayer;

        private GpuLayer(int indexCount, VertexFormat.IndexType indexType, boolean customIndices) {
            this.indexCount = indexCount;
            this.indexType = indexType;
            this.customIndices = customIndices;
        }

        private static GpuLayer create(MeshData mesh) {
            return new GpuLayer(mesh.drawState().indexCount(), mesh.drawState().indexType(),
                    mesh.indexBuffer() != null);
        }

        @Override public void close() {
            if (owner != null) {
                owner.remove(ownerLayer, this);
                owner = null;
                ownerLayer = null;
            }
        }
    }

    private static final class SharedBuffers implements AutoCloseable {
        private static final int VERTEX_HEAP_BYTES = 16 * 1024 * 1024;
        private static final int VERTEX_STAGING_BYTES = 8 * 1024 * 1024;
        private static final int INDEX_HEAP_BYTES = 8 * 1024 * 1024;
        private static final int INDEX_STAGING_BYTES = 2 * 1024 * 1024;
        private final Map<ChunkSectionLayer, UberGpuBuffer<GpuLayer>> vertices =
                new EnumMap<>(ChunkSectionLayer.class);
        private final UberGpuBuffer<GpuLayer> translucentIndices;

        private SharedBuffers() {
            var device = RenderSystem.getDevice();
            GraphicsWorkarounds workarounds = GraphicsWorkarounds.get(device);
            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                vertices.put(layer, new UberGpuBuffer<>("Yuushya editor environment " + layer.label(),
                        GpuBuffer.USAGE_VERTEX, VERTEX_HEAP_BYTES,
                        layer.pipeline().getVertexFormat().getVertexSize(), device,
                        VERTEX_STAGING_BYTES, workarounds));
            }
            translucentIndices = new UberGpuBuffer<>("Yuushya editor environment translucent indices",
                    GpuBuffer.USAGE_INDEX, INDEX_HEAP_BYTES, 8, device,
                    INDEX_STAGING_BYTES, workarounds);
        }

        private void upload(ChunkSectionLayer layer, GpuLayer key, MeshData mesh) {
            UberGpuBuffer<GpuLayer> verticesForLayer = vertices.get(layer);
            boolean vertexAllocated = false;
            boolean indexAllocated = false;
            try {
                if (!verticesForLayer.addAllocation(key, null, mesh.vertexBuffer())) {
                    throw new IllegalStateException("environment vertex staging buffer exhausted");
                }
                var device = RenderSystem.getDevice();
                var encoder = device.createCommandEncoder();
                verticesForLayer.uploadStagedAllocations(device, encoder);
                vertexAllocated = true;
                if (mesh.indexBuffer() != null) {
                    if (layer != ChunkSectionLayer.TRANSLUCENT) {
                        throw new IllegalStateException("custom environment indices outside translucent layer");
                    }
                    if (!translucentIndices.addAllocation(key, null, mesh.indexBuffer())) {
                        throw new IllegalStateException("environment index staging buffer exhausted");
                    }
                    translucentIndices.uploadStagedAllocations(device, encoder);
                    indexAllocated = true;
                }
                key.owner = this;
                key.ownerLayer = layer;
            } catch (Throwable failure) {
                if (indexAllocated || mesh.indexBuffer() != null) translucentIndices.removeAllocation(key);
                if (vertexAllocated) verticesForLayer.removeAllocation(key);
                throw failure;
            }
        }

        private LayerBufferSlice slice(ChunkSectionLayer layer, GpuLayer key) {
            UberGpuBuffer<GpuLayer> vertexBuffer = vertices.get(layer);
            TlsfAllocator.Allocation vertex = vertexBuffer.getAllocation(key);
            if (vertex == null) return null;
            TlsfAllocator.Allocation index = key.customIndices
                    ? translucentIndices.getAllocation(key) : null;
            if (key.customIndices && index == null) return null;
            return new LayerBufferSlice(vertexBuffer.getGpuBuffer(vertex), vertex.getOffsetFromHeap(),
                    index == null ? null : translucentIndices.getGpuBuffer(index),
                    index == null ? 0L : index.getOffsetFromHeap());
        }

        private void remove(ChunkSectionLayer layer, GpuLayer key) {
            vertices.get(layer).removeAllocation(key);
            if (key.customIndices) translucentIndices.removeAllocation(key);
        }

        @Override public void close() {
            vertices.values().forEach(UberGpuBuffer::close);
            vertices.clear();
            translucentIndices.close();
        }
    }

    private record LayerBufferSlice(GpuBuffer vertexBuffer, long vertexOffset,
            GpuBuffer indexBuffer, long indexOffset) { }

    private record RenderPlan(EnvironmentPreviewFrame frame, long residentRevision,
            Matrix4f modelView, Matrix4f viewProjection, List<GpuSection> visible,
            EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> draws,
            EnumMap<ChunkSectionLayer, Integer> largestSequentialIndices) { }

    private record UniformPlan(RenderPlan renderPlan, Matrix4f modelView,
            int cameraBlockX, int cameraBlockY, int cameraBlockZ,
            int atlasWidth, int atlasHeight, DynamicUniforms.ChunkSectionInfo[] infos) { }
}
