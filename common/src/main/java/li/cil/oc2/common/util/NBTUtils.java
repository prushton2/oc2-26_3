/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import li.cil.oc2.common.container.ItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public final class NBTUtils {
    /**
     * Reads and writes all entries of the map being decoded/encoded as one {@link CompoundTag}.
     * <p>
     * Used to bridge {@link net.minecraft.world.level.storage.ValueInput} and
     * {@link net.minecraft.world.level.storage.ValueOutput} to code that works on a flat {@link CompoundTag}
     * without changing the serialized layout.
     */
    public static final MapCodec<CompoundTag> FLAT_COMPOUND = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(final DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<CompoundTag> decode(final DynamicOps<T> ops, final MapLike<T> input) {
            final CompoundTag tag = new CompoundTag();
            input.entries().forEach(entry -> ops.getStringValue(entry.getFirst()).ifSuccess(key ->
                tag.put(key, ops.convertTo(NbtOps.INSTANCE, entry.getSecond()))));
            return DataResult.success(tag);
        }

        @Override
        public <T> RecordBuilder<T> encode(final CompoundTag input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
            for (final String key : input.keySet()) {
                final Tag value = input.get(key);
                if (value != null) {
                    prefix.add(key, NbtOps.INSTANCE.convertTo(ops, value));
                }
            }
            return prefix;
        }
    };

    public static Tag writeBlockPos(final BlockPos pos) {
        return BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow();
    }

    public static Optional<BlockPos> readBlockPos(final CompoundTag tag, final String key) {
        return tag.read(key, BlockPos.CODEC);
    }

    public static Tag saveItemStack(final HolderLookup.Provider registries, final ItemStack stack) {
        return ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }

    public static ItemStack parseItemStack(final HolderLookup.Provider registries, final Tag tag) {
        return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
            .resultOrPartial()
            .orElse(ItemStack.EMPTY);
    }

    public static <T extends Enum<T>> void putEnum(final CompoundTag compound, final String key, @Nullable final Enum<T> value) {
        if (value != null) {
            compound.putInt(key, value.ordinal());
        }
    }

    @Nullable
    public static <T extends Enum<T>> T getEnum(final CompoundTag compound, final String key, final Class<T> enumType) {
        if (!compound.contains(key)) {
            return null;
        }

        final int ordinal = compound.getIntOr(key, 0);
        try {
            return enumType.getEnumConstants()[ordinal];
        } catch (final IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    public static CompoundTag getChildTag(@Nullable final CompoundTag tag, final String... path) {
        if (tag == null) {
            return new CompoundTag();
        }

        CompoundTag childTag = tag;
        for (final String tagName : path) {
            if (!childTag.contains(tagName)) {
                return new CompoundTag();
            }
            childTag = childTag.getCompoundOrEmpty(tagName);
        }

        return childTag;
    }

    public static CompoundTag getOrCreateChildTag(final CompoundTag tag, final String... path) {
        CompoundTag childTag = tag;
        for (final String tagName : path) {
            if (!childTag.contains(tagName)) {
                childTag.put(tagName, new CompoundTag());
            }
            childTag = childTag.getCompoundOrEmpty(tagName);
        }
        return childTag;
    }

    public static CompoundTag makeInventoryTag(final HolderLookup.Provider provider, final ItemStack... items) {
        return new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, items)).serializeNBT(provider);
    }
}
