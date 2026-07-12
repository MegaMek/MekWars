package mekwars.common.gui.panels;

import java.awt.Rectangle;
import java.io.Serial;
import javax.swing.text.DefaultCaret;

/**
 * A {@link DefaultCaret} for text components (e.g. chat/log panes) that can be told to stop auto-scrolling the
 * viewport into view. Normally Swing scrolls a text component so the caret stays visible whenever the document
 * changes; setting {@link #showCaret} to {@code false} suppresses that so a user who has scrolled up to read older
 * text isn't yanked back to the bottom every time new text arrives.
 */
class ScrollCaret extends javax.swing.text.DefaultCaret {
    /** Serialization version identifier. */
    @Serial
    private static final long serialVersionUID = 7038682935535985621L;

    /** When {@code false}, {@link #adjustVisibility(Rectangle)} is a no-op and the view will not auto-scroll. */
    public boolean showCaret = true;

    /**
     * Scrolls the caret into view, unless {@link #showCaret} is {@code false}, in which case scrolling is
     * suppressed entirely.
     *
     * @see DefaultCaret#adjustVisibility(Rectangle)
     */
    @Override
    protected void adjustVisibility(Rectangle nloc) {
        if (showCaret) {super.adjustVisibility(nloc);}
    }
}
