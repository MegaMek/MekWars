/*
 * MekWars - Copyright (C) 2006
 * 
 * Original author - jtighe (torren@users.sourceforge.net)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */


package client.cmd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import client.MWClient;
import common.util.MWLogger;

/**
 * @author Torren
 * Ded MegaMekLog
 */
public class DMML extends Command {

	/**
	 * @see Command#Command(MMClient)
	 */
	public DMML(MWClient mwclient) {
		super(mwclient);
	}

	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		StringTokenizer st = decode(input);
        if (st.hasMoreElements())
        {
        	String logName = st.nextToken();
        	
        	File logFile = new File("./logs/"+logName+".log");
        	
        	try {
        		logFile.createNewFile();
        		FileOutputStream fos = new FileOutputStream(logFile,true);
    			PrintStream p = new PrintStream(fos);

    			while ( st.hasMoreElements() ){
    			    p.append(st.nextToken());
    			    p.append(" ");
    			}
    			p.append('\n');
    			p.flush();
    			p.close();
        	}catch(Exception ex) {
        		MWLogger.errLog(ex);
        	}
        }
	}
}
