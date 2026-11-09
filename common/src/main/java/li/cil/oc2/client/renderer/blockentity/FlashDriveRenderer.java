/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.common.block.OrientableBlock;
import li.cil.oc2.common.blockentity.FlashDriveBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

@Environment(EnvType.CLIENT)
public final class FlashDriveRenderer implements BlockEntityRenderer<FlashDriveBlockEntity, FlashDriveRenderer.State> {
    private static final float FLASH_MEMORY_Y = 10.5f / 16f;
    private static final float FLASH_MEMORY_Z = 9.0f / 16f;
    private static final float FLASH_MEMORY_SCALE = 6.0f / 16f;

    // --------------------------------------------------------------------- //

    private final ItemModelResolver itemModelResolver;

    // --------------------------------------------------------------------- //

    public FlashDriveRenderer(final BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    // --------------------------------------------------------------------- //

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final FlashDriveBlockEntity flashDrive, final State state, final float partialTicks, final Vec3 cameraPosition, @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(flashDrive, state, partialTicks, cameraPosition, breakProgress);

        final BlockState blockState = flashDrive.getBlockState();
        final Direction facing = blockState.getValue(OrientableBlock.FACING);
        state.rotationX = OrientableBlock.getRotationX(blockState);
        state.rotationY = OrientableBlock.getRotationY(blockState);
        state.neighborLight = flashDrive.getLevel() != null
            ? LightCoordsUtil.getLightCoords(flashDrive.getLevel(), flashDrive.getBlockPos().relative(facing))
            : state.lightCoords;
        itemModelResolver.updateForTopItem(state.media, flashDrive.getMedia(), ItemDisplayContext.FIXED,
            flashDrive.getLevel(), null, (int) flashDrive.getBlockPos().asLong());
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        if (state.media.isEmpty()) {
            return;
        }

        stack.pushPose();

        stack.translate(0.5f, 0.5f, 0.5f);
        stack.rotate(Axis.YN.rotationDegrees(state.rotationY));
        stack.rotate(Axis.XN.rotationDegrees(state.rotationX));
        stack.translate(-0.5f, -0.5f, -0.5f);

        stack.translate(0.5f, FLASH_MEMORY_Y, FLASH_MEMORY_Z);
        stack.rotate(Axis.XP.rotationDegrees(90));
        stack.scale(FLASH_MEMORY_SCALE, FLASH_MEMORY_SCALE, FLASH_MEMORY_SCALE);

        state.media.submit(stack, collector, state.neighborLight, OverlayTexture.NO_OVERLAY, 0);

        stack.popPose();
    }

    // --------------------------------------------------------------------- //

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState media = new ItemStackRenderState();
        public float rotationX, rotationY;
        public int neighborLight;
    }
}
