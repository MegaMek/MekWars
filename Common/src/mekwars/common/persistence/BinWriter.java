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

import java.io.PrintWriter;

/**
 * BinWriter is a minimal textual based writer optimized to output as few characters as possible. To do so, the
 * structure information is silently ignored. So no error handling is provided and invalid data structures will not be
 * detectable by BinReader.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BinWriter {

    /**
     * Output goes here
     */
    private PrintWriter out;

    /**
     * Construct a BinWriter.
     *
     * @param out The place to write the output to.
     */
    public BinWriter(PrintWriter out) {
        this.out = out;
    }

    public void write(int v, String name) {
        out.println(v);
    }

    public void write(boolean v, String name) {
        out.println(v);
    }

    public void write(double v, String name) {
        out.println(v);
    }

    public void write(String v, String name) {
        // TODO: This encoding is not safe. If there are <br> in the string, they will be converted back in BinReader.
        out.println();
    }

    public void flush() {
        out.flush();
    }

    public void close() {
        out.close();
    }

    /**
     * Ignored, since no additional structure information is saved anyway...
     */
    public void startDataBlock(String name) {
    }

    /**
     * Ignored, since no additional structure information is saved anyway...
     */
    public void endDataBlock(String name) {
    }
}
