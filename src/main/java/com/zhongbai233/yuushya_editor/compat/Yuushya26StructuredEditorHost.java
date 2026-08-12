package com.zhongbai233.yuushya_editor.compat;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.EditorType;
import com.zhongbai233.yuushya_editor.core.ItemModelData;
import com.zhongbai233.yuushya_editor.core.SceneDocument;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import com.zhongbai233.yuushya_editor.core.TextModelData;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector3d;
import org.joml.Vector3f;

/** Reflective adapter shared by Yuushya 26.1 item-block and text-block editors. */
public final class Yuushya26StructuredEditorHost implements YuushyaEditorHost<Object> {
    public static final String ITEM_SCREEN_CLASS = "com.yuushya.modelling.gui.itemblock.ItemBlockScreen";
    public static final String TEXT_SCREEN_CLASS = "com.yuushya.modelling.gui.textblock.TextBlockScreen";

    private final Variant variant;
    private final Object blockEntity;
    private final Bindings bindings;
    private final BlockPos blockPos;
    private final Object pendingContent;
    private final Map<UUID, Object> rawLayers = new LinkedHashMap<>();
    private final Map<UUID, Integer> rawSlots = new LinkedHashMap<>();
    private List<UUID> expectedLayerIds = List.of();
    private SceneLayer<Object> initialLayer;

    private Yuushya26StructuredEditorHost(Variant variant, Object blockEntity,
            Object pendingContent, Bindings bindings) {
        this.variant = Objects.requireNonNull(variant, "variant");
        this.blockEntity = Objects.requireNonNull(blockEntity, "blockEntity");
        this.pendingContent = pendingContent;
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.blockPos = cast(invoke(bindings.getBlockPos, blockEntity), BlockPos.class, "block position");
    }

    public static Yuushya26StructuredEditorHost fromOriginalScreen(Screen screen) {
        Objects.requireNonNull(screen, "screen");
        Variant variant = Variant.forScreen(screen.getClass().getName());
        try {
            Field blockEntityField = screen.getClass().getDeclaredField("blockEntity");
            Field pendingField = screen.getClass().getDeclaredField(variant.pendingFieldName);
            if (!blockEntityField.trySetAccessible() || !pendingField.trySetAccessible()) {
                throw new IllegalStateException("cannot access Yuushya structured editor fields");
            }
            Object blockEntity = blockEntityField.get(screen);
            Object pending = pendingField.get(screen);
            return new Yuushya26StructuredEditorHost(variant, blockEntity, pending,
                    Bindings.resolve(variant, screen.getClass().getClassLoader(), blockEntity.getClass()));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("cannot bind Yuushya " + variant.name().toLowerCase()
                    + " editor", exception);
        }
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
        List<?> source = cast(invoke(bindings.getTransformData, blockEntity), List.class, "transform data");
        int selectedSlot = ((Number) invoke(bindings.getSlot, blockEntity)).intValue();
        CollisionShape collisionShape = readCollisionShape(invoke(bindings.getBlockState, blockEntity));
        String documentKey = variant.name() + ':' + blockPos.toShortString();
        List<SceneLayer<Object>> layers = new ArrayList<>(source.size());
        rawLayers.clear();
        rawSlots.clear();
        for (int slot = 0; slot < source.size(); slot++) {
            Object raw = Objects.requireNonNull(source.get(slot), "transform data entry");
            if (isEmpty(raw)) continue;
            UUID id = UUID.nameUUIDFromBytes((documentKey + ':' + slot).getBytes(StandardCharsets.UTF_8));
            EditorTransform transform = readTransform(raw);
            Object hostData = readHostData(raw);
            boolean visible = readBoolean(bindings.visible, raw);
            layers.add(new SceneLayer<>(id, layerName(slot, hostData), hostData, transform, visible));
            rawLayers.put(id, raw);
            rawSlots.put(id, slot);
        }
        expectedLayerIds = layers.stream().map(SceneLayer::id).toList();
        UUID selected = rawSlots.entrySet().stream()
                .filter(entry -> entry.getValue() == selectedSlot)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        return new SceneDocument<>(layers, selected, collisionShape);
    }

