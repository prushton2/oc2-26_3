/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import li.cil.oc2.api.API;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Renders robot items using the robot entity's model. Referenced from the robot's item model definition.
 */
@Environment(EnvType.CLIENT)
public final class RobotItemRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath(API.MOD_ID, "robot");

    private final RobotModel model;

    // --------------------------------------------------------------------- //

    public RobotItemRenderer(final RobotModel model) {
        this.model = model;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void submit(final PoseStack poseStack, final SubmitNodeCollector collector, final int lightCoords, final int overlayCoords, final boolean hasFoil, final int outlineColor) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        model.submit(poseStack, collector, 0, 0, 0, lightCoords, overlayCoords);
        poseStack.popPose();
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        final PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5, 0, 0.5);
        model.getExtents(poseStack, output);
    }

    // --------------------------------------------------------------------- //

    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public RobotItemRenderer bake(final SpecialModelRenderer.BakingContext context) {
            return new RobotItemRenderer(new RobotModel(context.entityModelSet().bakeLayer(RobotModel.ROBOT_MODEL_LAYER)));
        }
    }
}
