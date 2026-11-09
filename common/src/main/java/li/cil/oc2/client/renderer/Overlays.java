/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.api.API;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class Overlays {
    private static final int PULSE_TICKS = 40;

    private static final Identifier POWER_LOCATION = Identifier.fromNamespaceAndPath(API.MOD_ID, "block/overlay/power");
    private static final Identifier STATUS_LOCATION = Identifier.fromNamespaceAndPath(API.MOD_ID, "block/overlay/status");
    private static final Identifier TERMINAL_LOCATION = Identifier.fromNamespaceAndPath(API.MOD_ID, "block/overlay/terminal");

    // --------------------------------------------------------------------- //

    public static void submitPower(final PoseStack stack, final SubmitNodeCollector collector) {
        submitQuad(stack, collector, POWER_LOCATION);
    }

    public static void submitStatus(final PoseStack stack, final SubmitNodeCollector collector) {
        submitQuad(stack, collector, STATUS_LOCATION);
    }

    public static void submitStatus(final PoseStack stack, final SubmitNodeCollector collector, final int frequency) {
        if (System.currentTimeMillis() / frequency % 2 == 1) {
            submitStatus(stack, collector);
        }
    }

    public static void submitTerminal(final PoseStack stack, final SubmitNodeCollector collector) {
        submitQuad(stack, collector, TERMINAL_LOCATION);
    }

    public static void submitBox(final PoseStack stack, final SubmitNodeCollector collector, final AABB bounds, final Vector3f color, final Vector3f colorBright, final long gameTime, final float partialTicks) {
        submitBox(ModRenderType.getIndicator(), stack, collector, bounds, color, colorBright, gameTime, partialTicks);
    }

    public static void submitBox(final RenderType renderType, final PoseStack stack, final SubmitNodeCollector collector, final AABB bounds, final Vector3f color, final Vector3f colorBright, final long gameTime, final float partialTicks) {
        final float phase = (gameTime % PULSE_TICKS + partialTicks) / PULSE_TICKS;
        final float blend = (1 + Mth.sin(phase * (float) (Math.PI * 2))) * 0.5f;

        final float r = Mth.lerp(blend, color.x(), colorBright.x());
        final float g = Mth.lerp(blend, color.y(), colorBright.y());
        final float b = Mth.lerp(blend, color.z(), colorBright.z());

        collector.submitCustomGeometry(stack, renderType, (pose, consumer) ->
            addBoxVertices(pose, consumer, bounds, r, g, b));
    }

    // --------------------------------------------------------------------- //

    private static void addBoxVertices(final PoseStack.Pose pose, final VertexConsumer consumer, final AABB bounds, final float r, final float g, final float b) {
        final float x0 = (float) bounds.minX;
        final float y0 = (float) bounds.minY;
        final float z0 = (float) bounds.minZ;
        final float x1 = (float) bounds.maxX;
        final float y1 = (float) bounds.maxY;
        final float z1 = (float) bounds.maxZ;

        addVertex(pose, consumer, x0, y0, z0, r, g, b);
        addVertex(pose, consumer, x0, y1, z0, r, g, b);
        addVertex(pose, consumer, x1, y1, z0, r, g, b);
        addVertex(pose, consumer, x1, y0, z0, r, g, b);

        addVertex(pose, consumer, x0, y0, z1, r, g, b);
        addVertex(pose, consumer, x1, y0, z1, r, g, b);
        addVertex(pose, consumer, x1, y1, z1, r, g, b);
        addVertex(pose, consumer, x0, y1, z1, r, g, b);

        addVertex(pose, consumer, x0, y0, z0, r, g, b);
        addVertex(pose, consumer, x0, y0, z1, r, g, b);
        addVertex(pose, consumer, x0, y1, z1, r, g, b);
        addVertex(pose, consumer, x0, y1, z0, r, g, b);

        addVertex(pose, consumer, x1, y0, z0, r, g, b);
        addVertex(pose, consumer, x1, y1, z0, r, g, b);
        addVertex(pose, consumer, x1, y1, z1, r, g, b);
        addVertex(pose, consumer, x1, y0, z1, r, g, b);

        addVertex(pose, consumer, x0, y0, z0, r, g, b);
        addVertex(pose, consumer, x1, y0, z0, r, g, b);
        addVertex(pose, consumer, x1, y0, z1, r, g, b);
        addVertex(pose, consumer, x0, y0, z1, r, g, b);

        addVertex(pose, consumer, x0, y1, z0, r, g, b);
        addVertex(pose, consumer, x0, y1, z1, r, g, b);
        addVertex(pose, consumer, x1, y1, z1, r, g, b);
        addVertex(pose, consumer, x1, y1, z0, r, g, b);
    }

    private static void addVertex(final PoseStack.Pose pose, final VertexConsumer consumer, final float x, final float y, final float z, final float r, final float g, final float b) {
        consumer.addVertex(pose, x, y, z).setColor(r, g, b, 1f);
    }

    /**
     * Looks up a sprite in the block texture atlas, for use with {@link ModRenderType#getUnlitBlock()}.
     */
    public static TextureAtlasSprite getBlockSprite(final Identifier location) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(location);
    }

    private static void submitQuad(final PoseStack stack, final SubmitNodeCollector collector, final Identifier spriteLocation) {
        final TextureAtlasSprite sprite = getBlockSprite(spriteLocation);
        final float u0 = sprite.getU0(), u1 = sprite.getU1();
        final float v0 = sprite.getV0(), v1 = sprite.getV1();

        collector.submitCustomGeometry(stack, ModRenderType.getUnlitBlock(), (pose, consumer) -> {
            consumer.addVertex(pose, 0, 0, 0).setUv(u0, v0);
            consumer.addVertex(pose, 0, 16, 0).setUv(u0, v1);
            consumer.addVertex(pose, 16, 16, 0).setUv(u1, v1);
            consumer.addVertex(pose, 16, 0, 0).setUv(u1, v0);
        });
    }

    private Overlays() {
    }
}
