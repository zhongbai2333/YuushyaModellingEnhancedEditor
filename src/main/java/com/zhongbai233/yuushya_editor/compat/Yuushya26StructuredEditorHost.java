package com.zhongbai233.yuushya_editor.compat;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.AbstractTransformBlockEntity;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.itemblock.ItemBlockEntity;
import com.yuushya.modelling.blockentity.textblock.TextBlockEntity;
import com.yuushya.modelling.blockentity.transformData.ITransformDataProvider;
import com.yuushya.modelling.blockentity.transformData.ItemTransformType;
import com.yuushya.modelling.blockentity.transformData.TextTransformType;
import com.yuushya.modelling.blockentity.transformData.TransformItemData;
import com.yuushya.modelling.blockentity.transformData.TransformTextData;
import com.yuushya.modelling.client.anvilcraft.rendering.CachedModeClient;
import com.yuushya.modelling.gui.itemblock.ItemBlockScreen;
import com.yuushya.modelling.gui.textblock.TextBlockScreen;
import com.yuushya.modelling.network.ItemStackPacket;
import com.yuushya.modelling.network.ItemTransformDataOncePacket;
import com.yuushya.modelling.network.TextLinesPacket;
import com.yuushya.modelling.network.TextTransformDataOncePacket;
import com.yuushya.modelling.registries.DataComponentRegistry;
import com.yuushya.modelling.utils.ShareUtils;
import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.EditorType;
import com.zhongbai233.yuushya_editor.core.ItemModelData;
import com.zhongbai233.yuushya_editor.core.SceneDocument;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import com.zhongbai233.yuushya_editor.core.TextModelData;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import com.zhongbai233.yuushya_editor.mixin.ItemBlockScreenAccessor;
import com.zhongbai233.yuushya_editor.mixin.TextBlockScreenAccessor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector3d;
import org.joml.Vector3f;

/** Strongly typed adapter shared by Yuushya 26.1 item-block and text-block editors. */
public final class Yuushya26StructuredEditorHost implements YuushyaEditorHost<Object> {
    private final Variant variant;
    private final AbstractTransformBlockEntity blockEntity;
    private final BlockPos blockPos;
    private final Object pendingContent;
    private final Map<UUID, ITransformDataProvider> rawLayers = new LinkedHashMap<>();
    private final Map<UUID, Integer> rawSlots = new LinkedHashMap<>();
    private List<UUID> expectedLayerIds = List.of();
    private SceneLayer<Object> initialLayer;

    private Yuushya26StructuredEditorHost(Variant variant, AbstractTransformBlockEntity blockEntity,
            Object pendingContent) {
        this.variant = Objects.requireNonNull(variant, "variant");
        this.blockEntity = Objects.requireNonNull(blockEntity, "blockEntity");
        this.pendingContent = pendingContent;
        this.blockPos = blockEntity.getBlockPos();
    }

    public static Yuushya26StructuredEditorHost fromOriginalScreen(Screen screen) {
        Objects.requireNonNull(screen, "screen");
        if (screen instanceof ItemBlockScreen itemScreen) {
            ItemBlockScreenAccessor accessor = (ItemBlockScreenAccessor) itemScreen;
            return new Yuushya26StructuredEditorHost(Variant.ITEM,
                    accessor.yuushya_editor$getBlockEntity(), accessor.yuushya_editor$getNewItemStack());
        }
        if (screen instanceof TextBlockScreen textScreen) {
            TextBlockScreenAccessor accessor = (TextBlockScreenAccessor) textScreen;
            return new Yuushya26StructuredEditorHost(Variant.TEXT,
                    accessor.yuushya_editor$getBlockEntity(), accessor.yuushya_editor$getNewTextLines());
        }
        throw new IllegalArgumentException("not a Yuushya item/text screen: " + screen.getClass().getName());
    }

    @Override
    public EditorType editorType() {
        return variant.editorType;
    }

    @Override
    public Optional<SceneLayer<Object>> initialLayer() {
        if (initialLayer == null) initialLayer = createInitialLayer();
        return Optional.ofNullable(initialLayer);
    }

