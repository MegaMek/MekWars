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

 package mekwars.common.commands;

 import mekwars.common.campaign.clientutils.protocol.IClient;
 import mekwars.common.util.MWLogger;

 /**
  * Updates the faction status screen
  *
  * @author Imi (immanuel.scholz@gmx.de)
  */
 public class FactionStatusScreenUpdateCommand extends Command {

     /**
      * @see Command#Command(IClient)
      */
     public FactionStatusScreenUpdateCommand(IClient client) {
         super(client);
     }

     /**
      * @see Command#execute(String)
      */
     @Override
     public void execute(String input) {

         java.util.StringTokenizer st = decode(input);
         String cmdName;
         String cmdData;

         while (st.hasMoreTokens()) {
             cmdName = st.nextToken();
             cmdData = st.nextToken();
             try {
                 this.issueSubCommand(cmdName, cmdData);
             } catch (Exception ex) {
                 MWLogger.errLog(ex);
             }

             if (cmdName.equals("CA")) {
                 return;//return without updating view
             }
         }

         //only update after all commands processed
         client.getMainFrame().getMainPanel().getHSPanel().updateDisplay();
     }

     /**
      * @param s
      */
     @Override
     public void parseReplyArgs(String s) {
         
     }

     /**
      * FactionStatusScreenUpdateCommand| Commands can be issued in bulk. This allows short ops, etc to send ALL changes
      * they make at impacted house players at once instead of sending 5-10 separate updates.
      */
     private void issueSubCommand(String cmdName, String cmdData) {

         switch (cmdName) {
             case "FN" -> client.getMainFrame()
                                .getMainPanel()
                                .getHSPanel()
                                .setFactionName(cmdData);
             case "AU" -> client.getMainFrame()
                                .getMainPanel()
                                .getHSPanel()
                                .addFactionUnit(cmdData);
             case "RU" -> client.getMainFrame()
                                .getMainPanel()
                                .getHSPanel()
                                .removeFactionUnit(cmdData);
             case "CC" -> client.getMainFrame()
                                .getMainPanel()
                                .getHSPanel()
                                .changeFactionComponents(cmdData);
             case "AF" -> client.getMainFrame()
                                .getMainPanel()
                                .getHSPanel()
                                .addFactionFactory(cmdData);
             case "RF" -> client.getMainFrame().getMainPanel().getHSPanel().removeFactionFactory(cmdData);
             case "CF" -> client.getMainFrame().getMainPanel().getHSPanel().changeFactionFactory(cmdData);
             case "CA" -> client.getMainFrame().getMainPanel().getHSPanel().clearHouseStatusData();
         }
     }

     /**
      * @param s
      */
     @Override
     public void parseArguments(String s) {

     }
 }
