/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.pip;

import li.cil.oc2.client.gui.widget.Texture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * A block (in its item form) rendered in a GUI, rotated freely, with textures overlaid on its faces.
 *
 * @param block          the block's item to render.
 * @param rotationDegrees the rotation of the block around its center, in GUI space, in degrees per axis.
 * @param faceOverlays   textures to render on top of block faces, applied in order.
 */
@Environment(EnvType.CLIENT)
public record BlockPreviewRenderState(
    ItemStackRenderState block,
    Vector3fc rotationDegrees,
    Map<Direction, Texture[]> faceOverlays,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public BlockPreviewRenderState(
        final ItemStackRenderState block,
        final Vector3fc rotationDegrees,
        final Map<Direction, Texture[]> faceOverlays,
        final int x0,
        final int y0,
        final int x1,
        final int y1,
        final float scale,
        @Nullable final ScreenRectangle scissorArea
    ) {
        this(block, rotationDegrees, faceOverlays, x0, y0, x1, y1, scale, scissorArea,
            PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
