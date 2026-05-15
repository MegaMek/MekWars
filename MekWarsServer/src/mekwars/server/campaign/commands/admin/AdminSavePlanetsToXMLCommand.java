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

package mekwars.server.campaign.commands.admin;

import common.Continent;
import common.House;
import common.Planet;
import common.Unit;
import common.UnitFactory;
import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;

public class AdminSavePlanetsToXMLCommand implements server.campaign.commands.Command {
    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "";

    public String getSyntax() {
        return syntax;
    }

    public void process(java.util.StringTokenizer command, String Username) {

        // access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream("./campaign/saveplanets.xml");
            java.io.PrintStream p = new java.io.PrintStream(out);
            p.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE DOCUMENT SYSTEM \"planets.dtd\">");
            p.println("<DOCUMENT>");
            p.println("<MEGAMEKNETPLANETDATA>");

            for (Planet planets : CampaignMain.campaignMain.getData().getAllPlanets()) {
                server.campaign.SPlanet planet = (server.campaign.SPlanet) planets;
                p.println("	<PLANET>");
                p.println("		<NAME>" + planet.getName() + "</NAME>");
                p.println("		<COMPPRODUCTION>" + planet.getCompProduction() + "</COMPPRODUCTION>");
                p.println("		<XCOOD>" + planet.getPosition().x + "</XCOOD>");
                p.println("		<YCOOD>" + planet.getPosition().y + "</YCOOD>");
                p.println("		<INFLUENCE>");
                for (House flu : planet.getInfluence().getHouses()) {
                    p.println("			<INF>");
                    server.campaign.SHouse faction = (server.campaign.SHouse) flu;
                    p.println("				<FACTION>" + faction.getName() + "</FACTION>");
                    p.println("				<AMOUNT>" +
                                    planet.getInfluence().getInfluence(faction.getId()) +
                                    "</AMOUNT>");
                    p.println("			</INF>");
                }
                p.println("		</INFLUENCE>");
                p.print("       <ORIGINALOWNER>");
                p.print(planet.getOriginalOwner());
                p.println("</ORIGINALOWNER>");
                for (UnitFactory UF : planet.getUnitFactories()) {
                    p.println("		<UNITFACTORY>");
                    server.campaign.SUnitFactory factory = (server.campaign.SUnitFactory) UF;
                    p.println("			<FACTORYNAME>" + factory.getName() + "</FACTORYNAME>");
                    p.println("			<SIZE>" + factory.getSize() + "</SIZE>");
                    p.println("			<FOUNDER>" + factory.getFounder() + "</FOUNDER>");
                    p.println("			<BUILDTABLEFOLDER>" + factory.getBuildTableFolder().substring(
                          server.campaign.BuildTable.STANDARD.length()) + "</BUILDTABLEFOLDER>");
                    if (factory.canProduce(Unit.MEK)) {
                        p.println("			<TYPE>Mek</TYPE>");
                    }
                    if (factory.canProduce(Unit.INFANTRY)) {
                        p.println("			<TYPE>Infantry</TYPE>");
                    }
                    if (factory.canProduce(Unit.VEHICLE)) {
                        p.println("			<TYPE>Vehicle</TYPE>");
                    }
                    if (factory.canProduce(Unit.PROTOMEK)) {
                        p.println("         <TYPE>PROTOMEK</TYPE>");
                    }
                    if (factory.canProduce(Unit.BATTLEARMOR)) {
                        p.println("         <TYPE>BATTLEARMOR</TYPE>");
                    }
                    if (factory.canProduce(Unit.AERO)) {
                        p.println("         <TYPE>AERO</TYPE>");
                    }
                    p.println("		</UNITFACTORY>");
                }

                for (Continent pe : planet.getEnvironments().toArray()) {
                    p.println("		<CONTINENT>");
                    p.println("			<TERRAIN>" + pe.getEnvironment().getName() + "</TERRAIN>");
                    p.println("			<ADVTERRAIN>" + pe.getAdvancedTerrain().getName() + "</ADVTERRAIN>");
                    p.println("			<SIZE>" + pe.getSize() + "</SIZE>");
                    p.println("		</CONTINENT>");
                }
                p.println("     <WAREHOUSE>" + planet.getBaysProvided() + "</WAREHOUSE>");
                if (planet.getPlanetFlags().size() > 0) {
                    p.println("     <PLANETOPFLAGS>");
                    for (String key : planet.getPlanetFlags().keySet()) {
                        p.println("          <OPKEY>" + key + "</OPKEY>");
                        p.println("          <OPNAME>" + planet.getPlanetFlags().get(key) + "</OPNAME>");
                    }
                    p.println("     </PLANETOPFLAGS>");
                }
                p.println("     <HOMEWORLD>" + planet.isHomeWorld() + "</HOMEWORLD>");
                p.println("	</PLANET>");
            }
            p.println("</MEGAMEKNETPLANETDATA>");
            p.println("</DOCUMENT>");
            p.close();
            out.close();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
        CampaignMain.campaignMain.toUser("XML saved!", Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has saved the universe to XML");

    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}
