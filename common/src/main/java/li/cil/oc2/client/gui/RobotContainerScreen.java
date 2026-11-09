/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import dev.architectury.hooks.client.fluid.ClientFluidStackHooks;
import dev.architectury.hooks.fluid.FluidStackHooks;
import li.cil.oc2.client.gui.util.TooltipRenderer;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.RobotInventoryContainer;
import li.cil.oc2.common.fluid.FluidStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

import static li.cil.oc2.common.util.TextFormatUtils.withFormat;

@Environment(EnvType.CLIENT)
public final class RobotContainerScreen extends AbstractMachineInventoryScreen<RobotInventoryContainer> {
    private static final int SLOT_SIZE = 18;
    private static final int INVENTORY_X = 115;
    private static final int INVENTORY_Y = 19;
    private static final int INVENTORY_COLUMNS = 3;
    private static final int TANK_X = 116;
    private static final int TANK_Y = 94;
    private static final int TANK_WIDTH = 52;
    private static final int TANK_HEIGHT = 5;
    private static final int FLUID_SPRITE_SIZE = 16;

    // --------------------------------------------------------------------- //

    public static void renderSelection(final GuiGraphicsExtractor graphics, final int selectedSlot, final int x, final int y, final int columns) {

        final int slotX = selectedSlot % columns * SLOT_SIZE;
        final int slotY = selectedSlot / columns * SLOT_SIZE;
        final int offset = SLOT_SIZE * (int) (15 * (System.currentTimeMillis() % 1000) / 1000);
        Sprites.SLOT_SELECTION.draw(graphics, x + slotX, y + slotY, 0, offset);
    }

    // --------------------------------------------------------------------- //

    public RobotContainerScreen(final RobotInventoryContainer container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title, Sprites.ROBOT_CONTAINER.width, Sprites.ROBOT_CONTAINER.height);
        inventoryLabelY = imageHeight - 94;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);

        Sprites.ROBOT_CONTAINER.draw(graphics, leftPos, topPos);
        renderSelection(graphics, menu.getRobot().getSelectedSlot(), leftPos + INVENTORY_X, topPos + INVENTORY_Y, INVENTORY_COLUMNS);
        renderTank(graphics);
    }

    @Override
    protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        if (isMouseOver(mouseX, mouseY, TANK_X, TANK_Y, TANK_WIDTH, TANK_HEIGHT)) {
            final FluidStack fluid = menu.getFluid();
            final Component name = fluid.isEmpty()
                ? Component.translatable(Constants.TOOLTIP_FLUID_EMPTY)
                : FluidStackHooks.getName(toArchitectury(fluid));
            final Component tooltip = Component.translatable(Constants.TOOLTIP_FLUID, name,
                withFormat(fluid.amount() + "/" + menu.getFluidCapacity(), ChatFormatting.GREEN));
            TooltipRenderer.drawTooltip(graphics, List.of(tooltip), mouseX, mouseY, 200);
        }
    }

    // --------------------------------------------------------------------- //

    private void renderTank(final GuiGraphicsExtractor graphics) {
        final FluidStack fluid = menu.getFluid();
        if (fluid.isEmpty()) {
            return;
        }

        final var stack = toArchitectury(fluid);
        final var sprite = ClientFluidStackHooks.getStillTexture(stack);
        if (sprite == null) {
            return;
        }

        final int color = ClientFluidStackHooks.getColor(stack);
        final int tint = 0xFF000000 | (color & 0xFFFFFF);

        final int x = leftPos + TANK_X;
        final int y = topPos + TANK_Y;
        final int width = Math.max(1, TANK_WIDTH * fluid.amount() / menu.getFluidCapacity());

        // Animated atlas sprites don't do tiling, so we hack around it like this, oh well.
        graphics.enableScissor(x, y, x + width, y + TANK_HEIGHT);
        for (int offset = 0; offset < width; offset += FLUID_SPRITE_SIZE) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + offset, y, FLUID_SPRITE_SIZE, FLUID_SPRITE_SIZE, tint);
        }
        graphics.disableScissor();
    }

    private static dev.architectury.fluid.FluidStack toArchitectury(final FluidStack fluid) {
        return dev.architectury.fluid.FluidStack.create(fluid.fluid(), fluid.amount(), fluid.components());
    }
}
