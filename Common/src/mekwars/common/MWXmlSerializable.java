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

/**
 * Marker/contract interface for objects that can serialize their entire state to XML (via XStream, see
 * {@link MWXMLWriter}), as opposed to {@link MutableSerializable} which only persists the frequently-changing
 * subset of fields through the binary stream format.
 * <p>
 * Implemented by e.g. {@code mekwars.common.campaign.operations.Operation} to save/export operation definitions as
 * standalone XML files.
 *
 * @author Helge Richter
 */
public interface MWXmlSerializable {

    /**
     * Serializes this object to XML and writes it to a file named {@code fileName} inside directory
     * {@code folderName}, creating the directory if necessary.
     *
     * @param folderName the destination directory (created if it does not exist).
     * @param fileName   the name of the file to write within {@code folderName}.
     */
    void writeToXmlFile(String folderName, String fileName);

    /**
     * @return the XML representation of this object as a string, without writing anything to disk.
     */
    String getXmlString();
}
