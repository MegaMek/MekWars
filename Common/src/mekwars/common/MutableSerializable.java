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

package mekwars.common;

import java.io.IOException;

import mekwars.common.persistence.BinReader;
import mekwars.common.persistence.BinWriter;


/**
 * Implementing this interface allows the object to be serialized with only mutable fields. Mutable fields are those
 * which change often and thus required to be transferred between Server and client a lot.
 * <p>
 * Since it could be impossible to decide which data to be transferred (when encoding) or what object instance should be
 * created (decoding), a data provider is given as argument to retrieve necessary cross references.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public interface MutableSerializable {
    /**
     * Encode all mutable fields into the stream. Use as few bits as possible.
     */
    void encodeMutableFields(BinWriter out, CampaignData dataProvider) throws IOException;

    /**
     * Decode all mutable fields from the stream.
     */
    void decodeMutableFields(BinReader in, CampaignData dataProvider) throws IOException;
}
