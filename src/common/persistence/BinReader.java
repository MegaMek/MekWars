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

package common.persistence;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Reads compact data from a stream written with BinWriter. No data structure
 * information is written/read, so expect bad results, if the data structure 
 * does not match.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class BinReader {
    private BufferedReader in;

    /**
     * Constructs a new BinReader from an buffered reader and start reading 
     * from it
     */
    public BinReader(BufferedReader in) {
        this.in = in;
    }

    /**
     * @see common.persistence.TreeReader#close()
     */
    public void close() throws IOException {
        in.close();
    }

    /**
     * @see common.persistence.TreeReader#readInt(java.lang.String)
     */
    public int readInt(String name) throws IOException {
        return Integer.parseInt(read());
    }

    /**
     * @see common.persistence.TreeReader#readDouble(java.lang.String)
     */
    public double readDouble(String name) throws IOException {
        return Double.parseDouble(read());
    }

    /**
     * @see common.persistence.TreeReader#readBoolean(java.lang.String)
     */
    public boolean readBoolean(String name) throws IOException {
        return Boolean.getBoolean(read());
    }

    /**
     * @see common.persistence.TreeReader#readLine(java.lang.String)
     */
    public String readString(String name) throws IOException {
        return read();
    }
    
    private String read() throws IOException {
        return in.readLine();
    }

    public void startDataBlock(String name) {}
    public void endDataBlock(String name) {}

    /**
     * @see common.persistence.TreeReader#readObject(common.persistence.MMNetSerializable, java.lang.String)
     *
    public void readObject(MMNetSerializable obj, CampaignData dataProvider, String name) throws IOException {
        obj.binIn(this, dataProvider);
    }*/

    /**
     * @see common.persistence.TreeReader#readCollection(java.util.Collection, java.lang.String)
     *
    public void readCollection(
            Collection col, 
            Class cl, 
            CampaignData 
            dataProvider, 
            String name) throws IOException {
        int size = readInt(null);
        for (int i = 0; i < size; ++i) {
            try {
                MMNetSerializable obj = (MMNetSerializable) cl.newInstance();
                obj.binIn(this, dataProvider);
                col.add(obj);
            } catch (InstantiationException e) {
                MWLogger.errLog(e);
            } catch (IllegalAccessException e) {
                MWLogger.errLog(e);
            }
        }
    }*/
}
