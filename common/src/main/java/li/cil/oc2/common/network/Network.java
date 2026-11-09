/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.GameInstance;
import li.cil.oc2.api.API;
import li.cil.oc2.common.network.message.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class Network {
    private static final Map<Class<?>, CustomPacketPayload.Type<? extends CustomPacketPayload>> MESSAGE_TYPES = new HashMap<>();

    // --------------------------------------------------------------------- //

    public static void initialize() {
        registerMessage(ComputerTerminalOutputMessage.class, ComputerTerminalOutputMessage::new, NetworkManager.s2c());
        registerMessage(ComputerTerminalInputMessage.class, ComputerTerminalInputMessage::new, NetworkManager.c2s());
        registerMessage(ComputerRunStateMessage.class, ComputerRunStateMessage::new, NetworkManager.s2c());
        registerMessage(ComputerBusStateMessage.class, ComputerBusStateMessage::new, NetworkManager.s2c());
        registerMessage(ComputerBootErrorMessage.class, ComputerBootErrorMessage::new, NetworkManager.s2c());
        registerMessage(ComputerPowerMessage.class, ComputerPowerMessage::new, NetworkManager.c2s());
        registerMessage(OpenComputerInventoryMessage.class, OpenComputerInventoryMessage::new, NetworkManager.c2s());
        registerMessage(OpenComputerTerminalMessage.class, OpenComputerTerminalMessage::new, NetworkManager.c2s());

        registerMessage(RobotTerminalOutputMessage.class, RobotTerminalOutputMessage::new, NetworkManager.s2c());
        registerMessage(RobotTerminalInputMessage.class, RobotTerminalInputMessage::new, NetworkManager.c2s());
        registerMessage(RobotRunStateMessage.class, RobotRunStateMessage::new, NetworkManager.s2c());
        registerMessage(RobotBusStateMessage.class, RobotBusStateMessage::new, NetworkManager.s2c());
        registerMessage(RobotBootErrorMessage.class, RobotBootErrorMessage::new, NetworkManager.s2c());
        registerMessage(RobotPowerMessage.class, RobotPowerMessage::new, NetworkManager.c2s());
        registerMessage(RobotInitializationRequestMessage.class, RobotInitializationRequestMessage::new, NetworkManager.c2s());
        registerMessage(RobotInitializationMessage.class, RobotInitializationMessage::new, NetworkManager.s2c());
        registerMessage(OpenRobotInventoryMessage.class, OpenRobotInventoryMessage::new, NetworkManager.c2s());
        registerMessage(OpenRobotTerminalMessage.class, OpenRobotTerminalMessage::new, NetworkManager.c2s());

        registerMessage(ChargerStateMessage.class, ChargerStateMessage::new, NetworkManager.s2c());
        registerMessage(NetworkConnectorConnectionsMessage.class, NetworkConnectorConnectionsMessage::new, NetworkManager.s2c());
        registerMessage(InternetGatewayStateMessage.class, InternetGatewayStateMessage::new, NetworkManager.s2c());
        registerMessage(DiskDriveFloppyMessage.class, DiskDriveFloppyMessage::new, NetworkManager.s2c());
        registerMessage(FlashDriveFlashMemoryMessage.class, FlashDriveFlashMemoryMessage::new, NetworkManager.s2c());

        registerMessage(BusInterfaceNameMessage.ToClient.class, BusInterfaceNameMessage.ToClient::new, NetworkManager.s2c());
        registerMessage(BusInterfaceNameMessage.ToServer.class, BusInterfaceNameMessage.ToServer::new, NetworkManager.c2s());

        registerMessage(ExportedFileMessage.class, ExportedFileMessage::new, NetworkManager.s2c());
        registerMessage(RequestImportedFileMessage.class, RequestImportedFileMessage::new, NetworkManager.s2c());
        registerMessage(ImportedFileMessage.class, ImportedFileMessage::new, NetworkManager.c2s());
        registerMessage(ServerCanceledImportFileMessage.class, ServerCanceledImportFileMessage::new, NetworkManager.s2c());
        registerMessage(ClientCanceledImportFileMessage.class, ClientCanceledImportFileMessage::new, NetworkManager.c2s());

        registerMessage(BusCableFacadeMessage.class, BusCableFacadeMessage::new, NetworkManager.s2c());

        registerMessage(SerialInterfaceCardConfigurationMessage.class, SerialInterfaceCardConfigurationMessage::new, NetworkManager.c2s());
        registerMessage(NetworkInterfaceCardConfigurationMessage.class, NetworkInterfaceCardConfigurationMessage::new, NetworkManager.c2s());
        registerMessage(NetworkTunnelLinkMessage.class, NetworkTunnelLinkMessage::new, NetworkManager.c2s());

        registerMessage(ProjectorRequestFramebufferMessage.class, ProjectorRequestFramebufferMessage::new, NetworkManager.c2s());
        registerMessage(ProjectorFramebufferMessage.class, ProjectorFramebufferMessage::new, NetworkManager.s2c());
        registerMessage(ProjectorStateMessage.class, ProjectorStateMessage::new, NetworkManager.s2c());
        registerMessage(SoundCardAudioMessage.class, SoundCardAudioMessage::new, NetworkManager.s2c());

        registerMessage(TerminalStateMessage.class, TerminalStateMessage::new, NetworkManager.s2c());
        registerMessage(TerminalKeepAliveMessage.class, TerminalKeepAliveMessage::new, NetworkManager.c2s());
        registerMessage(TerminalConfigurationMessage.ToClient.class, TerminalConfigurationMessage.ToClient::new, NetworkManager.s2c());
        registerMessage(TerminalConfigurationMessage.ToServer.class, TerminalConfigurationMessage.ToServer::new, NetworkManager.c2s());
        registerMessage(TerminalInputMessage.class, TerminalInputMessage::new, NetworkManager.c2s());
        registerMessage(TerminalOutputMessage.class, TerminalOutputMessage::new, NetworkManager.s2c());

        registerMessage(KeyboardKeepAliveMessage.class, KeyboardKeepAliveMessage::new, NetworkManager.c2s());
        registerMessage(KeyboardInputMessage.class, KeyboardInputMessage::new, NetworkManager.c2s());

        registerMessage(BlockDeviceDataMessage.class, BlockDeviceDataMessage::new, NetworkManager.s2c());
        registerMessage(ServerConfigMessage.class, ServerConfigMessage::new, NetworkManager.s2c());

        registerMessage(MultipartMessage.class, MultipartMessage::new, NetworkManager.c2s());

        MultipartMessage.registerMessage(ImportedFileMessage.class, ImportedFileMessage::new);
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getMessageType(final Class<?> type) {
        final CustomPacketPayload.Type<? extends CustomPacketPayload> messageType = MESSAGE_TYPES.get(type);
        if (messageType == null) {
            throw new IllegalStateException("Message type [" + type + "] was not registered in Network.initialize().");
        }
        return (CustomPacketPayload.Type<T>) messageType;
    }

    // --------------------------------------------------------------------- //

    public static void sendToServer(final AbstractMessage message) {
        NetworkManager.sendToServer(message);
    }

    public static void sendToClient(final AbstractMessage message, final ServerPlayer player) {
        NetworkManager.sendToPlayer(player, message);
    }

    public static void sendToAllClients(final AbstractMessage message) {
        final MinecraftServer server = GameInstance.getServer();
        if (server == null) {
            return;
        }

        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToClientIfSupported(message, player);
        }
    }

    /**
     * For messages sent unprompted, where the connection may not know the payload.
     */
    public static void sendToClientIfSupported(final AbstractMessage message, final ServerPlayer player) {
        if (NetworkManager.canPlayerReceive(player, message.type())) {
            NetworkManager.sendToPlayer(player, message);
        }
    }

    public static void sendToClientsTrackingChunk(final AbstractMessage message, final LevelChunk chunk) {
        if (chunk.getLevel().getChunkSource() instanceof final ServerChunkCache chunkCache) {
            NetworkManager.sendToPlayers(chunkCache.chunkMap.getPlayers(chunk.getPos(), false), message);
        }
    }

    public static void sendToClientsTrackingBlockEntity(final AbstractMessage message, final BlockEntity blockEntity) {
        final Level level = blockEntity.getLevel();
        if (!checkServerThread(level)) {
            return;
        }

        final BlockPos blockPos = blockEntity.getBlockPos();
        final int chunkX = SectionPos.blockToSectionCoord(blockPos.getX());
        final int chunkZ = SectionPos.blockToSectionCoord(blockPos.getZ());
        if (level.hasChunk(chunkX, chunkZ)) {
            sendToClientsTrackingChunk(message, level.getChunk(chunkX, chunkZ));
        }
    }

    public static void sendToClientsTrackingEntity(final AbstractMessage message, final Entity entity) {
        final Level level = entity.level();
        if (!checkServerThread(level)) {
            return;
        }

        if (level.getChunkSource() instanceof final ServerChunkCache chunkCache) {
            NetworkManager.sendToPlayers(chunkCache.chunkMap.getPlayers(entity.chunkPosition(), false), message);
        }
    }

    // --------------------------------------------------------------------- //

    private static String messageId(final Class<?> type) {
        final String name = type.getEnclosingClass() != null
            ? type.getEnclosingClass().getSimpleName().replaceAll("Message$", "") + "_" + type.getSimpleName()
            : type.getSimpleName().replaceAll("Message$", "");
        return name.replaceAll("(?<=[a-z0-9])(?=[A-Z])", "_").toLowerCase(Locale.ROOT);
    }

    private static <T extends AbstractMessage> void registerMessage(final Class<T> type,
                                                                    final Function<RegistryFriendlyByteBuf, T> decoder,
                                                                    final NetworkManager.Side side) {
        final Identifier id = Identifier.fromNamespaceAndPath(API.MOD_ID, messageId(type));
        final CustomPacketPayload.Type<T> payloadType = new CustomPacketPayload.Type<>(id);
        final StreamCodec<RegistryFriendlyByteBuf, T> codec =
            CustomPacketPayload.codec(AbstractMessage::toBytes, decoder::apply);

        MESSAGE_TYPES.put(type, payloadType);

        if (side == NetworkManager.s2c() && Platform.getEnvironment() != Env.CLIENT) {
            NetworkManager.registerS2CPayloadType(payloadType, codec);
        } else {
            NetworkManager.registerReceiver(side, payloadType, codec,
                (message, context) -> context.queue(() -> message.handle(context)));
        }
    }

    private static boolean checkServerThread(@Nullable final Level level) {
        if (level == null) {
            return false;
        }

        if (level.getServer() == null) {
            return false;
        }

        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException(
                "Attempting to send location aware network message to Entity from non-server " +
                    "thread [" + Thread.currentThread() + "]. This is not supported, " +
                    "because we need world state to figure out chunk/entity tracking. " +
                    "Queue this message send on the server thread, instead.");
        }

        return true;
    }
}
