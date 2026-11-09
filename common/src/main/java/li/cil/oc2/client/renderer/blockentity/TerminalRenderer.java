/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.Overlays;
import li.cil.oc2.client.renderer.TerminalOverlayRenderer;
import li.cil.oc2.common.block.OrientableBlock;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.vm.device.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

@Environment(EnvType.CLIENT)
public final class TerminalRenderer implements BlockEntityRenderer<TerminalBlockEntity, TerminalRenderer.State> {
    public TerminalRenderer(final BlockEntityRendererProvider.Context context) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final TerminalBlockEntity terminal, final State state, final float partialTicks, final Vec3 cameraPosition, @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(terminal, state, partialTicks, cameraPosition, breakProgress);

        final BlockState blockState = terminal.getBlockState();
        state.facing = blockState.getValue(OrientableBlock.FACING);
        state.rotationX = OrientableBlock.getRotationX(blockState);
        state.rotationY = OrientableBlock.getRotationY(blockState);
        state.cameraOffset = TerminalOverlayRenderer.getCameraOffset(cameraPosition, terminal.getBlockPos());
        state.terminal = terminal.getTerminal();
        state.isConnected = terminal.isConnected();
        state.hasRecentFrameError = terminal.hasRecentFrameError();
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        final double projectedCameraPosition = state.cameraOffset.dot(Vec3.atLowerCornerOf(state.facing.getUnitVec3i()));
        if (projectedCameraPosition <= 0 || state.terminal == null) {
            return;
        }

        stack.pushPose();

        // Align with front face of block.
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.rotate(Axis.YN.rotationDegrees(state.rotationY));
        stack.rotate(Axis.XN.rotationDegrees(state.rotationX));
        stack.translate(-0.5f, -0.5f, -0.5f);

        // Flip and align with top left corner.
        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);

        // Scale to make 1/16th of the block one unit and align with top left of terminal area.
        final float pixelScale = 1 / 16f;
        stack.scale(pixelScale, pixelScale, pixelScale);

        stack.translate(0, 0, -10f);
        TerminalOverlayRenderer.submitTerminal(state.terminal, stack, collector, state.cameraOffset);

        stack.translate(0, 0, -0.1f);

        if (state.isConnected) {
            Overlays.submitPower(stack, collector);
        }

        if (state.hasRecentFrameError) {
            Overlays.submitStatus(stack, collector);
        }

        stack.popPose();
    }

    // --------------------------------------------------------------------- //

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float rotationX, rotationY;
        public Vec3 cameraOffset = Vec3.ZERO;
        @Nullable public Terminal terminal;
        public boolean isConnected;
        public boolean hasRecentFrameError;
    }
}