    @Override
    public ValidationResult validate(SceneDocument<Object> draft) {
        Objects.requireNonNull(draft, "draft");
        Map<UUID, Integer> originalSlots = new HashMap<>();
        for (int slot = 0; slot < expectedLayerIds.size(); slot++) {
            originalSlots.put(expectedLayerIds.get(slot), slot);
        }
        int lastOriginalSlot = -1;
        boolean reachedNew = false;
        for (int slot = 0; slot < draft.layers().size(); slot++) {
            SceneLayer<Object> layer = draft.layers().get(slot);
            if (!variant.accepts(layer.hostData())) {
                return ValidationResult.rejected("screen.yuushya_modelling_enhanced_editor.validation.layer_type",
                        slot + 1, net.minecraft.network.chat.Component.translatable(
                                variant.editorType.translationKey()));
            }
            Integer originalSlot = originalSlots.get(layer.id());
            if (originalSlot == null) {
                reachedNew = true;
            } else {
                if (reachedNew || originalSlot <= lastOriginalSlot) {
                    return ValidationResult.rejected("screen.yuushya_modelling_enhanced_editor.validation.order");
                }
                lastOriginalSlot = originalSlot;
            }
        }
        return ValidationResult.ok();
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

        List<?> clientLayersView = cast(invoke(bindings.getTransformData, blockEntity), List.class,
                "transform data");
        @SuppressWarnings("unchecked")
        List<Object> clientLayers = (List<Object>) clientLayersView;

        for (int slot = 0; slot < draft.layers().size(); slot++) {
            SceneLayer<Object> after = draft.layers().get(slot);
            SceneLayer<Object> before = originals.get(after.id());
            Integer sourceSlot = rawSlots.get(after.id());
            if (before == null || sourceSlot == null || sourceSlot != slot) {
                submitFullLayer(slot, after);
            } else {
                submitLayerDiff(slot, rawLayers.get(after.id()), before, after);
            }
        }
        // Yuushya's REMOVE resets a slot instead of shrinking the list. Always pack
        // retained layers into [0, draft.size) and clear every trailing server slot.
        for (int slot = clientLayers.size() - 1; slot >= draft.layers().size(); slot--) {
            sendValue(slot, "REMOVE", 0.0D);
        }
        while (clientLayers.size() > draft.layers().size()) {
            clientLayers.removeLast();
        }
        if (!Objects.equals(original.collisionShape(), draft.collisionShape())) {
            sendValue(Math.max(0, ((Number) invoke(bindings.getSlot, blockEntity)).intValue()),
                    "SHAPE", draft.collisionShape().kind().ordinal());
        }
        if (!draft.layers().isEmpty()) {
            int selectedSlot = 0;
            for (int slot = 0; slot < draft.layers().size(); slot++) {
                if (draft.layers().get(slot).id().equals(draft.selectedLayerId())) {
                    selectedSlot = slot;
                    break;
                }
            }
            invoke(bindings.setSlot, blockEntity, selectedSlot);
        }
        rawLayers.clear();
        rawSlots.clear();
        for (int slot = 0; slot < draft.layers().size(); slot++) {
            SceneLayer<Object> layer = draft.layers().get(slot);
            rawLayers.put(layer.id(), clientLayers.get(slot));
            rawSlots.put(layer.id(), slot);
        }
        expectedLayerIds = draft.layers().stream().map(SceneLayer::id).toList();
        invoke(bindings.sendSuccess, null, blockPos);
    }

    private SceneLayer<Object> createInitialLayer() {
        Object content;
        if (variant == Variant.ITEM) {
            if (!(pendingContent instanceof ItemStack itemStack) || itemStack.isEmpty()) return null;
            content = new ItemModelData(itemStack, 0xFFFFFFFF, false);
        } else {
            if (!(pendingContent instanceof List<?> list) || list.isEmpty()
                    || list.stream().anyMatch(value -> !(value instanceof String))) return null;
            @SuppressWarnings("unchecked")
            List<String> textLines = (List<String>) list;
            content = new TextModelData(textLines, false, false);
        }
        return new SceneLayer<>(UUID.randomUUID(), (expectedLayerIds.size() + 1) + "  "
                + net.minecraft.network.chat.Component.translatable(
                        "screen.yuushya_modelling_enhanced_editor.new_layer",
                        net.minecraft.network.chat.Component.translatable(variant.editorType.translationKey()))
                        .getString(), content, EditorTransform.IDENTITY, true);
    }

