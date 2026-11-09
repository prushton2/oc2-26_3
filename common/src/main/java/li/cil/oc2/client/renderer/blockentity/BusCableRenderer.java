/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.block.BusCableSupportBlock;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.common.util.ItemStackUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Renders the parts of bus cables that depend on more than their block state:
 * <ul>
 *     <li>the block used as a facade, if any;</li>
 *     <li>the support bracket connecting the cable to an adjacent solid block, if any;</li>
 *     <li>the name of the bus interface the player is looking at while holding a wrench.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class BusCableRenderer implements BlockEntityRenderer<BusCableBlockEntity, BusCableRenderer.State> {
    private static final double LABEL_HOVER = 0.5;
    // The name tag renderer moves tags up by this much on its own.
    private static final double NAME_TAG_OFFSET = 0.5;

    // --------------------------------------------------------------------- //

    public BusCableRenderer(final BlockEntityRendererProvider.Context ignored) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final BusCableBlockEntity busCable, final State state, final float partialTicks, final Vec3 cameraPosition, @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(busCable, state, partialTicks, cameraPosition, breakProgress);

        state.name = null;
        state.facade = null;
        state.support = null;

        final BlockPos blockPos = busCable.getBlockPos();
        if (busCable.getLevel() instanceof final ClientLevel level) {
            final BlockState blockState = busCable.getBlockState();
            if (blockState.getValue(BusCableBlock.HAS_FACADE)) {
                BlockState facadeState = ItemStackUtils.getBlockState(busCable.getFacade());
                if (facadeState == null) {
                    facadeState = Blocks.IRON_BLOCK.defaultBlockState();
                }
                state.facade = createMovingBlock(level, blockPos, facadeState);
            } else {
                final Direction supportSide = BusCableBlock.getSupportSide(level, blockPos, blockState);
                if (supportSide != null) {
                    state.support = createMovingBlock(level, blockPos, li.cil.oc2.common.block.Blocks.BUS_CABLE_SUPPORT.get()
                        .defaultBlockState().setValue(BusCableSupportBlock.FACING, supportSide));
                }
            }
        }

        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;
        if (player == null || !Wrenches.isHoldingWrench(player)) {
            return;
        }

        if (!(mc.hitResult instanceof final BlockHitResult hit) || !blockPos.equals(hit.getBlockPos())) {
            return;
        }

        final Direction side = BusCableBlock.getHitSide(blockPos, hit);
        if (BusCableBlock.getConnectionType(busCable.getBlockState(), side) != BusCableBlock.ConnectionType.INTERFACE) {
            return;
        }

        final String name = busCable.getInterfaceName(side);
        if (name.isEmpty()) {
            return;
        }

        state.name = Component.literal(name);
        state.attachment = new Vec3(
            0.5 + side.getStepX() * 0.5,
            0.5 + side.getStepY() * 0.5 + (side == Direction.DOWN ? -LABEL_HOVER : LABEL_HOVER) - NAME_TAG_OFFSET,
            0.5 + side.getStepZ() * 0.5);
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        if (state.facade != null) {
            collector.submitMovingBlock(stack, state.facade, 0);
        }

        if (state.support != null) {
            collector.submitMovingBlock(stack, state.support, 0);
        }

        if (state.name != null) {
            collector.submitNameTag(stack, state.attachment, 0, state.name, true, LightCoordsUtil.FULL_BRIGHT, camera);
        }
    }

    // --------------------------------------------------------------------- //

    private static MovingBlockRenderState createMovingBlock(final ClientLevel level, final BlockPos pos, final BlockState blockState) {
        final MovingBlockRenderState renderState = new MovingBlockRenderState();
        renderState.randomSeedPos = pos;
        renderState.blockPos = pos;
        renderState.blockState = blockState;
        renderState.biome = level.getBiome(pos);
        renderState.cardinalLighting = level.cardinalLighting();
        renderState.lightEngine = level.getLightEngine();
        return renderState;
    }

    // --------------------------------------------------------------------- //

    public static final class State extends BlockEntityRenderState {
        @Nullable public MovingBlockRenderState facade;
        @Nullable public MovingBlockRenderState support;
        @Nullable public Component name;
        public Vec3 attachment = Vec3.ZERO;
    }
}
