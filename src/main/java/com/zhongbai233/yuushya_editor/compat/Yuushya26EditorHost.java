package com.zhongbai233.yuushya_editor.compat;

import com.zhongbai233.yuushya_editor.core.EditorTransform;
import com.zhongbai233.yuushya_editor.core.SceneDocument;
import com.zhongbai233.yuushya_editor.core.SceneLayer;
import com.zhongbai233.yuushya_editor.core.preview.CollisionShape;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;
import org.joml.Vector3f;

/** Reflective adapter for the audited Yuushya Modelling 26.1 show-block contract. */
public final class Yuushya26EditorHost implements YuushyaEditorHost<Object> {
    public static final String SCREEN_CLASS = "com.yuushya.modelling.gui.showblock.ShowBlockScreen";

    private final Object blockEntity;
    private final Bindings bindings;
    private final BlockPos blockPos;
    private final Map<UUID, Object> rawLayers = new LinkedHashMap<>();
    private final Map<UUID, Object> expectedHostData = new HashMap<>();
    private List<UUID> expectedLayerIds = List.of();

    private Yuushya26EditorHost(Object blockEntity, Bindings bindings) {
        this.blockEntity = Objects.requireNonNull(blockEntity, "blockEntity");
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.blockPos = cast(invoke(bindings.getBlockPos, blockEntity), BlockPos.class, "block position");
    }

    public static Yuushya26EditorHost fromOriginalScreen(Screen originalScreen) {
        Objects.requireNonNull(originalScreen, "originalScreen");
        if (!SCREEN_CLASS.equals(originalScreen.getClass().getName())) {
            throw new IllegalArgumentException("not a Yuushya show-block screen: " + originalScreen.getClass());
        }
        try {
            Field blockEntityField = originalScreen.getClass().getDeclaredField("blockEntity");
            if (!blockEntityField.trySetAccessible()) {
                throw new IllegalStateException("cannot access ShowBlockScreen.blockEntity");
            }
            Object blockEntity = blockEntityField.get(originalScreen);
            return new Yuushya26EditorHost(blockEntity,
                    Bindings.resolve(originalScreen.getClass().getClassLoader(), blockEntity.getClass()));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("cannot bind Yuushya Modelling 26.1 show-block screen", exception);
        }
    }

    @Override
    public SceneDocument<Object> loadDocument() {
        List<?> source = cast(invoke(bindings.getTransformData, blockEntity), List.class, "transform data");
        int selectedSlot = ((Number) invoke(bindings.getSlot, blockEntity)).intValue();
        CollisionShape collisionShape = readCollisionShape(invoke(bindings.getBlockState, blockEntity));
        String documentKey = blockPos.toShortString();
        List<SceneLayer<Object>> layers = new ArrayList<>(source.size());
        rawLayers.clear();
        expectedHostData.clear();
        for (int slot = 0; slot < source.size(); slot++) {
            Object raw = Objects.requireNonNull(source.get(slot), "transform data entry");
            UUID id = UUID.nameUUIDFromBytes((documentKey + ':' + slot).getBytes(StandardCharsets.UTF_8));
            Vector3d position = new Vector3d(cast(read(bindings.position, raw), Vector3d.class, "position"));
            Vector3f euler = new Vector3f(cast(read(bindings.rotation, raw), Vector3f.class, "rotation"));
            Vector3f scale = new Vector3f(cast(read(bindings.scale, raw), Vector3f.class, "scale"));
            Object blockState = Objects.requireNonNull(read(bindings.blockState, raw), "block state");
            boolean visible = readBoolean(bindings.visible, raw);
            EditorTransform transform = new EditorTransform(position,
                    YuushyaTransformConversion.fromEulerDegrees(euler), scale);
            layers.add(new SceneLayer<>(id, layerName(slot, blockState), blockState, transform, visible));
            rawLayers.put(id, raw);
            expectedHostData.put(id, blockState);
        }
        expectedLayerIds = layerIds(layers);
        UUID selected = selectedSlot >= 0 && selectedSlot < layers.size() ? layers.get(selectedSlot).id() : null;
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
        boolean reachedNewLayers = false;
        for (int slot = 0; slot < draft.layers().size(); slot++) {
            SceneLayer<Object> layer = draft.layers().get(slot);
            if (!(layer.hostData() instanceof BlockState)) {
                return ValidationResult.rejected("screen.yuushya_modelling_enhanced_editor.validation.layer_block", slot + 1);
            }
            Integer originalSlot = originalSlots.get(layer.id());
            if (originalSlot == null) {
                reachedNewLayers = true;
                continue;
            }
            if (reachedNewLayers || originalSlot <= lastOriginalSlot) {
                return ValidationResult.rejected("screen.yuushya_modelling_enhanced_editor.validation.order");
            }
            lastOriginalSlot = originalSlot;
            if (!Objects.equals(expectedHostData.get(layer.id()), layer.hostData())) {
                return ValidationResult.rejected("screen.yuushya_modelling_enhanced_editor.validation.replace");
            }
        }
        return ValidationResult.ok();
    }