    private EditorTransform readTransform(Object raw) {
        Vector3d position = new Vector3d(cast(read(bindings.position, raw), Vector3d.class, "position"));
        Vector3f euler = new Vector3f(cast(read(bindings.rotation, raw), Vector3f.class, "rotation"));
        Vector3f scale = new Vector3f(cast(read(bindings.scale, raw), Vector3f.class, "scale"));
        return new EditorTransform(position, YuushyaTransformConversion.fromEulerDegrees(euler), scale);
    }

    private Object readHostData(Object raw) {
        if (variant == Variant.ITEM) {
            ItemStack itemStack = cast(read(bindings.content, raw), ItemStack.class, "item stack");
            boolean enableBlock = readBoolean(bindings.secondaryFlag, raw);
            BlockState enabledBlockState = null;
            if (enableBlock && bindings.itemBlockStateComponent != null) {
                Object holder = readStatic(bindings.itemBlockStateComponent);
                Object componentType = invoke(bindings.supplierGet, holder);
                Object value = invoke(bindings.itemGetComponent, itemStack, componentType);
                if (value instanceof BlockState blockState) enabledBlockState = blockState;
            }
            return new ItemModelData(itemStack, readInt(bindings.color, raw), enableBlock, enabledBlockState);
        }
        List<?> values = cast(read(bindings.content, raw), List.class, "text lines");
        List<String> lines = new ArrayList<>(values.size());
        for (Object value : values) lines.add(cast(value, String.class, "text line"));
        return new TextModelData(lines, readBoolean(bindings.primaryFlag, raw),
                readBoolean(bindings.secondaryFlag, raw));
    }

    private boolean isEmpty(Object raw) {
        if (variant == Variant.ITEM) {
            return cast(read(bindings.content, raw), ItemStack.class, "item stack").isEmpty();
        }
        return cast(read(bindings.content, raw), List.class, "text lines").isEmpty();
    }

    private void submitFullLayer(int slot, SceneLayer<Object> layer) {
        invoke(bindings.setSlot, blockEntity, slot);
        Object raw = invoke(bindings.getTransformDataAt, blockEntity, slot);
        rawLayers.put(layer.id(), raw);
        EditorTransform transform = layer.transform();
        Vector3d position = transform.position();
        Vector3f rotation = YuushyaTransformConversion.toEulerDegrees(transform.rotation());
        Vector3f scale = transform.scale();
        sendTransform(slot, position, rotation, scale);
        sendValue(slot, "SHOWN", layer.visible() ? 1.0D : 0.0D);
        if (variant == Variant.ITEM) {
            ItemModelData data = (ItemModelData) layer.hostData();
            sendValue(slot, "COLOR", data.color());
            sendValue(slot, "ENABLE_BLOCK", data.enableBlock() ? 1.0D : 0.0D);
            sendItemStack(slot, data.itemStack());
        } else {
            TextModelData data = (TextModelData) layer.hostData();
            sendValue(slot, "CULLED", data.culled() ? 1.0D : 0.0D);
            sendValue(slot, "MIRROR", data.mirror() ? 1.0D : 0.0D);
            sendTextLines(slot, data.textLines());
        }
        updateRaw(raw, layer);
    }

