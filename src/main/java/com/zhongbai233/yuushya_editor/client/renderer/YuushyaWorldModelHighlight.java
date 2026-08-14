package com.zhongbai233.yuushya_editor.client.renderer;

import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.registries.ItemRegistry;
import com.zhongbai233.yuushya_editor.YuushyaModellingEnhancedEditor;
import com.zhongbai233.yuushya_editor.core.WorldModelHighlightPolicy;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** Adds a quiet locator frame to visible modeled blocks while Yuushya's editor tool is held. */
@EventBusSubscriber(value = Dist.CLIENT)
public final class YuushyaWorldModelHighlight {
    private static final int LOCATOR_COLOR = ARGB.colorFromFloat(0.34F, 0.70F, 0.70F, 0.70F);
    private static final ContextKey<Boolean> EDITOR_HELD = new ContextKey<>(Identifier.fromNamespaceAndPath(
            YuushyaModellingEnhancedEditor.MOD_ID, "editor_held"));

    private YuushyaWorldModelHighlight() {
    }

    public static boolean isEditorHeld() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        return player.getMainHandItem().is(ItemRegistry.GUI_ITEM.get())
                || player.getOffhandItem().is(ItemRegistry.GUI_ITEM.get());
    }

    @SubscribeEvent
    public static void extractEditorHeldState(ExtractLevelRenderStateEvent event) {
        event.getRenderState().setRenderData(EDITOR_HELD, isEditorHeld());
    }

    @SubscribeEvent
    public static void submitModeledBlockLocators(SubmitCustomGeometryEvent event) {
        if (!Boolean.TRUE.equals(event.getLevelRenderState().getRenderData(EDITOR_HELD))) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Set<Long> visibleSections = new HashSet<>();
        Set<ChunkPos> visibleChunks = new HashSet<>();
        event.getRenderableSections().forEach(section -> {
            BlockPos origin = section.getRenderOrigin();
            visibleSections.add(SectionPos.asLong(origin));
            visibleChunks.add(ChunkPos.containing(origin));
        });

        for (ChunkPos chunkPos : visibleChunks) {
            LevelChunk chunk = level.getChunkSource().getChunk(
                    chunkPos.x(), chunkPos.z(), ChunkStatus.FULL, false);
            if (chunk == null) continue;
            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                if (!visibleSections.contains(SectionPos.asLong(blockEntity.getBlockPos()))) continue;
                if (blockEntity instanceof AbstractTransformBlockEntity modeledBlock
                        && WorldModelHighlightPolicy.shouldDraw(
                                true,
                                modeledBlock.isShowFrame(),
                                modeledBlock.isShowAxis(),
                                modeledBlock.isShowText())) {
                    submitLocatorFrame(blockEntity.getBlockPos());
                }
            }
        }
    }

    private static void submitLocatorFrame(BlockPos blockPos) {
        AABB bounds = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D).move(blockPos);
        Gizmos.cuboid(bounds, GizmoStyle.stroke(LOCATOR_COLOR), true);
    }
}
