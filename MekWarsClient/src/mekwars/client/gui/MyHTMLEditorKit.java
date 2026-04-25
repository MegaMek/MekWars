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

package mekwars.client.gui;

public class MyHTMLEditorKit extends javax.swing.text.html.HTMLEditorKit {

    /**
     *
     */
    private static final long serialVersionUID = -891227318566572289L;


    @Override
    public javax.swing.text.ViewFactory getViewFactory() {
        return new mekwars.client.gui.MyHTMLEditorKit.HTMLFactoryX();
    }


    public static class HTMLFactoryX extends javax.swing.text.html.HTMLEditorKit.HTMLFactory
          implements javax.swing.text.ViewFactory {

        @Override
        public javax.swing.text.View create(javax.swing.text.Element elem) {
            Object o =
                  elem.getAttributes().getAttribute(javax.swing.text.StyleConstants.NameAttribute);
            if (o instanceof javax.swing.text.html.HTML.Tag) {
                javax.swing.text.html.HTML.Tag kind = (javax.swing.text.html.HTML.Tag) o;
                if (kind == javax.swing.text.html.HTML.Tag.IMG) {return new MyImageView(elem);}
            }
            return super.create(elem);
        }
    }
}










