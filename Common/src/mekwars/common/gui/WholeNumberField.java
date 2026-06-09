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

import java.awt.Toolkit;
import java.io.Serial;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import megamek.logging.MMLogger;

public class WholeNumberField extends JTextField {
    private static final MMLogger LOGGER = MMLogger.create(WholeNumberField.class);
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 3640879107242018821L;
    private final Toolkit toolkit;
    private final NumberFormat integerFormatter;

    public WholeNumberField(int value, int columns) {
        super(columns);
        toolkit = Toolkit.getDefaultToolkit();
        integerFormatter = NumberFormat.getNumberInstance(Locale.US);
        integerFormatter.setParseIntegerOnly(true);
        setValue(value);
    }

    public int getValue() {
        int retVal = 0;

        try {
            retVal = integerFormatter.parse(getText()).intValue();
        } catch (ParseException ex) {
            LOGGER.error(ex, "In theory, shouldn't occur. But theory doesn't match practice. : {}",
                  ex.getLocalizedMessage());
            toolkit.beep();
        }
        return retVal;
    }

    public void setValue(int value) {
        setText(integerFormatter.format(value));
    }

    @Override
    protected Document createDefaultModel() {
        return new WholeNumberField.WholeNumberDocument();
    }

    protected class WholeNumberDocument extends PlainDocument {
        /**
         *
         */
        @Serial
        private static final long serialVersionUID = -6680227995973307297L;

        @Override
        public void insertString(int offs, String str, AttributeSet attributeSet) throws BadLocationException {
            char[] source = str.toCharArray();
            char[] result = new char[source.length];
            int j = 0;

            for (int i = 0; i < result.length; i++) {
                if (Character.isDigit(source[i])) {result[j++] = source[i];} else {
                    toolkit.beep();
                    LOGGER.debug("insertString: {}", source[i]);
                }
            }
            
            super.insertString(offs, new String(result, 0, j), attributeSet);
        }
    }
}

