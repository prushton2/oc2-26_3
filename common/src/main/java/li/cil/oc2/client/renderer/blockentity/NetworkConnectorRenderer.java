/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the network cables connected to network connectors.
 * <p>
 * Each cable is rendered by one of the two connectors it connects, the one with the lower position, unless
 * that one is not loaded, in which case the other end renders it.
 */
@Environment(EnvType.CLIENT)
public final class NetworkConnectorRenderer implements BlockEntityRenderer<NetworkConnectorBlockEntity, NetworkConnectorRenderer.State> {
    private static final int MAX_RENDER_DISTANCE = 100;
    private static final int CABLE_VERTEX_COUNT = 9;
    private static final float CABLE_THICKNESS = 0.025f;
    private static final float CABLE_LENGTH_FOR_MAX_SWING = 6f;
    private static final float CABLE_MAX_SWING_AMOUNT = 0.05f;
    private static final int CABLE_SWING_INTERVAL = 8000;
    private static final float CABLE_HANG_MIN = 0.1f;
    private static final float CABLE_HANG_MAX = 0.5f;
    private static final float CABLE_MAX_LENGTH = 8f;
    private static final Vector3f CABLE_COLOR = new Vector3f(0.0f, 0.33f, 0.4f);
    private static final float WHITE_U = 0.5f, WHITE_V = 0.5f;
    private static final Vec3 POS_Y = new Vec3(0, 1, 0);

    // --------------------------------------------------------------------- //

    public NetworkConnectorRenderer(final BlockEntityRendererProvider.Context ignored) {
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean shouldRenderOffScreen() {
        // Cables extend far beyond the connector's block.
        return true;
    }

    @Override
    public int getViewDistance() {
        return MAX_RENDER_DISTANCE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(final NetworkConnectorBlockEntity connector, final State state, final float partialTicks, final Vec3 cameraPosition, @Nullable final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(connector, state, partialTicks, cameraPosition, breakProgress);

        state.cables.clear();

        final Level level = connector.getLevel();
        if (level == null) {
            return;
        }

        final BlockPos position = connector.getBlockPos();
        for (final BlockPos connectedPosition : connector.getConnectedPositions()) {
            if (!isOwner(level, position, connectedPosition)) {
                continue;
            }

            final BlockPos fromPos, toPos;
            if (position.compareTo(connectedPosition) > 0) {
                fromPos = connectedPosition;
                toPos = position;
            } else {
                fromPos = position;
                toPos = connectedPosition;
            }

            final Vec3 from = Vec3.atCenterOf(fromPos);
            final Vec3 to = Vec3.atCenterOf(toPos);
            if (!from.closerThan(cameraPosition, MAX_RENDER_DISTANCE) && !to.closerThan(cameraPosition, MAX_RENDER_DISTANCE)) {
                continue;
            }

            final Vec3 forward = to.subtract(from).normalize();
            final Vec3 right = fromPos.getX() == toPos.getX() && fromPos.getZ() == toPos.getZ()
                ? null : forward.cross(POS_Y);

            final Vec3 middle = animateCableSwing(
                lerp(from, to, 0.5f).subtract(0, computeCableHang(from, to), 0),
                right,
                computeCableSwingAmount(from, to),
                31 * fromPos.hashCode() + toPos.hashCode());

            state.cables.add(buildCable(level, cameraPosition, position, forward, from, to, middle));
        }
    }

    @Override
    public void submit(final State state, final PoseStack stack, final SubmitNodeCollector collector, final CameraRenderState camera) {
        if (state.cables.isEmpty()) {
            return;
        }

        final List<Cable> cables = List.copyOf(state.cables);
        collector.submitCustomGeometry(stack, ModRenderType.getNetworkCable(), (pose, consumer) -> {
            for (final Cable cable : cables) {
                addCable(pose, consumer, cable);
            }
        });
    }

    // --------------------------------------------------------------------- //

    private static boolean isOwner(final Level level, final BlockPos position, final BlockPos connectedPosition) {
        return position.compareTo(connectedPosition) < 0 ||
            !(level.getBlockEntity(connectedPosition) instanceof NetworkConnectorBlockEntity);
    }

    private static Cable buildCable(final Level level, final Vec3 eye, final BlockPos origin, final Vec3 forward,
                                    final Vec3 p0, final Vec3 p1, final Vec3 p2) {
        final Cable cable = new Cable();
        final BlockPos.MutableBlockPos cablePos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < CABLE_VERTEX_COUNT; i++) {
            final float t = i / (CABLE_VERTEX_COUNT - 1f);

            final Vec3 p = quadraticBezier(p0, p1, p2, t);
            final Vec3 n = getExtrusionVector(eye, p, forward);

            cablePos.set(p.x, p.y, p.z);
            cable.light[i] = LightCoordsUtil.getLightCoords(level, cablePos);

            final int o = i * 3;
            final double x = p.x - origin.getX(), y = p.y - origin.getY(), z = p.z - origin.getZ();
            cable.left[o] = (float) (x - n.x);
            cable.left[o + 1] = (float) (y - n.y);
            cable.left[o + 2] = (float) (z - n.z);
            cable.right[o] = (float) (x + n.x);
            cable.right[o + 1] = (float) (y + n.y);
            cable.right[o + 2] = (float) (z + n.z);
        }
        return cable;
    }

    private static void addCable(final PoseStack.Pose pose, final VertexConsumer consumer, final Cable cable) {
        for (int i = 0; i < CABLE_VERTEX_COUNT - 1; i++) {
            final int i0 = i * 3, i1 = (i + 1) * 3;
            final int light = cable.light[i];

            addVertex(pose, consumer, cable.left, i0, light);
            addVertex(pose, consumer, cable.right, i0, light);
            addVertex(pose, consumer, cable.right, i1, light);
            addVertex(pose, consumer, cable.left, i1, light);
        }
    }

    private static void addVertex(final PoseStack.Pose pose, final VertexConsumer consumer, final float[] positions, final int offset, final int light) {
        consumer.addVertex(pose, positions[offset], positions[offset + 1], positions[offset + 2])
            .setColor(CABLE_COLOR.x(), CABLE_COLOR.y(), CABLE_COLOR.z(), 1f)
            .setUv(WHITE_U, WHITE_V)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0, 1, 0);
    }

