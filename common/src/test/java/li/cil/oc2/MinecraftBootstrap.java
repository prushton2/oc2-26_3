/* SPDX-License-Identifier: MIT */

package li.cil.oc2;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class MinecraftBootstrap implements BeforeAllCallback {
    private static boolean isComponentsBound;

    @Override
    public void beforeAll(final ExtensionContext context) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindDefaultComponents();
    }

    /**
     * Default data components (e.g. of items) are bound when the server loads its resources. Without a server, bind
     * them from the built-in registries, so things like item stacks can be created in tests.
     */
    private static synchronized void bindDefaultComponents() {
        if (isComponentsBound) {
            return;
        }

        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
            .build(VanillaRegistries.createWorldLookup())
            .forEach(DataComponentInitializers.PendingComponents::apply);
        isComponentsBound = true;
    }
}
