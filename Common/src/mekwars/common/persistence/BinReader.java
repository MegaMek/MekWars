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
 * Reads the simple {@code name=value} line-oriented text format written by {@link BinWriter}, used to persist
 * fields of common data classes (e.g. save-game / campaign state) to disk.
 * <p>
 * Currently handled value types are: boolean, int, String, and double. Each read call consumes exactly one line
 * from the underlying stream and expects that line's key (the text before {@code '='}) to match the
 * {@code debugName} passed in, which acts both as a field identifier and a sanity check that the reader and writer
 * agree on field order.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BinReader {
    final private static MMLogger LOGGER = MMLogger.create(BinReader.class);

    private final BufferedReader in;

    /**
     * Wraps {@code in} in a {@link BufferedReader} for line-based reading.
     *
     * @param in the underlying character stream to read {@code name=value} lines from
     */
    public BinReader(Reader in) {
        this.in = new BufferedReader(in);
    }

    /**
     * Reads the next line as an integer field.
     *
     * @param debugName expected field name; must match the key on the next line
     *
     * @return the parsed integer, or {@code 0} if the line is missing, malformed, or the key does not match
     */
    public int readInt(String debugName) {
        return MathUtility.parseInt(read(debugName), 0);
    }

    /**
     * Reads the next line from the stream and validates that its {@code key=value} key matches {@code debugName}.
     *
     * @param debugName expected field name; must match the key on the next line
     *
     * @return the raw value portion of the line (everything after the first {@code '='}), or {@code null} if the
     *         key does not match {@code debugName} or the line could not be read
     */
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
     * Reads the next line as a double field.
     *
     * @param debugName expected field name; must match the key on the next line
     *
     * @return the parsed double, or {@code 0.0} if the line is missing, malformed, or the key does not match
     */
    public double readDouble(String debugName) {
        return MathUtility.parseDouble(read(debugName), 0.0);
    }

    /**
     * Reads the next line as a boolean field. Any value other than {@code "false"} (case-insensitive), {@code "0"},
     * or empty is treated as {@code true}.
     *
     * @param debugName expected field name; must match the key on the next line
     *
     * @return the parsed boolean, or {@code false} if the line is missing or the key does not match
     */
    public boolean readBoolean(String debugName) {
        String string = read(debugName);

        if (string == null) {
            return false;
        }

        return !string.equalsIgnoreCase("false") && !string.equals("0") && !string.isEmpty();
    }

    /**
     * Closes the underlying input stream.
     */
    public void close() throws IOException {
        in.close();
    }
}
