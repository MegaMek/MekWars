/*
 * MekWars - Copyright (C) 2014
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package mekwars.common.gui.dialogs.opviewer;

import java.io.Serial;
import javax.swing.JButton;

/**
 * A navigation button shown in the anchor/table-of-contents panel of the {@link OperationViewerDialog}.
 * Its visible label ({@code display}) can differ from the HTML named-anchor id ({@code url}) it jumps
 * to when clicked; the id is later used by {@link OperationViewerDialog#setHTMLLocation(String)} (via
 * an action listener registered by the dialog) to scroll the currently displayed operation pane to
 * that anchor.
 */
public class OpViewerAnchorButton extends JButton {

    @Serial
    private static final long serialVersionUID = 1L;
    /** The HTML named-anchor id this button scrolls to, not the button's visible text. */
    private final String url;

    /**
     * @param id the HTML named-anchor id to scroll to when this button is clicked
     * @param display the text shown on the button
     */
    public OpViewerAnchorButton(String id, String display) {
        super(display);
        url = id;
    }

    /**
     * @return the HTML named-anchor id associated with this button (not its visible label)
     */
    public String getUrl() {
        return url;
    }
}
