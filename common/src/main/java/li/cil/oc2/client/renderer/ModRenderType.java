/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import li.cil.oc2.api.API;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class ModRenderType {
    private static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath(API.MOD_ID, "textures/misc/white.png");
    private static final Identifier TERMINAL_FONT_TEXTURE = Identifier.fromNamespaceAndPath(API.MOD_ID, "textures/font/terminus.png");

    private static final RenderType INDICATOR = RenderType.create(
        API.MOD_ID + "/indicator",
        RenderSetup.builder(ModRenderPipelines.INDICATOR).createRenderSetup());

    private static final RenderType GLOW = RenderType.create(
        API.MOD_ID + "/glow",
        RenderSetup.builder(ModRenderPipelines.GLOW).createRenderSetup());

    private static final RenderType PROJECTOR_LIGHT = RenderType.create(
        API.MOD_ID + "/projector_light",
        RenderSetup.builder(ModRenderPipelines.PROJECTOR_LIGHT).sortOnUpload().createRenderSetup());

    private static final Function<Identifier, RenderType> UNLIT_BLOCK = Util.memoize(location -> RenderType.create(
        API.MOD_ID + "/unlit_block/" + location,
        RenderSetup.builder(ModRenderPipelines.UNLIT_BLOCK).withTexture("Sampler0", location).createRenderSetup()));

    private static final Function<Identifier, RenderType> OVERLAY = Util.memoize(location -> RenderType.create(
        API.MOD_ID + "/overlay/" + location,
        RenderSetup.builder(ModRenderPipelines.OVERLAY).withTexture("Sampler0", location).createRenderSetup()));

    private static final RenderType TERMINAL = RenderType.create(
        API.MOD_ID + "/terminal",
        RenderSetup.builder(ModRenderPipelines.TERMINAL).withTexture("Sampler0", TERMINAL_FONT_TEXTURE).createRenderSetup());

    // --------------------------------------------------------------------- //

    public static RenderType getNetworkCable() {
        return RenderTypes.entityCutout(WHITE_TEXTURE);
    }

    public static RenderType getIndicator() {
        return INDICATOR;
    }

    public static RenderType getGlow() {
        return GLOW;
    }

    public static RenderType getProjectorLight() {
        return PROJECTOR_LIGHT;
    }

    /**
     * Unlit quads using the block texture atlas.
     */
    public static RenderType getUnlitBlock() {
        return UNLIT_BLOCK.apply(TextureAtlas.LOCATION_BLOCKS);
    }

    public static RenderType getUnlitBlock(final Identifier location) {
        return UNLIT_BLOCK.apply(location);
    }

    public static RenderType getOverlay(final Identifier location) {
        return OVERLAY.apply(location);
    }

    public static RenderType getTerminal() {
        return TERMINAL;
    }

    public static Identifier getTerminalFontTexture() {
        return TERMINAL_FONT_TEXTURE;
    }

    // --------------------------------------------------------------------- //

    private ModRenderType() {
    }
}
