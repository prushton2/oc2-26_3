/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.InternetGatewayBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.inet.InternetConnection;
import li.cil.oc2.common.item.TooltipUtils;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class InternetGatewayBlock extends HorizontalDirectionalBlock implements EntityBlock, ItemTooltipProvider {
    private static final double PARTICLES_MIN = 3 / 16.0;
    private static final double PARTICLES_MAX = 13 / 16.0;

    // --------------------------------------------------------------------- //

    public InternetGatewayBlock() {
        super(RegistryUtils.blockProperties()
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    // --------------------------------------------------------------------- //

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag flag) {
        if (Config.internetGatewaysUseEnergy()) {
            TooltipUtils.addEnergyConsumption(0, InternetConnection.FRAME_QUEUE_SIZE * Config.internetGatewayEnergyPerPacket, tooltip);
        }
        if (!Config.internetEnabled) {
            tooltip.accept(Component.translatable(Constants.TOOLTIP_INTERNET_DISABLED)
                .withStyle(s -> s.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED))));
        }
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof final InternetGatewayBlockEntity gateway) || !gateway.isOperational()) {
            return;
        }

        level.addParticle(ParticleTypes.REVERSE_PORTAL,
            pos.getX() + Mth.lerp(random.nextDouble(), PARTICLES_MIN, PARTICLES_MAX),
            pos.getY() + 1.0,
            pos.getZ() + Mth.lerp(random.nextDouble(), PARTICLES_MIN, PARTICLES_MAX),
            0.0, 0.03, 0.0);
    }

    // --------------------------------------------------------------------- //
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.INTERNET_GATEWAY.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TickableBlockEntity.createServerTicker(level, type, BlockEntities.INTERNET_GATEWAY.get());
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}
