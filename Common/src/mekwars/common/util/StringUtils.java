package mekwars.common.util;

import java.awt.Color;

/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - nmorris (urgru@users.sourceforge.net)
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
 * Grab-bag of static string/color helper methods used across MekWars: choosing "a"/"an" for generated text,
 * pluralization helper, HTML/AWT color conversion (in both directions, including a small named-color lookup
 * table), color inversion, and a simple "illegal character" validator used to sanitize user-entered names
 * (pilot names, unit names, etc.).
 */
public final class StringUtils {

    /**
     * Parallel array of color names (each listed twice: a `Color.xxx`-style lowercase-first name and the matching
     * {@code Color.XXX} all-caps constant name) used as a fallback lookup in {@link #html2Color(String)} when the
     * input isn't a valid {@code "#rrggbb"} hex string. Indices line up 1:1 with {@link #colors}.
     */
    private static final String[] colorWheel = { "blue", "BLUE", "black", "BLACK", "yellow", "YELLOW", "green", "GREEN",
                                                 "red", "RED", "cyan", "CYAN", "gray", "GRAY", "darkGray", "DARK_GRAY",
                                                 "lightGray", "LIGHT_GRAY", "orange", "ORANGE", "pink", "PINK",
                                                 "magenta", "MAGENTA", "white", "WHITE" };
    /** {@link Color} constants corresponding index-for-index to the names in {@link #colorWheel}. */
    private static final Color[] colors = { Color.blue, Color.BLUE, Color.black, Color.BLACK, Color.yellow,
                                            Color.YELLOW, Color.green, Color.GREEN, Color.red, Color.RED, Color.cyan,
                                            Color.CYAN, Color.gray, Color.GRAY, Color.darkGray, Color.DARK_GRAY,
                                            Color.lightGray, Color.LIGHT_GRAY, Color.orange, Color.ORANGE, Color.pink,
                                            Color.PINK, Color.magenta, Color.MAGENTA, Color.white, Color.WHITE };

    /**
     * Convenience overload equivalent to {@code aOrAn(s, lowerCase, true)} — always returns the full
     * {@code "A/An <s>"} phrase rather than just the article. See {@link #aOrAn(String, boolean, boolean)}.
     */
    public static String aOrAn(String s, boolean lowerCase) {
        return aOrAn(s, lowerCase, true);
    }

    /**
     * Method which is used to determine whether "a" or "an" should be used in a string. Chooses "An" if {@code s}
     * starts with a vowel character (a/e/i/o/u, case-insensitive), otherwise "A".
     * <p><b>Quirk:</b> when {@code returnString} is {@code true}, the result is built as
     * {@code AorAn + " " + s} where {@code AorAn} already ends in a trailing space (e.g. {@code "A "} or
     * {@code "An "}), so the returned phrase contains two spaces between the article and {@code s}
     * (e.g. {@code "A  Mek"}), not one.
     *
     * @param s            the word/phrase to prefix with an article
     * @param lowerCase    if {@code true}, use lowercase "a"/"an" instead of capitalized "A"/"An"
     * @param returnString if {@code true}, return the article plus {@code s} (with the double-space quirk noted
     *                     above); if {@code false}, return just the article (with its own trailing space, no
     *                     second space and no {@code s} appended)
     * @return either the full "article + word" phrase or just the article, depending on {@code returnString}
     */
    public static String aOrAn(String s, boolean lowerCase, boolean returnString) {

        // get proper into ("A" or "An")
        String AorAn = "A ";
        String checkString = s.toLowerCase();
        if (checkString.toLowerCase().startsWith("a") ||
                  checkString.startsWith("e") ||
                  checkString.startsWith("i") ||
                  checkString.startsWith("o") ||
                  checkString.startsWith("u")) {
            AorAn = "An ";
        }

        if (lowerCase) {AorAn = AorAn.toLowerCase();}

        if (returnString) {return AorAn + " " + s;}
        // else
        return AorAn;
    }

    /**
     * As above, but whether or not to pluraize based on a number.
     *
     * @param i the count to check
     * @return {@code "s"} if {@code i > 1} (note: exactly {@code 1} and {@code 0} both yield {@code ""}, i.e. a
     *     count of {@code 0} is treated as singular), otherwise {@code ""}
     */
    public static String addAnS(int i) {

        if (i > 1) {return "s";}
        // else
        return "";
    }