    private void submitLayerDiff(int slot, Object raw, SceneLayer<Object> before, SceneLayer<Object> after) {
        Objects.requireNonNull(raw, "raw layer");
        EditorTransform oldTransform = before.transform();
        EditorTransform newTransform = after.transform();
        Vector3d oldPosition = oldTransform.position();
        Vector3d newPosition = newTransform.position();
        Vector3f oldScale = oldTransform.scale();
        Vector3f newScale = newTransform.scale();
        Vector3f newEuler = YuushyaTransformConversion.toEulerDegrees(newTransform.rotation());
        if (Double.compare(oldPosition.x, newPosition.x) != 0) sendValue(slot, "POS_X", newPosition.x);
        if (Double.compare(oldPosition.y, newPosition.y) != 0) sendValue(slot, "POS_Y", newPosition.y);
        if (Double.compare(oldPosition.z, newPosition.z) != 0) sendValue(slot, "POS_Z", newPosition.z);
        if (!oldTransform.rotation().equals(newTransform.rotation())) {
            sendValue(slot, "ROT_X", newEuler.x);
            sendValue(slot, "ROT_Y", newEuler.y);
            sendValue(slot, "ROT_Z", newEuler.z);
        }
        if (Float.compare(oldScale.x, newScale.x) != 0) sendValue(slot, "SCALE_X", newScale.x);
        if (Float.compare(oldScale.y, newScale.y) != 0) sendValue(slot, "SCALE_Y", newScale.y);
        if (Float.compare(oldScale.z, newScale.z) != 0) sendValue(slot, "SCALE_Z", newScale.z);
        if (before.visible() != after.visible()) sendValue(slot, "SHOWN", after.visible() ? 1.0D : 0.0D);
        if (!before.hostData().equals(after.hostData())) sendContentDiff(slot, before.hostData(), after.hostData());
        updateRaw(raw, after);
    }

    private void sendContentDiff(int slot, Object before, Object after) {
        if (variant == Variant.ITEM) {
            ItemModelData oldData = (ItemModelData) before;
            ItemModelData newData = (ItemModelData) after;
            if (!ItemStack.matches(oldData.itemStack(), newData.itemStack())) {
                sendItemStack(slot, newData.itemStack());
            }
            if (oldData.color() != newData.color()) sendValue(slot, "COLOR", newData.color());
            if (oldData.enableBlock() != newData.enableBlock()) {
                sendValue(slot, "ENABLE_BLOCK", newData.enableBlock() ? 1.0D : 0.0D);
            }
        } else {
            TextModelData oldData = (TextModelData) before;
            TextModelData newData = (TextModelData) after;
            if (!oldData.textLines().equals(newData.textLines())) sendTextLines(slot, newData.textLines());
            if (oldData.culled() != newData.culled()) sendValue(slot, "CULLED", newData.culled() ? 1.0D : 0.0D);
            if (oldData.mirror() != newData.mirror()) sendValue(slot, "MIRROR", newData.mirror() ? 1.0D : 0.0D);
        }
    }

    private void sendTransform(int slot, Vector3d position, Vector3f rotation, Vector3f scale) {
        sendValue(slot, "POS_X", position.x);
        sendValue(slot, "POS_Y", position.y);
        sendValue(slot, "POS_Z", position.z);
        sendValue(slot, "ROT_X", rotation.x);
        sendValue(slot, "ROT_Y", rotation.y);
        sendValue(slot, "ROT_Z", rotation.z);
        sendValue(slot, "SCALE_X", scale.x);
        sendValue(slot, "SCALE_Y", scale.y);
        sendValue(slot, "SCALE_Z", scale.z);
    }

    private void updateRaw(Object raw, SceneLayer<Object> layer) {
        Vector3d position = layer.transform().position();
        Vector3f rotation = YuushyaTransformConversion.toEulerDegrees(layer.transform().rotation());
        Vector3f scale = layer.transform().scale();
        cast(read(bindings.position, raw), Vector3d.class, "position").set(position);
        cast(read(bindings.rotation, raw), Vector3f.class, "rotation").set(rotation);
        cast(read(bindings.scale, raw), Vector3f.class, "scale").set(scale);
        writeBoolean(bindings.visible, raw, layer.visible());
        if (variant == Variant.ITEM) {
            ItemModelData data = (ItemModelData) layer.hostData();
            write(bindings.content, raw, data.itemStack());
            writeInt(bindings.color, raw, data.color());
            writeBoolean(bindings.secondaryFlag, raw, data.enableBlock());
        } else {
            TextModelData data = (TextModelData) layer.hostData();
            write(bindings.content, raw, new ArrayList<>(data.textLines()));
            writeBoolean(bindings.primaryFlag, raw, data.culled());
            writeBoolean(bindings.secondaryFlag, raw, data.mirror());
        }
    }

