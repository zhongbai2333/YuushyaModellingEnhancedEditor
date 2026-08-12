package com.zhongbai233.yuushya_editor.compat;

import java.util.List;
import java.util.Objects;

/**
 * Side-effect-free runtime capability probe. Concrete adapters are selected only when every required class exists.
 */
public final class YuushyaCompatibility {
    private static final List<Variant> VARIANTS = List.of(
            new Variant("26.1-block-item-text", List.of(
                    "com.yuushya.modelling.gui.showblock.ShowBlockScreen",
                    "com.yuushya.modelling.gui.itemblock.ItemBlockScreen",
                    "com.yuushya.modelling.gui.textblock.TextBlockScreen",
                    "com.yuushya.modelling.blockentity.showblock.ShowBlockEntity",
                    "com.yuushya.modelling.blockentity.transformData.TransformBlockData",
                    "com.yuushya.modelling.blockentity.transformData.TransformItemData",
                    "com.yuushya.modelling.blockentity.transformData.TransformTextData",
                    "com.yuushya.modelling.blockentity.transformData.TransformType",
                    "com.yuushya.modelling.network.TransformDataOncePacket",
                    "com.yuushya.modelling.network.ItemStackPacket",
                    "com.yuushya.modelling.network.TextLinesPacket"))
    );

    private YuushyaCompatibility() { }

    public static ProbeResult probe(ClassLoader loader) {
        Objects.requireNonNull(loader, "loader");
        for (Variant variant : VARIANTS) {
            if (variant.requiredClasses().stream().allMatch(name -> present(name, loader))) {
                return new ProbeResult(true, variant.id(), "compatible classes are present");
            }
        }
        return new ProbeResult(false, "none",
                "Yuushya Modelling 26.1 block/item/text classes were not found; original UI remains untouched");
    }

    private static boolean present(String name, ClassLoader loader) {
        try {
            Class.forName(name, false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError unavailable) {
            return false;
        }
    }

    private record Variant(String id, List<String> requiredClasses) {
        private Variant {
            requiredClasses = List.copyOf(requiredClasses);
        }
    }

    public record ProbeResult(boolean available, String variant, String message) {
        public ProbeResult {
            Objects.requireNonNull(variant, "variant");
            Objects.requireNonNull(message, "message");
        }
    }
}
