/*
 * MekWars - Copyright (C) 2008
 *
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

/**
 * @author jtighe
 *       <p>
 *       Allows for the reading of a StringTokenizer Token and for easy to use Error Trapping
 */

package mekwars.common.util;

import java.util.StringTokenizer;

/**
 * Static helper for reading typed values from a {@link StringTokenizer} (as used throughout MekWars' legacy
 * pipe/token-delimited save and network data formats) without every call site having to write its own
 * try/catch around {@code nextToken()}/parsing. Every method here swallows all exceptions (missing token,
 * malformed number, etc.) and returns a sentinel failure value instead of throwing, so callers cannot distinguish
 * "no more tokens" from "token present but unparsable" — both look like the same failure value. The original
 * error-logging calls are commented out at each catch site (not currently reinstated).
 *
 * @author jtighe
 */
public class TokenReader {

    /**
     * Reads and trims the next token as a string.
     *
     * @param st the tokenizer to read from
     * @return the next token, trimmed of leading/trailing whitespace, or the literal string {@code "-1"} if there
     *     is no next token or reading it fails
     */
    public static String readString(StringTokenizer st) {
        try {
            return st.nextToken().trim();
        } catch (Exception ex) {
            //MWLogger.errLog(ex);
            return "-1";
        }
    }

    /**
     * Reads the next token and parses it as an {@code int}.
     *
     * @param st the tokenizer to read from
     * @return the parsed value, or {@code -1} if there is no next token or it is not a valid integer
     */
    public static int readInt(StringTokenizer st) {
        try {
            return Integer.parseInt(st.nextToken());
        } catch (Exception ex) {
            //MWLogger.errLog(ex);
            return -1;
        }
    }

    /**
     * Reads the next token and parses it as a {@code long}.
     *
     * @param st the tokenizer to read from
     * @return the parsed value, or {@code -1} if there is no next token or it is not a valid long
     */
    public static long readLong(StringTokenizer st) {
        try {
            return Long.parseLong(st.nextToken());
        } catch (Exception ex) {
            //MWLogger.errLog(ex);
            return -1;
        }
    }

    /**
     * Reads the next token and parses it as a {@code float}.
     *
     * @param st the tokenizer to read from
     * @return the parsed value, or {@code -1} if there is no next token or it is not a valid float
     */
    public static float readFloat(StringTokenizer st) {
        try {
            return Float.parseFloat(st.nextToken());
        } catch (Exception ex) {
            //MWLogger.errLog(ex);
            return -1;
        }
    }

    /**
     * Reads the next token and parses it as a {@code double}.
     *
     * @param st the tokenizer to read from
     * @return the parsed value, or {@code -1} if there is no next token or it is not a valid double
     */
    public static double readDouble(StringTokenizer st) {
        try {
            return Double.parseDouble(st.nextToken());
        } catch (Exception ex) {
            //MWLogger.errLog(ex);
            return -1;
        }
    }

    /**
     * Reads the next token and parses it as a {@code Boolean} via {@link Boolean#parseBoolean(String)} (which
     * treats any value other than a case-insensitive match of "true" as false — it does not throw on malformed
     * input).
     *
     * @param st the tokenizer to read from
     * @return the parsed value, or {@code false} if there is no next token (parsing itself cannot fail since
     *     {@code parseBoolean} never throws)
     */
    public static Boolean readBoolean(StringTokenizer st) {
        try {
            return Boolean.parseBoolean(st.nextToken());
        } catch (Exception ex) {
            //MWLogger.errLog(ex);
            return false;
        }
    }

}
