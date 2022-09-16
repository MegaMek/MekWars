/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - Torren (torren@users.sourceforge.net)
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


/*
 * Derived from NFCChat, a GPL chat client/server. 
 * Original code can be found @ http://nfcchat.sourceforge.net
 * Our thanks to the original authors.
 */ 
/**
 * 
 * @author Torren (Jason Tighe) 11.5.05 
 * 
 */
package server.MWChatServer.auth;

import java.io.InputStream;


/**
 * A class that loads resources from locations relative to the
 * CLASSPATH, or relative to the ServletContext if one is specified.
 *
 * $Id: ResourceLoader.java,v 1.1 2005/11/07 23:37:17 torren Exp $
 */
public class ResourceLoader {

    /**
     * All static class - don't instantiate
     */
    protected ResourceLoader() {
    }


    /**
     * Load a resource from the CLASSPATH.  relativePath should use forward-slashes
     * and omit the first slash.
     *
     * e.g., getResource("path/to/my/resource");
     *
     * @param relativePath
     */
    public static InputStream getResource(String relativePath) throws Exception{

        // we use the actual ClassLoader rather than the simple,
        // Class.getResource() method, because we want the SYSTEM
        // ClassLoader, not a Servlet ClassLoader, or any other loader.
        InputStream is = ClassLoader.getSystemResourceAsStream(relativePath);
        if (is == null) {
            throw new Exception("Could not locate resource, " + relativePath);
        }
        return is;
    } 


}
