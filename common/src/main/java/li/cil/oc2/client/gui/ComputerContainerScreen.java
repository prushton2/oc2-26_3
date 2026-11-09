/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.common.container.ComputerInventoryContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public final class ComputerContainerScreen extends AbstractMachineInventoryScreen<ComputerInventoryContainer> {
    public ComputerContainerScreen(final ComputerInventoryContainer container, final Inventory inventory, final Component title) {
        super(container, inventory, title, Sprites.COMPUTER_CONTAINER.width, Sprites.COMPUTER_CONTAINER.height);
        inventoryLabelY = imageHeight - 94;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);
        Sprites.COMPUTER_CONTAINER.draw(graphics, leftPos, topPos);
    }
}
