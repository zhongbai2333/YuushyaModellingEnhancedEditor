package com.zhongbai233.yuushya_editor.compat;

import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.registries.ItemRegistry;
import com.yuushya.modelling.utils.VoxelShapeSerializer;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Uses Minecraft's creative-inventory synchronization as the transport for Yuushya's
 * server-owned shape tool. The server still applies the shape through Yuushya's normal
 * {@code ShapeItem#useOn} path, so this remains a client-only compatibility feature.
 */
public final class YuushyaShapeToolBridge {
    private YuushyaShapeToolBridge() { }

    public static boolean isShapeTool(ItemStack stack) {
        return stack != null && stack.is(ItemRegistry.SHAPE_ITEM.get());
    }

    public static List<Integer> shapeToolSlots(LocalPlayer player) {
        Objects.requireNonNull(player, "player");
        List<Integer> result = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            if (isShapeTool(player.getInventory().getItem(slot))) result.add(slot);
        }
        return List.copyOf(result);
    }

    public static CollisionShape read(ItemStack tool, BlockPos target) {
        if (!isShapeTool(tool)) throw new IllegalArgumentException("selected stack is not a Yuushya shape tool");
        CompoundTag encoded = tool.get(DataComponentRegistry.SHAPE_DATA.get());
        if (encoded == null) return CollisionShape.custom(List.of());
        Minecraft minecraft = requireCreativeClient();
        Objects.requireNonNull(target, "target");
        return YuushyaCollisionCoordinates.toEditor(minecraft.level.getBlockState(target),
                fromVoxelShape(VoxelShapeSerializer.deserializeVoxelShape(encoded)));
    }

    /** Writes the edited shape into the selected server-side creative inventory slot. */
    public static ItemStack writeTool(int inventorySlot, CollisionShape shape, BlockPos target) {
        Minecraft minecraft = requireCreativeClient();
        Objects.requireNonNull(target, "target");
        LocalPlayer player = minecraft.player;
        Inventory inventory = player.getInventory();
        validateInventorySlot(inventorySlot);
        ItemStack current = inventory.getItem(inventorySlot);
        if (!isShapeTool(current)) throw new IllegalStateException("selected shape tool is no longer present");
        ItemStack changed = current.copy();
        CollisionShape worldShape = YuushyaCollisionCoordinates.toWorld(
                minecraft.level.getBlockState(target), shape);
        changed.set(DataComponentRegistry.SHAPE_DATA.get(),
                VoxelShapeSerializer.serializeVoxelShape(toVoxelShape(worldShape)));
        inventory.setItem(inventorySlot, changed.copy());
        minecraft.gameMode.handleCreativeModeItemAdd(changed.copy(), menuSlot(inventorySlot));
        return changed;
    }

    /**
     * Synchronizes the tool, invokes its normal main-hand use on the target, then restores
     * any hotbar stack temporarily borrowed to use a tool stored in the main inventory.
     */
    public static void apply(int inventorySlot, CollisionShape shape, BlockPos target) {
        Objects.requireNonNull(target, "target");
        Minecraft minecraft = requireCreativeClient();
        LocalPlayer player = minecraft.player;
        Inventory inventory = player.getInventory();
        ItemStack changedTool = writeTool(inventorySlot, shape, target);
        int originalSelected = inventory.getSelectedSlot();
        boolean toolAlreadyInHotbar = Inventory.isHotbarSlot(inventorySlot);
        int useSlot = toolAlreadyInHotbar ? inventorySlot : originalSelected;
        ItemStack borrowedStack = toolAlreadyInHotbar ? ItemStack.EMPTY : inventory.getItem(useSlot).copy();

        if (!toolAlreadyInHotbar) {
            inventory.setItem(useSlot, changedTool.copy());
            minecraft.gameMode.handleCreativeModeItemAdd(changedTool.copy(), menuSlot(useSlot));
        }
        if (inventory.getSelectedSlot() != useSlot) {
            inventory.setSelectedSlot(useSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(useSlot));
        }

        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);

        if (inventory.getSelectedSlot() != originalSelected) {
            inventory.setSelectedSlot(originalSelected);
            player.connection.send(new ServerboundSetCarriedItemPacket(originalSelected));
        }
        if (!toolAlreadyInHotbar) {
            inventory.setItem(useSlot, borrowedStack.copy());
            minecraft.gameMode.handleCreativeModeItemAdd(borrowedStack.copy(), menuSlot(useSlot));
        }
    }

    public static VoxelShape toVoxelShape(CollisionShape shape) {
        Objects.requireNonNull(shape, "shape");
        VoxelShape result = Shapes.empty();
        for (CollisionShape.Box box : shape.boxes()) {
            result = Shapes.joinUnoptimized(result, Shapes.create(box.minX(), box.minY(), box.minZ(),
                    box.maxX(), box.maxY(), box.maxZ()), BooleanOp.OR);
        }
        return result.optimize();
    }

    public static CollisionShape fromVoxelShape(VoxelShape shape) {
        Objects.requireNonNull(shape, "shape");
        List<CollisionShape.Box> boxes = new ArrayList<>();
        for (AABB box : shape.toAabbs()) {
            boxes.add(new CollisionShape.Box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ));
        }
        return CollisionShape.custom(boxes);
    }

    private static Minecraft requireCreativeClient() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || minecraft.player.connection == null) {
            throw new IllegalStateException("Minecraft client is not connected");
        }
        if (!minecraft.player.isCreative()) {
            throw new IllegalStateException("custom collision editing requires creative mode");
        }
        return minecraft;
    }

    private static void validateInventorySlot(int inventorySlot) {
        if (inventorySlot < 0 || inventorySlot >= 36) {
            throw new IllegalArgumentException("shape tool must be in the main inventory or hotbar");
        }
    }

    private static int menuSlot(int inventorySlot) {
        validateInventorySlot(inventorySlot);
        return Inventory.isHotbarSlot(inventorySlot) ? 36 + inventorySlot : inventorySlot;
    }
}
