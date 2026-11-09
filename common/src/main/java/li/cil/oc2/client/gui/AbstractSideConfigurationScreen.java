/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.pip.BlockPreviewRenderState;
import li.cil.oc2.client.gui.pip.BlockPreviewRenderer;
import li.cil.oc2.client.gui.widget.Texture;
import li.cil.oc2.common.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

import static li.cil.oc2.common.util.TranslationUtils.key;
import static li.cil.oc2.common.util.TranslationUtils.text;

public abstract class AbstractSideConfigurationScreen extends Screen {
    private static final String SIDE_STATE_TEXT = key("gui.{mod}.sided_device.side_state");
    private static final Component CONNECTIVITY_ENABLED_TEXT = text("gui.{mod}.sided_device.connectivity.enabled");
    private static final Component CONNECTIVITY_DISABLED_TEXT = text("gui.{mod}.sided_device.connectivity.disabled");
    private static final Component HELP_TEXT = text("gui.{mod}.sided_device.help");
    private static final Component HELP_LABEL = Component.literal("?");

    public static final int UI_WIDTH = Sprites.SIDED_DEVICE_SCREEN.width;
    public static final int UI_HEIGHT = Sprites.SIDED_DEVICE_SCREEN.height;
    public static final int BLOCK_LEFT = UI_WIDTH / 2;
    public static final int BLOCK_TOP = 61;
    public static final int BLOCK_AREA_RIGHT = 139;
    public static final int BLOCK_AREA_LEFT = 37;
    public static final int BLOCK_AREA_TOP = 10;
    public static final int BLOCK_AREA_SIZE = 102;
    public static final int HELP_MARGIN = 2;
    public static final int HELP_SIZE = 9;
    public static final int HELP_COLOR = 0x555555;
    public static final int MAX_BLOCK_PITCH = 30;

    // --------------------------------------------------------------------- //

    protected final Player player;
    protected final InteractionHand hand;

    private final ComputerBlockItemRenderer computerBlockItemRenderer = new ComputerBlockItemRenderer();
    private final Item item;

    private Vector3f blockRotation = new Vector3f(-30, 45, 0);
    protected int left, top;
    @Nullable
    private Direction focusedSide;
    private boolean isDraggingBlock, hasDraggedBlock;
    private double dragStartX, dragStartY;

    // --------------------------------------------------------------------- //

    protected AbstractSideConfigurationScreen(final Player player, final InteractionHand hand, final Item item) {
        super(Component.translatable(item.getDescriptionId()));
        this.player = player;
        this.hand = hand;
        this.item = item;
    }

    // --------------------------------------------------------------------- //

    protected abstract boolean getConfiguration(@Nullable final Direction side);

    protected abstract void sendConfiguration(final Direction side, final boolean value);

    // --------------------------------------------------------------------- //

    @Override
    protected void init() {
        super.init();

        left = (width - UI_WIDTH) / 2;
        top = (height - UI_HEIGHT) / 2;
    }