    @Override
    public SceneDocument<Object> loadDocument() {
        List<? extends ITransformDataProvider> source = transformData();
        int selectedSlot = blockEntity.getSlot();
        CollisionShape collisionShape = readCollisionShape(blockEntity.getBlockState());
        String documentKey = variant.name() + ':' + blockPos.toShortString();
        List<SceneLayer<Object>> layers = new ArrayList<>(source.size());
        rawLayers.clear();
        rawSlots.clear();
        for (int slot = 0; slot < source.size(); slot++) {
            ITransformDataProvider raw = Objects.requireNonNull(source.get(slot), "transform data entry");
            if (isEmpty(raw)) continue;
            UUID id = UUID.nameUUIDFromBytes((documentKey + ':' + slot).getBytes(StandardCharsets.UTF_8));
            Object hostData = readHostData(raw);
            layers.add(new SceneLayer<>(id, layerName(slot, hostData), hostData,
                    readTransform(raw), raw.isShown()));
            rawLayers.put(id, raw);
            rawSlots.put(id, slot);
        }
        expectedLayerIds = layerIds(layers);
        UUID selected = null;
        for (Map.Entry<UUID, Integer> entry : rawSlots.entrySet()) {
            if (entry.getValue() == selectedSlot) {
                selected = entry.getKey();
                break;
            }
        }
        return new SceneDocument<>(layers, selected, collisionShape);
    }

    @Override
    public boolean rebindDocumentIdentity(SceneDocument<Object> cachedDocument) {
        Objects.requireNonNull(cachedDocument, "cachedDocument");
        if (cachedDocument.layers().size() != rawLayers.size()) return false;
        if (cachedDocument.layers().stream().anyMatch(layer -> !variant.accepts(layer.hostData()))) return false;
        List<ITransformDataProvider> raw = new ArrayList<>(rawLayers.values());
        List<Integer> slots = new ArrayList<>(rawSlots.values());
        rawLayers.clear();
        rawSlots.clear();
        for (int index = 0; index < cachedDocument.layers().size(); index++) {
            SceneLayer<Object> layer = cachedDocument.layers().get(index);
            rawLayers.put(layer.id(), raw.get(index));
            rawSlots.put(layer.id(), slots.get(index));
        }
        expectedLayerIds = layerIds(cachedDocument.layers());
        return true;
    }

    @Override
    public ValidationResult validate(SceneDocument<Object> draft) {
        Objects.requireNonNull(draft, "draft");
        for (int slot = 0; slot < draft.layers().size(); slot++) {
            if (!variant.accepts(draft.layers().get(slot).hostData())) {
                return ValidationResult.rejected(
                        "screen.yuushya_modelling_enhanced_editor.validation.layer_type", slot + 1,
                        net.minecraft.network.chat.Component.translatable(variant.editorType.translationKey()));
            }
        }
        return ValidationResult.ok();
    }

    @Override
    public String exportDocument(SceneDocument<Object> draft) {
        Objects.requireNonNull(draft, "draft");
        if (variant == Variant.ITEM) {
            List<TransformItemData> raw = new ArrayList<>(draft.layers().size());
            for (SceneLayer<Object> layer : draft.layers()) {
                EditorTransform transform = layer.transform();
                ItemModelData data = (ItemModelData) layer.hostData();
                raw.add(new TransformItemData(transform.position(),
                        YuushyaTransformConversion.toEulerDegrees(transform.rotation()), transform.scale(),
                        YuushyaItemModelSupport.stackForHost(data), data.color(), layer.visible(), data.enableBlock()));
            }
            return ShareUtils.transferItems(raw);
        }
        List<TransformTextData> raw = new ArrayList<>(draft.layers().size());
        for (SceneLayer<Object> layer : draft.layers()) {
            EditorTransform transform = layer.transform();
            TextModelData data = (TextModelData) layer.hostData();
            raw.add(new TransformTextData(transform.position(),
                    YuushyaTransformConversion.toEulerDegrees(transform.rotation()), transform.scale(),
                    data.textLines(), data.culled(), data.mirror(), layer.visible()));
        }
        return ShareUtils.transferText(raw);
    }

