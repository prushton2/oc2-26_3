/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.client.renderer.Overlays;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.entity.Robot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class RobotRenderer extends EntityRenderer<Robot, RobotRenderer.State> {
    private static final Vector3f GLOW_COLOR = new Vector3f(0.16f, 0.43f, 0.45f);
    private static final Vector3f GLOW_COLOR_BRIGHT = new Vector3f(0.35f, 0.95f, 1f);

    private static final AABB GLOW_BOUNDS = new AABB(-6.25 / 16.0, 7 / 16.0, -6.25 / 16.0, 6.25 / 16.0, 8 / 16.0, 6.25 / 16.0);

    private static final float STATUS_INNER_X = 2 / 16f;
    private static final float STATUS_OUTER_X = 4 / 16f;
    private static final float STATUS_Y = 10 / 16f;
    private static final float STATUS_HEIGHT = 1 / 16f;
    private static final float STATUS_Z = 7 / 16f;

    private final RobotModel model;

    // --------------------------------------------------------------------- //

    public RobotRenderer(final EntityRendererProvider.Context context) {
        super(context);
        model = new RobotModel(context.bakeLayer(RobotModel.ROBOT_MODEL_LAYER));
    }

    // --------------------------------------------------------------------- //

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final Robot entity, final State state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        final Robot.AnimationState animation = entity.getAnimationState();
        state.baseY = animation.getBaseRenderOffsetY(partialTicks);
        state.topY = animation.getTopRenderOffsetY(partialTicks);
        state.topRotationY = animation.getTopRenderRotationY(partialTicks);

        // NB: we don't use the interpolated yaw because that uses a plain lerp which can lead to ugly
        //     jumps in case we get a wrapped rotationYaw synced from the server (leading to ~360
        //     degree delta to the last known previous rotation).
        final float partialRotation = Mth.degreesDifferenceAbs(entity.yRotO, entity.getYRot()) * partialTicks;
        state.rotation = Mth.approachDegrees(entity.yRotO, entity.getYRot(), partialRotation);

        state.isRunning = entity.getVirtualMachine().isRunning();
        state.statusColor = entity.getStatusColor();
        state.statusValue = entity.getStatusValue();
        state.gameTime = entity.level().getGameTime();
        state.partialTicks = partialTicks;
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        super.submit(state, stack, collector, camera);

        stack.pushPose();
        stack.rotate(Axis.YN.rotationDegrees(state.rotation));

        model.submit(stack, collector, state.baseY, state.topY, state.topRotationY, state.lightCoords, OverlayTexture.NO_OVERLAY);

        if (state.isRunning) {
            stack.pushPose();
            RobotModel.applyTopTransform(stack, state.topY, state.topRotationY);
            submitStatusLights(stack, collector, state.statusColor, state.statusValue);
            stack.popPose();
        }

        if (state.topY > Robot.AnimationState.TOP_IDLE_Y) {
            stack.translate(0, state.baseY, 0);
            Overlays.submitBox(ModRenderType.getGlow(), stack, collector, GLOW_BOUNDS, GLOW_COLOR, GLOW_COLOR_BRIGHT, state.gameTime, state.partialTicks);
        }

        stack.popPose();
    }

    // --------------------------------------------------------------------- //

    private static void submitStatusLights(final PoseStack stack, final SubmitNodeCollector collector, final int color, final float value) {
        if (value <= 0) {
            return;
        }

        final float r = (color >>> 16) / 255f;
        final float g = ((color >>> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        final float top = STATUS_Y + STATUS_HEIGHT * value;
        collector.submitCustomGeometry(stack, ModRenderType.getIndicator(), (pose, consumer) -> {
            addQuad(pose, consumer, -STATUS_OUTER_X, -STATUS_INNER_X, top, r, g, b);
            addQuad(pose, consumer, STATUS_INNER_X, STATUS_OUTER_X, top, r, g, b);
        });
    }

    private static void addQuad(final PoseStack.Pose pose, final VertexConsumer consumer, final float x0, final float x1, final float y1, final float r, final float g, final float b) {
        addVertex(pose, consumer, x0, STATUS_Y, r, g, b);
        addVertex(pose, consumer, x1, STATUS_Y, r, g, b);
        addVertex(pose, consumer, x1, y1, r, g, b);
        addVertex(pose, consumer, x0, y1, r, g, b);
    }

    private static void addVertex(final PoseStack.Pose pose, final VertexConsumer consumer, final float x, final float y, final float r, final float g, final float b) {
        consumer.addVertex(pose, x, y, STATUS_Z).setColor(r, g, b, 1f);
    }

    // --------------------------------------------------------------------- //

    public static final class State extends EntityRenderState {
        public float baseY, topY, topRotationY;
        public float rotation;
        public boolean isRunning;
        public int statusColor;
        public float statusValue;
        public long gameTime;
        public float partialTicks;
    }
}
