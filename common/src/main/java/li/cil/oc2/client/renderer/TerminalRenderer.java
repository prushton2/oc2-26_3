/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import li.cil.oc2.common.vm.device.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import javax.annotation.Nullable;

import static li.cil.oc2.common.vm.device.Terminal.*;

/**
 * Builds the quads representing the contents of a {@link Terminal}.
 * <p>
 * Coordinates are in terminal pixels, i.e. a cell is {@link Terminal#CHAR_WIDTH} by {@link Terminal#CHAR_HEIGHT}
 * units, with the origin in the top left corner of the terminal.
 */
@Environment(EnvType.CLIENT)
public final class TerminalRenderer {
    private static final int TEXTURE_RESOLUTION = 256;
    private static final float ONE_OVER_TEXTURE_RESOLUTION = 1.0f / TEXTURE_RESOLUTION;
    private static final float CHAR_WIDTH_IN_UV = CHAR_WIDTH * ONE_OVER_TEXTURE_RESOLUTION;
    private static final float CHAR_HEIGHT_IN_UV = CHAR_HEIGHT * ONE_OVER_TEXTURE_RESOLUTION;
    private static final int TEXTURE_COLUMNS = 16;
    private static final int TEXTURE_BOLD_SHIFT = TEXTURE_COLUMNS; // Bold chars are in right half of texture.
    private static final int WHITE_CELL = 0x7F;
    private static final int WHITE_CELL_X = (WHITE_CELL % TEXTURE_COLUMNS + TEXTURE_BOLD_SHIFT) * CHAR_WIDTH + CHAR_WIDTH / 2;
    private static final int WHITE_CELL_Y = WHITE_CELL / TEXTURE_COLUMNS * CHAR_HEIGHT + CHAR_HEIGHT / 2;
    private static final float WHITE_U = WHITE_CELL_X * ONE_OVER_TEXTURE_RESOLUTION;
    private static final float WHITE_V = WHITE_CELL_Y * ONE_OVER_TEXTURE_RESOLUTION;

    private static final int[] COLORS = {
        0x010101, // Black
        0xEE3322, // Red
        0x33CC44, // Green
        0xDDBB11, // Yellow
        0x1188EE, // Blue
        0xDD33CC, // Magenta
        0x22BBCC, // Cyan
        0xCCCCCC, // White
    };

    private static final int[] BRIGHT_COLORS = {
        0x555555, // Black
        0xFF7766, // Red
        0x66FF77, // Green
        0xFFEE55, // Yellow
        0x55BBFF, // Blue
        0xFF88FF, // Magenta
        0x66EEFF, // Cyan
        0xFFFFFF, // White
    };

    private static final int[] DIM_COLORS = {
        0x010101, // Black
        0x772211, // Red
        0x116622, // Green
        0x886611, // Yellow
        0x115588, // Blue
        0x771177, // Magenta
        0x116677, // Cyan
        0x777777, // White
    };

    // --------------------------------------------------------------------- //

    /**
     * Receives the vertices of the quads making up the terminal, four per quad.
     */
    @FunctionalInterface
    public interface VertexSink {
        void vertex(float x, float y, float u, float v, int color);
    }

    // --------------------------------------------------------------------- //