    @Override
    public SceneDocument<Object> importDocument(String serialized, SceneDocument<Object> current) {
        Objects.requireNonNull(serialized, "serialized");
        Objects.requireNonNull(current, "current");
        List<? extends ITransformDataProvider> raw;
        if (variant == Variant.ITEM) {
            ShareUtils.ShareItemInformation shared = ShareUtils.fromItems(serialized);
            if (shared == null || shared.items().isEmpty()) throw new IllegalArgumentException("No Yuushya item data found");
            List<TransformItemData> imported = new ArrayList<>();
            shared.transferItems(imported);
            raw = imported;
        } else {
            ShareUtils.SharedTextInformation shared = ShareUtils.fromText(serialized);
            if (shared == null || shared.texts().isEmpty()) throw new IllegalArgumentException("No Yuushya text data found");
            List<TransformTextData> imported = new ArrayList<>();
            shared.transferTexts(imported);
            raw = imported;
        }
        List<SceneLayer<Object>> layers = new ArrayList<>(raw.size());
        for (ITransformDataProvider value : raw) {
            if (isEmpty(value)) continue;
            Object hostData = readHostData(value);
            layers.add(new SceneLayer<>(UUID.randomUUID(), layerName(layers.size(), hostData), hostData,
                    readTransform(value), value.isShown()));
        }
        if (layers.isEmpty()) throw new IllegalArgumentException("No non-empty Yuushya model data found");
        return new SceneDocument<>(layers, layers.getFirst().id(), current.collisionShape());
    }

    @Override
    public Optional<BlockPos> worldOrigin() {
        return Optional.of(blockPos);
    }

    @Override
    public void submit(SceneDocument<Object> original, SceneDocument<Object> draft) {
        Objects.requireNonNull(original, "original");
        ValidationResult validation = validate(draft);
        if (!validation.valid()) throw new IllegalArgumentException(validation.message());
        Map<UUID, SceneLayer<Object>> originals = new HashMap<>();
        original.layers().forEach(layer -> originals.put(layer.id(), layer));
        List<? extends ITransformDataProvider> clientLayers = transformData();
        for (int slot = 0; slot < draft.layers().size(); slot++) {
            SceneLayer<Object> after = draft.layers().get(slot);
            SceneLayer<Object> before = originals.get(after.id());
            Integer sourceSlot = rawSlots.get(after.id());
            if (before == null || sourceSlot == null || sourceSlot != slot) submitFullLayer(slot, after);
            else submitLayerDiff(slot, rawLayers.get(after.id()), before, after);
        }
        for (int slot = clientLayers.size() - 1; slot >= draft.layers().size(); slot--) {
            sendValue(slot, ValueType.REMOVE, 0.0D);
        }
        while (clientLayers.size() > draft.layers().size()) clientLayers.removeLast();
        if (!Objects.equals(original.collisionShape(), draft.collisionShape())) {
            sendValue(Math.max(0, blockEntity.getSlot()), ValueType.SHAPE,
                    draft.collisionShape().kind().ordinal());
        }
        if (!draft.layers().isEmpty()) blockEntity.setSlot(selectedSlot(draft));
        rawLayers.clear();
        rawSlots.clear();
        for (int slot = 0; slot < draft.layers().size(); slot++) {
            SceneLayer<Object> layer = draft.layers().get(slot);
            rawLayers.put(layer.id(), clientLayers.get(slot));
            rawSlots.put(layer.id(), slot);
        }
        expectedLayerIds = layerIds(draft.layers());
        sendSuccess();
        refreshWorldRendering();
    }

    private List<? extends ITransformDataProvider> transformData() {
        if (blockEntity instanceof ItemBlockEntity itemBlock) return itemBlock.getTransformData();
        return ((TextBlockEntity) blockEntity).getTransformData();
    }

    private ITransformDataProvider transformData(int slot) {
        if (blockEntity instanceof ItemBlockEntity itemBlock) return itemBlock.getTransformData(slot);
        return ((TextBlockEntity) blockEntity).getTransformData(slot);
    }

    private static int selectedSlot(SceneDocument<Object> draft) {
        for (int slot = 0; slot < draft.layers().size(); slot++) {
            if (draft.layers().get(slot).id().equals(draft.selectedLayerId())) return slot;
        }
        return 0;
    }

    private static List<UUID> layerIds(List<SceneLayer<Object>> layers) {
        List<UUID> ids = new ArrayList<>(layers.size());
        for (SceneLayer<Object> layer : layers) ids.add(layer.id());
        return List.copyOf(ids);
    }

