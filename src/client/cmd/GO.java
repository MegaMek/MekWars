/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
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

package client.cmd;

import java.io.File;
import java.util.HashMap;
import java.util.StringTokenizer;

import client.MWClient;
import common.util.MWLogger;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.Option;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class GO extends Command {
	
	public GO(MWClient mwclient) {
		super(mwclient);
	}
	
	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		StringTokenizer st = decode(input);
		mwclient.getGameOptions().clear();
		HashMap<String, IBasicOption> optionsHash = new HashMap<String, IBasicOption>();
        IBasicOption gameOption = null;
		
        File mmconf = new File("./mmconf");
        
        if ( !mmconf.exists() ){
            mmconf.mkdir();
        }
        
		while (st.hasMoreElements()) {
			String option = st.nextToken();
            String value = st.nextToken();
            
            try{
                gameOption = new Option(new GameOptions(),option,Integer.parseInt(value));
                optionsHash.put(gameOption.getName(),gameOption);
            }catch (Exception ex){
                try{
                    gameOption = new Option(new GameOptions(),option,Float.parseFloat(value));
                    optionsHash.put(gameOption.getName(),gameOption);
                }catch (Exception ex1){
                    try{
                    	// This was always detecting as boolean - strings were returning false
                    	if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                    		gameOption = new Option(new GameOptions(), option, Boolean.parseBoolean(value));
                            optionsHash.put(gameOption.getName(),gameOption);
                    	} else {
                    		gameOption = new Option(new GameOptions(),option,value);
                            optionsHash.put(gameOption.getName(),gameOption);
                    	}
                    }catch (Exception ex2){
                    		MWLogger.infoLog("Uknown format: " + option + " :: " + value);
                        }
                }
            }
		}//end while

		mwclient.getGameOptions().addAll(optionsHash.values());
		
		GameOptions.saveOptions(mwclient.getGameOptions());
		
		mwclient.setWaiting(false);
	}//end execute
}//end GO.java
