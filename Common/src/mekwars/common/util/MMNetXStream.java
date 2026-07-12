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


import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;

import mekwars.common.House;
import mekwars.common.Planet;


/**
 * MekWars-specific preconfigured {@link XStream} used for XML (de)serialization over the network and to/from disk.
 * Pre-registers short XML element aliases for frequently (de)serialized MekWars domain classes ({@link House} as
 * {@code <faction>}, {@link Planet} as {@code <planet>}) so the full class name is not repeated in every element,
 * and switches to reference-by-id marshalling so that objects shared by multiple references (e.g. the same
 * {@link Planet} referenced from several places) are written once and linked rather than duplicated.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class MMNetXStream extends XStream {
    /**
     * Creates an instance using XStream's {@link PureJavaReflectionProvider} (avoids relying on
     * sun.misc.Unsafe/JVM-specific instantiation tricks) and the default hierarchical stream driver, then applies
     * the MekWars aliases and reference-by-id marshalling strategy described in the class Javadoc.
     */
    public MMNetXStream() {
        super(new PureJavaReflectionProvider());
        // you may add shortcuts here, so XStream will not
        // write the whole class name each time ;-)
        alias("faction", House.class);
        alias("planet", Planet.class);
        // Enables reference marshalling.
        setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());
    }

    /**
     * Creates an instance using the given stream driver (e.g. for a specific XML or binary format) with the
     * default reflection provider, then applies the same MekWars aliases and reference-by-id marshalling strategy
     * as {@link #MMNetXStream()}.
     *
     * @param hierarchicalStreamDriver the stream driver XStream should use to read/write the underlying format
     */
    public MMNetXStream(HierarchicalStreamDriver hierarchicalStreamDriver) {
        super(hierarchicalStreamDriver);
        // you may add shortcuts here, so XStream will not
        // write the whole class name each time ;-)
        alias("faction", House.class);
        alias("planet", Planet.class);
        // Enables reference marshalling.
        setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());
    }
}
