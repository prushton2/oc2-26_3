/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.client.audio.SoundCardAudio;
import li.cil.oc2.client.gui.ComputerContainerScreen;
import li.cil.oc2.client.gui.ComputerTerminalScreen;
import li.cil.oc2.client.gui.NetworkTunnelScreen;
import li.cil.oc2.client.gui.RobotContainerScreen;
import li.cil.oc2.client.gui.RobotTerminalScreen;
import li.cil.oc2.client.gui.pip.BlockPreviewRenderState;
import li.cil.oc2.client.gui.pip.BlockPreviewRenderer;
import li.cil.oc2.client.renderer.blockentity.BusCableRenderer;
import li.cil.oc2.client.renderer.blockentity.ComputerRenderer;
import li.cil.oc2.client.renderer.blockentity.DiskDriveRenderer;
import li.cil.oc2.client.renderer.blockentity.FlashDriveRenderer;
import li.cil.oc2.client.renderer.blockentity.NetworkConnectorRenderer;
import li.cil.oc2.client.renderer.blockentity.TerminalRenderer;
import li.cil.oc2.client.renderer.blockentity.neoforge.ChargerRendererNeoForge;
import li.cil.oc2.client.renderer.blockentity.neoforge.ProjectorRendererNeoForge;
import li.cil.oc2.client.renderer.entity.RobotItemRenderer;
import li.cil.oc2.client.renderer.entity.RobotRenderer;
import li.cil.oc2.client.renderer.entity.model.RobotModel;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.container.Containers;
import li.cil.oc2.common.entity.Entities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

@EventBusSubscriber(modid = API.MOD_ID, value = Dist.CLIENT)
public final class ClientSetupNeoForge {
    @SubscribeEvent
    public static void handleSetupEvent(final FMLClientSetupEvent event) {
        SoundCardAudio.initialize();
    }

    @SubscribeEvent
    public static void handleRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntities.BUS_CABLE.get(), BusCableRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.COMPUTER.get(), ComputerRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.DISK_DRIVE.get(), DiskDriveRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.FLASH_DRIVE.get(), FlashDriveRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.CHARGER.get(), ChargerRendererNeoForge::new);
        event.registerBlockEntityRenderer(BlockEntities.NETWORK_CONNECTOR.get(), NetworkConnectorRenderer::new);
        event.registerBlockEntityRenderer(BlockEntities.PROJECTOR.get(), ProjectorRendererNeoForge::new);
        event.registerBlockEntityRenderer(BlockEntities.TERMINAL.get(), TerminalRenderer::new);

        event.registerEntityRenderer(Entities.ROBOT.get(), RobotRenderer::new);
    }

    @SubscribeEvent
    public static void handleRegisterLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RobotModel.ROBOT_MODEL_LAYER, RobotModel::createRobotLayer);
    }

    @SubscribeEvent
    public static void handleRegisterMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(Containers.COMPUTER.get(), ComputerContainerScreen::new);
        event.register(Containers.COMPUTER_TERMINAL.get(), ComputerTerminalScreen::new);
        event.register(Containers.ROBOT.get(), RobotContainerScreen::new);
        event.register(Containers.ROBOT_TERMINAL.get(), RobotTerminalScreen::new);
        event.register(Containers.NETWORK_TUNNEL.get(), NetworkTunnelScreen::new);
    }

    @SubscribeEvent
    public static void handleRegisterSpecialModelRenderers(final RegisterSpecialModelRendererEvent event) {
        event.register(RobotItemRenderer.TYPE, RobotItemRenderer.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    public static void handleRegisterPictureInPictureRenderers(final RegisterPictureInPictureRenderersEvent event) {
        event.register(BlockPreviewRenderState.class, BlockPreviewRenderer::new);
    }

    private ClientSetupNeoForge() {
    }
}
