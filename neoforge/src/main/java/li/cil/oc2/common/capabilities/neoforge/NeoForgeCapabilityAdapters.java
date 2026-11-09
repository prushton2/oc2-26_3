/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.neoforge;

import li.cil.oc2.common.energy.EnergyHandler;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.inventory.DeferredNotifyItemHandler;
import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bridges the mod's simulate-flag based transfer interfaces and NeoForge's transactional transfer API.
 */
public final class NeoForgeCapabilityAdapters {
    @Nullable
    public static EnergyHandler energy(@Nullable final net.neoforged.neoforge.transfer.energy.EnergyHandler handler) {
        return handler == null ? null : new EnergyHandlerAdapter(handler);
    }

    @Nullable
    public static ItemHandler items(@Nullable final ResourceHandler<ItemResource> handler) {
        return handler == null ? null : new ItemHandlerAdapter(handler);
    }

    @Nullable
    public static FluidHandler fluids(@Nullable final ResourceHandler<FluidResource> handler) {
        return handler == null ? null : new FluidHandlerAdapter(handler);
    }

    /**
     * Fluid handler of the item in a slot of an inventory. Changes to the item, e.g. a bucket being emptied,
     * are written back to the inventory.
     */
    @Nullable
    public static FluidHandler fluids(final ItemHandler inventory, final int slot) {
        final ItemAccess access = ItemAccess.forHandlerIndex(toNeoForge(inventory), slot);
        return fluids(access.getCapability(Capabilities.Fluid.ITEM));
    }

    public static ResourceHandler<ItemResource> toNeoForge(final ItemHandler handler) {
        return new ReverseItemHandlerAdapter(handler);
    }

    public static net.neoforged.neoforge.transfer.energy.EnergyHandler toNeoForge(final EnergyHandler handler) {
        return new ReverseEnergyHandlerAdapter(handler);
    }

    public static ResourceHandler<FluidResource> toNeoForge(final FluidHandler handler) {
        return new ReverseFluidHandlerAdapter(handler);
    }

    // --------------------------------------------------------------------- //

    /**
     * Opens a transaction nested in the currently open one, if any, so adapters can be used from within
     * other transactions.
     */
    private static Transaction open() {
        final TransactionContext current = Transaction.getCurrentOpenedTransaction();
        return current != null ? Transaction.open(current) : Transaction.openRoot();
    }

