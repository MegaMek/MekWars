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

/**
 * A {@link JTextField} restricted to whole (non-negative integer) numeric input, used throughout the MekWars
 * client wherever a user must enter a plain integer (e.g. quantity fields). Non-digit characters typed or pasted
 * into the field are silently dropped and the system beeps, rather than the field rejecting the whole edit; see
 * {@link WholeNumberDocument#insertString}.
 */
public class WholeNumberField extends JTextField {
    private static final MMLogger LOGGER = MMLogger.create(WholeNumberField.class);
    /**
     * Serialization version identifier for this {@link JTextField}.
     */
    @Serial
    private static final long serialVersionUID = 3640879107242018821L;
    /** Used to produce an audible beep when invalid (non-digit) input is rejected. */
    private final Toolkit toolkit;
    /** Formatter used both to render the field's integer value as text and to parse it back out. */
    private final NumberFormat integerFormatter;

    /**
     * Creates a field of the given display width, initialized to the given value.
     *
     * @param value   the initial integer value to display
     * @param columns the number of columns (roughly, characters) wide the text field should be
     */
    public WholeNumberField(int value, int columns) {
        super(columns);
        toolkit = Toolkit.getDefaultToolkit();
        integerFormatter = NumberFormat.getNumberInstance(Locale.US);
        integerFormatter.setParseIntegerOnly(true);
        setValue(value);
    }

    /**
     * Parses the field's current text back into an {@code int} using the US-locale integer formatter. If parsing
     * fails (which the original author notes "in theory, shouldn't occur" given the input filtering in
     * {@link WholeNumberDocument}), the error is logged, the system beeps, and {@code 0} is returned instead of
     * throwing.
     *
     * @return the field's current value as an integer, or {@code 0} if it could not be parsed
     */
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

    /**
     * Replaces the field's text with the formatted representation of the given value.
     */
    public void setValue(int value) {
        setText(integerFormatter.format(value));
    }

    /**
     * Overrides the default document model with {@link WholeNumberDocument} so all edits (typing, pasting) are
     * filtered to digits only.
     */
    @Override
    protected Document createDefaultModel() {
        return new WholeNumberField.WholeNumberDocument();
    }

    /**
     * A {@link PlainDocument} that filters out any non-digit characters from text being inserted, so the field can
     * never contain anything but digits (0-9). Note this means only non-negative whole numbers can be entered -
     * there is no support for a leading minus sign.
     */
    protected class WholeNumberDocument extends PlainDocument {
        /**
         * Serialization version identifier for this {@link PlainDocument}.
         */
        @Serial
        private static final long serialVersionUID = -6680227995973307297L;

        /**
         * Filters {@code str} down to just its digit characters before inserting it into the document. Any
         * non-digit character is silently dropped (not inserted) and triggers a beep and a debug log entry, rather
         * than rejecting the whole insert or throwing.
         *
         * @param offs          offset into the document to insert at
         * @param str           the text being inserted (e.g. typed or pasted)
         * @param attributeSet  attributes to associate with the inserted text
         *
         * @throws BadLocationException if the insert offset is invalid for the document
         */
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

