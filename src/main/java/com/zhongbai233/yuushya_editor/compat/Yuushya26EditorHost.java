package com.zhongbai233.yuushya_editor.compat;

import com.yuushya.modelling.block.blockstate.YuushyaBlockStates;
import com.yuushya.modelling.blockentity.BlockShape;
import com.yuushya.modelling.blockentity.showblock.ShowBlockEntity;
import com.yuushya.modelling.blockentity.transformData.TransformBlockData;
import com.yuushya.modelling.blockentity.transformData.TransformType;
import com.yuushya.modelling.gui.showblock.ShowBlockScreen;
import com.yuushya.modelling.network.TransformDataOncePacket;
import com.yuushya.modelling.utils.ShareUtils;
import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.SceneDocument;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import com.zhongbai233.yuushya_editor.mixin.ShowBlockScreenAccessor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import org.joml.Vector3f;

/** Strongly typed adapter for the audited Yuushya Modelling 26.1 show-block contract. */
public final class Yuushya26EditorHost implements YuushyaEditorHost<Object> {
    private final ShowBlockEntity blockEntity;
    private final BlockPos blockPos;
    private final Map<UUID, TransformBlockData> rawLayers = new LinkedHashMap<>();
    private final Map<UUID, Integer> rawSlots = new LinkedHashMap<>();

    private Yuushya26EditorHost(ShowBlockEntity blockEntity) {
        this.blockEntity = Objects.requireNonNull(blockEntity, "blockEntity");
        this.blockPos = blockEntity.getBlockPos();
    }

    public static Yuushya26EditorHost fromOriginalScreen(ShowBlockScreen screen) {
        Objects.requireNonNull(screen, "screen");
        ShowBlockEntity blockEntity = ((ShowBlockScreenAccessor) screen).yuushya_editor$getBlockEntity();
        return new Yuushya26EditorHost(blockEntity);
    }

    @Override
    public SceneDocument<Object> loadDocument() {
        List<TransformBlockData> source = blockEntity.getTransformData();
        int selectedSlot = blockEntity.getSlot();
        CollisionShape collisionShape = readCollisionShape(blockEntity.getBlockState());
        String documentKey = blockPos.toShortString();
        List<SceneLayer<Object>> layers = new ArrayList<>(source.size());
        rawLayers.clear();
        rawSlots.clear();
        for (int slot = 0; slot < source.size(); slot++) {
            TransformBlockData raw = Objects.requireNonNull(source.get(slot), "transform data entry");
            BlockState blockState = Objects.requireNonNull(raw.blockState, "block state");
            if (blockState.isAir()) continue;
            UUID id = UUID.nameUUIDFromBytes((documentKey + ':' + slot).getBytes(StandardCharsets.UTF_8));
            EditorTransform transform = new EditorTransform(new Vector3d(raw.pos),
                    YuushyaTransformConversion.fromEulerDegrees(new Vector3f(raw.rot)),
                    new Vector3f(raw.scales));
            layers.add(new SceneLayer<>(id, layerName(slot, blockState), blockState, transform, raw.isShown));
            rawLayers.put(id, raw);
            rawSlots.put(id, slot);
        }
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
        if (cachedDocument.layers().stream().anyMatch(layer -> !(layer.hostData() instanceof BlockState))) {
            return false;
        }
        List<TransformBlockData> raw = new ArrayList<>(rawLayers.values());
        List<Integer> slots = new ArrayList<>(rawSlots.values());
        rawLayers.clear();
        rawSlots.clear();
        for (int slot = 0; slot < cachedDocument.layers().size(); slot++) {
            SceneLayer<Object> layer = cachedDocument.layers().get(slot);
            rawLayers.put(layer.id(), raw.get(slot));
            rawSlots.put(layer.id(), slots.get(slot));
        }
        return true;
    }

