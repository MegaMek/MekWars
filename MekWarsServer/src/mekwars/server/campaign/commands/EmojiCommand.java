/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - Jason Tighe (torren@users.sourceforge.net)
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
//@Salient
package mekwars.server.campaign.commands;


import mekwars.server.campaign.CampaignMain;

public class EmojiCommand implements Command {

    int accessLevel = 0;
    String syntax = "ec#emoji";
    private String coloredName;
    private String username;
    private String emoji;
    private boolean allowEmoji;

    public void process(java.util.StringTokenizer command, String Username) {
        username = Username;

        initVars();

        if (!checkAccess()) {return;}

        if (!command.hasMoreTokens()) {
            CampaignMain.campaignMain.toUser("AM:You forgot to specify an emoji to display!. ", username, true);
            return;
        } else {emoji = command.nextToken();}

        processEmoji();
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    private void initVars() {
        allowEmoji = CampaignMain.campaignMain.getBooleanConfig("AllowEmoji");
        coloredName = CampaignMain.campaignMain.getPlayer(username).getColoredNameBold();
    }

    private boolean checkAccess() {
        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", username, true);
                return false;
            }
        }

        if (!allowEmoji) {
            CampaignMain.campaignMain.toUser("AM:Emojis have been disabled, the SO does NOT like fun.",
                  username,
                  true);
            return false;
        }

        return true;
    }

    private void processEmoji() {
        if (emoji.equalsIgnoreCase("fl") || emoji.equalsIgnoreCase("flip")) {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers(coloredName + ": (╯°□°)╯︵ ┻━┻", true);
        }
        //		else if(emoji.equalsIgnoreCase("be") || emoji.equalsIgnoreCase("bear"))
        //			CampaignMain.cm.doSendToAllOnlinePlayers(coloredName + ": ʕ •ᴥ•ʔ" ,true);
        else if (emoji.equalsIgnoreCase("sh") || emoji.equalsIgnoreCase("shrug")) {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers(coloredName + ": ¯\\_(ツ)_/¯", true);
        } else if (emoji.equalsIgnoreCase("fi") || emoji.equalsIgnoreCase("fingers")) {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers(coloredName + ": t(-.-t)", true);
        } else if (emoji.equalsIgnoreCase("ki") || emoji.equalsIgnoreCase("kiss")) {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers(coloredName + ": ( ˘ ³˘)♥", true);
        } else if (emoji.equalsIgnoreCase("sm") || emoji.equalsIgnoreCase("smile")) {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers(coloredName + ": ◉‿◉", true);
        } else if (emoji.equalsIgnoreCase("de") || emoji.equalsIgnoreCase("deal")) {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers(coloredName + ": (•_•) ( •_•)>⌐■-■ (⌐■_■)", true);
        } else if (emoji.equalsIgnoreCase("li") || emoji.equalsIgnoreCase("list")) {showList();} else {
            CampaignMain.campaignMain.toUser("AM:That emoji does not exist! Check the list by typing /ec#list ",
                  username,
                  true);
        }
    }

    private void showList() {
        String br = "<br>";
        String list = "Emoji List" + br +
                            "/ec#fl or /ec#flip outputs: (╯°□°)╯︵ ┻━┻ " + br +
                            "/ec#sh or /ec#shrug outputs: ¯\\_(ツ)_/¯" + br +
                            "/ec#fi or /ec#fingers outputs: t(-.-t)" + br +
                            "/ec#ki or /ec#kiss outputs: ( ˘ ³˘)♥" + br +
                            "/ec#sm or /ec#smile outputs: ◉‿◉" + br +
                            "/ec#de or /ec#deal outputs: (•_•) ( •_•)>⌐■-■ (⌐■_■)" + br;
        CampaignMain.campaignMain.toUser(list, username, true);
    }
}
