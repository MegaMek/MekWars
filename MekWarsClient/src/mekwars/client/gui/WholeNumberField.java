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

import common.util.MWLogger;

public class WholeNumberField extends javax.swing.JTextField {
    /**
     *
     */
    private static final long serialVersionUID = 3640879107242018821L;
    private java.awt.Toolkit toolkit;
    private java.text.NumberFormat integerFormatter;

    public WholeNumberField(int value, int columns) {
        super(columns);
        toolkit = java.awt.Toolkit.getDefaultToolkit();
        integerFormatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US);
        integerFormatter.setParseIntegerOnly(true);
        setValue(value);
    }

    public int getValue() {
        int retVal = 0;
        try {
            retVal = integerFormatter.parse(getText()).intValue();
        } catch (java.text.ParseException e) {
            // This should never happen because insertString allows
            // only properly formatted data to get in the field.
            toolkit.beep();
        }
        return retVal;
    }

    public void setValue(int value) {
        setText(integerFormatter.format(value));
    }

    @Override
    protected javax.swing.text.Document createDefaultModel() {
        return new mekwars.client.gui.WholeNumberField.WholeNumberDocument();
    }

    protected class WholeNumberDocument extends javax.swing.text.PlainDocument {
        /**
         *
         */
        private static final long serialVersionUID = -6680227995973307297L;

        @Override
        public void insertString(int offs,
              String str,
              javax.swing.text.AttributeSet a)
              throws javax.swing.text.BadLocationException {
            char[] source = str.toCharArray();
            char[] result = new char[source.length];
            int j = 0;

            for (int i = 0; i < result.length; i++) {
                if (Character.isDigit(source[i])) {result[j++] = source[i];} else {
                    toolkit.beep();
                    MWLogger.errLog("insertString: " + source[i]);
                }
            }
            super.insertString(offs, new String(result, 0, j), a);
        }
    }
}

