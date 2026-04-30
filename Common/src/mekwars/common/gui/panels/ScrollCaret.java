package mekwars.common.gui.panels;

import java.awt.Rectangle;
import java.io.Serial;
import javax.swing.text.DefaultCaret;

/**
 * Caret that does not auto-scroll if nothing is selected.
 */
class ScrollCaret extends javax.swing.text.DefaultCaret {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 7038682935535985621L;
    public boolean showCaret = true;

    /**
     * @see DefaultCaret#adjustVisibility(Rectangle)
     */
    @Override
    protected void adjustVisibility(Rectangle nloc) {
        if (showCaret) {super.adjustVisibility(nloc);}
    }
}
