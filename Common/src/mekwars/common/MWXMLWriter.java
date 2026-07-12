/*
 * MekWars - Copyright (C) 2013
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
 *
 */

package mekwars.common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import mekwars.common.util.MMNetXStream;


/**
 * Small helper that serializes an arbitrary object to an XML file using {@link MMNetXStream} (an XStream wrapper).
 * Used by {@link MWXmlSerializable} implementers (e.g. {@code Operation}) to persist their full state to disk as a
 * standalone XML file.
 * <p>
 * This is a one-shot, non-reusable helper: construct it with the target object and destination, then call
 * {@link #writeToFile()}.
 *
 * @author Helge Richter
 */
public class MWXMLWriter {
    /** Destination directory for the XML file; created (including parents) if it does not already exist. */
    String _folderName;
    /** Name of the XML file to write inside {@link #_folderName}. */
    String _fileName;
    /** The object to serialize to XML. */
    Object _o;

    /**
     * @param folderName the directory the XML file will be written into.
     * @param fileName   the file name to write within {@code folderName}.
     * @param o          the object to serialize.
     */
    public MWXMLWriter(String folderName, String fileName, Object o) {
        _folderName = folderName;
        _fileName = fileName;
        _o = o;
    }

    /**
     * Creates the destination folder if needed and serializes {@link #_o} to {@code _folderName/_fileName} as XML.
     * <p>
     * Note: any {@link IOException} while writing is caught and only printed to stderr via
     * {@link Exception#printStackTrace()} — it is not propagated or logged through the application's logging
     * framework, so callers cannot detect failure other than by inspecting the console.
     */
    public void writeToFile() {
        File folder = new File(_folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        MMNetXStream xml = new MMNetXStream();
        try {
            xml.toXML(_o, new FileWriter(_folderName + "/" + _fileName));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
