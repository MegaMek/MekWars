/*
 * MekWars - Copyright (C) 2013
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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
 *
 */
package mekwars.common.gui.dialogs.opviewer;


import java.io.Serial;
import javax.swing.JTextPane;

/**
 * A {@link JTextPane} that renders a single Operation's fully-substituted HTML content (as produced by
 * {@link OperationViewerDialog#getOpHTML(mekwars.common.campaign.operations.Operation)}) inside the
 * {@link OperationViewerDialog}. One instance is created per Operation the dialog knows about
 * (including the synthetic "Defaults" entry), and the dialog swaps which instance is visible when the
 * player changes the operation selector combo box.
 */
public class OpViewerOpPane extends JTextPane implements IOpViewerPane {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates the pane and immediately loads the given HTML as its content.
     *
     * @param s complete HTML markup for this operation, ready to display
     */
    public OpViewerOpPane(String s) {
        super();
        setHTMLContents(s);
    }

    /**
     * Switches this pane's content type to {@code text/html} (so Swing's HTML renderer is used) and then
     * sets the text. The content type must be set before the text for the markup to be parsed as HTML
     * rather than shown as literal text.
     */
    public void setHTMLContents(String s) {
        this.setContentType("text/html");
        this.setText(s);
    }

}
