package com.zhongbai233.yuushya_editor.client.environment;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Compiles immutable environment snapshots once into vanilla terrain meshes grouped by render layer. */
public final class EnvironmentSectionCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentSectionCompiler.class);
    private static final int INITIAL_LAYER_BUFFER_BYTES = 256 * 1024;
    private static final ChunkSectionLayer[] LAYERS = ChunkSectionLayer.values();

    private EnvironmentSectionCompiler() { }

    public static CompiledSection compile(EnvironmentPreviewFrame frame,
            EnvironmentSectionSnapshot snapshot, BlockStateModelSet models,
            FluidStateModelSet fluidModels, BlockColors blockColors) {
        ModelBlockRenderer renderer = new ModelBlockRenderer(true, true,
                java.util.Objects.requireNonNull(blockColors, "blockColors"));
        FluidRenderer fluidRenderer = new FluidRenderer(
                java.util.Objects.requireNonNull(fluidModels, "fluidModels"));
        EnvironmentBlockAndTintGetter snapshotView = new EnvironmentBlockAndTintGetter(snapshot);
        ByteBufferBuilder[] storageByLayer = new ByteBufferBuilder[LAYERS.length];
        BufferBuilder[] buildersByLayer = new BufferBuilder[LAYERS.length];
        Map<ChunkSectionLayer, MeshData> meshes = new EnumMap<>(ChunkSectionLayer.class);
        BlockQuadOutput output = (x, y, z, quad, instance) -> builderFor(
                quad.materialInfo().layer(), storageByLayer, buildersByLayer)
                .putBlockBakedQuad(x, y, z, quad, instance);
        Map<net.minecraft.world.level.block.state.BlockState, BlockStateModel> modelByState = new HashMap<>();

        BlockModelLighter.enableCaching();
        try {
            BlockPos.MutableBlockPos worldPos = new BlockPos.MutableBlockPos();
            for (EnvironmentSectionSnapshot.VisibleBlock block : snapshot.blocks()) {
                int worldX = snapshot.section().minBlockX() + block.localX();
                int worldY = snapshot.section().minBlockY() + block.localY();
                int worldZ = snapshot.section().minBlockZ() + block.localZ();
                worldPos.set(worldX, worldY, worldZ);
                try {
                    var fluid = block.state().getFluidState();
                    if (!fluid.isEmpty()) {
                        fluidRenderer.tesselate(snapshotView, worldPos,
                                layer -> translatedFluidOutput(builderFor(
                                        layer, storageByLayer, buildersByLayer)),
                                block.state(), fluid);
                    }
                    if (block.state().getRenderShape()
                            == net.minecraft.world.level.block.RenderShape.MODEL) {
                        BlockStateModel model = modelByState.computeIfAbsent(block.state(),
                                java.util.Objects.requireNonNull(models, "models")::get);
                        renderer.tesselateBlock(output,
                                block.localX() - 0.5F, block.localY() - 0.5F, block.localZ() - 0.5F,
                                snapshotView, worldPos, block.state(), model, block.state().getSeed(worldPos));
                    }
                } catch (Throwable incompatibleBlock) {
                    if (incompatibleBlock instanceof VirtualMachineError fatal) throw fatal;
                    if (incompatibleBlock instanceof Error fatal) throw fatal;
                    LOGGER.warn("Skipping incompatible environment block at ({}, {}, {})",
                            worldX, worldY, worldZ, incompatibleBlock);
                }
            }
            Map<ChunkSectionLayer, ByteBufferBuilder> storage = new EnumMap<>(ChunkSectionLayer.class);
            for (int i = 0; i < LAYERS.length; i++) {
                BufferBuilder builder = buildersByLayer[i];
                if (builder == null) continue;
                MeshData mesh = builder.build();
                if (mesh != null) meshes.put(LAYERS[i], mesh);
                storage.put(LAYERS[i], storageByLayer[i]);
            }
            return new CompiledSection(snapshot, meshes, storage);
        } catch (Throwable failure) {
            for (MeshData mesh : meshes.values()) mesh.close();
            closeStorage(storageByLayer);
            throw failure;
        } finally {
            BlockModelLighter.clearCache();
        }
    }

    private static BufferBuilder builderFor(ChunkSectionLayer layer,
            ByteBufferBuilder[] storage, BufferBuilder[] builders) {
        int index = layer.ordinal();
        if (builders[index] == null) {
            storage[index] = new ByteBufferBuilder(INITIAL_LAYER_BUFFER_BYTES);
            builders[index] = new BufferBuilder(storage[index], VertexFormat.Mode.QUADS,
                    layer.vertexFormat());
        }
        return builders[index];
    }

    private static VertexConsumer translatedFluidOutput(VertexConsumer delegate) {
        return new TranslatedVertexConsumer(delegate, -0.5F, -0.5F, -0.5F);
    }

    private static void closeStorage(ByteBufferBuilder[] storage) {
        for (ByteBufferBuilder value : storage) if (value != null) value.close();
    }

    private static final class TranslatedVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float x;
        private final float y;
        private final float z;

        private TranslatedVertexConsumer(VertexConsumer delegate, float x, float y, float z) {
            this.delegate = delegate;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x + this.x, y + this.y, z + this.z);
            return this;
        }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) {
            delegate.setColor(r, g, b, a); return this;
        }
        @Override public VertexConsumer setColor(int color) { delegate.setColor(color); return this; }
        @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z); return this;
        }
        @Override public VertexConsumer setLineWidth(float width) {
            delegate.setLineWidth(width); return this;
        }
    }

    public record CompiledSection(EnvironmentSectionSnapshot source,
            Map<ChunkSectionLayer, MeshData> layers,
            Map<ChunkSectionLayer, ByteBufferBuilder> storage) implements AutoCloseable {
        public CompiledSection {
            layers = new EnumMap<>(layers);
            storage = new EnumMap<>(storage);
        }

        @Override public void close() {
            for (MeshData mesh : layers.values()) mesh.close();
            layers.clear();
            for (ByteBufferBuilder buffer : storage.values()) buffer.close();
            storage.clear();
        }
    }
}
