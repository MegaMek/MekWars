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

package common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.mekwars.libpk.converters.HTML;


/**
 * Helper to encode and decode typical fields of classes
 *
 * currently handled types are:
 * - boolean
 * - int
 * - String
 * - double
 *  
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BinReader {

    private BufferedReader in;
    private boolean debug;

    
    private String read(String debugName) throws IOException {
//        if ( !in.ready() )
  //          throw new IOException("EOF");
        String s = in.readLine();
        if (debug) {
            if (!s.substring(0,s.indexOf('=')).equals(debugName))
                throw new RuntimeException("serialization mismatch");
            return s.substring(s.indexOf('=')+1);
        }
        return s;
    }
    
    /**
     * Construct an BinReader
     */
    public BinReader(Reader in) {
        this.in = new BufferedReader(in);
        try {
            this.in.mark(100);
            String s = this.in.readLine();
            debug = s.equals("###DEBUG_ON###");
            if (!debug)
                this.in.reset();
        } catch (IOException e) {
            MWLogger.errLog(e);
            debug = false;
        }
    }

    /**
     * Reads an integer
     */
    public int readInt(String debugName) throws IOException {
        return Integer.parseInt(read(debugName));
    }

    /**
     * Reads an double
     */
    public double readDouble(String debugName) throws IOException {
        return Double.parseDouble(read(debugName));
    }

    /**
     * Reads an boolean
     */
    public boolean readBoolean(String debugName) throws IOException {
        String s = read(debugName);
        return !s.equalsIgnoreCase("false") && !s.equals("0") &&
            !s.equals("");
    }

    /**
     * Reads an string
     */
    public String readLine(String debugName) throws IOException {
        return HTML.br2cr(read(debugName));
    }

    /**
     * Reads a string
     */
    public String readStringLine(String debugName) throws IOException {
        return read(debugName);
    }

    /**
     * Closes the input.
     */
    public void close() throws IOException {
        in.close();
    }
}
