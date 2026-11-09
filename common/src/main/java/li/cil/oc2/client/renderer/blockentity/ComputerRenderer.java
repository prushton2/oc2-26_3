/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.Overlays;
import li.cil.oc2.client.renderer.TerminalOverlayRenderer;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.VirtualMachine;
import li.cil.oc2.common.vm.device.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ComputerRenderer implements BlockEntityRenderer<ComputerBlockEntity, ComputerRenderer.State> {
    private static final int ERROR_TEXT_COLOR = 0xFFEE3322;
    private static final int ERROR_TEXT_MAX_WIDTH = 100;
    private static final double ERROR_TEXT_DISTANCE = 12;

    public ComputerRenderer(final BlockEntityRendererProvider.Context context) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final ComputerBlockEntity computer, final State state, final float partialTicks, final Vec3 cameraPosition, @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(computer, state, partialTicks, cameraPosition, breakProgress);

        final VirtualMachine virtualMachine = computer.getVirtualMachine();
        state.facing = computer.getBlockState().getValue(ComputerBlock.FACING);
        state.cameraOffset = TerminalOverlayRenderer.getCameraOffset(cameraPosition, computer.getBlockPos());
        state.terminal = computer.getTerminal();
        state.isRunning = virtualMachine.isRunning();
        state.busState = virtualMachine.getBusState();
        state.runState = virtualMachine.getRunState();
        state.error = virtualMachine.getError();
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        // If viewer is not in front of the block we can skip the rest, it cannot be visible.
        // We check against the center of the block instead of the actual relevant face for simplicity.
        final double projectedCameraPosition = state.cameraOffset.dot(Vec3.atLowerCornerOf(state.facing.getUnitVec3i()));
        if (projectedCameraPosition <= 0) {
            return;
        }

        stack.pushPose();

        // Align with front face of block.
        stack.translate(0.5f, 0, 0.5f);
        stack.rotate(Axis.YN.rotationDegrees(state.facing.toYRot() + 180));
        stack.translate(-0.5f, 0, -0.5f);

        // Flip and align with top left corner.
        stack.translate(1, 1, 0);
        stack.scale(-1, -1, -1);

        // Scale to make 1/16th of the block one unit and align with top left of terminal area.
        final float pixelScale = 1 / 16f;
        stack.scale(pixelScale, pixelScale, pixelScale);

        if (state.isRunning) {
            TerminalOverlayRenderer.submitTerminal(state.terminal, stack, collector, state.cameraOffset);
        } else {
            submitStatusText(state, stack, collector);
        }

        stack.translate(0, 0, -0.1f);

        switch (state.busState) {
            case SCAN_PENDING:
            case INCOMPLETE:
                Overlays.submitStatus(stack, collector);
                break;
            case TOO_COMPLEX:
                Overlays.submitStatus(stack, collector, 1000);
                break;
            case MULTIPLE_CONTROLLERS:
                Overlays.submitStatus(stack, collector, 250);
                break;
            case READY:
                switch (state.runState) {
                    case STOPPED:
                        break;
                    case LOADING_DEVICES:
                        Overlays.submitStatus(stack, collector);
                        break;
                    case RUNNING:
                        Overlays.submitPower(stack, collector);
                        break;
                }
                break;
        }

        stack.popPose();
    }

    // --------------------------------------------------------------------- //

    private static void submitStatusText(final State state, final PoseStack stack, final SubmitNodeCollector collector) {
        if (state.error == null || state.cameraOffset.lengthSqr() >= ERROR_TEXT_DISTANCE * ERROR_TEXT_DISTANCE) {
            return;
        }

        stack.pushPose();
        stack.translate(3, 3, -0.9f);

        final float textScale = 10f / ERROR_TEXT_MAX_WIDTH;
        stack.scale(textScale, textScale, textScale);

        final Font font = Minecraft.getInstance().font;
        final List<FormattedText> wrappedText = font.getSplitter().splitLines(state.error, ERROR_TEXT_MAX_WIDTH, Style.EMPTY);
        if (wrappedText.size() == 1) {
            final int textWidth = font.width(state.error);
            submitTextLine(stack, collector, state.error.getVisualOrderText(), (ERROR_TEXT_MAX_WIDTH - textWidth) * 0.5f, 0);
        } else {
            for (int i = 0; i < wrappedText.size(); i++) {
                submitTextLine(stack, collector, Component.literal(wrappedText.get(i).getString()).getVisualOrderText(), 0, i * font.lineHeight);
            }
        }

        stack.popPose();
    }

    private static void submitTextLine(final PoseStack stack, final SubmitNodeCollector collector, final FormattedCharSequence text, final float x, final float y) {
        collector.submitText(stack, x, y, text, false, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, ERROR_TEXT_COLOR, 0, 0);
    }

    // --------------------------------------------------------------------- //

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public Vec3 cameraOffset = Vec3.ZERO;
        @Nullable public Terminal terminal;
        public boolean isRunning;
        public CommonDeviceBusController.BusState busState = CommonDeviceBusController.BusState.SCAN_PENDING;
        public VMRunState runState = VMRunState.STOPPED;
        @Nullable public Component error;
    }
}
