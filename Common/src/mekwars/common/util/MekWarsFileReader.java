package mekwars.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Thin convenience wrapper around a {@link FileInputStream} plus a {@link BufferedReader} over it, giving
 * line-oriented text reading of a file using the platform default charset (the stream is wrapped in an
 * {@link InputStreamReader} with no explicit encoding). Used throughout MekWars wherever plain-text data/config
 * files are parsed line by line. Callers must invoke {@link #close()} when done to release both the reader and the
 * underlying stream.
 */
public class MekWarsFileReader {
    /** Underlying raw byte stream for the opened file; kept so it can be closed alongside {@link #dis}. */
    FileInputStream fis;
    /** Buffered, line-oriented reader wrapping {@link #fis}. */
    BufferedReader dis;

    /**
     * Opens the file at the given path for reading.
     *
     * @param filename path of the file to open
     * @throws FileNotFoundException if the file does not exist, is a directory, or cannot be opened for reading
     */
    public MekWarsFileReader(String filename) throws FileNotFoundException {
        fis = new FileInputStream(filename);
        dis = new BufferedReader(new InputStreamReader(fis));
    }

    /**
     * Opens the given file for reading.
     *
     * @param file file to open
     * @throws FileNotFoundException if the file does not exist, is a directory, or cannot be opened for reading
     */
    public MekWarsFileReader(File file) throws FileNotFoundException {
        fis = new FileInputStream(file);
        dis = new BufferedReader(new InputStreamReader(fis));
    }

    /**
     * @return {@code true} if the underlying reader is ready to be read without blocking (see
     *     {@link BufferedReader#ready()})
     * @throws IOException if an I/O error occurs
     */
    public boolean ready() throws IOException {
        return dis.ready();
    }

    /**
     * @return the next line of text, not including the line terminator, or {@code null} if the end of the stream
     *     has been reached
     * @throws IOException if an I/O error occurs
     */
    public String readLine() throws IOException {
        return dis.readLine();
    }

    /**
     * Closes both the buffered reader and the underlying file input stream. Note that if closing the reader
     * throws, {@link #fis} is never closed since the two calls are not wrapped in a try/finally.
     *
     * @throws IOException if closing either the reader or the underlying stream fails
     */
    public void close() throws IOException {
        dis.close();
        fis.close();
    }


}
