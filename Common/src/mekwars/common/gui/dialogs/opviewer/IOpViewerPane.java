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

/**
 * Contract for a Swing component within the {@link OperationViewerDialog} that can display a block of
 * rendered HTML. Implemented by {@link OpViewerOpPane}, which shows the fully-substituted template
 * content for a single Operation.
 */
public interface IOpViewerPane {
    /**
     * Replaces the pane's displayed content with the given HTML markup.
     *
     * @param s a complete HTML document/fragment to render
     */
    void setHTMLContents(String s);
}