    @Override
    public void tick() {
        super.tick();

        final ItemStack heldItem = player.getItemInHand(hand);
        if (!heldItem.is(item)) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        final double mouseX = event.x();
        final double mouseY = event.y();
        final int button = event.button();
        final boolean result = super.mouseClicked(event, doubleClick);

        if (!result && isMouseInBlockArea(mouseX, mouseY) && button == 0) {
            isDraggingBlock = true;
            hasDraggedBlock = false;
            dragStartX = mouseX;
            dragStartY = mouseY;
        }

        return result;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        final int button = event.button();
        if (isDraggingBlock && button == 0) {
            isDraggingBlock = false;
            if (!hasDraggedBlock && focusedSide != null) {
                sendConfiguration(focusedSide, !getConfiguration(focusedSide));
                Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
            }
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double deltaX, final double deltaY) {
        final double mouseX = event.x();
        final double mouseY = event.y();
        final int activeButton = event.button();
        if (!isDraggingBlock) {
            return super.mouseDragged(event, deltaX, deltaY);
        }

        if (activeButton == 0) {
            if (!hasDraggedBlock) {
                final double dx = mouseX - dragStartX;
                final double dy = mouseY - dragStartY;
                final double delta = Math.sqrt(dx * dx + dy * dy);
                hasDraggedBlock = delta > 3;
            }
            if (hasDraggedBlock) {
                blockRotation = new Vector3f(
                    Mth.clamp(blockRotation.x() - (float) deltaY, -MAX_BLOCK_PITCH, MAX_BLOCK_PITCH),
                    Mth.wrapDegrees(blockRotation.y() + (float) deltaX),
                    blockRotation.z()
                );
            }
        }

        return true;
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        extractTransparentBackground(graphics);
        Sprites.SIDED_DEVICE_SCREEN.draw(graphics, left, top);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

        final int blockX = left + BLOCK_LEFT;
        final int blockY = top + BLOCK_TOP;
        focusedSide = computerBlockItemRenderer.getFocusedSide(blockX - mouseX, blockY - mouseY, blockRotation);
        computerBlockItemRenderer.extract(graphics, blockRotation);

        if (focusedSide != null) {
            final Component enabledComponent = getConfiguration(focusedSide) ? CONNECTIVITY_ENABLED_TEXT : CONNECTIVITY_DISABLED_TEXT;
            final MutableComponent tooltip = Component.translatable(SIDE_STATE_TEXT, enabledComponent);
            graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }

        graphics.text(font, HELP_LABEL, left + helpLeft(), top + BLOCK_AREA_TOP + HELP_MARGIN, HELP_COLOR, false);
        if (isMouseOverHelp(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, font.split(HELP_TEXT, 160), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --------------------------------------------------------------------- //

    private int helpLeft() {
        return BLOCK_AREA_RIGHT - HELP_MARGIN - font.width(HELP_LABEL);
    }

    private boolean isMouseOverHelp(final double mouseX, final double mouseY) {
        return mouseX >= left + helpLeft() && mouseX <= left + helpLeft() + font.width(HELP_LABEL) &&
            mouseY >= top + BLOCK_AREA_TOP + HELP_MARGIN && mouseY <= top + BLOCK_AREA_TOP + HELP_MARGIN + HELP_SIZE;
    }

    private boolean isMouseInBlockArea(final double mouseX, final double mouseY) {
        return mouseX >= left + BLOCK_AREA_LEFT && mouseX <= left + BLOCK_AREA_LEFT + BLOCK_AREA_SIZE &&
            mouseY >= top + BLOCK_AREA_TOP && mouseY <= top + BLOCK_AREA_TOP + BLOCK_AREA_SIZE;
    }

    // --------------------------------------------------------------------- //

    private final class ComputerBlockItemRenderer {
        public static final int BLOCK_RENDER_SIZE = 48;

        private final ItemStack computerItemStack = new ItemStack(Items.COMPUTER.get());
        private final ItemStackRenderState computerRenderState = new ItemStackRenderState();

        @Nullable
        private Direction getFocusedSide(final float mouseX, final float mouseY, final Vector3f rotation) {
            // Rotate ray inversely around block to represent visual block rotation.
            final Quaternionf quaternion = fromXYZDegrees(toRenderRotation(rotation));
            quaternion.conjugate();

            // Move ray in screen space to mouse position.
            final float relMouseX = -mouseX / BLOCK_RENDER_SIZE;
            final float relMouseY = -mouseY / BLOCK_RENDER_SIZE;

            final Vector3f source = new Vector3f();
            source.add(relMouseX, relMouseY, 1);
            source.rotate(quaternion);

            final Vector3f target = new Vector3f();
            target.add(relMouseX, relMouseY, -1);
            target.rotate(quaternion);

            // Intersect rotated ray with bounding box representing block.
            final AABB aabb = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
            return aabb.clip(new Vec3(source), new Vec3(target))
                .map(hit -> Direction.getApproximateNearest(-hit.x, -hit.y(), -hit.z()))
                .filter(side -> side != Direction.SOUTH)
                .orElse(null);
        }

        public void extract(final GuiGraphicsExtractor graphics, final Vector3f rotation) {
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                computerRenderState, computerItemStack, ItemDisplayContext.NONE, null, null, 0);

            final Map<Direction, Texture[]> overlays = new EnumMap<>(Direction.class);
            for (final Direction side : Direction.values()) {
                // South face of computers is the front face (screen) and there's no connectivity allowed there.
                if (side == Direction.SOUTH) {
                    continue;
                }

                final Texture state = getConfiguration(side)
                    ? Textures.BLOCK_FACE_ENABLED_TEXTURE
                    : Textures.BLOCK_FACE_DISABLED_TEXTURE;
                overlays.put(side, side == focusedSide
                    ? new Texture[]{state, Textures.BLOCK_FACE_FOCUSED_TEXTURE}
                    : new Texture[]{state});
            }

            BlockPreviewRenderer.extract(graphics, new BlockPreviewRenderState(
                computerRenderState,
                toRenderRotation(rotation),
                overlays,
                left + BLOCK_AREA_LEFT, top + BLOCK_AREA_TOP,
                left + BLOCK_AREA_LEFT + BLOCK_AREA_SIZE, top + BLOCK_AREA_TOP + BLOCK_AREA_SIZE,
                BLOCK_RENDER_SIZE,
                graphics.scissorStack.peek()));
        }

        private static Vector3f toRenderRotation(final Vector3f rotation) {
            return new Vector3f(rotation).add(0, 180, 0);
        }

        private static Quaternionf fromXYZDegrees(final Vector3f degrees) {
            return new Quaternionf().rotationXYZ(
                (float) Math.toRadians(degrees.x()),
                (float) Math.toRadians(degrees.y()),
                (float) Math.toRadians(degrees.z()));
        }
    }
}