    private SceneLayer<Object> createInitialLayer() {
        Object content;
        if (variant == Variant.ITEM) {
            if (!(pendingContent instanceof ItemStack itemStack) || itemStack.isEmpty()) return null;
            content = YuushyaItemModelSupport.create(itemStack);
        } else {
            if (!(pendingContent instanceof List<?> list) || list.isEmpty()
                    || list.stream().anyMatch(value -> !(value instanceof String))) return null;
            @SuppressWarnings("unchecked") List<String> textLines = (List<String>) list;
            content = new TextModelData(textLines, false, false);
        }
        return new SceneLayer<>(UUID.randomUUID(), (expectedLayerIds.size() + 1) + "  "
                + net.minecraft.network.chat.Component.translatable(
                        "screen.yuushya_modelling_enhanced_editor.new_layer",
                        net.minecraft.network.chat.Component.translatable(variant.editorType.translationKey()))
                        .getString(), content, EditorTransform.IDENTITY, true);
    }

    private static EditorTransform readTransform(ITransformDataProvider raw) {
        return new EditorTransform(new Vector3d(raw.getPosition()),
                YuushyaTransformConversion.fromEulerDegrees(new Vector3f(raw.getRotation())),
                new Vector3f(raw.getScale()));
    }

    private Object readHostData(ITransformDataProvider raw) {
        if (raw instanceof TransformItemData item) {
            BlockState blockState = item.itemStack.get(DataComponentRegistry.BLOCKSTATE.get());
            if (blockState == null) blockState = YuushyaItemModelSupport.resolveBlockState(item.itemStack);
            return new ItemModelData(item.itemStack, item.color, item.enableBlock, blockState);
        }
        TransformTextData text = (TransformTextData) raw;
        return new TextModelData(text.textLines, text.isCulled, text.isMirror);
    }

    private static boolean isEmpty(ITransformDataProvider raw) {
        if (raw instanceof TransformItemData item) return item.itemStack.isEmpty();
        return ((TransformTextData) raw).textLines.isEmpty();
    }

    private void submitFullLayer(int slot, SceneLayer<Object> layer) {
        blockEntity.setSlot(slot);
        ITransformDataProvider raw = transformData(slot);
        rawLayers.put(layer.id(), raw);
        EditorTransform transform = layer.transform();
        Vector3d position = transform.position();
        Vector3f rotation = YuushyaTransformConversion.toEulerDegrees(transform.rotation());
        Vector3f scale = transform.scale();
        sendTransform(slot, position, rotation, scale);
        sendValue(slot, ValueType.SHOWN, layer.visible() ? 1.0D : 0.0D);
        if (variant == Variant.ITEM) {
            ItemModelData data = (ItemModelData) layer.hostData();
            sendValue(slot, ValueType.COLOR, data.color());
            sendValue(slot, ValueType.ENABLE_BLOCK, data.enableBlock() ? 1.0D : 0.0D);
            sendItemStack(slot, YuushyaItemModelSupport.stackForHost(data));
        } else {
            TextModelData data = (TextModelData) layer.hostData();
            sendValue(slot, ValueType.CULLED, data.culled() ? 1.0D : 0.0D);
            sendValue(slot, ValueType.MIRROR, data.mirror() ? 1.0D : 0.0D);
            sendTextLines(slot, data.textLines());
        }
        updateRaw(raw, layer);
    }

    private void submitLayerDiff(int slot, ITransformDataProvider raw,
            SceneLayer<Object> before, SceneLayer<Object> after) {
        Objects.requireNonNull(raw, "raw layer");
        EditorTransform oldTransform = before.transform();
        EditorTransform newTransform = after.transform();
        Vector3d oldPosition = oldTransform.position();
        Vector3d newPosition = newTransform.position();
        Vector3f oldScale = oldTransform.scale();
        Vector3f newScale = newTransform.scale();
        Vector3f newEuler = YuushyaTransformConversion.toEulerDegrees(newTransform.rotation());
        if (Double.compare(oldPosition.x, newPosition.x) != 0) sendValue(slot, ValueType.POS_X, newPosition.x);
        if (Double.compare(oldPosition.y, newPosition.y) != 0) sendValue(slot, ValueType.POS_Y, newPosition.y);
        if (Double.compare(oldPosition.z, newPosition.z) != 0) sendValue(slot, ValueType.POS_Z, newPosition.z);
        if (!oldTransform.rotation().equals(newTransform.rotation())) {
            sendValue(slot, ValueType.ROT_X, newEuler.x);
            sendValue(slot, ValueType.ROT_Y, newEuler.y);
            sendValue(slot, ValueType.ROT_Z, newEuler.z);
        }
        if (Float.compare(oldScale.x, newScale.x) != 0) sendValue(slot, ValueType.SCALE_X, newScale.x);
        if (Float.compare(oldScale.y, newScale.y) != 0) sendValue(slot, ValueType.SCALE_Y, newScale.y);
        if (Float.compare(oldScale.z, newScale.z) != 0) sendValue(slot, ValueType.SCALE_Z, newScale.z);
        if (before.visible() != after.visible()) sendValue(slot, ValueType.SHOWN, after.visible() ? 1.0D : 0.0D);
        if (!before.hostData().equals(after.hostData())) sendContentDiff(slot, before.hostData(), after.hostData());
        updateRaw(raw, after);
    }

