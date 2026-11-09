/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.neoforge;

import li.cil.oc2.api.API;
import li.cil.oc2.common.Main;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(API.MOD_ID)
public final class MainNeoForge {
    public static ModContainer MOD_CONTAINER;

    public MainNeoForge(final ModContainer modContainer) {
        MOD_CONTAINER = modContainer;

        Main.initialize();
    }
}
