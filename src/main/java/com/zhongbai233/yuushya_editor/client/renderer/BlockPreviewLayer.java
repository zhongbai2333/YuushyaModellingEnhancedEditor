package com.zhongbai233.yuushya_editor.client.renderer;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable Minecraft-facing block/item/text layer consumed by the shared PIP renderer. */
public record BlockPreviewLayer(Content content, EditorTransform transform, boolean selected) {
    public BlockPreviewLayer {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(transform, "transform");
    }

    public BlockPreviewLayer(BlockState blockState, EditorTransform transform, boolean selected) {
        this(new BlockContent(blockState), transform, selected);
    }

    public sealed interface Content permits BlockContent, ItemContent, TextContent { }

    public record BlockContent(BlockState blockState) implements Content {
        public BlockContent { Objects.requireNonNull(blockState, "blockState"); }
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