    @Override
    public ValidationResult validate(SceneDocument<Object> draft) {
        Objects.requireNonNull(draft, "draft");
        for (int slot = 0; slot < draft.layers().size(); slot++) {
            SceneLayer<Object> layer = draft.layers().get(slot);
            if (!(layer.hostData() instanceof BlockState)) {
                return ValidationResult.rejected(
                        "screen.yuushya_modelling_enhanced_editor.validation.layer_block", slot + 1);
            }
        }
        return ValidationResult.ok();
    }

    @Override
    public String exportDocument(SceneDocument<Object> draft) {
        Objects.requireNonNull(draft, "draft");
        List<TransformBlockData> raw = new ArrayList<>(draft.layers().size());
        for (SceneLayer<Object> layer : draft.layers()) {
            EditorTransform transform = layer.transform();
            raw.add(new TransformBlockData(transform.position(),
                    YuushyaTransformConversion.toEulerDegrees(transform.rotation()), transform.scale(),
                    (BlockState) layer.hostData(), layer.visible()));
        }
        return ShareUtils.transfer(raw);
    }

    @Override
    public SceneDocument<Object> importDocument(String serialized, SceneDocument<Object> current) {
        Objects.requireNonNull(serialized, "serialized");
        Objects.requireNonNull(current, "current");
        ShareUtils.ShareBlockInformation shared = ShareUtils.from(serialized);
        if (shared == null || shared.blocks().isEmpty()) {
            throw new IllegalArgumentException("No Yuushya block data found");
        }
        List<TransformBlockData> raw = new ArrayList<>();
        shared.transfer(raw);
        List<SceneLayer<Object>> layers = new ArrayList<>(raw.size());
        for (int slot = 0; slot < raw.size(); slot++) {
            TransformBlockData value = raw.get(slot);
            if (value.blockState.isAir()) continue;
            EditorTransform transform = new EditorTransform(new Vector3d(value.pos),
                    YuushyaTransformConversion.fromEulerDegrees(new Vector3f(value.rot)),
                    new Vector3f(value.scales));
            layers.add(new SceneLayer<>(UUID.randomUUID(), layerName(layers.size(), value.blockState), value.blockState,
                    transform, value.isShown));
        }
        if (layers.isEmpty()) throw new IllegalArgumentException("No non-air Yuushya block data found");
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
        List<TransformBlockData> clientLayers = blockEntity.getTransformData();
        List<UUID> draftIds = draft.layers().stream()
                .map((SceneLayer<Object> layer) -> layer.id())
                .toList();
        YuushyaSlotRewritePlan.Plan plan = YuushyaSlotRewritePlan.create(
                draftIds, rawSlots, clientLayers.size());
        for (YuushyaSlotRewritePlan.Write write : plan.writes()) {
            SceneLayer<Object> after = draft.layers().get(write.targetSlot());
            SceneLayer<Object> before = originals.get(after.id());
            if (write.fullRewrite() || before == null) {
                submitFullLayer(write.targetSlot(), after);
            } else {
                submitLayerDiff(write.targetSlot(),
                        Objects.requireNonNull(rawLayers.get(after.id()), "raw layer"), before, after);
            }
        }
        for (int slot : plan.resetSlots()) send(slot, TransformType.REMOVE, 0.0D);
        while (clientLayers.size() > draft.layers().size()) clientLayers.removeLast();
        if (!Objects.equals(original.collisionShape(), draft.collisionShape())) {
            send(Math.max(0, blockEntity.getSlot()), TransformType.SHAPE, draft.collisionShape().kind().ordinal());
        }
        if (!draft.layers().isEmpty()) blockEntity.setSlot(selectedSlot(draft));
        rawLayers.clear();
        rawSlots.clear();
        for (int slot = 0; slot < draft.layers().size(); slot++) {
            SceneLayer<Object> layer = draft.layers().get(slot);
            rawLayers.put(layer.id(), clientLayers.get(slot));
            rawSlots.put(layer.id(), slot);
        }
        TransformDataOncePacket.sendToServerSideSuccess(blockPos);
    }

