/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.common.vm.device.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class TerminalOverlayRenderer {
    private static final double TERMINAL_CONTENT_DISTANCE = 6;

    /**
     * The offset of the camera relative to the center of the block at the specified position.
     */
    public static Vec3 getCameraOffset(final Vec3 cameraPosition, final BlockPos blockPos) {
        return cameraPosition.subtract(Vec3.atCenterOf(blockPos));
    }

    public static void submitTerminal(final Terminal terminal, final PoseStack stack, final SubmitNodeCollector collector, final Vec3 cameraOffset) {
        // Render terminal content if close enough.
        if (cameraOffset.lengthSqr() < TERMINAL_CONTENT_DISTANCE * TERMINAL_CONTENT_DISTANCE) {
            stack.pushPose();
            stack.translate(2, 2, -0.9f);

            // Scale to make terminal fit fully.
            final float textScaleX = 12f / terminal.getWidth();
            final float textScaleY = 7f / terminal.getHeight();
            final float scale = Math.min(textScaleX, textScaleY) * 0.95f;

            // Center it on both axes.
            final float scaleDeltaX = textScaleX - scale;
            final float scaleDeltaY = textScaleY - scale;
            stack.translate(
                terminal.getWidth() * scaleDeltaX * 0.5f,
                terminal.getHeight() * scaleDeltaY * 0.5f,
                0f);

            stack.scale(scale, scale, 1f);

            TerminalRenderer.submit(stack, collector, terminal);

            stack.popPose();
        } else {
            stack.pushPose();
            stack.translate(0, 0, -0.9f);

            Overlays.submitTerminal(stack, collector);

            stack.popPose();
        }
    }

    private TerminalOverlayRenderer() {
    }
}
