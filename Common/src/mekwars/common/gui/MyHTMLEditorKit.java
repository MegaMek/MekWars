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

import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import mekwars.client.gui.MyImageView;

public class MyHTMLEditorKit extends HTMLEditorKit {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -891227318566572289L;


    @Override
    public ViewFactory getViewFactory() {
        return new MyHTMLEditorKit.HTMLFactoryX();
    }


    public static class HTMLFactoryX extends HTMLEditorKit.HTMLFactory implements ViewFactory {

        @Override
        public View create(Element elem) {
            Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (o instanceof HTML.Tag kind) {
                if (kind == HTML.Tag.IMG) {
                    return new MyImageView(elem);
                }
            }
            return super.create(elem);
        }
    }
}










