/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;


import javax.annotation.Nullable;
import java.util.function.IntSupplier;

public final class MemoryItem extends AbstractStorageItem {
    @Nullable

    // --------------------------------------------------------------------- //

    public MemoryItem(final IntSupplier defaultCapacity) {
        super(createProperties().overrideDescription("item.oc2.memory"), defaultCapacity);
    }

    // --------------------------------------------------------------------- //

}
