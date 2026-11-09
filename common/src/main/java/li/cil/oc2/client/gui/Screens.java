/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.blockentity.KeyboardBlockEntity;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * Opens the mod's client side only screens.
 * <p>
 * Common code (blocks, items) must open screens through this class instead of constructing them itself: NeoForge
 * no longer strips client-only members from mod classes, and code like {@code setScreen(new SomeScreen())} makes the
 * bytecode verifier load the screen classes when the containing class is linked, which fails on dedicated servers.
 * Calling into this class only loads it once a call is actually executed, which only happens on the client.
 */
@Environment(EnvType.CLIENT)
public final class Screens {
    public static void openBusInterfaceScreen(final BusCableBlockEntity busCable, final Direction side) {
        open(new BusInterfaceScreen(busCable, side));
    }

    public static void openKeyboardScreen(final KeyboardBlockEntity keyboard) {
        open(new KeyboardScreen(keyboard));
    }

    public static void openTerminalScreen(final TerminalBlockEntity terminal) {
        open(new TerminalScreen(terminal));
    }

    public static void openTerminalConfigurationScreen(final TerminalBlockEntity terminal) {
        open(new TerminalConfigurationScreen(terminal));
    }

    public static void openSerialInterfaceCardScreen(final Player player, final InteractionHand hand) {
        open(new SerialInterfaceCardScreen(player, hand));
    }

    public static void openNetworkInterfaceCardScreen(final Player player, final InteractionHand hand) {
        open(new NetworkInterfaceCardScreen(player, hand));
    }

    // --------------------------------------------------------------------- //

    private static void open(final Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }

    private Screens() {
    }
}
