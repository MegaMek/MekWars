package common.util;

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

public final class StringUtils {

    private static String[] colorWheel = { "blue", "BLUE", "black", "BLACK", "yellow", "YELLOW", "green", "GREEN", "red", "RED", "cyan", "CYAN", "gray", "GRAY", "darkGray", "DARK_GRAY", "lightGray", "LIGHT_GRAY", "orange", "ORANGE", "pink", "PINK", "magenta", "MAGENTA", "white", "WHITE" };
    private static Color[] colors = { Color.blue, Color.BLUE, Color.black, Color.BLACK, Color.yellow, Color.YELLOW, Color.green, Color.GREEN, Color.red, Color.RED, Color.cyan, Color.CYAN, Color.gray, Color.GRAY, Color.darkGray, Color.DARK_GRAY, Color.lightGray, Color.LIGHT_GRAY, Color.orange, Color.ORANGE, Color.pink, Color.PINK, Color.magenta, Color.MAGENTA, Color.white, Color.WHITE };

    public static String aOrAn(String s, boolean lowerCase) {
        return aOrAn(s, lowerCase, true);
    }

    /**
     * Method which is used to determine whether "a" or "an" should be used in a string.
     */
    public static String aOrAn(String s, boolean lowerCase, boolean returnString) {

        // get proper into ("A" or "An")
        String AorAn = "A ";
        String checkString = s.toLowerCase();
        if (checkString.toLowerCase().startsWith("a") || checkString.startsWith("e") || checkString.startsWith("i") || checkString.startsWith("o") || checkString.startsWith("u")) {
            AorAn = "An ";
        }

        if (lowerCase)
            AorAn = AorAn.toLowerCase();

        if (returnString)
            return AorAn + " " + s;
        // else
        return AorAn;
    }

    /**
     * As above, but whether or not to pluraize based on a number.
     */
    public static String addAnS(int i) {

        if (i > 1)
            return "s";
        // else
        return "";
    }

    /**
     * Converts a html-color reference to a java.awt.Color. Will attempt to append a missing "#". If all else fails, will return a light grey.
     * 
     * @param htmlColor
     *            color in format "#rrggbb"
     */
    public static Color html2Color(String htmlColor) {
        try {
            return Color.decode(htmlColor);
        } catch (RuntimeException e) {
            try {
                return Color.decode("#" + htmlColor);
            } catch (RuntimeException ex) {

                for (int pos = 0; pos < colorWheel.length; pos++) {
                    if (colorWheel[pos].equals(htmlColor))
                        return colors[pos];
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
     * Used by color2html
     */
    private static String int2hex(int i) {
        String s = Integer.toHexString(i);
        return s.length() == 2 ? s : "0" + s;
    }

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

        try{
            newColor = new Color(red,green,blue);
        }catch(Exception ex){
            
        }
        return newColor;

    }

    public static String hasBadChars(String string){
        return StringUtils.hasBadChars(string,false);
    }
    
    public static String hasBadChars(String string, boolean pilot){
        
        char[] badChars = {'%','~', '$', '|', '*', '#' , '@', '&', '^', '+', '=',
                            ';', ':', '\'', '"', '/', '\\', '{', '}' };

        for (int pos = badChars.length -1; pos >= 0; pos-- ){
            if (string.indexOf(badChars[pos]) != -1) {
                return "AM:Illegal string("+badChars[pos]+" forbidden).";
                
            }
        }

        if (string.toLowerCase().startsWith("vacant") && pilot) {
            return "AM:Illegal pilot name (\"vacant\" forbidden).";
            
        }
        
        
        return "";
    }
}// end AorAnChecker class