    /**
     * Converts a html-color reference to a java.awt.Color. Will attempt to append a missing "#". If all else fails,
     * will return a light grey.
     * <p>
     * Resolution order: (1) try {@link Color#decode} on {@code htmlColor} as-is; (2) if that fails, retry with a
     * {@code "#"} prepended; (3) if that also fails, look up {@code htmlColor} verbatim (case-sensitive) in the
     * {@link #colorWheel} name table; (4) if still not found, fall back to {@link Color#lightGray}.
     *
     * @param htmlColor color in format "#rrggbb", or one of the names in {@link #colorWheel}
     */
    public static Color html2Color(String htmlColor) {
        try {
            return Color.decode(htmlColor);
        } catch (RuntimeException e) {
            try {
                return Color.decode("#" + htmlColor);
            } catch (RuntimeException ex) {

                for (int pos = 0; pos < colorWheel.length; pos++) {
                    if (colorWheel[pos].equals(htmlColor)) {return colors[pos];}
                }
                return Color.lightGray;
            }
        }

    }

    /**
     * Converts a java.awt.Color to a html-color
     *
     * @return Color as String in format "#rrggbb"
     */
    public static String color2html(Color color) {
        return "#" + int2hex(color.getRed()) + int2hex(color.getGreen()) + int2hex(color.getBlue());
    }

    /**
     * Used by color2html. Converts a single 0-255 color channel value to a zero-padded two-digit hex string.
     *
     * @param i a color channel value; expected to be in the 0-255 range (0-2 hex digits)
     * @return the two-character hex representation of {@code i}, left-padded with "0" if needed
     */
    private static String int2hex(int i) {
        String s = Integer.toHexString(i);
        return s.length() == 2 ? s : "0" + s;
    }

    /**
     * Computes a rough color inversion by shifting each RGB channel by 128 (wrapping toward the opposite end of the
     * 0-255 range: channels below 128 are increased by 128, channels at or above 128 are decreased by 128), rather
     * than the more common {@code 255 - channel} inversion.
     * <p>
     * Since {@code red}/{@code green}/{@code blue} always start in [0, 255], this shift always keeps the result in
     * [0, 255] as well, so the surrounding {@code try/catch (Exception)} guarding {@code new Color(...)} can never
     * actually catch anything (dead code retained as-is; not fixed here).
     *
     * @param color the color to invert
     * @return the shifted/"inverted" color
     */
    public static Color invertColor(Color color) {

        Color newColor = Color.white;

        int red = color.getRed();
        int blue = color.getBlue();
        int green = color.getGreen();

        if (red < 128) {
            red += 128;
        } else {
            red -= 128;
        }

        if (blue < 128) {
            blue += 128;
        } else {
            blue -= 128;
        }

        if (green < 128) {
            green += 128;
        } else {
            green -= 128;
        }

        try {
            newColor = new Color(red, green, blue);
        } catch (Exception ex) {

        }
        return newColor;

    }

    /**
     * Convenience overload equivalent to {@code hasBadChars(string, false)}, i.e. does not apply the "vacant"
     * pilot-name check. See {@link #hasBadChars(String, boolean)}.
     */
    public static String hasBadChars(String string) {
        return StringUtils.hasBadChars(string, false);
    }

    /**
     * Validates a user-supplied name (unit name, callsign, pilot name, etc.) against a blacklist of characters that
     * are not allowed (mostly characters with special meaning in MekWars' pipe/token-delimited save format or HTML
     * output, such as {@code | # { } " ' \}), and, if {@code pilot} is {@code true}, additionally rejects names
     * starting with "vacant" (case-insensitive), which is reserved to mean an empty pilot slot.
     *
     * @param string the candidate name to validate
     * @param pilot  if {@code true}, also reject names starting with "vacant"
     * @return an empty string if the name is valid; otherwise a human-readable "AM:..." error message describing
     *     the first violation found (bad chars are checked from the end of {@code badChars} backwards, so if
     *     multiple bad characters are present, the message reports whichever one appears last in the
     *     {@code badChars} array, not necessarily the first one present in {@code string})
     */
    public static String hasBadChars(String string, boolean pilot) {

        char[] badChars = { '%', '~', '$', '|', '*', '#', '@', '&', '^', '+', '=',
                            ';', ':', '\'', '"', '/', '\\', '{', '}' };

        for (int pos = badChars.length - 1; pos >= 0; pos--) {
            if (string.indexOf(badChars[pos]) != -1) {
                return "AM:Illegal string(" + badChars[pos] + " forbidden).";

            }
        }

        if (string.toLowerCase().startsWith("vacant") && pilot) {
            return "AM:Illegal pilot name (\"vacant\" forbidden).";

        }


        return "";
    }
}// end AorAnChecker class
