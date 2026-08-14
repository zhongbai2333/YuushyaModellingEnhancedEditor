package com.zhongbai233.yuushya_editor.client.renderer;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/** Immutable Minecraft-facing block/item/text layer consumed by the shared PIP renderer. */
public record BlockPreviewLayer(Content content, EditorTransform transform, boolean selected, boolean hovered,
        Vector3d worldOffset) {
    public BlockPreviewLayer {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(transform, "transform");
        worldOffset = new Vector3d(Objects.requireNonNull(worldOffset, "worldOffset"));
    }

    public BlockPreviewLayer(Content content, EditorTransform transform, boolean selected) {
        this(content, transform, selected, false, new Vector3d());
    }

    public BlockPreviewLayer(BlockState blockState, EditorTransform transform, boolean selected) {
        this(new BlockContent(blockState), transform, selected);
    }

    public BlockPreviewLayer(Content content, EditorTransform transform, boolean selected, Vector3dc worldOffset) {
        this(content, transform, selected, false, new Vector3d(worldOffset));
    }

    public BlockPreviewLayer(Content content, EditorTransform transform, boolean selected,
            boolean hovered, Vector3dc worldOffset) {
        this(content, transform, selected, hovered, new Vector3d(worldOffset));
    }

    @Override
    public Vector3d worldOffset() {
        return new Vector3d(worldOffset);
    }

    public sealed interface Content permits BlockContent, ItemContent, TextContent { }

    public record BlockContent(BlockState blockState, boolean centerOnPivot) implements Content {
        public BlockContent { Objects.requireNonNull(blockState, "blockState"); }

        public BlockContent(BlockState blockState) {
            this(blockState, true);
        }
    }

    public record ItemContent(ItemStack itemStack) implements Content {
        public ItemContent {
            itemStack = Objects.requireNonNull(itemStack, "itemStack").copy();
        }
    }

    public record TextContent(Component component, boolean culled, boolean mirror) implements Content {
        public TextContent { Objects.requireNonNull(component, "component"); }
    }
}
