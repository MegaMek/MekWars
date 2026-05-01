/*
 * MekWars - Copyright (C) 2004
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
 */

package mekwars.common.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

/**
 * Helper to encode and decode typical fields of classes
 * <p>
 * currently handled types are: - boolean - int - String - double
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BinReader {
    final private static MMLogger LOGGER = MMLogger.create(BinReader.class);

    private final BufferedReader in;

    /**
     * Construct an BinReader
     */
    public BinReader(Reader in) {
        this.in = new BufferedReader(in);
    }

    /**
     * Reads an integer
     */
    public int readInt(String debugName) {
        return MathUtility.parseInt(read(debugName), 0);
    }

    @Nullable
    public String read(String debugName) {
        try {
            String s = in.readLine();

            if (!s.substring(0, s.indexOf('=')).equals(debugName)) {
                return null;
            }

            return s.substring(s.indexOf('=') + 1);
        } catch (IOException e) {
            LOGGER.error(e, "Could not read from input stream");
        }

        return null;
    }

    /**
     * Reads an double
     */
    public double readDouble(String debugName) {
        return MathUtility.parseDouble(read(debugName), 0.0);
    }

    /**
     * Reads an boolean
     */
    public boolean readBoolean(String debugName) {
        String string = read(debugName);

        if (string == null) {
            return false;
        }

        return !string.equalsIgnoreCase("false") && !string.equals("0") && !string.isEmpty();
    }

    /**
     * Closes the input.
     */
    public void close() throws IOException {
        in.close();
    }
}
