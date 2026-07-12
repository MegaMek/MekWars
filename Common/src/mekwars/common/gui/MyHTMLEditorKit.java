/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

package mekwars.common.gui;

import java.io.Serial;

import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;

/**
 * A custom {@link HTMLEditorKit} used throughout the MekWars client wherever HTML content is rendered in Swing
 * text components (e.g. chat displays, unit tooltips, informational panes). The only customization it provides is
 * swapping in {@link HTMLFactoryX} as the view factory, which in turn renders {@code <img>} tags using
 * {@link MyImageView} instead of Swing's default image view.
 */
public class MyHTMLEditorKit extends HTMLEditorKit {

    /**
     * Serialization version identifier for this {@link javax.swing.text.EditorKit}.
     */
    @Serial
    private static final long serialVersionUID = -891227318566572289L;

    /**
     * Supplies the custom view factory ({@link HTMLFactoryX}) so that this editor kit renders images via
     * {@link MyImageView}.
     *
     * @return a new {@link HTMLFactoryX} instance
     */
    @Override
    public ViewFactory getViewFactory() {
        return new HTMLFactoryX();
    }
}
