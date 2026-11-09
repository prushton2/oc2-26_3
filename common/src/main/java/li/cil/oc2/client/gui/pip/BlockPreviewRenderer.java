/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.client.gui.widget.Texture;
import li.cil.oc2.client.renderer.ModRenderType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Map;

/**
 * Renders {@link BlockPreviewRenderState}s. Needs to be registered with the platform's GUI renderer.
 */
@Environment(EnvType.CLIENT)
public final class BlockPreviewRenderer extends PictureInPictureRenderer<BlockPreviewRenderState> {
    /**
     * Distance of face overlays from the block's center, slightly outside the block to avoid z-fighting.
     */
    private static final float OVERLAY_OFFSET = 0.51f;

    // --------------------------------------------------------------------- //

    public static void extract(final GuiGraphicsExtractor graphics, final BlockPreviewRenderState state) {
        graphics.guiRenderState.addPicturesInPictureState(state);
    }

    // --------------------------------------------------------------------- //

    @Override
    public Class<BlockPreviewRenderState> getRenderStateClass() {
        return BlockPreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(final BlockPreviewRenderState state, final PoseStack poseStack, final SubmitNodeCollector collector) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);

        // The texture is set up with its y-axis pointing up, while the rotation is in GUI space, with y pointing
        // down. Mirroring through the xy-plane flips the sign of rotations around the x- and y-axes.
        final Vector3fc rotation = state.rotationDegrees();
        poseStack.rotate(new Quaternionf().rotationXYZ(
            (float) Math.toRadians(-rotation.x()),
            (float) Math.toRadians(-rotation.y()),
            (float) Math.toRadians(rotation.z())));
        poseStack.scale(1, -1, -1);

        state.block().submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);

        for (final Map.Entry<Direction, Texture[]> entry : state.faceOverlays().entrySet()) {
            submitFaceOverlays(poseStack, collector, entry.getKey(), entry.getValue());
        }
    }

    @Override
    protected float getTranslateY(final int height, final int guiScale) {
        return height / 2f;
    }

    @Override
    protected String getTextureLabel() {
        return "oc2_block_preview";
    }

    // --------------------------------------------------------------------- //

    private static void submitFaceOverlays(final PoseStack poseStack, final SubmitNodeCollector collector, final Direction side, final Texture[] textures) {
        poseStack.pushPose();

        poseStack.translate(-side.getStepX() * OVERLAY_OFFSET, side.getStepY() * OVERLAY_OFFSET, -side.getStepZ() * OVERLAY_OFFSET);

        final Vector3f sideRotation = switch (side) {
            case DOWN -> new Vector3f(-90, 0, 0);
            case UP -> new Vector3f(90, 0, 0);
            case NORTH -> new Vector3f(0, 180, 0);
            case SOUTH -> new Vector3f(0, 0, 0);
            case WEST -> new Vector3f(0, -90, 0);
            case EAST -> new Vector3f(0, 90, 0);
        };
        poseStack.rotate(new Quaternionf().rotationXYZ(
            (float) Math.toRadians(sideRotation.x()),
            (float) Math.toRadians(sideRotation.y()),
            (float) Math.toRadians(sideRotation.z())));

        poseStack.translate(-0.5, -0.5, 0);

        for (final Texture texture : textures) {
            collector.submitCustomGeometry(poseStack, ModRenderType.getOverlay(texture.location), (pose, consumer) -> {
                consumer.addVertex(pose, 0, 0, 0).setUv(0, 0);
                consumer.addVertex(pose, 0, 1, 0).setUv(0, 1);
                consumer.addVertex(pose, 1, 1, 0).setUv(1, 1);
                consumer.addVertex(pose, 1, 0, 0).setUv(1, 0);
            });
        }

        poseStack.popPose();
    }
}
