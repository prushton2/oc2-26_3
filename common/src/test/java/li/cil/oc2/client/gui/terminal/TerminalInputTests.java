/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.terminal;

import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDLScancode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TerminalInputTests {
    @Test
    public void returnSendsCarriageReturn() {
        assertArrayEquals(new byte[]{'\r'}, sequence(false));
    }

    @Test
    public void returnSendsCarriageReturnAndLineFeedInNewLineMode() {
        assertArrayEquals(new byte[]{'\r', '\n'}, sequence(true));
    }

    private static byte[] sequence(final boolean isNewLineMode) {
        return TerminalInput.getSequence(SDLScancode.SDL_SCANCODE_RETURN, 0, false, isNewLineMode);
    }
}
