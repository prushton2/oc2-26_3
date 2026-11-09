/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import javax.annotation.Nullable;

/**
 * Converts text components to and from their JSON string representation.
 */
public final class ComponentJson {
    private static final Gson GSON = new Gson();

    public static String toJson(final Component component, final HolderLookup.Provider registries) {
        final JsonElement json = ComponentSerialization.CODEC
            .encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), component)
            .getOrThrow(JsonParseException::new);
        return GSON.toJson(json);
    }

    @Nullable
    public static Component fromJson(final String json, final HolderLookup.Provider registries) {
        final JsonElement element;
        try {
            element = JsonParser.parseString(json);
        } catch (final JsonParseException e) {
            return null;
        }
        return ComponentSerialization.CODEC
            .parse(registries.createSerializationContext(JsonOps.INSTANCE), element)
            .result()
            .orElse(null);
    }

    private ComponentJson() {
    }
}
