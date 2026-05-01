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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import megamek.logging.MMLogger;
import mekwars.common.util.TeePrinter;

/**
 * Used to write the data fields of common data classes
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BinWriter {
    private final static MMLogger LOGGER = MMLogger.create(BinWriter.class);

    private PrintWriter out;
    private BinWriter dataBlock = null;
    private boolean open = true;

    public BinWriter(PrintWriter out) {
        this.out = out;
    }

    public BinWriter(PrintWriter out, String debugFilename) {
        try {
            this.out = new PrintWriter(new TeePrinter(out, new FileWriter(debugFilename)));
        } catch (IOException e) {
            LOGGER.error(e, "Could not open debug file: {}", debugFilename);
            this.out = new PrintWriter(out);
        }
        this.out.println("###DEBUG_ON###");
    }


    public void println(int v, String debugName) {
        out.println(STR."\{debugName}=\{v}");
    }

    public void println(double v, String debugName) {
        out.print(STR."\{debugName}=\{v}");
    }

    public void println(String v, String debugName) {
        out.print(STR."\{debugName}=\{v}");
    }

    public void println(boolean v, String debugName) {
        out.print(STR."\{debugName}=\{v}");
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
     * Signals a new data block within the stream. Use the returned object to write to this data block. If you write
     * once to this one again, the data block is considered closed and you may not write to the returned BinWriter
     * again. Flushing this stream also closes the data block.
     * <p>
     * Of course, if this writer is closed, so is the returned writer.
     *
     * @param name Name of the new datablock
     *
     * @return An Writer to use for writing to the new data block.
     */
    public BinWriter newBlock(String name) {
        return new BinWriter(out);
    }

}
