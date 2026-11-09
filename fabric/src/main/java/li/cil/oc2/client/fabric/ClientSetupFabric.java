/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.fabric;

import dev.architectury.registry.client.gui.MenuScreenRegistry;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import li.cil.oc2.client.audio.SoundCardAudio;
import li.cil.oc2.client.gui.ComputerContainerScreen;
import li.cil.oc2.client.gui.ComputerTerminalScreen;
import li.cil.oc2.client.gui.NetworkTunnelScreen;
import li.cil.oc2.client.gui.RobotContainerScreen;
import li.cil.oc2.client.gui.RobotTerminalScreen;
import li.cil.oc2.client.gui.pip.BlockPreviewRenderer;
import li.cil.oc2.client.renderer.blockentity.BusCableRenderer;
import li.cil.oc2.client.renderer.blockentity.ChargerRenderer;
import li.cil.oc2.client.renderer.blockentity.ComputerRenderer;
import li.cil.oc2.client.renderer.blockentity.DiskDriveRenderer;
import li.cil.oc2.client.renderer.blockentity.FlashDriveRenderer;
import li.cil.oc2.client.renderer.blockentity.NetworkConnectorRenderer;
import li.cil.oc2.client.renderer.blockentity.ProjectorRenderer;
import li.cil.oc2.client.renderer.blockentity.TerminalRenderer;
import li.cil.oc2.client.renderer.entity.RobotItemRenderer;
import li.cil.oc2.client.renderer.entity.RobotRenderer;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.renderer.special.SpecialModelRenderers;

public final class ClientSetupFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SoundCardAudio.initialize();

        BlockEntityRendererRegistry.register(BlockEntities.BUS_CABLE.get(), BusCableRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.COMPUTER.get(), ComputerRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.DISK_DRIVE.get(), DiskDriveRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.FLASH_DRIVE.get(), FlashDriveRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.CHARGER.get(), ChargerRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.NETWORK_CONNECTOR.get(), NetworkConnectorRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.PROJECTOR.get(), ProjectorRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntities.TERMINAL.get(), TerminalRenderer::new);

        EntityRendererRegistry.register(Entities.ROBOT, RobotRenderer::new);
        EntityModelLayerRegistry.register(RobotModel.ROBOT_MODEL_LAYER, RobotModel::createRobotLayer);

        MenuScreenRegistry.registerScreenFactory(Containers.COMPUTER.get(), ComputerContainerScreen::new);
        MenuScreenRegistry.registerScreenFactory(Containers.COMPUTER_TERMINAL.get(), ComputerTerminalScreen::new);
        MenuScreenRegistry.registerScreenFactory(Containers.ROBOT.get(), RobotContainerScreen::new);
        MenuScreenRegistry.registerScreenFactory(Containers.ROBOT_TERMINAL.get(), RobotTerminalScreen::new);
        MenuScreenRegistry.registerScreenFactory(Containers.NETWORK_TUNNEL.get(), NetworkTunnelScreen::new);

        SpecialModelRenderers.ID_MAPPER.put(RobotItemRenderer.TYPE, RobotItemRenderer.Unbaked.MAP_CODEC);

        PictureInPictureRendererRegistry.register(context -> new BlockPreviewRenderer());

        // Matches canceling NeoForge's VanillaGuiLayers.HOTBAR layer.
        HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, hotbar -> (graphics, deltaTracker) -> {
            if (ClientPlatformImpl.isHotbarVisible()) {
                hotbar.extractRenderState(graphics, deltaTracker);
            }
        });
    }
}
