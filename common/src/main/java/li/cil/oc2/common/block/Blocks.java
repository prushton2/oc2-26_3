/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

public final class Blocks {
    private static final DeferredRegister<Block> BLOCKS = RegistryUtils.getInitializerFor(Registries.BLOCK);

    // --------------------------------------------------------------------- //

    public static final RegistrySupplier<BusCableBlock> BUS_CABLE = RegistryUtils.register(BLOCKS, "bus_cable", BusCableBlock::new);
    public static final RegistrySupplier<BusCableSupportBlock> BUS_CABLE_SUPPORT = RegistryUtils.register(BLOCKS, "bus_cable_support", BusCableSupportBlock::new);
    public static final RegistrySupplier<ChargerBlock> CHARGER = RegistryUtils.register(BLOCKS, "charger", ChargerBlock::new);
    public static final RegistrySupplier<ComputerBlock> COMPUTER = RegistryUtils.register(BLOCKS, "computer", ComputerBlock::new);
    public static final RegistrySupplier<CreativeEnergyBlock> CREATIVE_ENERGY = RegistryUtils.register(BLOCKS, "creative_energy", CreativeEnergyBlock::new);
    public static final RegistrySupplier<DiskDriveBlock> DISK_DRIVE = RegistryUtils.register(BLOCKS, "disk_drive", DiskDriveBlock::new);
    public static final RegistrySupplier<FlashDriveBlock> FLASH_DRIVE = RegistryUtils.register(BLOCKS, "flash_drive", FlashDriveBlock::new);
    public static final RegistrySupplier<KeyboardBlock> KEYBOARD = RegistryUtils.register(BLOCKS, "keyboard", KeyboardBlock::new);
    public static final RegistrySupplier<NetworkConnectorBlock> NETWORK_CONNECTOR = RegistryUtils.register(BLOCKS, "network_connector", NetworkConnectorBlock::new);
    public static final RegistrySupplier<NetworkHubBlock> NETWORK_HUB = RegistryUtils.register(BLOCKS, "network_hub", NetworkHubBlock::new);
    public static final RegistrySupplier<InternetGatewayBlock> INTERNET_GATEWAY = RegistryUtils.register(BLOCKS, "internet_gateway", InternetGatewayBlock::new);
    public static final RegistrySupplier<ProjectorBlock> PROJECTOR = RegistryUtils.register(BLOCKS, "projector", ProjectorBlock::new);
    public static final RegistrySupplier<RedstoneInterfaceBlock> REDSTONE_INTERFACE = RegistryUtils.register(BLOCKS, "redstone_interface", RedstoneInterfaceBlock::new);
    public static final RegistrySupplier<TerminalBlock> TERMINAL = RegistryUtils.register(BLOCKS, "terminal", TerminalBlock::new);
    public static final RegistrySupplier<TransposerBlock> TRANSPOSER = RegistryUtils.register(BLOCKS, "transposer", TransposerBlock::new);

    // --------------------------------------------------------------------- //

    public static void initialize() {
    }
}
