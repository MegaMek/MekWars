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
 * Writes the simple {@code name=value} line-oriented text format read back by {@link BinReader}, used to persist
 * the data fields of common data classes (e.g. save-game / campaign state) to disk.
 * <p>
 * Field order matters: {@link BinReader} validates each line's key against the name it expects next, so fields
 * must be written in the same order the corresponding {@code BinReader} calls will read them.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BinWriter {
    private final static MMLogger LOGGER = MMLogger.create(BinWriter.class);

    private PrintWriter out;

    /** Tracks a nested data block opened via {@link #newBlock(String)} so it can be closed before this writer is. */
    private BinWriter dataBlock = null;

    /** Whether this writer is still open; used by a parent writer to decide whether a child block needs closing. */
    private boolean open = true;

    /**
     * Wraps an already-open {@link PrintWriter} for plain (non-debug) writing.
     *
     * @param out the destination to write {@code name=value} lines to
     */
    public BinWriter(PrintWriter out) {
        this.out = out;
    }

    /**
     * Wraps {@code out} but also tees every line to {@code debugFilename} on disk, for troubleshooting what was
     * written. If the debug file cannot be opened, falls back to writing only to {@code out} and logs the error.
     *
     * @param out           the primary destination to write {@code name=value} lines to
     * @param debugFilename path of a file to additionally mirror all output into
     */
    public BinWriter(PrintWriter out, String debugFilename) {
        try {
            this.out = new PrintWriter(new TeePrinter(out, new FileWriter(debugFilename)));
        } catch (IOException e) {
            LOGGER.error(e, "Could not open debug file: {}", debugFilename);
            this.out = new PrintWriter(out);
        }
        this.out.println("###DEBUG_ON###");
    }

    /** Writes an integer field as {@code debugName=v}, terminated with a newline. */
    public void println(int v, String debugName) {
        out.println(String.format("%s=%s", debugName, v));
    }

    /**
     * Writes a double field as {@code debugName=v}. Unlike the other {@code println} overloads, this does not
     * append a newline itself ({@link PrintWriter#print} is used rather than {@code println}).
     */
    public void println(double v, String debugName) {
        out.print(String.format("%s=%s", debugName, v));
    }

    /**
     * Writes a String field as {@code debugName=v}. Unlike {@link #println(int, String)}, this does not append a
     * newline itself.
     */
    public void println(String v, String debugName) {
        out.print(String.format("%s=%s", debugName, v));
    }

    /**
     * Writes a boolean field as {@code debugName=v}. Unlike {@link #println(int, String)}, this does not append a
     * newline itself.
     */
    public void println(boolean v, String debugName) {
        out.print(String.format("%s=%s", debugName, v));
    }

    /**
     * Closes any still-open nested data block, then closes the underlying stream. After calling this, the writer
     * must not be used again.
     */
    public void close() {
        if (dataBlock != null && dataBlock.open) {
            dataBlock.close();
            dataBlock = null;
        }
        out.close();
        open = false;
    }

    /**
     * Closes (not merely flushes) any still-open nested data block, then flushes the underlying stream. The nested
     * block is closed rather than flushed because, per {@link #newBlock(String)}, writing again to a stale block
     * reference after its parent has moved on is not supported.
     */
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