    private static int clamp(final long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    private static FluidStack fromNeoForge(final FluidResource resource, final int amount) {
        return resource.isEmpty() || amount <= 0
            ? FluidStack.EMPTY
            : new FluidStack(resource.getFluid(), resource.getComponentsPatch(), amount);
    }

    private static FluidResource toNeoForge(final FluidStack stack) {
        return FluidResource.of(stack.fluid(), stack.components());
    }

    // --------------------------------------------------------------------- //

    private record EnergyHandlerAdapter(net.neoforged.neoforge.transfer.energy.EnergyHandler inner) implements EnergyHandler {
        @Override
        public long receiveEnergy(final long maxReceive, final boolean simulate) {
            if (maxReceive <= 0) {
                return 0;
            }
            try (Transaction transaction = open()) {
                final int received = inner.insert(clamp(maxReceive), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return received;
            }
        }

        @Override
        public long extractEnergy(final long maxExtract, final boolean simulate) {
            if (maxExtract <= 0) {
                return 0;
            }
            try (Transaction transaction = open()) {
                final int extracted = inner.extract(clamp(maxExtract), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return extracted;
            }
        }

        @Override
        public long getEnergyStored() {
            return inner.getAmountAsLong();
        }

        @Override
        public long getMaxEnergyStored() {
            return inner.getCapacityAsLong();
        }

        @Override
        public boolean canExtract() {
            return extractEnergy(1, true) > 0;
        }

        @Override
        public boolean canReceive() {
            return receiveEnergy(1, true) > 0;
        }
    }

    private record ReverseEnergyHandlerAdapter(EnergyHandler inner) implements net.neoforged.neoforge.transfer.energy.EnergyHandler {
        @Override
        public long getAmountAsLong() {
            return inner.getEnergyStored();
        }

        @Override
        public long getCapacityAsLong() {
            return inner.getMaxEnergyStored();
        }

        @Override
        public int insert(final int amount, final TransactionContext transaction) {
            return inner.canReceive() && amount > 0 ? new EnergyMovement(inner).insert(amount, transaction) : 0;
        }

        @Override
        public int extract(final int amount, final TransactionContext transaction) {
            return inner.canExtract() && amount > 0 ? new EnergyMovement(inner).extract(amount, transaction) : 0;
        }
    }

    /**
     * Applies energy changes immediately and reverts them if the transaction is aborted.
     */
    private static final class EnergyMovement extends SnapshotJournal<Long> {
        private final EnergyHandler inner;
        private long moved;

        private EnergyMovement(final EnergyHandler inner) {
            this.inner = inner;
        }

        int insert(final int amount, final TransactionContext transaction) {
            updateSnapshots(transaction);
            final int inserted = clamp(inner.receiveEnergy(amount, false));
            moved += inserted;
            return inserted;
        }

        int extract(final int amount, final TransactionContext transaction) {
            updateSnapshots(transaction);
            final int extracted = clamp(inner.extractEnergy(amount, false));
            moved -= extracted;
            return extracted;
        }

        @Override
        protected Long createSnapshot() {
            return moved;
        }

        @Override
        protected void revertToSnapshot(final Long snapshot) {
            final long delta = moved - snapshot;
            if (delta > 0) {
                inner.extractEnergy(delta, false);
            } else if (delta < 0) {
                inner.receiveEnergy(-delta, false);
            }
            moved = snapshot;
        }
    }

    // --------------------------------------------------------------------- //

    private record ItemHandlerAdapter(ResourceHandler<ItemResource> inner) implements ItemHandler {
        @Override
        public int getSlots() {
            return inner.size();
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            final ItemResource resource = inner.getResource(slot);
            return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(inner.getAmountAsInt(slot));
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            if (stack.isEmpty()) {
                return stack;
            }

            try (Transaction transaction = open()) {
                final int inserted = inner.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
            }
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            final ItemResource resource = inner.getResource(slot);
            if (resource.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            try (Transaction transaction = open()) {
                final int extracted = inner.extract(slot, resource, amount, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return extracted <= 0 ? ItemStack.EMPTY : resource.toStack(extracted);
            }
        }

        @Override
        public int getSlotLimit(final int slot) {
            return inner.getCapacityAsInt(slot, inner.getResource(slot));
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return inner.isValid(slot, ItemResource.of(stack));
        }
    }

    private static final class ReverseItemHandlerAdapter implements ResourceHandler<ItemResource> {
        private final ItemHandler inner;
        private final List<SlotJournal> slots = new ArrayList<>();

        private ReverseItemHandlerAdapter(final ItemHandler inner) {
            this.inner = inner;
            for (int slot = 0; slot < inner.getSlots(); slot++) {
                slots.add(new SlotJournal((DeferredNotifyItemHandler) inner, slot));
            }
        }

        @Override
        public int size() {
            return slots.size();
        }

        @Override
        public ItemResource getResource(final int index) {
            return ItemResource.of(inner.getStackInSlot(index));
        }

        @Override
        public long getAmountAsLong(final int index) {
            return inner.getStackInSlot(index).getCount();
        }

        @Override
        public long getCapacityAsLong(final int index, final ItemResource resource) {
            final int limit = inner.getSlotLimit(index);
            return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
        }

        @Override
        public boolean isValid(final int index, final ItemResource resource) {
            return resource.isEmpty() || inner.isItemValid(index, resource.toStack());
        }

        @Override
        public int insert(final int index, final ItemResource resource, final int amount, final TransactionContext transaction) {
            return slots.get(index).insert(resource, amount, transaction);
        }

        @Override
        public int extract(final int index, final ItemResource resource, final int amount, final TransactionContext transaction) {
            return slots.get(index).extract(resource, amount, transaction);
        }

        @Override
        public String toString() {
            return "ItemHandlerResourceHandler[" + inner + "]";
        }
    }

    /**
     * Modifies a single slot of an inventory without notifying it until the transaction is committed.
     */
    private static final class SlotJournal extends SnapshotJournal<ItemStack> {
        private final DeferredNotifyItemHandler handler;
        private final int slot;

        private SlotJournal(final DeferredNotifyItemHandler handler, final int slot) {
            this.handler = handler;
            this.slot = slot;
        }

        int insert(final ItemResource resource, final int amount, final TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }

            final int count = Math.min(amount, resource.getMaxStackSize());
            final int inserted = count - handler.insertItem(slot, resource.toStack(count), true).getCount();
            if (inserted <= 0) {
                return 0;
            }

            updateSnapshots(transaction);
            final ItemStack existing = handler.getStackInSlot(slot);
            handler.setStackInSlotSilently(slot, existing.isEmpty()
                ? resource.toStack(inserted)
                : existing.copyWithCount(existing.getCount() + inserted));
            return inserted;
        }

        int extract(final ItemResource resource, final int amount, final TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }

            final ItemStack existing = handler.getStackInSlot(slot);
            if (existing.isEmpty() || !resource.matches(existing)) {
                return 0;
            }

            final int extracted = handler.extractItem(slot, amount, true).getCount();
            if (extracted <= 0) {
                return 0;
            }

            updateSnapshots(transaction);
            handler.setStackInSlotSilently(slot, existing.getCount() > extracted
                ? existing.copyWithCount(existing.getCount() - extracted)
                : ItemStack.EMPTY);
            return extracted;
        }

        @Override
        protected ItemStack createSnapshot() {
            return handler.getStackInSlot(slot);
        }

        @Override
        protected void revertToSnapshot(final ItemStack snapshot) {
            handler.setStackInSlotSilently(slot, snapshot);
        }

        @Override
        protected void onRootCommit(final ItemStack originalState) {
            handler.notifySlotChanged(slot);
        }
    }

    // --------------------------------------------------------------------- //

    private record FluidHandlerAdapter(ResourceHandler<FluidResource> inner) implements FluidHandler {
        @Override
        public int getTanks() {
            return inner.size();
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            return fromNeoForge(inner.getResource(tank), inner.getAmountAsInt(tank));
        }

        @Override
        public int getTankCapacity(final int tank) {
            return inner.getCapacityAsInt(tank, inner.getResource(tank));
        }

        @Override
        public int fill(final FluidStack stack, final boolean simulate) {
            if (stack.isEmpty()) {
                return 0;
            }

            try (Transaction transaction = open()) {
                final int filled = inner.insert(toNeoForge(stack), stack.amount(), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return filled;
            }
        }

        @Override
        public FluidStack drain(final FluidStack stack, final boolean simulate) {
            if (stack.isEmpty()) {
                return FluidStack.EMPTY;
            }

            return drain(toNeoForge(stack), stack.amount(), simulate);
        }

        @Override
        public FluidStack drain(final int amount, final boolean simulate) {
            if (amount <= 0) {
                return FluidStack.EMPTY;
            }

            for (int tank = 0; tank < inner.size(); tank++) {
                final FluidResource resource = inner.getResource(tank);
                if (!resource.isEmpty() && inner.getAmountAsInt(tank) > 0) {
                    final FluidStack drained = drain(resource, amount, simulate);
                    if (!drained.isEmpty()) {
                        return drained;
                    }
                }
            }

            return FluidStack.EMPTY;
        }

        private FluidStack drain(final FluidResource resource, final int amount, final boolean simulate) {
            try (Transaction transaction = open()) {
                final int drained = inner.extract(resource, amount, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return fromNeoForge(resource, drained);
            }
        }
    }

    private record ReverseFluidHandlerAdapter(FluidHandler inner) implements ResourceHandler<FluidResource> {
        @Override
        public int size() {
            return inner.getTanks();
        }

        @Override
        public FluidResource getResource(final int index) {
            final FluidStack stack = inner.getFluidInTank(index);
            return stack.isEmpty() ? FluidResource.EMPTY : toNeoForge(stack);
        }

        @Override
        public long getAmountAsLong(final int index) {
            return inner.getFluidInTank(index).amount();
        }

        @Override
        public long getCapacityAsLong(final int index, final FluidResource resource) {
            return inner.getTankCapacity(index);
        }

        @Override
        public boolean isValid(final int index, final FluidResource resource) {
            return true;
        }

        @Override
        public int insert(final int index, final FluidResource resource, final int amount, final TransactionContext transaction) {
            return insert(resource, amount, transaction);
        }

        @Override
        public int insert(final FluidResource resource, final int amount, final TransactionContext transaction) {
            final FluidStack stack = fromNeoForge(resource, amount);
            return stack.isEmpty() ? 0 : new FluidMovement(inner, stack).fill(transaction);
        }

        @Override
        public int extract(final int index, final FluidResource resource, final int amount, final TransactionContext transaction) {
            return resource.equals(getResource(index)) ? extract(resource, amount, transaction) : 0;
        }

        @Override
        public int extract(final FluidResource resource, final int amount, final TransactionContext transaction) {
            final FluidStack stack = fromNeoForge(resource, amount);
            return stack.isEmpty() ? 0 : new FluidMovement(inner, stack).drain(transaction);
        }
    }

    /**
     * Applies fluid changes immediately and reverts them if the transaction is aborted.
     */
    private static final class FluidMovement extends SnapshotJournal<Integer> {
        private final FluidHandler inner;
        private final FluidStack stack;
        private int moved;

        private FluidMovement(final FluidHandler inner, final FluidStack stack) {
            this.inner = inner;
            this.stack = stack;
        }

        int fill(final TransactionContext transaction) {
            updateSnapshots(transaction);
            final int filled = inner.fill(stack, false);
            moved += filled;
            return filled;
        }

        int drain(final TransactionContext transaction) {
            updateSnapshots(transaction);
            final int drained = inner.drain(stack, false).amount();
            moved -= drained;
            return drained;
        }

        @Override
        protected Integer createSnapshot() {
            return moved;
        }

        @Override
        protected void revertToSnapshot(final Integer snapshot) {
            final int delta = moved - snapshot;
            if (delta > 0) {
                inner.drain(stack.withAmount(delta), false);
            } else if (delta < 0) {
                inner.fill(stack.withAmount(-delta), false);
            }
            moved = snapshot;
        }
    }

    private NeoForgeCapabilityAdapters() {
    }
}
