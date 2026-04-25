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


import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;

import common.House;
import common.Planet;


/**
 * 
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class MMNetXStream extends XStream {
    public MMNetXStream() {
        super(new PureJavaReflectionProvider());
        // you may add shortcuts here, so XStream will not 
        // write the whole class name each time ;-)
        alias("faction",House.class);
        alias("planet",Planet.class);
        // Enables reference marshalling.
        setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());
    }
    
    public MMNetXStream(HierarchicalStreamDriver hierarchicalStreamDriver){
    	super(hierarchicalStreamDriver);
        // you may add shortcuts here, so XStream will not 
        // write the whole class name each time ;-)
        alias("faction",House.class);
        alias("planet",Planet.class);
        // Enables reference marshalling.
        setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());
    }
}
