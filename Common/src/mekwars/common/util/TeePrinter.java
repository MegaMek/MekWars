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

import java.io.IOException;
import java.io.Writer;

/**
 * Splits a stream into two.
 * @author Imi (immanuel.scholz@gmx.de)
 */
public final class TeePrinter extends Writer {
    Writer tee, too;

    public TeePrinter(Writer tee, Writer too) {
        this.tee = tee;
        this.too = too;
    }
    /**
     * @see java.io.Writer#close()
     */
    @Override
	public void close() throws IOException {
        tee.close();
        too.close();
    }
    /**
     * @see java.io.Writer#flush()
     */
    @Override
	public void flush() throws IOException {
        tee.flush();
        too.flush();
    }
    /**
     * @see java.io.Writer#write(char[], int, int)
     */
    @Override
	public void write(char[] cbuf, int off, int len) throws IOException {
        tee.write(cbuf, off, len);
        too.write(cbuf, off, len);
    }
}