    private void sendContentDiff(int slot, Object before, Object after) {
        if (variant == Variant.ITEM) {
            ItemModelData oldData = (ItemModelData) before;
            ItemModelData newData = (ItemModelData) after;
            ItemStack oldStack = YuushyaItemModelSupport.stackForHost(oldData);
            ItemStack newStack = YuushyaItemModelSupport.stackForHost(newData);
            if (!ItemStack.matches(oldStack, newStack)) sendItemStack(slot, newStack);
            if (oldData.color() != newData.color()) sendValue(slot, ValueType.COLOR, newData.color());
            if (oldData.enableBlock() != newData.enableBlock()) {
                sendValue(slot, ValueType.ENABLE_BLOCK, newData.enableBlock() ? 1.0D : 0.0D);
            }
        } else {
            TextModelData oldData = (TextModelData) before;
            TextModelData newData = (TextModelData) after;
            if (!oldData.textLines().equals(newData.textLines())) sendTextLines(slot, newData.textLines());
            if (oldData.culled() != newData.culled()) sendValue(slot, ValueType.CULLED, newData.culled() ? 1.0D : 0.0D);
            if (oldData.mirror() != newData.mirror()) sendValue(slot, ValueType.MIRROR, newData.mirror() ? 1.0D : 0.0D);
        }
    }

    private void sendTransform(int slot, Vector3d position, Vector3f rotation, Vector3f scale) {
        sendValue(slot, ValueType.POS_X, position.x);
        sendValue(slot, ValueType.POS_Y, position.y);
        sendValue(slot, ValueType.POS_Z, position.z);
        sendValue(slot, ValueType.ROT_X, rotation.x);
        sendValue(slot, ValueType.ROT_Y, rotation.y);
        sendValue(slot, ValueType.ROT_Z, rotation.z);
        sendValue(slot, ValueType.SCALE_X, scale.x);
        sendValue(slot, ValueType.SCALE_Y, scale.y);
        sendValue(slot, ValueType.SCALE_Z, scale.z);
    }

    private static void updateRaw(ITransformDataProvider raw, SceneLayer<Object> layer) {
        raw.getPosition().set(layer.transform().position());
        raw.getRotation().set(YuushyaTransformConversion.toEulerDegrees(layer.transform().rotation()));
        raw.getScale().set(layer.transform().scale());
        raw.setShown(layer.visible());
        if (raw instanceof TransformItemData item) {
            ItemModelData data = (ItemModelData) layer.hostData();
            item.itemStack = YuushyaItemModelSupport.stackForHost(data);
            item.color = data.color();
            item.enableBlock = data.enableBlock();
        } else {
            TransformTextData text = (TransformTextData) raw;
            TextModelData data = (TextModelData) layer.hostData();
            text.textLines = new ArrayList<>(data.textLines());
            text.isCulled = data.culled();
            text.isMirror = data.mirror();
        }
    }

    private void sendValue(int slot, ValueType type, double value) {
        if (variant == Variant.ITEM) {
            ItemTransformDataOncePacket.sendToServerSide(blockPos, slot, type.itemType(), value);
        } else {
            TextTransformDataOncePacket.sendToServerSide(blockPos, slot, type.textType(), value);
        }
    }

    private void sendSuccess() {
        if (variant == Variant.ITEM) ItemTransformDataOncePacket.sendToServerSideSuccess(blockPos);
        else TextTransformDataOncePacket.sendToServerSideSuccess(blockPos);
    }

    private void sendItemStack(int slot, ItemStack itemStack) {
        ClientPacketDistributor.sendToServer(new ItemStackPacket(blockPos, slot, itemStack.copy()));
    }