    private void sendValue(int slot, String typeName, double value) {
        Object type = Objects.requireNonNull(bindings.transformTypes.get(typeName), "transform type " + typeName);
        invoke(bindings.sendValue, null, blockPos, slot, type, value);
    }

    private void sendItemStack(int slot, ItemStack itemStack) {
        try {
            Object packet = bindings.contentPacketConstructor.newInstance(blockPos, slot, itemStack.copy());
            ClientPacketDistributor.sendToServer(cast(packet, CustomPacketPayload.class, "item packet"));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Yuushya item packet construction failed", exception);
        }
    }

    private void sendTextLines(int slot, List<String> textLines) {
        invoke(bindings.sendContent, null, blockPos, slot, textLines);
    }

    private CollisionShape readCollisionShape(Object blockState) {
        Object value = invoke(bindings.blockStateGetValue, blockState, readStatic(bindings.shapeProperty));
        if (!(value instanceof Enum<?> enumValue)) {
            throw new IllegalStateException("Yuushya collision shape has unexpected type: " + value);
        }
        CollisionShape.Kind kind;
        try {
            kind = CollisionShape.Kind.valueOf(enumValue.name());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Unsupported Yuushya collision shape: " + enumValue.name(), exception);
        }
        if (kind != CollisionShape.Kind.CUSTOM) return CollisionShape.forKind(kind);
        VoxelShape custom = cast(read(bindings.shapeVoxelShape, value), VoxelShape.class, "custom collision shape");
        List<CollisionShape.Box> boxes = new ArrayList<>();
        for (AABB box : custom.toAabbs()) {
            boxes.add(new CollisionShape.Box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ));
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
        ITEM(EditorType.ITEM, ITEM_SCREEN_CLASS, "newItemStack"),
        TEXT(EditorType.TEXT, TEXT_SCREEN_CLASS, "newTextLines");

        private final EditorType editorType;
        private final String screenClass;
        private final String pendingFieldName;

        Variant(EditorType editorType, String screenClass, String pendingFieldName) {
            this.editorType = editorType;
            this.screenClass = screenClass;
            this.pendingFieldName = pendingFieldName;
        }

        private static Variant forScreen(String className) {
            for (Variant variant : values()) if (variant.screenClass.equals(className)) return variant;
            throw new IllegalArgumentException("not a Yuushya item/text screen: " + className);
        }

        private boolean accepts(Object value) {
            return this == ITEM ? value instanceof ItemModelData : value instanceof TextModelData;
        }
    }

