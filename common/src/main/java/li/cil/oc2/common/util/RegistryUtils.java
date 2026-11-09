/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrarBuilder;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.api.API;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

public abstract class RegistryUtils {
    private enum Phase {
        PRE_INIT,
        INIT,
        POST_INIT,
    }

    private static final List<DeferredRegister<?>> ENTRIES = new ArrayList<>();
    private static final ThreadLocal<Deque<ResourceKey<?>>> CONSTRUCTING = ThreadLocal.withInitial(ArrayDeque::new);
    private static Phase phase = Phase.PRE_INIT;

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> RegistrarBuilder<T> builder(final ResourceKey<Registry<T>> key, final T... typeGetter) {
        if (phase != Phase.INIT) throw new IllegalStateException();

        return RegistrarManager.get(API.MOD_ID).builder(key.identifier(), typeGetter);
    }

    public static <T> DeferredRegister<T> getInitializerFor(final ResourceKey<Registry<T>> key) {
        if (phase != Phase.INIT) throw new IllegalStateException();

        final DeferredRegister<T> entry = DeferredRegister.create(API.MOD_ID, key);
        ENTRIES.add(entry);
        return entry;
    }

    /**
     * Registers an entry whose factory needs to know its own registry key while being constructed.
     * <p>
     * Blocks and items must have their id set on their properties before construction. The factory
     * passed here can obtain that id via {@link #constructingKey(ResourceKey)}, or more conveniently
     * {@link #itemProperties()} and {@link #blockProperties()}.
     */
    public static <T, R extends T> RegistrySupplier<R> register(final DeferredRegister<T> register, final String name, final Supplier<R> factory) {
        final ResourceKey<? extends Registry<T>> registryKey = register.getRegistrar().key();
        final ResourceKey<T> key = ResourceKey.create(registryKey, Identifier.fromNamespaceAndPath(API.MOD_ID, name));
        return register.register(name, () -> {
            final Deque<ResourceKey<?>> stack = CONSTRUCTING.get();
            stack.push(key);
            try {
                return factory.get();
            } finally {
                stack.pop();
            }
        });
    }

    /**
     * The key of the registry entry currently being constructed via {@link #register(DeferredRegister, String, Supplier)}.
     */
    @SuppressWarnings("unchecked")
    public static <T> ResourceKey<T> constructingKey(final ResourceKey<? extends Registry<T>> registryKey) {
        final ResourceKey<?> key = CONSTRUCTING.get().peek();
        if (key == null || !key.isFor(registryKey)) {
            throw new IllegalStateException("Not currently constructing an entry for registry [" + registryKey.identifier() + "].");
        }
        return (ResourceKey<T>) key;
    }

    public static Item.Properties itemProperties() {
        return new Item.Properties().setId(constructingKey(Registries.ITEM));
    }

    public static BlockBehaviour.Properties blockProperties() {
        return BlockBehaviour.Properties.of().setId(constructingKey(Registries.BLOCK));
    }

    public static void begin() {
        if (phase != Phase.PRE_INIT) throw new IllegalStateException();
        phase = Phase.INIT;
    }

    public static void finish() {
        if (phase != Phase.INIT) throw new IllegalStateException();
        phase = Phase.POST_INIT;

        for (final DeferredRegister<?> register : ENTRIES) {
            register.register();
        }

        ENTRIES.clear();
    }

    private RegistryUtils() {
    }
}
