/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.widget.FilteredEditBox;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.BusInterfaceNameMessage;
import li.cil.sedna.api.device.bus.DeviceDescription;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.sdl.SDLScancode;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class BusInterfaceScreen extends Screen {
    private static final int TEXT_LEFT = 9;
    private static final int TEXT_TOP = 11;
    private static final int CONFIRM_LEFT = 206;
    private static final int CONFIRM_TOP = 9;
    private static final int CANCEL_LEFT = 219;
    private static final int CANCEL_TOP = 9;

    private final BusCableBlockEntity busCable;
    private final Direction side;

    private EditBox nameField;

    private int left, top;

    // --------------------------------------------------------------------- //

    public BusInterfaceScreen(final BusCableBlockEntity busCable, final Direction side) {
        super(Component.translatable(Items.BUS_INTERFACE.get().getDescriptionId()));
        this.busCable = busCable;
        this.side = side;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void init() {
        super.init();

        left = (width - Sprites.BUS_INTERFACE_SCREEN.width) / 2;
        top = (height - Sprites.BUS_INTERFACE_SCREEN.height) / 2;

        nameField = new FilteredEditBox(font, left + TEXT_LEFT, top + TEXT_TOP, 192, 12, text("{mod}.gui.bus_interface_name"),
            value -> value.isEmpty() || DeviceDescription.isValidName(value));
        nameField.setCanLoseFocus(false);
        nameField.setTextColor(0xFFFFFFFF);
        nameField.setBordered(false);
        nameField.setMaxLength(32);
        nameField.setValue(busCable.getInterfaceName(side));
        addWidget(nameField);
        setInitialFocus(nameField);

        addRenderableWidget(new ImageButton(
            left + CONFIRM_LEFT, top + CONFIRM_TOP,
            Sprites.CONFIRM_BASE.width, Sprites.CONFIRM_BASE.height,
            Sprites.CONFIRM_BASE,
            Sprites.CONFIRM_PRESSED
        ) {
            @Override
            public void onPress(final InputWithModifiers input) {
                super.onPress(input);
                setInterfaceName(nameField.getValue());
                onClose();
            }
        }).withTooltip(Component.translatable(Constants.TOOLTIP_CONFIRM));

        addRenderableWidget(new ImageButton(
            left + CANCEL_LEFT, top + CANCEL_TOP,
            Sprites.CANCEL_BASE.width, Sprites.CANCEL_BASE.height,
            Sprites.CANCEL_BASE,
            Sprites.CANCEL_PRESSED
        ) {
            @Override
            public void onPress(final InputWithModifiers input) {
                super.onPress(input);
                onClose();
            }
        }).withTooltip(Component.translatable(Constants.TOOLTIP_CANCEL));
    }

    @Override
    public void tick() {
        super.tick();

        final Vec3 busCableCenter = Vec3.atCenterOf(busCable.getBlockPos());
        if (!busCable.isValid() ||
            minecraft.player == null ||
            minecraft.player.distanceToSqr(busCableCenter) > 8 * 8) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        final int keyCode = event.key();
        if (keyCode == SDLScancode.SDL_SCANCODE_RETURN ||
            keyCode == SDLScancode.SDL_SCANCODE_KP_ENTER) {
            setInterfaceName(nameField.getValue());
            onClose();
            return true;
        }

        return nameField.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        extractTransparentBackground(graphics);
        Sprites.BUS_INTERFACE_SCREEN.draw(graphics, left, top);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        nameField.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --------------------------------------------------------------------- //

    private void setInterfaceName(final String name) {
        Network.sendToServer(new BusInterfaceNameMessage.ToServer(busCable, side, name));
    }
}