    private static List<UUID> layerIds(List<SceneLayer<Object>> layers) {
        List<UUID> ids = new ArrayList<>(layers.size());
        for (SceneLayer<Object> layer : layers) ids.add(layer.id());
        return List.copyOf(ids);
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

        java.util.Set<UUID> retainedIds = new java.util.HashSet<>();
        draft.layers().forEach(layer -> retainedIds.add(layer.id()));
        List<?> clientLayersView = cast(invoke(bindings.getTransformData, blockEntity), List.class,
                "transform data");
        @SuppressWarnings("unchecked")
        List<Object> clientLayers = (List<Object>) clientLayersView;
        for (int slot = original.layers().size() - 1; slot >= 0; slot--) {
            SceneLayer<Object> removed = original.layers().get(slot);
            if (retainedIds.contains(removed.id())) continue;
            send(slot, "REMOVE", 0.0D);
            clientLayers.remove(slot);
            rawLayers.remove(removed.id());
        }

        for (int slot = 0; slot < draft.layers().size(); slot++) {
            SceneLayer<Object> after = draft.layers().get(slot);
            SceneLayer<Object> before = originals.get(after.id());
            if (before == null) {
                submitNewLayer(slot, after);
            } else {
                Object raw = Objects.requireNonNull(rawLayers.get(after.id()), "raw layer");
                submitLayerDiff(slot, raw, before, after);
            }
        }
        if (!Objects.equals(original.collisionShape(), draft.collisionShape())) {
            int shapeSlot = Math.max(0, ((Number) invoke(bindings.getSlot, blockEntity)).intValue());
            send(shapeSlot, "SHAPE", draft.collisionShape().kind().ordinal());
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
        invoke(bindings.sendSuccess, null, blockPos);
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

    private void submitNewLayer(int slot, SceneLayer<Object> layer) {
        BlockState blockState = cast(layer.hostData(), BlockState.class, "new layer block state");
        // Yuushya's own ShowBlockScreen grows a slot by selecting index == size before it sends
        // BLOCK_STATE. Calling setSlot here reproduces the same client-side list semantics.
        invoke(bindings.setSlot, blockEntity, slot);
        Object raw = invoke(bindings.getTransformDataAt, blockEntity, slot);
        rawLayers.put(layer.id(), raw);

        EditorTransform transform = layer.transform();
        Vector3d position = transform.position();
        Vector3f rotation = YuushyaTransformConversion.toEulerDegrees(transform.rotation());
        Vector3f scale = transform.scale();
        send(slot, "BLOCK_STATE", Block.getId(blockState));
        send(slot, "POS_X", position.x);
        send(slot, "POS_Y", position.y);
        send(slot, "POS_Z", position.z);
        send(slot, "ROT_X", rotation.x);
        send(slot, "ROT_Y", rotation.y);
        send(slot, "ROT_Z", rotation.z);
        send(slot, "SCALE_X", scale.x);
        send(slot, "SCALE_Y", scale.y);
        send(slot, "SCALE_Z", scale.z);
        send(slot, "SHOWN", layer.visible() ? 1.0D : 0.0D);

        cast(read(bindings.position, raw), Vector3d.class, "position").set(position);
        cast(read(bindings.rotation, raw), Vector3f.class, "rotation").set(rotation);
        cast(read(bindings.scale, raw), Vector3f.class, "scale").set(scale);
        write(bindings.blockState, raw, blockState);
        writeBoolean(bindings.visible, raw, layer.visible());
    }

    private void submitLayerDiff(int slot, Object raw, SceneLayer<Object> before, SceneLayer<Object> after) {
        EditorTransform oldTransform = before.transform();
        EditorTransform newTransform = after.transform();
        Vector3d oldPosition = oldTransform.position();
        Vector3d newPosition = newTransform.position();
        Vector3f oldScale = oldTransform.scale();
        Vector3f newScale = newTransform.scale();
        Vector3f newEuler = YuushyaTransformConversion.toEulerDegrees(newTransform.rotation());

        Vector3d rawPosition = cast(read(bindings.position, raw), Vector3d.class, "position");
        Vector3f rawRotation = cast(read(bindings.rotation, raw), Vector3f.class, "rotation");
        Vector3f rawScale = cast(read(bindings.scale, raw), Vector3f.class, "scale");

        if (Double.compare(oldPosition.x, newPosition.x) != 0) send(slot, "POS_X", newPosition.x);
        if (Double.compare(oldPosition.y, newPosition.y) != 0) send(slot, "POS_Y", newPosition.y);
        if (Double.compare(oldPosition.z, newPosition.z) != 0) send(slot, "POS_Z", newPosition.z);
        if (!oldTransform.rotation().equals(newTransform.rotation())) {
            send(slot, "ROT_X", newEuler.x);
            send(slot, "ROT_Y", newEuler.y);
            send(slot, "ROT_Z", newEuler.z);
        }
        if (Float.compare(oldScale.x, newScale.x) != 0) send(slot, "SCALE_X", newScale.x);
        if (Float.compare(oldScale.y, newScale.y) != 0) send(slot, "SCALE_Y", newScale.y);
        if (Float.compare(oldScale.z, newScale.z) != 0) send(slot, "SCALE_Z", newScale.z);
        if (before.visible() != after.visible()) send(slot, "SHOWN", after.visible() ? 1.0D : 0.0D);

        rawPosition.set(newPosition);
        rawRotation.set(newEuler);
        rawScale.set(newScale);
        writeBoolean(bindings.visible, raw, after.visible());
    }

    private void send(int slot, String typeName, double value) {
        Object type = Objects.requireNonNull(bindings.transformTypes.get(typeName), "transform type " + typeName);
        invoke(bindings.sendValue, null, blockPos, slot, type, value);
    }

    private static String layerName(int slot, Object blockState) {
        String value = String.valueOf(blockState);
        if (value.length() > 42) value = value.substring(0, 39) + "...";
        return (slot + 1) + "  " + value;
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
        try {
            return field.get(target);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Yuushya field read failed: " + field, exception);
        }
    }

    private static Object readStatic(Field field) {
        try {
            return field.get(null);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Yuushya static field read failed: " + field, exception);
        }
    }

    private static void writeBoolean(Field field, Object target, boolean value) {
        try {
            field.setBoolean(target, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Yuushya field write failed: " + field, exception);
        }
    }

    private static void write(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Yuushya field write failed: " + field, exception);
        }
    }

    private static boolean readBoolean(Field field, Object target) {
        try {
            return field.getBoolean(target);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Yuushya field read failed: " + field, exception);
        }
    }

    private static <T> T cast(Object value, Class<T> type, String description) {
        if (!type.isInstance(value)) {
            throw new IllegalStateException(description + " has unexpected type: "
                    + (value == null ? "null" : value.getClass().getName()));
        }
        return type.cast(value);
    }

    private record Bindings(Method getTransformData, Method getTransformDataAt, Method getSlot, Method setSlot,
            Method getBlockPos, Method getBlockState, Method blockStateGetValue,
            Field shapeProperty, Field shapeVoxelShape,
            Field position, Field rotation, Field scale, Field blockState, Field visible,
            Method sendValue, Method sendSuccess, Map<String, Object> transformTypes) {
        private static Bindings resolve(ClassLoader loader, Class<?> blockEntityClass)
                throws ReflectiveOperationException {
            Class<?> transformDataClass = Class.forName(
                    "com.yuushya.modelling.blockentity.transformData.TransformBlockData", false, loader);
            Class<?> transformTypeClass = Class.forName(
                    "com.yuushya.modelling.blockentity.transformData.TransformType", false, loader);
            Class<?> yuushyaBlockStatesClass = Class.forName(
                    "com.yuushya.modelling.block.blockstate.YuushyaBlockStates", false, loader);
            Class<?> blockShapeClass = Class.forName(
                    "com.yuushya.modelling.blockentity.BlockShape", false, loader);
            Class<?> packetClass = Class.forName(
                    "com.yuushya.modelling.network.TransformDataOncePacket", false, loader);
            Map<String, Object> types = new HashMap<>();
            for (Object value : transformTypeClass.getEnumConstants()) {
                types.put(((Enum<?>) value).name(), value);
            }
            return new Bindings(
                    blockEntityClass.getMethod("getTransformData"),
                    blockEntityClass.getMethod("getTransformData", int.class),
                    blockEntityClass.getMethod("getSlot"),
                    blockEntityClass.getMethod("setSlot", int.class),
                    blockEntityClass.getMethod("getBlockPos"),
                    blockEntityClass.getMethod("getBlockState"),
                    BlockState.class.getMethod("getValue", Property.class),
                    yuushyaBlockStatesClass.getField("SHAPES"),
                    blockShapeClass.getField("voxelShape"),
                    transformDataClass.getField("pos"),
                    transformDataClass.getField("rot"),
                    transformDataClass.getField("scales"),
                    transformDataClass.getField("blockState"),
                    transformDataClass.getField("isShown"),
                    packetClass.getMethod("sendToServerSide", BlockPos.class, int.class,
                            transformTypeClass, double.class),
                    packetClass.getMethod("sendToServerSideSuccess", BlockPos.class),
                    Map.copyOf(types));
        }
    }
}
