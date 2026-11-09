/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.neoforge;

import li.cil.oc2.api.API;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class ClientRenderEventsNeoForge {
    @SubscribeEvent
    public static void handleRenderGuiLayer(final RenderGuiLayerEvent.Pre event) {
        if (VanillaGuiLayers.HOTBAR.equals(event.getName()) && !ClientPlatformImpl.isHotbarVisible()) {
            event.setCanceled(true);
        }
    }

    private ClientRenderEventsNeoForge() {
    }
}
