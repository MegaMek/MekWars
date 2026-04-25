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
 * 
 * Allows for the reading of a StringTokenizer Token and for easy to use Error Trapping
 */

package common.util;

import java.util.StringTokenizer;

public class TokenReader{
    
    public static String readString(StringTokenizer st){
        try{
            return st.nextToken().trim();
        }catch(Exception ex){
            //MWLogger.errLog(ex);
            return "-1";
        }
    }

    public static int readInt(StringTokenizer st){
        try{
            return Integer.parseInt(st.nextToken());
        }catch(Exception ex){
            //MWLogger.errLog(ex);
            return -1;
        }
    }
    
    public static long readLong(StringTokenizer st){
        try{
            return Long.parseLong(st.nextToken());
        }catch(Exception ex){
            //MWLogger.errLog(ex);
            return -1;
        }
    }
    
    public static float readFloat(StringTokenizer st){
        try{
            return Float.parseFloat(st.nextToken());
        }catch(Exception ex){
            //MWLogger.errLog(ex);
            return -1;
        }
    }
    
    public static double readDouble(StringTokenizer st){
        try{
            return Double.parseDouble(st.nextToken());
        }catch(Exception ex){
            //MWLogger.errLog(ex);
            return -1;
        }
    }
    
    public static Boolean readBoolean(StringTokenizer st){
        try{
            return Boolean.parseBoolean(st.nextToken());
        }catch(Exception ex){
            //MWLogger.errLog(ex);
            return false;
        }
    }
    
}