    private static int selectedSlot(SceneDocument<Object> draft) {
        for (int slot = 0; slot < draft.layers().size(); slot++) {
            if (draft.layers().get(slot).id().equals(draft.selectedLayerId())) return slot;
        }
        return 0;
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

    private void submitFullLayer(int slot, SceneLayer<Object> layer) {
        BlockState blockState = (BlockState) layer.hostData();
        blockEntity.setSlot(slot);
        TransformBlockData raw = blockEntity.getTransformData(slot);
        rawLayers.put(layer.id(), raw);
        EditorTransform transform = layer.transform();
        Vector3d position = transform.position();
        Vector3f rotation = YuushyaTransformConversion.toEulerDegrees(transform.rotation());
        Vector3f scale = transform.scale();
        send(slot, TransformType.BLOCK_STATE, Block.getId(blockState));
        sendTransform(slot, position, rotation, scale);
        send(slot, TransformType.SHOWN, layer.visible() ? 1.0D : 0.0D);
        raw.set(position, rotation, scale, blockState, layer.visible());
    }

    private void submitLayerDiff(int slot, TransformBlockData raw,
            SceneLayer<Object> before, SceneLayer<Object> after) {
        EditorTransform oldTransform = before.transform();
        EditorTransform newTransform = after.transform();
        Vector3d oldPosition = oldTransform.position();
        Vector3d newPosition = newTransform.position();
        Vector3f oldScale = oldTransform.scale();
        Vector3f newScale = newTransform.scale();
        Vector3f newEuler = YuushyaTransformConversion.toEulerDegrees(newTransform.rotation());
        if (Double.compare(oldPosition.x, newPosition.x) != 0) send(slot, TransformType.POS_X, newPosition.x);
        if (Double.compare(oldPosition.y, newPosition.y) != 0) send(slot, TransformType.POS_Y, newPosition.y);
        if (Double.compare(oldPosition.z, newPosition.z) != 0) send(slot, TransformType.POS_Z, newPosition.z);
        if (!oldTransform.rotation().equals(newTransform.rotation())) {
            send(slot, TransformType.ROT_X, newEuler.x);
            send(slot, TransformType.ROT_Y, newEuler.y);
            send(slot, TransformType.ROT_Z, newEuler.z);
        }
        if (Float.compare(oldScale.x, newScale.x) != 0) send(slot, TransformType.SCALE_X, newScale.x);
        if (Float.compare(oldScale.y, newScale.y) != 0) send(slot, TransformType.SCALE_Y, newScale.y);
        if (Float.compare(oldScale.z, newScale.z) != 0) send(slot, TransformType.SCALE_Z, newScale.z);
        if (before.visible() != after.visible()) send(slot, TransformType.SHOWN, after.visible() ? 1.0D : 0.0D);
        BlockState newBlockState = (BlockState) after.hostData();
        if (!before.hostData().equals(newBlockState)) {
            send(slot, TransformType.BLOCK_STATE, Block.getId(newBlockState));
        }
        raw.pos.set(newPosition);
        raw.rot.set(newEuler);
        raw.scales.set(newScale);
        raw.blockState = newBlockState;
        raw.isShown = after.visible();
    }

    private void sendTransform(int slot, Vector3d position, Vector3f rotation, Vector3f scale) {
        send(slot, TransformType.POS_X, position.x);
        send(slot, TransformType.POS_Y, position.y);
        send(slot, TransformType.POS_Z, position.z);
        send(slot, TransformType.ROT_X, rotation.x);
        send(slot, TransformType.ROT_Y, rotation.y);
        send(slot, TransformType.ROT_Z, rotation.z);
        send(slot, TransformType.SCALE_X, scale.x);
        send(slot, TransformType.SCALE_Y, scale.y);
        send(slot, TransformType.SCALE_Z, scale.z);
    }

    private void send(int slot, TransformType type, double value) {
        TransformDataOncePacket.sendToServerSide(blockPos, slot, type, value);
    }

    private static String layerName(int slot, Object blockState) {
        String value = String.valueOf(blockState);
        if (value.length() > 42) value = value.substring(0, 39) + "...";
        return (slot + 1) + "  " + value;
    }
}
