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

/**
 * Reads compact data from a stream written with BinWriter. No data structure information is written/read, so expect bad
 * results, if the data structure does not match.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class BinReader {
    private final BufferedReader in;

    /**
     * Constructs a new BinReader from an buffered reader and start reading from it
     */
    public BinReader(BufferedReader in) {
        this.in = in;
    }

    public void close() throws IOException {
        in.close();
    }

    public int readInt(String name) throws IOException {
        return Integer.parseInt(read());
    }

    public double readDouble(String name) throws IOException {
        return Double.parseDouble(read());
    }

    public boolean readBoolean(String name) throws IOException {
        return Boolean.getBoolean(read());
    }

    public String readString(String name) throws IOException {
        return read();
    }

    private String read() throws IOException {
        return in.readLine();
    }

    public void startDataBlock(String name) {}

    public void endDataBlock(String name) {}
}
