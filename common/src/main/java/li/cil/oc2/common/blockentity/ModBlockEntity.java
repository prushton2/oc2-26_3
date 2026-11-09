/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import com.google.common.annotations.VisibleForTesting;
import dev.architectury.utils.GameInstance;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.ServerScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public abstract class ModBlockEntity extends BlockEntity {
    private final Runnable onWorldUnloaded = this::onWorldUnloaded;
    private boolean needsWorldUnloadEvent;
    private boolean isUnloaded;
    private boolean isDestroyed;

    // --------------------------------------------------------------------- //

    protected ModBlockEntity(final BlockEntityType<?> blockEntityType, final BlockPos pos, final BlockState state) {
        super(blockEntityType, pos, state);
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public <T> T getCapability(final CapabilityType<T> capability, @Nullable final Direction side) {
        if (!isValid()) {
            return null;
        }

        final SingleResultCollector<T> collector = new SingleResultCollector<>(capability);
        collectCapabilities(collector, side);

        return collector.result;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();

        if (level == null) {
            return;
        }

        if (level.isClientSide()) {
            loadClient();
        } else {
            loadServer();

            ServerScheduler.schedule(level, () -> {
                if (isValid()) {
                    loadServerInLoadedLevel();
                }
            });

            if (needsWorldUnloadEvent) {
                ServerScheduler.scheduleOnUnload(level, onWorldUnloaded);
            }
        }
    }

    public void handleChunkUnloaded() {
        Capabilities.invalidate(this);
        onUnload(false);
        isUnloaded = true;
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        // Only called when the block is replaced by a different block, i.e. this block entity is being destroyed
        // (as opposed to being unloaded with its chunk). The new block state is already set at this point, which
        // tells us whether the block is just being moved by a piston, which does not count as being destroyed.
        if (level == null || !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.MOVING_PISTON)) {
            isDestroyed = true;
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected final void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        final CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries());
        output.store(NBTUtils.FLAT_COMPOUND, tag);
    }

    @Override
    protected final void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        loadAdditional(input.read(NBTUtils.FLAT_COMPOUND).orElseGet(CompoundTag::new), input.lookup());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        Capabilities.invalidate(this);
        if (!isUnloaded) {
            onUnload(isDestroyed);
        }
    }

    public boolean isValid() {
        return !isRemoved() && !isUnloaded;
    }

    @Nullable
    public AABB getExpandedRenderBoundingBox() {
        return null;
    }

    @VisibleForTesting
    void onWorldUnloaded() {
        Capabilities.invalidate(this);
        onUnload(false);
        isUnloaded = true;
    }

    // --------------------------------------------------------------------- //

    protected void onUnload(final boolean isDestroyed) {
        if (level != null && !level.isClientSide()) {
            unloadServer(isDestroyed);
            ServerScheduler.cancelOnUnload(level, onWorldUnloaded);
        }
    }

    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
    }

    /**
     * Registries to use when serializing data that references registry entries (e.g. item stacks).
     */
    protected HolderLookup.Provider registries() {
        if (level != null) {
            return level.registryAccess();
        }
        final MinecraftServer server = GameInstance.getServer();
        if (server != null) {
            return server.registryAccess();
        }
        throw new IllegalStateException("No registries available to serialize block entity data.");
    }

    protected void setNeedsLevelUnloadEvent() {
        needsWorldUnloadEvent = true;
    }

    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
    }

    protected void loadClient() {
    }

    protected void loadServer() {
    }

    protected void loadServerInLoadedLevel() {
    }

    protected void unloadServer(final boolean isDestroyed) {
    }

    // --------------------------------------------------------------------- //

    @FunctionalInterface
    protected interface CapabilityCollector {
        <T> void offer(CapabilityType<T> capability, T instance);
    }

    private static final class SingleResultCollector<T> implements CapabilityCollector {
        private final CapabilityType<T> capability;
        @Nullable
        private T result;

        private SingleResultCollector(final CapabilityType<T> capability) {
            this.capability = capability;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <TOffered> void offer(final CapabilityType<TOffered> offeredCapability, final TOffered instance) {
            if (result == null && offeredCapability == capability) {
                result = (T) instance;
            }
        }
    }
}
