/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Not placed in the world; only provides the rotated models of the support brackets rendered for bus cables
 * resting against solid blocks.
 */
public final class BusCableSupportBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public BusCableSupportBlock() {
        super(RegistryUtils.blockProperties()
            .noCollision()
            .noOcclusion()
            .noLootTable());
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}
