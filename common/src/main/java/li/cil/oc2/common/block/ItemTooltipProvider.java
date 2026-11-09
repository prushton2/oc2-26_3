/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Implemented by blocks that want to add tooltip lines to their item form.
 * <p>
 * Blocks no longer receive tooltip callbacks themselves; {@link li.cil.oc2.common.item.ModBlockItem}
 * forwards to blocks implementing this interface.
 */
public interface ItemTooltipProvider {
    void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag);
}