    private void sendTextLines(int slot, List<String> textLines) {
        TextLinesPacket.sendToServerSide(blockPos, slot, List.copyOf(textLines));
    }

    private void refreshWorldRendering() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.isLoaded(blockPos)) return;
        BlockState state = level.getBlockState(blockPos);
        if (variant == Variant.ITEM) {
            CachedModeClient.INSTANCE.safeSet.add(ChunkPos.containing(blockPos));
        }
        level.sendBlockUpdated(blockPos, state, state, 11);
    }

    private static CollisionShape readCollisionShape(BlockState blockState) {
        BlockShape value = blockState.getValue(YuushyaBlockStates.SHAPES);
        CollisionShape.Kind kind = CollisionShape.Kind.valueOf(value.name());
        if (kind != CollisionShape.Kind.CUSTOM) return CollisionShape.forKind(kind);
        List<CollisionShape.Box> boxes = new ArrayList<>();
        for (AABB box : value.voxelShape.toAabbs()) {
            boxes.add(new CollisionShape.Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
        }
        return CollisionShape.custom(boxes);
    }

    private String layerName(int slot, Object hostData) {
        if (hostData instanceof ItemModelData itemData) {
            return (slot + 1) + "  " + itemData.itemStack().getHoverName().getString();
        }
        TextModelData text = (TextModelData) hostData;
        String value = text.textLines().isEmpty()
                ? net.minecraft.network.chat.Component.translatable(
                        "screen.yuushya_modelling_enhanced_editor.type.text").getString()
                : text.textLines().getFirst();
        if (value.length() > 34) value = value.substring(0, 31) + "...";
        return (slot + 1) + "  " + value;
    }

    private enum Variant {
        ITEM(EditorType.ITEM), TEXT(EditorType.TEXT);

        private final EditorType editorType;

        Variant(EditorType editorType) {
            this.editorType = editorType;
        }

        private boolean accepts(Object value) {
            return this == ITEM ? value instanceof ItemModelData : value instanceof TextModelData;
        }
    }

    private enum ValueType {
        POS_X, POS_Y, POS_Z, ROT_X, ROT_Y, ROT_Z, SCALE_X, SCALE_Y, SCALE_Z,
        SHOWN, REMOVE, SHAPE, COLOR, ENABLE_BLOCK, CULLED, MIRROR;

        private ItemTransformType itemType() {
            return switch (this) {
                case POS_X -> ItemTransformType.POS_X;
                case POS_Y -> ItemTransformType.POS_Y;
                case POS_Z -> ItemTransformType.POS_Z;
                case ROT_X -> ItemTransformType.ROT_X;
                case ROT_Y -> ItemTransformType.ROT_Y;
                case ROT_Z -> ItemTransformType.ROT_Z;
                case SCALE_X -> ItemTransformType.SCALE_X;
                case SCALE_Y -> ItemTransformType.SCALE_Y;
                case SCALE_Z -> ItemTransformType.SCALE_Z;
                case SHOWN -> ItemTransformType.SHOWN;
                case REMOVE -> ItemTransformType.REMOVE;
                case SHAPE -> ItemTransformType.SHAPE;
                case COLOR -> ItemTransformType.COLOR;
                case ENABLE_BLOCK -> ItemTransformType.ENABLE_BLOCK;
                case CULLED, MIRROR -> throw new IllegalStateException("text-only transform type: " + this);
            };
        }

        private TextTransformType textType() {
            return switch (this) {
                case POS_X -> TextTransformType.POS_X;
                case POS_Y -> TextTransformType.POS_Y;
                case POS_Z -> TextTransformType.POS_Z;
                case ROT_X -> TextTransformType.ROT_X;
                case ROT_Y -> TextTransformType.ROT_Y;
                case ROT_Z -> TextTransformType.ROT_Z;
                case SCALE_X -> TextTransformType.SCALE_X;
                case SCALE_Y -> TextTransformType.SCALE_Y;
                case SCALE_Z -> TextTransformType.SCALE_Z;
                case SHOWN -> TextTransformType.SHOWN;
                case REMOVE -> TextTransformType.REMOVE;
                case SHAPE -> TextTransformType.SHAPE;
                case CULLED -> TextTransformType.CULLED;
                case MIRROR -> TextTransformType.MIRROR;
                case COLOR, ENABLE_BLOCK -> throw new IllegalStateException("item-only transform type: " + this);
            };
        }
    }
}
