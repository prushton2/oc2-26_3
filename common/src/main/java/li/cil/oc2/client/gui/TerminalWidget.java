/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.client.renderer.TerminalRenderer;
import li.cil.oc2.common.vm.device.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.sdl.SDLScancode;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Environment(EnvType.CLIENT)
public final class TerminalWidget {
    public interface Parent {
        int getWidth();

        int getHeight();

        boolean shouldRenderTerminal();

        void sendTerminalInputToServer(final ByteBuffer input);
    }

    // --------------------------------------------------------------------- //

    private static final int TERMINAL_WIDTH = Terminal.WIDTH * Terminal.CHAR_WIDTH / 2;
    private static final int TERMINAL_HEIGHT = Terminal.HEIGHT * Terminal.CHAR_HEIGHT / 2;

    private static final int WHEEL_UP_BUTTON = 64;
    private static final int WHEEL_DOWN_BUTTON = 65;
    private static final int WHEEL_SCROLL_LINES = 3;

    private static final int MARGIN_SIZE = 8;
    private static final int TERMINAL_X = MARGIN_SIZE;
    private static final int TERMINAL_Y = MARGIN_SIZE;

    public static final int WIDTH = Sprites.TERMINAL_SCREEN.width;
    public static final int HEIGHT = Sprites.TERMINAL_SCREEN.height;

    // --------------------------------------------------------------------- //

    private final Parent parent;
    private final Terminal terminal;
    private int leftPos, topPos;
    private boolean isMouseOverTerminal;

    // --------------------------------------------------------------------- //

    public TerminalWidget(final Parent parent, final Terminal terminal) {
        this.parent = parent;
        this.terminal = terminal;
    }

    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        isMouseOverTerminal = isMouseOverTerminal(mouseX, mouseY);

        Sprites.TERMINAL_SCREEN.draw(graphics, leftPos, topPos);

        if (shouldCaptureInput()) {
            Sprites.TERMINAL_FOCUSED.draw(graphics, leftPos, topPos);
        }
    }

    public void render(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, @Nullable final Component error) {
        if (parent.shouldRenderTerminal()) {
            TerminalRenderer.extractGui(graphics, terminal, leftPos + TERMINAL_X, topPos + TERMINAL_Y, TERMINAL_WIDTH, TERMINAL_HEIGHT);
        } else {
            final Font font = getClient().font;
            if (error != null) {
                final int textWidth = font.width(error);
                final int textOffsetX = (TERMINAL_WIDTH - textWidth) / 2;
                final int textOffsetY = (TERMINAL_HEIGHT - font.lineHeight) / 2;
                graphics.text(font,
                    error,
                    leftPos + TERMINAL_X + textOffsetX,
                    topPos + TERMINAL_Y + textOffsetY,
                    0xFFEE3322);
            }
        }
    }

    public void tick() {
        final ByteBuffer input = terminal.getInput();
        if (input != null) {
            parent.sendTerminalInputToServer(input);
        }
    }

    public boolean mouseClicked(final MouseButtonEvent event) {
        return putMouseEvent(event.x(), event.y(), toReportedButton(event.button()), true);
    }

    public boolean mouseReleased(final MouseButtonEvent event) {
        return putMouseEvent(event.x(), event.y(), toReportedButton(event.button()), false);
    }

    public boolean mouseScrolled(final double mouseX, final double mouseY, final double delta) {
        if (delta == 0) {
            return false;
        }

        if (putMouseEvent(mouseX, mouseY, delta > 0 ? WHEEL_UP_BUTTON : WHEEL_DOWN_BUTTON, true)) {
            return true;
        }

        return shouldCaptureInput() && terminal.putScroll(delta > 0, WHEEL_SCROLL_LINES);
    }

    public boolean charTyped(final CharacterEvent event) {
        // Typed characters carry no modifiers; control chords are sent as sequences by keyPressed.
        final Minecraft client = getClient();
        final boolean isControlChord = client.hasControlDown() && !client.hasAltDown();
        if (!isControlChord) {
            putInput(Character.toString(event.codepoint()));
        }
        return true;
    }

    public boolean keyPressed(final KeyEvent event) {
        final int keyCode = event.key();
        final int modifiers = event.modifiers();
        if (!shouldCaptureInput() && keyCode == SDLScancode.SDL_SCANCODE_ESCAPE) {
            return false;
        }

        if ((modifiers & InputConstants.MOD_CONTROL) != 0 && keyCode == SDLScancode.SDL_SCANCODE_V) {
            terminal.putPaste(getClient().keyboardHandler.getClipboard());
        } else {
            final byte[] sequence = TerminalInput.getSequence(keyCode, modifiers, terminal.isCursorKeyApplicationMode(), terminal.isNewLineMode());
            if (sequence != null) {
                for (final byte b : sequence) {
                    terminal.putInput(b);
                }
            }
        }

        return true;
    }

    public void init() {
        this.leftPos = (parent.getWidth() - WIDTH) / 2;
        this.topPos = (parent.getHeight() - HEIGHT) / 2;
    }

    public void onClose() {
    }

    // --------------------------------------------------------------------- //

    private Minecraft getClient() {
        return Minecraft.getInstance();
    }

    private boolean shouldCaptureInput() {
        return isMouseOverTerminal && InputCapture.isEnabled() &&
            parent.shouldRenderTerminal();
    }

    private void putInput(final String value) {
        for (final byte b : value.getBytes(StandardCharsets.UTF_8)) {
            terminal.putInput(b);
        }
    }

    private static int toReportedButton(final int button) {
        return switch (button) {
            case InputConstants.MOUSE_BUTTON_RIGHT -> 2;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> 1;
            default -> button;
        };
    }

    private boolean putMouseEvent(final double mouseX, final double mouseY, final int button, final boolean isPressed) {
        if (!shouldCaptureInput() || !terminal.isMouseReportingEnabled()) {
            return false;
        }

        final int column = ((int) mouseX - leftPos - TERMINAL_X) * 2 / Terminal.CHAR_WIDTH;
        final int row = ((int) mouseY - topPos - TERMINAL_Y) * 2 / Terminal.CHAR_HEIGHT;
        terminal.putMouseEvent(button,
            Math.clamp(column, 0, Terminal.WIDTH - 1),
            Math.clamp(row, 0, Terminal.HEIGHT - 1),
            isPressed);

        return true;
    }

    private boolean isMouseOverTerminal(final int mouseX, final int mouseY) {
        final int localMouseX = mouseX - leftPos - TERMINAL_X;
        final int localMouseY = mouseY - topPos - TERMINAL_Y;
        return localMouseX >= 0 &&
            localMouseX < TERMINAL_WIDTH &&
            localMouseY >= 0 &&
            localMouseY < TERMINAL_HEIGHT;
    }
}