    /**
     * Adds the terminal to the GUI being extracted.
     *
     * @param graphics the graphics to add the terminal to.
     * @param terminal the terminal to render.
     * @param x        the left of the terminal on screen.
     * @param y        the top of the terminal on screen.
     * @param width    the width of the terminal on screen.
     * @param height   the height of the terminal on screen.
     */
    public static void extractGui(final GuiGraphicsExtractor graphics, final Terminal terminal, final int x, final int y, final int width, final int height) {
        final Matrix3x2f pose = new Matrix3x2f(graphics.pose())
            .translate(x, y)
            .scale(width / (float) terminal.getWidth(), height / (float) terminal.getHeight());
        final ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(graphics.pose());
        if (scissor != null) {
            bounds = scissor.intersection(bounds);
        }

        final AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(ModRenderType.getTerminalFontTexture());
        graphics.guiRenderState.addGuiElement(new GuiTerminalRenderState(
            terminal,
            ModRenderPipelines.GUI_TERMINAL,
            TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()),
            pose,
            scissor,
            bounds));
    }

    /**
     * Submits the terminal for rendering in the world, using the current pose of the specified stack.
     */
    public static void submit(final PoseStack poseStack, final SubmitNodeCollector collector, final Terminal terminal) {
        collector.submitCustomGeometry(poseStack, ModRenderType.getTerminal(), (pose, consumer) ->
            build(terminal, (x, y, u, v, color) -> consumer.addVertex(pose, x, y, 0).setUv(u, v).setColor(color)));
    }

    /**
     * Emits the quads representing the terminal's current contents, including the cursor, if it is visible.
     */
    public static void build(final Terminal terminal, final VertexSink sink) {
        for (int row = 0; row < HEIGHT; row++) {
            final float y = row * CHAR_HEIGHT;
            buildBackground(terminal, sink, row, y);
            buildForeground(terminal, sink, row, y);
        }

        if (shouldRenderCursor(terminal)) {
            buildCursor(terminal, sink);
        }
    }

    @VisibleForTesting
    static boolean isPrintableCharacter(final char ch) {
        return ch > 0 && ch != ' ' && (ch < 0x7F || ch > 0xA0);
    }

    // --------------------------------------------------------------------- //

    private static boolean shouldRenderCursor(final Terminal terminal) {
        return terminal.isCursorVisible() && (System.currentTimeMillis() + terminal.hashCode()) % 1000 > 500;
    }

    private static void buildBackground(final Terminal terminal, final VertexSink sink, final int row, final float y) {
        // State tracking for drawing background quads spanning multiple characters.
        float backgroundStartX = -1;
        int backgroundColor = 0;

        float tx = 0f;
        for (int col = 0, index = row * WIDTH; col < WIDTH; col++, index++) {
            final int cell = terminal.getCell(index);

            final int background = resolveColor(getBackgroundColorIndex(cell), isBackgroundBright(cell), isDim(cell));

            final boolean hadBackground = backgroundStartX >= 0;
            final boolean hasBackground = background != resolveColor(0, false, isDim(cell));
            if (!hadBackground && hasBackground) {
                backgroundStartX = tx;
                backgroundColor = background;
            } else if (hadBackground && (!hasBackground || backgroundColor != background)) {
                buildBackground(sink, backgroundStartX, tx, y, backgroundColor);

                if (hasBackground) {
                    backgroundStartX = tx;
                    backgroundColor = background;
                } else {
                    backgroundStartX = -1;
                }
            }

            tx += CHAR_WIDTH;
        }

        if (backgroundStartX >= 0) {
            buildBackground(sink, backgroundStartX, tx, y, backgroundColor);
        }
    }

    private static void buildBackground(final VertexSink sink, final float x0, final float x1, final float y, final int rgb) {
        final int color = 0xFF000000 | rgb;
        sink.vertex(x0, y + CHAR_HEIGHT, WHITE_U, WHITE_V, color);
        sink.vertex(x1, y + CHAR_HEIGHT, WHITE_U, WHITE_V, color);
        sink.vertex(x1, y, WHITE_U, WHITE_V, color);
        sink.vertex(x0, y, WHITE_U, WHITE_V, color);
    }

    private static void buildForeground(final Terminal terminal, final VertexSink sink, final int row, final float y) {
        float tx = 0f;
        for (int col = 0, index = row * WIDTH; col < WIDTH; col++, index++) {
            final int cell = terminal.getCell(index);

            if (isVisible(cell)) {
                final int foreground = resolveColor(getForegroundColorIndex(cell), isForegroundBright(cell), isDim(cell));
                buildForeground(sink, tx, y, getCharacter(cell), foreground, isBold(cell), isUnderline(cell));
            }

            tx += CHAR_WIDTH;
        }
    }

    private static void buildForeground(final VertexSink sink, final float x, final float y, final int character, final int rgb, final boolean isBold, final boolean isUnderline) {
        final int color = 0xFF000000 | rgb;

        if (isPrintableCharacter((char) character)) {
            final int column = character % TEXTURE_COLUMNS + (isBold ? TEXTURE_BOLD_SHIFT : 0);
            final int line = character / TEXTURE_COLUMNS;
            final float u0 = column * CHAR_WIDTH_IN_UV;
            final float u1 = (column + 1) * CHAR_WIDTH_IN_UV;
            final float v0 = line * CHAR_HEIGHT_IN_UV;
            final float v1 = (line + 1) * CHAR_HEIGHT_IN_UV;

            sink.vertex(x, y + CHAR_HEIGHT, u0, v1, color);
            sink.vertex(x + CHAR_WIDTH, y + CHAR_HEIGHT, u1, v1, color);
            sink.vertex(x + CHAR_WIDTH, y, u1, v0, color);
            sink.vertex(x, y, u0, v0, color);
        }

        if (isUnderline) {
            sink.vertex(x, y + CHAR_HEIGHT - 2, WHITE_U, WHITE_V, color);
            sink.vertex(x + CHAR_WIDTH, y + CHAR_HEIGHT - 2, WHITE_U, WHITE_V, color);
            sink.vertex(x + CHAR_WIDTH, y + CHAR_HEIGHT - 3, WHITE_U, WHITE_V, color);
            sink.vertex(x, y + CHAR_HEIGHT - 3, WHITE_U, WHITE_V, color);
        }
    }

    private static void buildCursor(final Terminal terminal, final VertexSink sink) {
        final int cursorX = terminal.getCursorX();
        final int cursorY = terminal.getCursorY();
        final int cell = terminal.getCell(cursorY * WIDTH + cursorX);

        final int blockColor = resolveColor(getForegroundColorIndex(cell), isForegroundBright(cell), isDim(cell));
        final int glyphColor = resolveColor(getBackgroundColorIndex(cell), isBackgroundBright(cell), isDim(cell));

        final float x = cursorX * CHAR_WIDTH;
        final float y = cursorY * CHAR_HEIGHT;
        buildBackground(sink, x, x + CHAR_WIDTH, y, blockColor);
        if (isVisible(cell)) {
            buildForeground(sink, x, y, getCharacter(cell), glyphColor, isBold(cell), isUnderline(cell));
        }
    }

    private static int resolveColor(final int index, final boolean isBright, final boolean isDim) {
        if (isDim) {
            return DIM_COLORS[index];
        }
        if (isBright) {
            return BRIGHT_COLORS[index];
        }
        return COLORS[index];
    }

    // --------------------------------------------------------------------- //

    private record GuiTerminalRenderState(
        Terminal terminal,
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public void buildVertices(final VertexConsumer consumer) {
            build(terminal, (x, y, u, v, color) -> consumer.addVertexWith2DPose(pose, x, y).setUv(u, v).setColor(color));
        }
    }

    private TerminalRenderer() {
    }
}
