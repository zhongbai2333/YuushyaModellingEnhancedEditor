package com.zhongbai233.yuushya_editor.compat;

import com.zhongbai233.yuushya_editor.core.preview.CollisionBoxOperations;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Converts between the editor's canonical model space and Yuushya's world-local custom shape. */
public final class YuushyaCollisionCoordinates {
    private YuushyaCollisionCoordinates() { }

    public static CollisionShape toWorld(BlockState targetState, CollisionShape editorShape) {
        return rotateCustom(editorShape, clockwiseQuarterTurns(targetState));
    }

    public static CollisionShape toEditor(BlockState targetState, CollisionShape worldShape) {
        return rotateCustom(worldShape, -clockwiseQuarterTurns(targetState));
    }

    static int clockwiseQuarterTurns(BlockState targetState) {
        Objects.requireNonNull(targetState, "targetState");
        if (!targetState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return 0;
        Direction facing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        // ShowBlockModel applies Axis.YP.rotationDegrees(-facing.toYRot()) around the cell center.
        return Math.floorMod(Math.round(facing.toYRot() / 90.0F), 4);
    }

    private static CollisionShape rotateCustom(CollisionShape shape, int clockwiseQuarterTurns) {
        Objects.requireNonNull(shape, "shape");
        if (shape.kind() != CollisionShape.Kind.CUSTOM) return shape;
        return CollisionShape.custom(CollisionBoxOperations.rotateYClockwise(
                shape.boxes(), clockwiseQuarterTurns));
    }
}
