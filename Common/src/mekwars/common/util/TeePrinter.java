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

package mekwars.common.util;

import java.io.IOException;
import java.io.Writer;

import jakarta.annotation.Nonnull;

/**
 * Splits a stream into two: a {@link Writer} decorator that forwards every {@code write}/{@code flush}/{@code close}
 * call to two underlying writers, so output written to this "tee" is duplicated to both destinations (e.g. writing
 * simultaneously to a log file and to the console). Named after the Unix {@code tee} command.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public final class TeePrinter extends Writer {
    /** The two underlying writers that every write on this instance is forwarded to, in order. */
    Writer tee, too;

    /**
     * @param tee the first destination writer
     * @param too the second destination writer
     */
    public TeePrinter(Writer tee, Writer too) {
        this.tee = tee;
        this.too = too;
    }

    /**
     * Writes the given character range to both underlying writers, {@code tee} first then {@code too}. Note: if
     * writing to {@code tee} throws, {@code too} is never written to for this call.
     *
     * @see java.io.Writer#write(char[], int, int)
     */
    @Override
    public void write(@Nonnull char[] charBuff, int off, int len) throws IOException {
        tee.write(charBuff, off, len);
        too.write(charBuff, off, len);
    }

    /**
     * Flushes both underlying writers, {@code tee} first then {@code too}. Note: if flushing {@code tee} throws,
     * {@code too} is never flushed for this call.
     *
     * @see java.io.Writer#flush()
     */
    @Override
    public void flush() throws IOException {
        tee.flush();
        too.flush();
    }

    /**
     * Closes both underlying writers, {@code tee} first then {@code too}. Note: if closing {@code tee} throws,
     * {@code too} is never closed for this call.
     *
     * @see java.io.Writer#close()
     */
    @Override
    public void close() throws IOException {
        tee.close();
        too.close();
    }
}
