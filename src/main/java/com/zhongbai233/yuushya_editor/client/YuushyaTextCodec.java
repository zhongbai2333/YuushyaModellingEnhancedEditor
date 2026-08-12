package com.zhongbai233.yuushya_editor.client;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;

/** Converts Yuushya's serialized component fragments without linking to Yuushya internals. */
public final class YuushyaTextCodec {
    private YuushyaTextCodec() { }

    public static Component component(List<String> jsonLines) {
        Component result = Component.empty();
        for (String json : jsonLines) result = result.copy().append(component(json));
        return result;
    }

    public static String plainText(List<String> jsonLines) {
        List<String> values = new ArrayList<>(jsonLines.size());
        for (String json : jsonLines) values.add(component(json).getString());
        return String.join("\n", values);
    }

    public static List<String> encodePlainLines(String value) {
        String[] lines = value.split("\\R", -1);
        List<String> result = new ArrayList<>(lines.length);
        for (String line : lines) result.add(new JsonPrimitive(line).toString());
        return List.copyOf(result);
    }

    private static Component component(String json) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) return Component.literal(json);
            RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE,
                    minecraft.level.registryAccess());
            return ComponentSerialization.CODEC.parse(ops, JsonParser.parseString(json))
                    .result().orElseGet(() -> Component.literal(json));
        } catch (RuntimeException ignored) {
            return Component.literal(json);
        }
    }
}
