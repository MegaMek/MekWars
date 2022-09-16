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

package dedicatedhost.cmd;

import java.io.File;
import java.util.StringTokenizer;

import dedicatedhost.MWDedHost;
import megamek.common.options.GameOptions;
import megamek.common.options.Option;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class GO extends Command {
	
	public GO(MWDedHost mwclient) {
		super(mwclient);
	}
	
	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		StringTokenizer st = decode(input);
		mwclient.getGameOptions().clear();
		mwclient.loadServerMegaMekGameOptions();
		
        File mmconf = new File("./mmconf");
        
        if ( !mmconf.exists() ){
            mmconf.mkdir();
        }
        
		while (st.hasMoreElements()) {
			String option = st.nextToken();
            String value = st.nextToken();
            
            
            try{
                mwclient.getGameOptions().add(new Option(new GameOptions(),option,Integer.parseInt(value)));
            }catch (Exception ex){
                try{
                    mwclient.getGameOptions().add(new Option(new GameOptions(),option, Float.parseFloat(value)));
                }catch (Exception ex1){
                    try{
                        mwclient.getGameOptions().add(new Option(new GameOptions(),option,Boolean.parseBoolean(value)));
                    }catch (Exception ex2){
                        mwclient.getGameOptions().add(new Option(new GameOptions(),option,value));
                    }
                }
            }
		}//end while
		
	}//end execute
}//end GO.java
