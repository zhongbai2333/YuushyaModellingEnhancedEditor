package com.zhongbai233.yuushya_editor.client;

import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewLayer;
import com.zhongbai233.yuushya_editor.client.renderer.BlockPreviewPipRenderer;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import com.zhongbai233.yuushya_editor.core.preview.CollisionBoxOperations;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Builds a conservative, sixteenth-snapped custom collision shape from visible model layers. */
public final class CollisionAutoGenerator {
    private static final double GRID = 16.0D;

    private CollisionAutoGenerator() { }

    public static CollisionShape generate(List<SceneLayer<Object>> layers) {
        List<CollisionShape.Box> boxes = new ArrayList<>();
        for (SceneLayer<Object> layer : layers) {
            if (!layer.visible()) continue;
            BlockPreviewLayer.Content content = YuushyaEditorScreen.previewContent(layer.hostData());
            if (content == null) continue;
            Matrix4f matrix = BlockPreviewPipRenderer.previewMatrix(content, layer.transform());
            for (AABB bounds : localShapeBoxes(content)) {
                addTransformedBox(boxes, matrix, bounds);
            }
        }
        return CollisionShape.custom(CollisionBoxOperations.simplify(boxes));
    }

    static List<AABB> localShapeBoxes(BlockPreviewLayer.Content content) {
        if (content instanceof BlockPreviewLayer.BlockContent block) {
            try {
                // Keep every constituent outline box. Collapsing these first into one enclosing
                // AABB turns stairs, slabs with details, fences, etc. into a full cube.
                return block.blockState().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs()
                        .stream().filter(CollisionAutoGenerator::valid).toList();
            } catch (RuntimeException ignored) {
                // A few modded blocks require a real level for their outline shape. Retain the
                // previous conservative fallback for those blocks only.
                return List.of(BlockPreviewPipRenderer.interactionBounds(content));
            }
        }
        return List.of(BlockPreviewPipRenderer.interactionBounds(content));
    }

    private static void addTransformedBox(List<CollisionShape.Box> boxes, Matrix4f matrix, AABB bounds) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            float x = (float) ((corner & 4) == 0 ? bounds.minX : bounds.maxX);
            float y = (float) ((corner & 2) == 0 ? bounds.minY : bounds.maxY);
            float z = (float) ((corner & 1) == 0 ? bounds.minZ : bounds.maxZ);
            Vector3f point = matrix.transformPosition(new Vector3f(x, y, z));
            minX = Math.min(minX, point.x + 0.5D);
            minY = Math.min(minY, point.y + 0.5D);
            minZ = Math.min(minZ, point.z + 0.5D);
            maxX = Math.max(maxX, point.x + 0.5D);
            maxY = Math.max(maxY, point.y + 0.5D);
            maxZ = Math.max(maxZ, point.z + 0.5D);
        }
        CollisionShape.Box box = snapped(minX, minY, minZ, maxX, maxY, maxZ);
        if (box.maxX() > box.minX() && box.maxY() > box.minY() && box.maxZ() > box.minZ()) {
            boxes.add(box);
        }
    }

    private static boolean valid(AABB box) {
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
                && box.maxX > box.minX && box.maxY > box.minY && box.maxZ > box.minZ;
    }

    static CollisionShape.Box snapped(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        return CollisionBoxOperations.snapOutward(
                new CollisionShape.Box(minX, minY, minZ, maxX, maxY, maxZ), GRID);
    }
}
