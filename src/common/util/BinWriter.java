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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.mekwars.libpk.converters.HTML;


/**
 * Used to write the data fields of common data classes
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BinWriter {
	
    private PrintWriter out;
    private boolean debug;
    private BinWriter dataBlock = null;
    private boolean open = true;
    
    public BinWriter(PrintWriter out) {
        this.out = out;
        debug = false;
    }
    
    public BinWriter(PrintWriter out, String debugFilename) {
        try {
            String ls = System.getProperty("line.seperator");
            System.setProperty("line.seperator",String.valueOf((char)13));
            this.out = new PrintWriter(new TeePrinter(out,new FileWriter(debugFilename)));
            System.setProperty("line.seperator",ls);
        } catch (IOException e) {
            MWLogger.errLog(e);
            this.out = new PrintWriter(out);
        }
        debug = true;
        this.out.println("###DEBUG_ON###");
    }
    
    
    public void println(int v, String debugName) {
        if (debug) out.print(debugName+"=");
        out.println(v);
    }
    
    public void println(double v, String debugName) {
        if (debug) out.print(debugName+"=");
        out.println(v);
    }
    
    public void println(String v, String debugName) {
        if (debug) out.print(debugName+"=");
        out.println(HTML.cr2br(v));
    }
    
    public void println(boolean v, String debugName) {
        if (debug) out.print(debugName+"=");
        out.println(v);
    }
    
    public void printStringln(String v, String debugName) {
        if (debug) out.print(debugName+"=");
        out.println(v);
    }
    
    public void close() {
        if (dataBlock != null && dataBlock.open) {
            dataBlock.close();
            dataBlock = null;
        }
        out.close();
        open = false;
    }
    
    public void flush() {
        if (dataBlock != null && dataBlock.open) {
            dataBlock.close(); // yes, close it, not flush it.
            dataBlock = null;
        }
        out.flush();
    }

    /**
     * Signals a new data block within the stream. Use the returned object to
     * write to this data block. If you write once to this one again, the data
     * block is considered closed and you may not write to the returned 
     * BinWriter again. Flushing this stream also closes the data block. 
     * 
     * Of course, if this writer is closed, so is the returned writer.
     * 
     * @param name Name of the new datablock
     * @return An Writer to use for writing to the new data block. 
     */
    public BinWriter newBlock(String name) {
        return new BinWriter(out);
    }
    
}
