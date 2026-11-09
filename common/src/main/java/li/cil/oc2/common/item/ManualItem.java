/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

/**
 * The in-game manual relies on the Markdown Manual library, which is not yet available for this
 * Minecraft version. The item is kept registered so that existing recipes, loot and worlds stay
 * valid, but it currently has no behavior beyond its tooltip.
 */
public final class ManualItem extends ModItem {
}
