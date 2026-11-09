/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An edit box that rejects edits resulting in values not accepted by a filter.
 * <p>
 * Invalid edits are reverted to the last valid value and are never passed on to the responder.
 */
@Environment(EnvType.CLIENT)
public final class FilteredEditBox extends EditBox {
    private final Predicate<String> filter;
    private Consumer<String> responder = value -> { };
    private String lastValidValue = "";
    private boolean isReverting;

    // --------------------------------------------------------------------- //

    public FilteredEditBox(final Font font, final int x, final int y, final int width, final int height, final Component narration, final Predicate<String> filter) {
        super(font, x, y, width, height, narration);
        this.filter = filter;
        super.setResponder(this::handleValueChanged);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void setResponder(final Consumer<String> responder) {
        this.responder = responder;
    }

    @Override
    public void setValue(final String value) {
        if (filter.test(value)) {
            super.setValue(value);
        }
    }

    // --------------------------------------------------------------------- //

    private void handleValueChanged(final String value) {
        if (filter.test(value)) {
            lastValidValue = value;
            responder.accept(value);
        } else if (!isReverting) {
            isReverting = true;
            try {
                final int cursor = Math.min(getCursorPosition(), lastValidValue.length());
                super.setValue(lastValidValue);
                setCursorPosition(cursor);
            } finally {
                isReverting = false;
            }
        }
    }
}
