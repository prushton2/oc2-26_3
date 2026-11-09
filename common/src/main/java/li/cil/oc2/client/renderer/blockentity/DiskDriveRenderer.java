/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import li.cil.oc2.common.block.DiskDriveBlock;
import li.cil.oc2.common.blockentity.DiskDriveBlockEntity;
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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

@Environment(EnvType.CLIENT)
public final class DiskDriveRenderer implements BlockEntityRenderer<DiskDriveBlockEntity, DiskDriveRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    // --------------------------------------------------------------------- //

    public DiskDriveRenderer(final BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    // --------------------------------------------------------------------- //

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final DiskDriveBlockEntity diskDrive, final State state, final float partialTicks, final Vec3 cameraPosition, @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(diskDrive, state, partialTicks, cameraPosition, breakProgress);

        state.facing = diskDrive.getBlockState().getValue(DiskDriveBlock.FACING);
        state.neighborLight = diskDrive.getLevel() != null
            ? LightCoordsUtil.getLightCoords(diskDrive.getLevel(), diskDrive.getBlockPos().relative(state.facing))
            : state.lightCoords;
        itemModelResolver.updateForTopItem(state.media, diskDrive.getMedia(), ItemDisplayContext.FIXED,
            diskDrive.getLevel(), null, (int) diskDrive.getBlockPos().asLong());
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        if (state.media.isEmpty()) {
            return;
        }

        stack.pushPose();

        stack.translate(0.5f, 0.5f, 0.5f);
        stack.rotate(Axis.YN.rotationDegrees(state.facing.toYRot()));
        stack.translate(0.0f, 0.0f, 0.5f);
        stack.rotate(Axis.XN.rotationDegrees(90));
        stack.translate(0.0f, 0.2375f, 2.5f / 16f);
        stack.scale(0.55f, 0.55f, 0.55f);

        state.media.submit(stack, collector, state.neighborLight, OverlayTexture.NO_OVERLAY, 0);

        stack.popPose();
    }

    // --------------------------------------------------------------------- //

    public static final class State extends BlockEntityRenderState {
        public final ItemStackRenderState media = new ItemStackRenderState();
        public Direction facing = Direction.NORTH;
        public int neighborLight;
    }
}