    private static Vec3 lerp(final Vec3 a, final Vec3 b, final float t) {
        return a.add(b.subtract(a).scale(t)); // a + (b - a)*t = a*(1-t) + b*t
    }

    private static Vec3 quadraticBezier(final Vec3 a, final Vec3 b, final Vec3 c, final float t) {
        final Vec3 a1 = lerp(a, c, t);
        final Vec3 b1 = lerp(c, b, t);
        return lerp(a1, b1, t);
    }

    private static Vec3 getExtrusionVector(final Vec3 eye, final Vec3 v, final Vec3 forward) {
        return forward.cross(eye.subtract(v)).normalize().scale(CABLE_THICKNESS);
    }

    private static float computeCableHang(final Vec3 a, final Vec3 b) {
        final double length = a.distanceTo(b);
        final double hangFactor = Mth.clamp(length / CABLE_MAX_LENGTH, 0, 1);
        return (float) (CABLE_HANG_MIN + (CABLE_HANG_MAX - CABLE_HANG_MIN) * hangFactor);
    }

    private static float computeCableSwingAmount(final Vec3 p0, final Vec3 p1) {
        return Mth.clamp((float) p0.distanceTo(p1) / CABLE_LENGTH_FOR_MAX_SWING, 0.1f, 1f) * CABLE_MAX_SWING_AMOUNT;
    }

    private static Vec3 animateCableSwing(final Vec3 c, @Nullable final Vec3 right, final float swingAmount, final int seed) {
        final float relTime = (System.currentTimeMillis() + seed) % CABLE_SWING_INTERVAL / (float) CABLE_SWING_INTERVAL;
        final float relRadialTime = relTime * 2 * (float) Math.PI;

        if (right == null) {
            return c.add(swingAmount * Mth.sin(relRadialTime),
                0,
                swingAmount * Mth.cos(relRadialTime));
        } else {
            return c.add(swingAmount * Mth.cos(relRadialTime) * right.x,
                0.5f * swingAmount * Mth.sin(relRadialTime * 2 - (float) Math.PI) - swingAmount,
                swingAmount * Mth.cos(relRadialTime) * right.z);
        }
    }

    // --------------------------------------------------------------------- //

    private static final class Cable {
        final float[] left = new float[CABLE_VERTEX_COUNT * 3];
        final float[] right = new float[CABLE_VERTEX_COUNT * 3];
        final int[] light = new int[CABLE_VERTEX_COUNT];
    }

    public static final class State extends BlockEntityRenderState {
        final List<Cable> cables = new ArrayList<>();
    }
}
