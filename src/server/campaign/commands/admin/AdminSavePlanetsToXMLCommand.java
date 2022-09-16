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

package server.campaign.commands.admin;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import common.Continent;
import common.House;
import common.Planet;
import common.Unit;
import common.UnitFactory;
import common.util.MWLogger;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.BuildTable;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SUnitFactory;
import server.campaign.commands.Command;

public class AdminSavePlanetsToXMLCommand implements Command {
	int accessLevel = IAuthenticator.ADMIN;
    String syntax = "";

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

    public void process(StringTokenizer command, String Username) {

        // access level check
        int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
            return;
        }

        try {
            FileOutputStream out = new FileOutputStream("./campaign/saveplanets.xml");
            PrintStream p = new PrintStream(out);
            p.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE DOCUMENT SYSTEM \"planets.dtd\">");
            p.println("<DOCUMENT>");
            p.println("<MEGAMEKNETPLANETDATA>");

            for (Planet planets : CampaignMain.cm.getData().getAllPlanets()) {
                SPlanet planet = (SPlanet) planets;
                p.println("	<PLANET>");
                p.println("		<NAME>" + planet.getName() + "</NAME>");
                p.println("		<COMPPRODUCTION>" + planet.getCompProduction() + "</COMPPRODUCTION>");
                p.println("		<XCOOD>" + planet.getPosition().x + "</XCOOD>");
                p.println("		<YCOOD>" + planet.getPosition().y + "</YCOOD>");
                p.println("		<INFLUENCE>");
                for (House flu : planet.getInfluence().getHouses()) {
                    p.println("			<INF>");
                    SHouse faction = (SHouse) flu;
                    p.println("				<FACTION>" + faction.getName() + "</FACTION>");
                    p.println("				<AMOUNT>" + planet.getInfluence().getInfluence(faction.getId()) + "</AMOUNT>");
                    p.println("			</INF>");
                }
                p.println("		</INFLUENCE>");
                p.print("       <ORIGINALOWNER>");
                p.print(planet.getOriginalOwner());
                p.println("</ORIGINALOWNER>");
                for (UnitFactory UF : planet.getUnitFactories()) {
                    p.println("		<UNITFACTORY>");
                    SUnitFactory factory = (SUnitFactory) UF;
                    p.println("			<FACTORYNAME>" + factory.getName() + "</FACTORYNAME>");
                    p.println("			<SIZE>" + factory.getSize() + "</SIZE>");
                    p.println("			<FOUNDER>" + factory.getFounder() + "</FOUNDER>");
                    p.println("			<BUILDTABLEFOLDER>" + factory.getBuildTableFolder().substring(BuildTable.STANDARD.length()) + "</BUILDTABLEFOLDER>");
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
        CampaignMain.cm.toUser("XML saved!", Username, true);
        CampaignMain.cm.doSendModMail("NOTE", Username + " has saved the universe to XML");

    }
}