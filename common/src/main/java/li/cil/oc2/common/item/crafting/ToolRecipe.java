/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item.crafting;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.StorageItemUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A shapeless recipe that returns tools (wrenches, CPUs) used in it instead of consuming them.
 */
public final class ToolRecipe extends NormalCraftingRecipe {
    public static final MapCodec<ToolRecipe> MAP_CODEC = ShapelessRecipe.MAP_CODEC.xmap(ToolRecipe::new, recipe -> recipe.recipe);
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolRecipe> STREAM_CODEC = ShapelessRecipe.STREAM_CODEC.map(ToolRecipe::new, recipe -> recipe.recipe);

    // --------------------------------------------------------------------- //

    private final ShapelessRecipe recipe;

    // --------------------------------------------------------------------- //

    public ToolRecipe(final ShapelessRecipe recipe) {
        super(new Recipe.CommonInfo(recipe.showNotification()), new CraftingRecipe.CraftingBookInfo(recipe.category(), recipe.group()));
        this.recipe = recipe;
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean matches(final CraftingInput input, final Level level) {
        if (!recipe.matches(input, level)) {
            return false;
        }

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (StorageItemUtils.needsRepair(stack)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(final CraftingInput input) {
        return recipe.assemble(input);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
        final NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            final ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
            if (remainder != null) {
                result.set(slot, remainder.create());
            } else if (isTool(stack)) {
                final ItemStack copy = stack.copy();
                copy.setCount(1);
                result.set(slot, copy);
            } else {
                // Eager cleanup of blob storage; we convert to something new, so
                // the old data is unreachable anyway.
                StorageItemUtils.clearBlobData(stack);
            }
        }

        return result;
    }

    @Override
    public List<RecipeDisplay> display() {
        return recipe.display();
    }

    @Override
    public RecipeSerializer<ToolRecipe> getSerializer() {
        return RecipeSerializers.TOOL.get();
    }

    // --------------------------------------------------------------------- //

    @Override
    protected PlacementInfo createPlacementInfo() {
        return recipe.placementInfo();
    }

    // --------------------------------------------------------------------- //

    private static boolean isTool(final ItemStack stack) {
        return Wrenches.isWrench(stack) || stack.is(ItemTags.DEVICES_CPU);
    }
}
