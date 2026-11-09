/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.terminal;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.Int2CharArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.lwjgl.sdl.SDLScancode;

import javax.annotation.Nullable;

public final class TerminalInput {
    private static final Int2ObjectArrayMap<Int2ObjectArrayMap<byte[]>> KEYCODE_SEQUENCES = new Int2ObjectArrayMap<>();
    private static final Int2CharArrayMap CSI_FINAL_BYTES = new Int2CharArrayMap();
    private static final Int2IntArrayMap CSI_CODES = new Int2IntArrayMap();
    private static final byte[] RETURN = {'\r'};
    private static final byte[] RETURN_NEW_LINE = {'\r', '\n'};

    static {
        CSI_FINAL_BYTES.defaultReturnValue('\0');
        CSI_CODES.defaultReturnValue(0);

        addSequence(SDLScancode.SDL_SCANCODE_TAB, '\t');
        addSequence(SDLScancode.SDL_SCANCODE_BACKSPACE, '\b');
        addSequence(InputConstants.MOD_ALT, SDLScancode.SDL_SCANCODE_BACKSPACE, (byte) '\033', (byte) '\b');

        addSequence(SDLScancode.SDL_SCANCODE_ESCAPE, "\33");

        addCsiCode(SDLScancode.SDL_SCANCODE_HOME, 1);
        addCsiCode(SDLScancode.SDL_SCANCODE_INSERT, 2);
        addCsiCode(SDLScancode.SDL_SCANCODE_DELETE, 3);
        addCsiCode(SDLScancode.SDL_SCANCODE_END, 4);
        addCsiCode(SDLScancode.SDL_SCANCODE_PAGEUP, 5);
        addCsiCode(SDLScancode.SDL_SCANCODE_PAGEDOWN, 6);

        addCsiCode(SDLScancode.SDL_SCANCODE_F1, 11);
        addCsiCode(SDLScancode.SDL_SCANCODE_F2, 12);
        addCsiCode(SDLScancode.SDL_SCANCODE_F3, 13);
        addCsiCode(SDLScancode.SDL_SCANCODE_F4, 14);
        addCsiCode(SDLScancode.SDL_SCANCODE_F5, 15);
        addCsiCode(SDLScancode.SDL_SCANCODE_F6, 17);
        addCsiCode(SDLScancode.SDL_SCANCODE_F7, 18);
        addCsiCode(SDLScancode.SDL_SCANCODE_F8, 19);
        addCsiCode(SDLScancode.SDL_SCANCODE_F9, 20);
        addCsiCode(SDLScancode.SDL_SCANCODE_F10, 21);
        addCsiCode(SDLScancode.SDL_SCANCODE_F11, 23);
        addCsiCode(SDLScancode.SDL_SCANCODE_F12, 24);

        addCsiFinalByte(SDLScancode.SDL_SCANCODE_UP, 'A');
        addCsiFinalByte(SDLScancode.SDL_SCANCODE_DOWN, 'B');
        addCsiFinalByte(SDLScancode.SDL_SCANCODE_RIGHT, 'C');
        addCsiFinalByte(SDLScancode.SDL_SCANCODE_LEFT, 'D');

        for (int i = 'A'; i <= 'Z'; i++) {
            addSequence(
                InputConstants.MOD_CONTROL,
                SDLScancode.SDL_SCANCODE_A + i - 'A',
                (byte) (1 + i - 'A')
            );
            addSequence(
                InputConstants.MOD_CONTROL | InputConstants.MOD_SHIFT,
                SDLScancode.SDL_SCANCODE_A + i - 'A',
                (byte) (1 + i - 'A')
            );

            addSequence(
                InputConstants.MOD_ALT,
                SDLScancode.SDL_SCANCODE_A + i - 'A',
                (byte) '\033', (byte) ('a' + i - 'A')
            );
            addSequence(
                InputConstants.MOD_ALT | InputConstants.MOD_SHIFT,
                SDLScancode.SDL_SCANCODE_A + i - 'A',
                (byte) '\033', (byte) i
            );
        }

        addSequence(InputConstants.MOD_CONTROL, SDLScancode.SDL_SCANCODE_LEFTBRACKET, (byte) '\033');
        addSequence(InputConstants.MOD_CONTROL | InputConstants.MOD_SHIFT, SDLScancode.SDL_SCANCODE_LEFTBRACKET, (byte) '\033');
        addSequence(InputConstants.MOD_CONTROL, SDLScancode.SDL_SCANCODE_BACKSLASH, (byte) '\034');
        addSequence(InputConstants.MOD_CONTROL | InputConstants.MOD_SHIFT, SDLScancode.SDL_SCANCODE_BACKSLASH, (byte) '\034');
        addSequence(InputConstants.MOD_CONTROL, SDLScancode.SDL_SCANCODE_RIGHTBRACKET, (byte) '\035');
        addSequence(InputConstants.MOD_CONTROL | InputConstants.MOD_SHIFT, SDLScancode.SDL_SCANCODE_RIGHTBRACKET, (byte) '\035');
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public static byte[] getSequence(final int keyCode, final int modifiers, final boolean isCursorKeyApplicationMode, final boolean isNewLineMode) {
        final int relevantModifiers = normalizeModifiers(modifiers);

        if (keyCode == SDLScancode.SDL_SCANCODE_RETURN && relevantModifiers == 0) {
            return isNewLineMode ? RETURN_NEW_LINE : RETURN;
        }

        final Int2ObjectArrayMap<byte[]> map = KEYCODE_SEQUENCES.get(relevantModifiers);
        if (map != null) {
            final byte[] sequence = map.get(keyCode);
            if (sequence != null) {
                return sequence;
            }
        }

        return getCsiSequence(keyCode, relevantModifiers, isCursorKeyApplicationMode);
    }

    // --------------------------------------------------------------------- //

    @Nullable
    private static byte[] getCsiSequence(final int keyCode, final int modifiers, final boolean isCursorKeyApplicationMode) {
        final int parameter = 1
            + ((modifiers & InputConstants.MOD_SHIFT) != 0 ? 1 : 0)
            + ((modifiers & InputConstants.MOD_ALT) != 0 ? 2 : 0)
            + ((modifiers & InputConstants.MOD_CONTROL) != 0 ? 4 : 0);

        final char finalByte = CSI_FINAL_BYTES.get(keyCode);
        if (finalByte != '\0') {
            if (parameter == 1) {
                return toBytes((isCursorKeyApplicationMode ? "\033O" : "\033[") + finalByte);
            }
            return toBytes("\033[1;" + parameter + finalByte);
        }

        final int code = CSI_CODES.get(keyCode);
        if (code != 0) {
            return toBytes(parameter == 1
                ? "\033[" + code + "~"
                : "\033[" + code + ";" + parameter + "~");
        }

        return null;
    }

    /**
     * Maps modifier masks that distinguish left and right keys to the combined masks used as keys here.
     */
    private static int normalizeModifiers(final int modifiers) {
        int result = 0;
        if ((modifiers & InputConstants.MOD_SHIFT) != 0) result |= InputConstants.MOD_SHIFT;
        if ((modifiers & InputConstants.MOD_CONTROL) != 0) result |= InputConstants.MOD_CONTROL;
        if ((modifiers & InputConstants.MOD_ALT) != 0) result |= InputConstants.MOD_ALT;
        return result;
    }

    private static void addCsiFinalByte(final int keyCode, final char finalByte) {
        CSI_FINAL_BYTES.put(keyCode, finalByte);
    }

    private static void addCsiCode(final int keyCode, final int code) {
        CSI_CODES.put(keyCode, code);
    }

    private static void addSequence(final int keyCode, final char ch) {
        addSequence(keyCode, (byte) ch);
    }

    private static void addSequence(final int keyCode, final byte... sequence) {
        addSequence(0, keyCode, sequence);
    }

    private static void addSequence(final int keyCode, final String sequence) {
        addSequence(0, keyCode, sequence);
    }

    private static void addSequence(final int modifiers, final int keyCode, final String sequence) {
        addSequence(modifiers, keyCode, toBytes(sequence));
    }

    private static void addSequence(final int modifiers, final int keyCode, final byte... sequence) {
        KEYCODE_SEQUENCES
            .computeIfAbsent(modifiers, i -> new Int2ObjectArrayMap<>())
            .put(keyCode, sequence);
    }

    private static byte[] toBytes(final String sequence) {
        final byte[] bytes = new byte[sequence.length()];
        for (int i = 0; i < sequence.length(); i++) {
            bytes[i] = (byte) sequence.charAt(i);
        }
        return bytes;
    }
}
