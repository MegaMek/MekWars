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

package common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import common.util.MMNetXStream;

public class MWXMLWriter {
	String _folderName;
	String _fileName;
	Object _o;
	
	public MWXMLWriter(String folderName, String fileName, Object o) {
		_folderName = folderName;
		_fileName = fileName;
		_o = o;
	}
	
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