    private record Bindings(Method getTransformData, Method getTransformDataAt, Method getSlot, Method setSlot,
            Method getBlockPos, Method getBlockState, Method blockStateGetValue,
            Field shapeProperty, Field shapeVoxelShape, Field position, Field rotation, Field scale,
            Field content, Field color, Field visible, Field primaryFlag, Field secondaryFlag,
            Method sendValue, Method sendSuccess, Method sendContent, Constructor<?> contentPacketConstructor,
            Field itemBlockStateComponent, Method supplierGet, Method itemGetComponent,
            Map<String, Object> transformTypes) {
        private static Bindings resolve(Variant variant, ClassLoader loader, Class<?> blockEntityClass)
                throws ReflectiveOperationException {
            String dataName = variant == Variant.ITEM ? "TransformItemData" : "TransformTextData";
            String typeName = variant == Variant.ITEM ? "ItemTransformType" : "TextTransformType";
            String packetName = variant == Variant.ITEM
                    ? "ItemTransformDataOncePacket" : "TextTransformDataOncePacket";
            Class<?> dataClass = Class.forName("com.yuushya.modelling.blockentity.transformData." + dataName,
                    false, loader);
            Class<?> typeClass = Class.forName("com.yuushya.modelling.blockentity.transformData." + typeName,
                    false, loader);
            Class<?> packetClass = Class.forName("com.yuushya.modelling.network." + packetName, false, loader);
            Class<?> yuushyaBlockStatesClass = Class.forName(
                    "com.yuushya.modelling.block.blockstate.YuushyaBlockStates", false, loader);
            Class<?> blockShapeClass = Class.forName(
                    "com.yuushya.modelling.blockentity.BlockShape", false, loader);
            Map<String, Object> types = new HashMap<>();
            for (Object value : typeClass.getEnumConstants()) types.put(((Enum<?>) value).name(), value);
            Method sendContent = null;
            Constructor<?> contentPacketConstructor = null;
            Field itemBlockStateComponent = null;
            Method supplierGet = null;
            Method itemGetComponent = null;
            Field content;
            Field color = null;
            Field primaryFlag = null;
            Field secondaryFlag;
            if (variant == Variant.ITEM) {
                content = dataClass.getField("itemStack");
                color = dataClass.getField("color");
                secondaryFlag = dataClass.getField("enableBlock");
                Class<?> contentPacket = Class.forName("com.yuushya.modelling.network.ItemStackPacket", false, loader);
                contentPacketConstructor = contentPacket.getConstructor(BlockPos.class, int.class, ItemStack.class);
                Class<?> dataComponents = Class.forName(
                        "com.yuushya.modelling.registries.DataComponentRegistry", false, loader);
                itemBlockStateComponent = dataComponents.getField("BLOCKSTATE");
                supplierGet = java.util.function.Supplier.class.getMethod("get");
                itemGetComponent = ItemStack.class.getMethod("get", DataComponentType.class);
            } else {
                content = dataClass.getField("textLines");
                primaryFlag = dataClass.getField("isCulled");
                secondaryFlag = dataClass.getField("isMirror");
                Class<?> contentPacket = Class.forName("com.yuushya.modelling.network.TextLinesPacket", false, loader);
                sendContent = contentPacket.getMethod("sendToServerSide", BlockPos.class, int.class, List.class);
            }
            return new Bindings(blockEntityClass.getMethod("getTransformData"),
                    blockEntityClass.getMethod("getTransformData", int.class),
                    blockEntityClass.getMethod("getSlot"), blockEntityClass.getMethod("setSlot", int.class),
                    blockEntityClass.getMethod("getBlockPos"), blockEntityClass.getMethod("getBlockState"),
                    BlockState.class.getMethod("getValue", Property.class),
                    yuushyaBlockStatesClass.getField("SHAPES"), blockShapeClass.getField("voxelShape"),
                    dataClass.getField("pos"), dataClass.getField("rot"), dataClass.getField("scales"),
                    content, color, dataClass.getField("isShown"), primaryFlag, secondaryFlag,
                    packetClass.getMethod("sendToServerSide", BlockPos.class, int.class, typeClass, double.class),
                    packetClass.getMethod("sendToServerSideSuccess", BlockPos.class), sendContent,
                    contentPacketConstructor, itemBlockStateComponent, supplierGet, itemGetComponent,
                    Map.copyOf(types));
        }
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                    ? invocation.getCause() : exception;
            throw new IllegalStateException("Yuushya invocation failed: " + method, cause);
        }
    }

    private static Object read(Field field, Object target) {
        try { return field.get(target); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Yuushya field read failed", exception); }
    }

    private static Object readStatic(Field field) {
        try { return field.get(null); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Yuushya static field read failed", exception); }
    }

    private static void write(Field field, Object target, Object value) {
        try { field.set(target, value); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Yuushya field write failed", exception); }
    }

    private static boolean readBoolean(Field field, Object target) {
        try { return field.getBoolean(target); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Yuushya boolean read failed", exception); }
    }

    private static void writeBoolean(Field field, Object target, boolean value) {
        try { field.setBoolean(target, value); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Yuushya boolean write failed", exception); }
    }

    private static int readInt(Field field, Object target) {
        try { return field.getInt(target); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Yuushya int read failed", exception); }
    }

    private static void writeInt(Field field, Object target, int value) {
        try { field.setInt(target, value); }
        catch (IllegalAccessException exception) { throw new IllegalStateException("Yuushya int write failed", exception); }
    }

    private static <T> T cast(Object value, Class<T> type, String description) {
        if (!type.isInstance(value)) {
            throw new IllegalStateException(description + " has unexpected type: "
                    + (value == null ? "null" : value.getClass().getName()));
        }
        return type.cast(value);
    }
}
