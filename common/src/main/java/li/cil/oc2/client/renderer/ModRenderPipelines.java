/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import li.cil.oc2.api.API;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * Render pipelines (shader + fixed function state) used by the mod's custom render types.
 * <p>
 * Pipelines are compiled lazily by the game the first time they are used.
 */
@Environment(EnvType.CLIENT)
public final class ModRenderPipelines {
    private static final Identifier TERMINAL_SHADER = Identifier.fromNamespaceAndPath(API.MOD_ID, "core/terminal");

    /**
     * Uniforms and topology shared by all pipelines here: the camera/GUI projection and per-draw transforms.
     */
    private static final RenderPipeline.Snippet MATRICES_SNIPPET = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .buildSnippet();

    private static final RenderPipeline.Snippet POSITION_COLOR_SNIPPET = RenderPipeline.builder(MATRICES_SNIPPET)
        .withVertexShader("core/position_color")
        .withFragmentShader("core/position_color")
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .buildSnippet();

    private static final RenderPipeline.Snippet POSITION_TEX_SNIPPET = RenderPipeline.builder(MATRICES_SNIPPET)
        .withVertexShader("core/position_tex")
        .withFragmentShader("core/position_tex")
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
        .buildSnippet();

    private static final RenderPipeline.Snippet TERMINAL_SNIPPET = RenderPipeline.builder(MATRICES_SNIPPET)
        .withVertexShader(TERMINAL_SHADER)
        .withFragmentShader(TERMINAL_SHADER)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .buildSnippet();

    // --------------------------------------------------------------------- //

    /**
     * Opaque, flat colored quads, pulled slightly towards the camera so they can sit on block faces.
     */
    public static final RenderPipeline INDICATOR = RenderPipeline.builder(POSITION_COLOR_SNIPPET)
        .withLocation(location("indicator"))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0f, 10.0f))
        .build();

    /**
     * Additively blended, flat colored quads that do not write depth.
     */
    public static final RenderPipeline GLOW = RenderPipeline.builder(POSITION_COLOR_SNIPPET)
        .withLocation(location("glow"))
        .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.ADDITIVE), ColorTargetState.DEFAULT.format(), ColorTargetState.WRITE_COLOR))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .build();

    /**
     * The light cone in front of projectors, like lightning: additive, double-sided, no depth writes.
     */
    public static final RenderPipeline PROJECTOR_LIGHT = RenderPipeline.builder(POSITION_COLOR_SNIPPET)
        .withLocation(location("projector_light"))
        .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.LIGHTNING), ColorTargetState.DEFAULT.format(), ColorTargetState.WRITE_COLOR))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
        .withCull(false)
        .build();

    /**
     * Unlit, additively blended textured quads, e.g. status lights on blocks.
     */
    public static final RenderPipeline UNLIT_BLOCK = RenderPipeline.builder(POSITION_TEX_SNIPPET)
        .withLocation(location("unlit_block"))
        .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0f, 10.0f))
        .withCull(false)
        .build();

    /**
     * Like {@link #UNLIT_BLOCK} but only visible from the front.
     */
    public static final RenderPipeline OVERLAY = RenderPipeline.builder(POSITION_TEX_SNIPPET)
        .withLocation(location("overlay"))
        .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0f, 10.0f))
        .build();

    /**
     * Terminal glyphs in the world.
     */
    public static final RenderPipeline TERMINAL = RenderPipeline.builder(TERMINAL_SNIPPET)
        .withLocation(location("terminal"))
        .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 1.0f, 10.0f))
        .build();

    /**
     * Terminal glyphs in GUIs.
     */
    public static final RenderPipeline GUI_TERMINAL = RenderPipeline.builder(TERMINAL_SNIPPET)
        .withLocation(location("gui_terminal"))
        .build();

    // --------------------------------------------------------------------- //

    private static Identifier location(final String name) {
        return Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/" + name);
    }

    private ModRenderPipelines() {
    }
}
