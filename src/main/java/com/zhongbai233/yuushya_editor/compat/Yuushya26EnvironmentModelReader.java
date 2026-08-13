package com.zhongbai233.yuushya_editor.compat;

import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.zhongbai233.yuushya_editor.client.environment.EnvironmentModeledBlock;
import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.ItemModelData;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import com.zhongbai233.yuushya_editor.core.TextModelData;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector3f;

/** Strongly typed reader for neighboring Yuushya block, item, and text model entities. */
public final class Yuushya26EnvironmentModelReader {
    private Yuushya26EnvironmentModelReader() { }

    public static EnvironmentModeledBlock read(BlockEntity blockEntity, BlockPos position) {
        if (blockEntity instanceof ShowBlockEntity showBlock) {
            return readBlocks(showBlock, position);
        }
        if (blockEntity instanceof ItemBlockEntity itemBlock) {
            return readItems(itemBlock, position);
        }
        if (blockEntity instanceof TextBlockEntity textBlock) {
            return readTexts(textBlock, position);
        }
        return null;
    }

    private static EnvironmentModeledBlock readBlocks(ShowBlockEntity blockEntity, BlockPos position) {
        List<SceneLayer<Object>> layers = new ArrayList<>(blockEntity.getTransformData().size());
        for (int slot = 0; slot < blockEntity.getTransformData().size(); slot++) {
            TransformBlockData data = blockEntity.getTransformData().get(slot);
            layers.add(layer(blockEntity, position, slot, data, data.blockState));
        }
        return result(position, layers);
    }

    private static EnvironmentModeledBlock readItems(ItemBlockEntity blockEntity, BlockPos position) {
        List<SceneLayer<Object>> layers = new ArrayList<>(blockEntity.getTransformData().size());
        for (int slot = 0; slot < blockEntity.getTransformData().size(); slot++) {
            TransformItemData data = blockEntity.getTransformData().get(slot);
            BlockState blockState = data.itemStack.get(
                    com.yuushya.modelling.registries.DataComponentRegistry.BLOCKSTATE.get());
            if (blockState == null) blockState = YuushyaItemModelSupport.resolveBlockState(data.itemStack);
            layers.add(layer(blockEntity, position, slot, data,
                    new ItemModelData(data.itemStack, data.color, data.enableBlock, blockState)));
        }
        return result(position, layers);
    }

    private static EnvironmentModeledBlock readTexts(TextBlockEntity blockEntity, BlockPos position) {
        List<SceneLayer<Object>> layers = new ArrayList<>(blockEntity.getTransformData().size());
        for (int slot = 0; slot < blockEntity.getTransformData().size(); slot++) {
            TransformTextData data = blockEntity.getTransformData().get(slot);
            layers.add(layer(blockEntity, position, slot, data,
                    new TextModelData(data.textLines, data.isCulled, data.isMirror)));
        }
        return result(position, layers);
    }

    private static SceneLayer<Object> layer(BlockEntity owner, BlockPos position, int slot,
            ITransformDataProvider data, Object content) {
        UUID id = UUID.nameUUIDFromBytes((owner.getClass().getName() + ':'
                + position.getX() + ':' + position.getY() + ':' + position.getZ() + ':' + slot)
                .getBytes(StandardCharsets.UTF_8));
        EditorTransform transform = new EditorTransform(
                new Vector3d(data.getPosition()),
                YuushyaTransformConversion.fromEulerDegrees(new Vector3f(data.getRotation())),
                new Vector3f(data.getScale()));
        return new SceneLayer<>(id, "environment " + (slot + 1), content, transform, data.isShown());
    }

    private static EnvironmentModeledBlock result(BlockPos position, List<SceneLayer<Object>> layers) {
        return layers.isEmpty() ? null : new EnvironmentModeledBlock(position, layers);
    }
}
