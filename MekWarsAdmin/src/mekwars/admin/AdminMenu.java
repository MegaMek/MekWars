/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - nmorris (urgru@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package admin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import admin.dialog.BannedAmmoDialog;
import admin.dialog.BannedTargetingDialog;
import admin.dialog.CommandNameDialog;
import admin.dialog.ComponentDisplayDialog;
import admin.dialog.FactionConfigurationDialog;
import admin.dialog.FactionToFactionRewardPointMultiplierDialog;
import admin.dialog.PlanetEditorDialog;
import admin.dialog.ServerConfigurationDialog;
import admin.dialog.SubFactionConfigurationDialog;
import admin.dialog.playerFlags.DefaultPlayerFlagListDialog;
import client.MWClient;
import client.gui.dialog.HouseNameDialog;
import client.gui.dialog.NewUnitViewerDialog;
import client.gui.dialog.PlanetNameDialog;
import client.gui.dialog.SubFactionNameDialog;
import client.gui.dialog.TraitDialog;
import common.CampaignData;
import common.Planet;
import common.Terrain;
import common.Unit;
import common.UnitFactory;
import common.util.MWLogger;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.common.TechConstants;

public class AdminMenu extends JMenu {

    /**
     *
     */
    private static final long serialVersionUID = -4734543796361026030L;
    /**
     *
     */
    // admin menu components
    JMenu jMenuAdminSubSave = new JMenu();// sub menus
    JMenu jMenuAdminSubSet = new JMenu();
    JMenu jMenuAdminSubCreate = new JMenu();
    JMenu jMenuAdminSubDestroy = new JMenu();
    JMenu jMenuAdminBlackMarketSettings = new JMenu("Black Market Settings");
    JMenu jMenuAdminOperations = new JMenu("Operations");
    JMenu jMenuAdminBuildTables = new JMenu("Build Tables");
    JMenu jMenuAdminMuls = new JMenu("Muls");

    JMenuItem jMenuAdminServerConfig = new JMenuItem();
    JMenuItem jMenuAdminFactionConfig = new JMenuItem();
    JMenuItem jMenuAdminTerminateAll = new JMenuItem();
    JMenuItem jMenuAdminCreatePlanet = new JMenuItem();
    JMenuItem jMenuAdminDestroyPlanet = new JMenuItem();
    JMenuItem jMenuAdminCreateFactory = new JMenuItem();
    JMenuItem jMenuAdminDestroyFactory = new JMenuItem();
    JMenuItem jMenuAdminCreateTerrain = new JMenuItem();
    JMenuItem jMenuAdminDestroyTerrain = new JMenuItem();
    JMenuItem jMenuAdminChangePlanetOwner = new JMenuItem();
    JMenuItem jMenuAdminHouseAmmoBan = new JMenuItem();
    JMenuItem jMenuAdminSetHouseFluFile = new JMenuItem();
    JMenuItem jMenuAdminSetHouseTechLevel = new JMenuItem();
    JMenuItem jMenuAdminSetFactionTraits = new JMenuItem();
    JMenuItem jMenuAdminSetSubFactionConfigs = new JMenuItem();
    JMenuItem jMenuAdminSetFactionToFactionRewardPointMultiplier = new JMenuItem();
    JMenuItem jMenuAdminSetFactionTechPoints = new JMenuItem();
    JMenuItem jMenuAdminSaveTheUniverse = new JMenuItem();
    JMenuItem jMenuAdminSaveBlackMaketSettings = new JMenuItem();
    JMenuItem jMenuAdminSavePlanetsToXML = new JMenuItem();
    JMenuItem jMenuAdminSaveServerConfigs = new JMenuItem();
    JMenuItem jMenuAdminSaveCommandLevels = new JMenuItem();
    JMenuItem jMenuAdminGrantComponents = new JMenuItem();
    JMenuItem jMenuAdminExchangePlanetOwnership = new JMenuItem();
    JMenuItem jMenuAdminLockFactory = new JMenuItem();
    JMenuItem jMenuAdminSetPlanetMapSize = new JMenuItem();
    JMenuItem jMenuAdminSetPlanetBoardSize = new JMenuItem();
    JMenuItem jMenuAdminSetPlanetTemperature = new JMenuItem();
    JMenuItem jMenuAdminSetPlanetGravity = new JMenuItem();
    JMenuItem jMenuAdminSetPlanetVacuum = new JMenuItem();
    JMenuItem jMenuAdminSetPlanetHomeWorld = new JMenuItem();
    JMenuItem jMenuAdminSetPlanetOriginalOwner = new JMenuItem();
    JMenuItem jMenuAdminSetServerAmmoBan = new JMenuItem();
    JMenuItem jMenuAdminSetServerTargetBan = new JMenuItem();
    JMenuItem jMenuAdminSetCommandLevel = new JMenuItem();
    JMenuItem jMenuAdminSetMegaMekGameOptions = new JMenuItem();
    JMenuItem jMenuAdminSetAmmoCost = new JMenuItem();
    JMenuItem jMenuAdminRemoveOMG = new JMenuItem();
    JMenuItem jMenuAdminOmniVariantMod = new JMenuItem();
    JMenuItem jMenuAdminCommandLists = new JMenuItem();
    JMenuItem jMenuAdminComponentMiscList = new JMenuItem();
    JMenuItem jMenuAdminComponentWeaponList = new JMenuItem();
    JMenuItem jMenuAdminComponentAmmoList = new JMenuItem();
    JMenuItem jMenuAdminSetHouseBasePilotSkill = new JMenuItem();
    JMenuItem jMenuAdminUploadBuildTable = new JMenuItem();
    JMenuItem jMenuAdminSynchBuildTables = new JMenuItem();
    JMenuItem jMenuAdminPruneBuildTables = new JMenuItem();
    JMenuItem jMenuAdminUploadMul = new JMenuItem();
    JMenuItem jMenuAdminListMuls = new JMenuItem();
    JMenuItem jMenuAdminRetrieveMul = new JMenuItem();
    JMenuItem jMenuAdminRetrieveAllMuls = new JMenuItem();
    JMenuItem jMenuAdminCreateMulArmy = new JMenuItem();
    JMenuItem jMenuAdminReloadSupportUnits = new JMenuItem();
    JMenuItem jMenuAdminReloadSanitizerConfigs = new JMenuItem();
    JMenuItem jMenuAdminPlayerFlags = new JMenuItem();

    MWClient mwclient;
    private int userLevel = 0;

    // constructor
    public AdminMenu() {
        super("Server Configs");
    }

    public void createMenu(MWClient client) {

        mwclient = client;

        userLevel = mwclient.getUser(mwclient.getUsername()).getUserlevel();
        /*
         * Code to create the actual menu, add action listeners, etc. This is
         * extracted from CMainFrame and could be improved dramatically ...
         */
        jMenuAdminSubSave.setText("Save");
        jMenuAdminSubSet.setText("Set");
        jMenuAdminSubCreate.setText("Create");
        jMenuAdminSubDestroy.setText("Destroy");

        jMenuAdminCreatePlanet.setText("Create Planet");
        jMenuAdminCreatePlanet.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminCreatePlanet_actionPerformed(e);
            }
        });

        jMenuAdminDestroyPlanet.setText("Destroy Planet");
        jMenuAdminDestroyPlanet.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminDestroyPlanet_actionPerformed(e);
            }
        });
        jMenuAdminCreateFactory.setText("Create Factory");
        jMenuAdminCreateFactory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminCreateFactory_actionPerformed(e);
            }
        });
        jMenuAdminDestroyFactory.setText("Destroy Factory");
        jMenuAdminDestroyFactory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminDestroyFactory_actionPerformed(e);
            }
        });

        jMenuAdminCreateTerrain.setText("Create Terrain");
        jMenuAdminCreateTerrain.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminCreateTerrain_actionPerformed(e);
            }
        });

        jMenuAdminDestroyTerrain.setText("Destroy Terrain");
        jMenuAdminDestroyTerrain.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminDestroyTerrain_actionPerformed(e);
            }
        });

        jMenuAdminHouseAmmoBan.setText("Set Banned Ammo");
        jMenuAdminHouseAmmoBan.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminHouseAmmoBan_actionPerformed(e);
            }
        });

        jMenuAdminChangePlanetOwner.setText("Change Planet Owner");
        jMenuAdminChangePlanetOwner.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminChangePlanetOwner_actionPerformed(e);
            }
        });

        jMenuAdminServerConfig.setText("Server Configuration");
        jMenuAdminServerConfig.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mwclient.getServerConfigData();
                new ServerConfigurationDialog(mwclient);
            }
        });

        jMenuAdminFactionConfig.setText("Faction Configuration");
        jMenuAdminFactionConfig.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Faction", false, false);
                factionDialog.setVisible(true);
                String faction = factionDialog.getHouseName();
                factionDialog.dispose();
                if ((faction == null) || (faction.length() == 0)) {
                    return;
                }

                try {
                    mwclient.getServerConfigData();
                    // Give the server configs a head start.
                    Thread.sleep(1000);
                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c GetFactionConfigs#-1#" + faction);
                    mwclient.setWaiting(true);

                    while (mwclient.isWaiting()) {
                        Thread.sleep(120);
                        // MWLogger.errLog("Waiting for faction config");
                    }
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                    mwclient.setWaiting(false);
                }

                new FactionConfigurationDialog(mwclient, faction);
            }
        });

        jMenuAdminTerminateAll.setText("Terminate All Games");
        jMenuAdminTerminateAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminTerminateAll_actionPerformed(e);
            }
        });

        jMenuAdminSetFactionToFactionRewardPointMultiplier.setText("Inter-Faction Reward Points");
        jMenuAdminSetFactionToFactionRewardPointMultiplier.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new FactionToFactionRewardPointMultiplierDialog(mwclient);
            }
        });

        jMenuAdminSetSubFactionConfigs.setText("Sub Faction Configs");
        jMenuAdminSetSubFactionConfigs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Faction", false, false);
                factionDialog.setVisible(true);
                String faction = factionDialog.getHouseName();
                factionDialog.dispose();
                if ((faction == null) || (faction.length() == 0)) {
                    return;
                }

                try {
                    mwclient.refreshData();
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
                SubFactionNameDialog subFactionDialog = new SubFactionNameDialog(mwclient, "SubFaction", faction);
                subFactionDialog.setVisible(true);
                String subFactionName = subFactionDialog.getSubFactionName();
                subFactionDialog.dispose();
                if ((subFactionName == null) || (subFactionName.length() == 0)) {
                    return;
                }

                new SubFactionConfigurationDialog(mwclient, faction, subFactionName);
            }
        });

        jMenuAdminSetFactionTraits.setText("Faction Traits");
        jMenuAdminSetFactionTraits.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new TraitDialog(mwclient, false);
            }
        });

        jMenuAdminSetFactionTechPoints.setText("Grant Faction Tech Points");
        jMenuAdminSetFactionTechPoints.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSetFactionTechPoints_actionPerformed(e);
            }
        });

        jMenuAdminSetHouseFluFile.setText("Set House Flu File");
        jMenuAdminSetHouseFluFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSetHouseFluFile_actionPerformed(e);
            }
        });

        jMenuAdminSetHouseTechLevel.setText("Set House Tech Level");
        jMenuAdminSetHouseTechLevel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSetHouseTechLevel_actionPerformed(e);
            }
        });

        jMenuAdminSaveTheUniverse.setText("Save The Universe");
        jMenuAdminSaveTheUniverse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSaveTheUniverse_actionPerformed(e);
            }
        });

        jMenuAdminSaveBlackMaketSettings.setText("Save Black Market Settings");
        jMenuAdminSaveBlackMaketSettings.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSaveBlackMaketSettings_actionPerformed(e);
            }
        });

        jMenuAdminSavePlanetsToXML.setText("Save Planets to XML");
        jMenuAdminSavePlanetsToXML.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSavePlanetsToXML_actionPerformed(e);
            }
        });

        jMenuAdminRemoveOMG.setText("List and Remove OMG Units");
        jMenuAdminRemoveOMG.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminlistandremoveomg");
            }
        });

        jMenuAdminGrantComponents.setText("Grant Components");
        jMenuAdminGrantComponents.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminGrantComponents_actionPerformed(e);
            }
        });

        jMenuAdminExchangePlanetOwnership.setText("Exchange Planet Ownership");
        jMenuAdminExchangePlanetOwnership.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminExchangePlanetOwnership_actionPerformed(e);
            }
        });

        jMenuAdminLockFactory.setText("Lock Factory");
        jMenuAdminLockFactory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminLockFactory_actionPerformed(e);
            }
        });

        jMenuAdminSaveServerConfigs.setText("Save Server Configuration");
        jMenuAdminSaveServerConfigs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminSaveServerConfigs");
                mwclient.reloadData();
            }
        });

        jMenuAdminSaveCommandLevels.setText("Save Command Levels");
        jMenuAdminSaveCommandLevels.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminSaveCommandLevels");
            }
        });

        jMenuAdminSetPlanetMapSize.setText("Set Planet Map Size");
        jMenuAdminSetPlanetMapSize.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSetPlanetMapSize_actionPerformed(e);
            }
        });

        jMenuAdminSetPlanetBoardSize.setText("Set Planet Board Size");
        jMenuAdminSetPlanetBoardSize.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSetPlanetBoardSize_actionPerformed(e);
            }
        });

        jMenuAdminSetPlanetTemperature.setText("Set Planet Temperature");
        jMenuAdminSetPlanetTemperature.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSetPlanetTemperature_actionPerformed(e);
            }
        });

        jMenuAdminSetPlanetGravity.setText("Set Planet Gravity");
        jMenuAdminSetPlanetGravity.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSetPlanetGravity_actionPerformed(e);
            }
        });

        jMenuAdminSetPlanetVacuum.setText("Set Planet Vacuum");
        jMenuAdminSetPlanetVacuum.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminSetPlanetVacuum");
                mwclient.reloadData();
            }
        });

        jMenuAdminSetPlanetHomeWorld.setText("Set Planet Home World");
        jMenuAdminSetPlanetHomeWorld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSetPlanetHomeWorld_actionPerformed(e);
            }
        });

        jMenuAdminSetPlanetOriginalOwner.setText("Set Planet Original Owner");
        jMenuAdminSetPlanetOriginalOwner.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSetPlanetOriginalOwner_actionPerformed(e);
            }
        });

        jMenuAdminSetServerAmmoBan.setText("Set Server Ammo Ban");
        jMenuAdminSetServerAmmoBan.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminServerAmmoBan_actionPerformed(e);
            }
        });

        jMenuAdminSetServerTargetBan.setText("Set Server Target System Ban");
        jMenuAdminSetServerTargetBan.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		jMenuAdminServerTargetBan_actionPerformed(e);
        	}
        });

        jMenuAdminReloadSupportUnits.setText("Reload supportunits.txt");
        jMenuAdminReloadSupportUnits.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		jMenuAdminReloadSupportUnits_actionPerformed(e);
        	}
        });

        jMenuAdminReloadSanitizerConfigs.setText("Reload HTML Sanitizer");
        jMenuAdminReloadSanitizerConfigs.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		jMenuAdminReloadSanitizer_actionPerformed(e);
        	}
        });

        jMenuAdminUploadBuildTable.setText("Upload a build table");
        jMenuAdminUploadBuildTable.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminUploadBuildTable_actionPerformed(e);
            }
        });

        jMenuAdminSynchBuildTables.setText("Synch local build tables");
        jMenuAdminSynchBuildTables.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminRequestBuildTable_actionPerformed(e);
            }
        });

        jMenuAdminPruneBuildTables.setText("Prune server backups");
        jMenuAdminPruneBuildTables.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminPruneBuildTable_actionPerformed(e);
            }
        });

        jMenuAdminUploadMul.setText("Upload a Mul File");
        jMenuAdminUploadMul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminUploadMul_actionPerformed(e);
            }
        });

        jMenuAdminListMuls.setText("List Muls");
        jMenuAdminListMuls.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminListMuls_actionPerformed(e);
            }
        });

        jMenuAdminRetrieveMul.setText("Retrieve Mul File");
        jMenuAdminRetrieveMul.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminRetrieveMul_actionPerformed(e);
            }
        });

        jMenuAdminRetrieveAllMuls.setText("Retrieve All Muls");
        jMenuAdminRetrieveAllMuls.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminRetrieveAllMuls_actionPerformed(e);
            }
        });

        jMenuAdminCreateMulArmy.setText("Create Mul Army");
        jMenuAdminCreateMulArmy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminCreateMulArmy_actionPerformed(e);
            }
        });

        jMenuAdminSetAmmoCost.setText("Set Ammo Cost");
        jMenuAdminSetAmmoCost.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                e.setSource(ComponentDisplayDialog.AMMO_COSTS_TYPE);
                jMenuAdminComponentList_actionPerformed(e);
            }
        });

        jMenuAdminOmniVariantMod.setText("Set Omni Variant Mod");
        jMenuAdminOmniVariantMod.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminOmniVariantMod_actionPerformed(e);
            }
        });

        jMenuAdminSetCommandLevel.setText("Set Command Level");
        jMenuAdminSetCommandLevel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSetCommandLevel_actionPerformed(e);
            }
        });

        jMenuAdminSetHouseBasePilotSkill.setText("Set House Base Pilot Skills");
        jMenuAdminSetHouseBasePilotSkill.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminSetHouseBasePilotSkills_actionPerformed(e);
            }
        });

        jMenuAdminCommandLists.setText("List Commands");
        jMenuAdminCommandLists.setMnemonic('L');
        jMenuAdminCommandLists.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuAdminCommandLists_actionPerformed(e);
            }
        });

        jMenuAdminComponentWeaponList.setText("List Weapon Components");
        jMenuAdminComponentWeaponList.setMnemonic('W');
        jMenuAdminComponentWeaponList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                e.setSource(ComponentDisplayDialog.WEAPON_TYPE);
                jMenuAdminComponentList_actionPerformed(e);
            }
        });

        jMenuAdminComponentAmmoList.setText("List Ammo Components");
        jMenuAdminComponentAmmoList.setMnemonic('A');
        jMenuAdminComponentAmmoList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                e.setSource(ComponentDisplayDialog.AMMO_TYPE);
                jMenuAdminComponentList_actionPerformed(e);
            }
        });

        jMenuAdminComponentMiscList.setText("List Misc Components");
        jMenuAdminComponentMiscList.setMnemonic('M');
        jMenuAdminComponentMiscList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                e.setSource(ComponentDisplayDialog.MISC_TYPE);
                jMenuAdminComponentList_actionPerformed(e);
            }
        });

        jMenuAdminSetMegaMekGameOptions.setText("Set MegaMek Game Options");
        jMenuAdminSetMegaMekGameOptions.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mwclient.loadMegaMekClient();
            }
        });

        jMenuAdminPlayerFlags.setText("Player Flags");
        jMenuAdminPlayerFlags.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		new DefaultPlayerFlagListDialog(mwclient);
        	}
        });

        // clear the entire menu, incase this is a reconstruction call
        removeAll();

        // then, set the menu up
        if (userLevel >= mwclient.getData().getAccessLevel("AdminChangeServerConfig")) {
            this.add(jMenuAdminServerConfig);
        }

        if (userLevel >= mwclient.getData().getAccessLevel("AdminChangeFactionConfig")) {
            this.add(jMenuAdminFactionConfig);
        }

        if (userLevel >= mwclient.getData().getAccessLevel("AdminTerminateAll")) {
            this.add(jMenuAdminTerminateAll);
        }

        if (userLevel >= mwclient.getData().getAccessLevel("AdminReloadSupportUnits")) {
        	this.add(jMenuAdminReloadSupportUnits);
        }

        if (userLevel >= mwclient.getData().getAccessLevel("AdminReloadHTMLSanitizerConfig")) {
        	this.add(jMenuAdminReloadSanitizerConfigs);
        }

        if (getItemCount() > 0) {
            addSeparator();
        }

        if (userLevel >= mwclient.getData().getAccessLevel("AdminCreatePlanet")) {
            jMenuAdminSubCreate.add(jMenuAdminCreatePlanet);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminCreateFactory")) {
            jMenuAdminSubCreate.add(jMenuAdminCreateFactory);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminCreateTerrain")) {
            jMenuAdminSubCreate.add(jMenuAdminCreateTerrain);
        }
        if (jMenuAdminSubCreate.getItemCount() > 0) {
            this.add(jMenuAdminSubCreate);
        }

        if (userLevel >= mwclient.getData().getAccessLevel("AdminDestroyPlanet")) {
            jMenuAdminSubDestroy.add(jMenuAdminDestroyPlanet);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminDestroyFactory")) {
            jMenuAdminSubDestroy.add(jMenuAdminDestroyFactory);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminDestroyTerrain")) {
            jMenuAdminSubDestroy.add(jMenuAdminDestroyTerrain);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminListAndRemoveOMG")) {
            jMenuAdminSubDestroy.add(jMenuAdminRemoveOMG);
        }
        if (jMenuAdminSubDestroy.getItemCount() > 0) {
            this.add(jMenuAdminSubDestroy);
        }

        JMenu jMenuAdminSubSetHouse = new JMenu();
        JMenu jMenuAdminSubSetPlanet = new JMenu();

        if (userLevel >= mwclient.getData().getAccessLevel("AdminChangePlanetOwner")) {
            jMenuAdminSubSetHouse.add(jMenuAdminChangePlanetOwner);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetHouseFluFile")) {
            jMenuAdminSubSetHouse.add(jMenuAdminSetHouseFluFile);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSsetHouseTechLevel")) {
            jMenuAdminSubSetHouse.add(jMenuAdminSetHouseTechLevel);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminGrantComponents")) {
            jMenuAdminSubSetHouse.add(jMenuAdminGrantComponents);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminExchangePlanetOwnership")) {
            jMenuAdminSubSetHouse.add(jMenuAdminExchangePlanetOwnership);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetHouseAmmoBan")) {
            jMenuAdminSubSetHouse.add(jMenuAdminHouseAmmoBan);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AddTrait")) {
            jMenuAdminSubSetHouse.add(jMenuAdminSetFactionTraits);
        }
        if ((userLevel >= mwclient.getData().getAccessLevel("CreateSubFaction")) && (userLevel >= mwclient.getData().getAccessLevel("SetSubFactionConfig"))) {
            jMenuAdminSubSetHouse.add(jMenuAdminSetSubFactionConfigs);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("SetFactionToFactionRewardPointMultiplier")) {
            jMenuAdminSubSetHouse.add(jMenuAdminSetFactionToFactionRewardPointMultiplier);
        }

        jMenuAdminSubSetHouse.setText("Factions");
        if (jMenuAdminSubSetHouse.getItemCount() > 0) {
            jMenuAdminSubSet.add(jMenuAdminSubSetHouse);
        }

        if (userLevel >= mwclient.getData().getAccessLevel("AdminLockFactory")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminLockFactory);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetMapSize")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetMapSize);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetBoardSize")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetBoardSize);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetTemperature")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetTemperature);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetGravity")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetGravity);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetVacuum")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetVacuum);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetHomeWorld")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetHomeWorld);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetOriginalOwner")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetOriginalOwner);
        }

        jMenuAdminSubSetPlanet.setText("Planets");
        if (jMenuAdminSubSetPlanet.getItemCount() > 0) {
            jMenuAdminSubSet.add(jMenuAdminSubSetPlanet);
        }

        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetServerAmmoBan")) {
            jMenuAdminSubSet.add(jMenuAdminSetServerAmmoBan);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetServerTargetBan")) {
        	jMenuAdminSubSet.add(jMenuAdminSetServerTargetBan);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetCommandLevel")) {
            jMenuAdminSubSet.add(jMenuAdminSetCommandLevel);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AddOmniVariantMod")) {
            jMenuAdminSubSet.add(jMenuAdminOmniVariantMod);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetAmmoCost")) {
            jMenuAdminSubSet.add(jMenuAdminSetAmmoCost);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("SetHouseBasePilotSkills")) {
            jMenuAdminSubSet.add(jMenuAdminSetHouseBasePilotSkill);
        }
        if (userLevel >= 200) {
            jMenuAdminSubSet.addSeparator();
            jMenuAdminSubSet.add(jMenuAdminSetMegaMekGameOptions);
        }

        if (jMenuAdminSubSet.getItemCount() > 0) {
            this.add(jMenuAdminSubSet);
        }

        if (userLevel >= mwclient.getData().getAccessLevel("AdminUploadBuildTable")) {
            jMenuAdminBuildTables.add(jMenuAdminUploadBuildTable);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminRequestBuildTable")) {
            jMenuAdminBuildTables.add(jMenuAdminSynchBuildTables);
            jMenuAdminBuildTables.add(jMenuAdminPruneBuildTables);
        }

        if (jMenuAdminBuildTables.getItemCount() > 0) {
            this.add(jMenuAdminBuildTables);
        }

        if (userLevel >= mwclient.getData().getAccessLevel("ListMuls")) {
            jMenuAdminMuls.add(jMenuAdminListMuls);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("RetrieveMul")) {
            jMenuAdminMuls.add(jMenuAdminRetrieveMul);
            jMenuAdminMuls.add(jMenuAdminRetrieveAllMuls);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("UploadMul")) {
            jMenuAdminMuls.add(jMenuAdminUploadMul);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("CreateArmyFromMul")) {
            jMenuAdminMuls.add(jMenuAdminCreateMulArmy);
        }

        if (jMenuAdminMuls.getItemCount() > 0) {
            this.add(jMenuAdminMuls);
        }

        if (userLevel >= mwclient.getData().getAccessLevel("AdminSave")) {
            jMenuAdminSubSave.add(jMenuAdminSaveTheUniverse);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSavePlanetsToXML")) {
            jMenuAdminSubSave.add(jMenuAdminSavePlanetsToXML);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSaveServerConfigs")) {
            jMenuAdminSubSave.add(jMenuAdminSaveServerConfigs);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSaveCommandLevels")) {
            jMenuAdminSubSave.add(jMenuAdminSaveCommandLevels);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("AdminSaveBlackMarketConfigs")) {
            jMenuAdminSubSave.add(jMenuAdminSaveBlackMaketSettings);
        }
        if (jMenuAdminSubSave.getItemCount() > 0) {
            this.add(jMenuAdminSubSave);
            addSeparator();
        }

        if (userLevel >= 101) {
            this.add(jMenuAdminCommandLists);
        }

        if (userLevel >= mwclient.getData().getAccessLevel("AdminSetBlackMarketSetting")) {
            jMenuAdminBlackMarketSettings.add(jMenuAdminComponentWeaponList);
            jMenuAdminBlackMarketSettings.add(jMenuAdminComponentAmmoList);
            jMenuAdminBlackMarketSettings.add(jMenuAdminComponentMiscList);
            this.add(jMenuAdminBlackMarketSettings);
        }

        if (userLevel >= 101) {
        	this.add(jMenuAdminPlayerFlags);
        }
    }// end CreateMenu();

    /*
     * Various admin methods. These really don't need to be stand alone, and
     * could (should?) be worked back into a unified ActionPerformed command, or
     * back into the overloaded actionPerformed commands is createMenu();
     *
     * For now, these methods remain as they were in CMainFrame.
     *
     * @urgru, 6.26.05
     */

    public void jMenuAdminCreatePlanet_actionPerformed(ActionEvent e) {
        String planetName = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Planet Name?");
        if ((planetName == null) || (planetName.length() == 0)) {
            return;
        }

        String xcord = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Planet x coord");
        if ((xcord == null) || (xcord.length() == 0)) {
            return;
        }

        String ycord = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Planet y coord?");
        if ((ycord == null) || (ycord.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c admincreateplanet#" + planetName + "#" + xcord + "#" + ycord + "#");
        mwclient.reloadData();
        int id = CampaignData.cd.getPlanetByName(planetName).getId();
        new PlanetEditorDialog(mwclient, planetName, id);

    }

    public void jMenuAdminDestroyPlanet_actionPerformed(ActionEvent e) {

        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c admindestroyplanet#" + planetNamestr);
        mwclient.reloadData();
    }

    public void jMenuAdminCreateFactory_actionPerformed(ActionEvent e) {
        Object[] factoryTypes = { "All", "Mek", "Vehicles", "Mek & Vehicles", "Infantry", "Mek & Infantry", "Vehicles & Infantry", "Mek & Infantry & Vehicles", "ProtoMeks", "Mek & ProtoMeks", "Vehicles & ProtoMek", "Mek & Vehicles & ProtoMek", "Infantry & ProtoMek", "Mek & Infantry & ProtoMek", "Vehicles & Infantry & ProtoMek", "Mek & Vehicles & Infantry & ProtoMek", "BattleArmor", "Mek & BattleArmor", "Vehicles & BattleArmor", "Mek & Vehicles & BattleArmor", "Infantry & BattleArmor", "Mek & Infantry & BattleArmor", "Vehicles & Infantry & BattleArmor", "Mek & Vehicles & Infantry & BattleArmor", "ProtoMeks & BattleArmor", "Mek & ProtoMeks & BattleArmor", "Vehicles & ProtoMek & BattleArmor", "Mek & Vehicles & ProtoMek & BattleArmor", "Infantry & ProtoMek & BattleArmor", "Mek & Infantry & ProtoMek & BattleArmor", "Vehicles & Infantry & ProtoMek & BattleArmor", "Mek & Vehicles & Infantry & ProtoMek & BattleArmor", "VTOL", "Aero" };

        Object[] factorySize = { "Light", "Medium", "Heavy", "Assault" };
        int i;

        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        String factoryName = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Factory Name");

        if ((factoryName == null) || (factoryName.length() == 0)) {
            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.length() == 0)) {
            return;
        }

        String factoryTypestr = (String) JOptionPane.showInputDialog(mwclient.getMainFrame(), "Select factory production", "Factory Production", JOptionPane.INFORMATION_MESSAGE, null, factoryTypes, factoryTypes[0]);

        if ((factoryTypestr == null) || (factoryTypestr.length() == 0)) {
            return;
        }

        for (i = 0; i < factoryTypes.length; i++) {
            if (factoryTypestr.equals(factoryTypes[i])) {
                break;
            }
        }

        int factoryTypeint = i;

        String factorySizestr = (String) JOptionPane.showInputDialog(mwclient.getMainFrame(), "Select a factory size", "FactorySize", JOptionPane.INFORMATION_MESSAGE, null, factorySize, factorySize[0]);

        if ((factorySizestr == null) || (factorySizestr.length() == 0)) {
            return;
        }

        String factoryBuildTable = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Factory Build Table");

        if (factoryBuildTable == null) {
            return;
        }

        String factoryAccessLevel = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Factory Access Level", 0);

        if ((factoryAccessLevel == null) || (factoryAccessLevel.length() == 0)) {
            return;
        }

        StringBuilder sendCommand = new StringBuilder();
        sendCommand.append(planetNamestr.trim() + "#" + factoryName.trim() + "#" + factorySizestr.trim() + "#" + factionName.trim() + "#" + factoryTypeint);
        sendCommand.append("#");
        sendCommand.append(factoryBuildTable);
        sendCommand.append("#");
        sendCommand.append(factoryAccessLevel);

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c admincreatefactory#" + sendCommand.toString());
        mwclient.reloadData();

    }

    public void jMenuAdminDestroyFactory_actionPerformed(ActionEvent e) {
        TreeSet<String> names = new TreeSet<String>();

        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        Planet planet = mwclient.getData().getPlanetByName(planetNamestr);

        names.clear();

        for (UnitFactory UF : planet.getUnitFactories()) {
            names.add(UF.getName());
        }

        JComboBox<String> combo = new JComboBox<String>(names.toArray(new String[names.size()]));
        combo.setEditable(true);
        JOptionPane jop = new JOptionPane(combo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(mwclient.getMainFrame(), "Select factory to destroy.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        String factoryName = (String) combo.getSelectedItem();

        if ((factoryName == null) || (factoryName.length() == 0)) {
            return;
        }

        int value = ((Integer) jop.getValue()).intValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c admindestroyfactory#" + planetNamestr + "#" + factoryName);
        mwclient.reloadData();

    }

    public void jMenuAdminCreateTerrain_actionPerformed(ActionEvent e) {
        TreeSet<String> names = new TreeSet<String>();
        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        for (Terrain pe : mwclient.getData().getAllTerrains()) {
            names.add(pe.getName());
        }

        /*
         * String terrainType =
         * JOptionPane.showInputDialog(mwclient.getMainFrame(),"Terrain Type");
         */

        JComboBox<String> combo = new JComboBox<String>(names.toArray(new String[names.size()]));
        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(mwclient.getMainFrame(), "Select a Terrain Type.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);
        String terrainType = (String) combo.getSelectedItem();

        int value = ((Integer) jop.getValue()).intValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        if ((terrainType == null) || (terrainType.length() == 0)) {
            return;
        }

        String terrainChance = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Terrain Chance", 100);

        if ((terrainChance == null) || (terrainChance.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c admincreateterrain#" + planetNamestr + "#" + terrainType + "#" + terrainChance);
        mwclient.reloadData();

    }

    public void jMenuAdminDestroyTerrain_actionPerformed(ActionEvent e) {

        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        String terrainType = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Select the Terrain position: start with 0 for the top most terrain in the information box");

        if ((terrainType == null) || (terrainType.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c admindestroyterrain#" + planetNamestr + "#" + terrainType);
        mwclient.reloadData();

    }

    public void jMenuAdminChangePlanetOwner_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "New Owner", false, false);
        factionDialog.setVisible(true);
        String newOwner = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((newOwner == null) || (newOwner.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminchangeplanetowner#" + planetNamestr + "#" + newOwner);
        mwclient.reloadData();

    }

    public void jMenuAdminSetFactionTechPoints_actionPerformed(ActionEvent e) {
        HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.length() == 0)) {
            return;
        }

        String points = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Number of Points(Negative to remove Points)", "0");

        if ((points == null) || (points.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c GrantTechPoints#" + factionName + "#" + points);

    }

    public void jMenuAdminTerminateAll_actionPerformed(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(mwclient.getMainFrame(), "Are you sure you want to terminate all waiting/running games?");
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminterminateall");
    }

    public void jMenuAdminSetHouseFluFile_actionPerformed(ActionEvent e) {

        HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.length() == 0)) {
            return;
        }

        String fluFilePrefix = JOptionPane.showInputDialog(mwclient.getMainFrame(), mwclient.moneyOrFluMessage(false, true, -1) + " File Prefix:");

        if ((fluFilePrefix == null) || (fluFilePrefix.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsethouseflufile#" + factionName + "#" + fluFilePrefix);
    }

    public void jMenuAdminGrantComponents_actionPerformed(ActionEvent e) {
        Object[] Types = { "Mek", "Vehicles", "Infantry", "ProtoMek", "BattleArmor", "Aero" };

        Object[] Size = { "Light", "Medium", "Heavy", "Assault" };

        HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.length() == 0)) {
            return;
        }

        String Typestr = (String) JOptionPane.showInputDialog(mwclient.getMainFrame(), "Select component type", "Component Type", JOptionPane.INFORMATION_MESSAGE, null, Types, Types[0]);

        if ((Typestr == null) || (Typestr.length() == 0)) {
            return;
        }

        String Sizestr = (String) JOptionPane.showInputDialog(mwclient.getMainFrame(), "Select a component size", "Component Size", JOptionPane.INFORMATION_MESSAGE, null, Size, Size[0]);
        if ((Sizestr == null) || (Sizestr.length() == 0)) {
            return;
        }

        String components = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Amount of Components to add(negative number to subtract)");
        if ((components == null) || (components.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c admingrantcomponents#" + factionName + "#" + Typestr + "#" + Sizestr + "#" + components);
    }

    public void jMenuAdminExchangePlanetOwnership_actionPerformed(ActionEvent e) {

        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Planet", null);
        planetDialog.setVisible(true);
        String planetName = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetName == null) || (planetName.length() == 0)) {
            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Gaining Faction", false, false);
        factionDialog.setVisible(true);
        String winningHouseName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((winningHouseName == null) || (winningHouseName.length() == 0)) {
            return;
        }

        factionDialog = new HouseNameDialog(mwclient, "Losing Faction", false, false);
        factionDialog.setVisible(true);
        String losingHouseName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((losingHouseName == null) || (losingHouseName.length() == 0)) {
            return;
        }

        String amount = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Amount");
        if ((amount == null) || (amount.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminexchangeplanetownership#" + planetName + "#" + winningHouseName + "#" + losingHouseName + "#" + amount);
    }

    public void jMenuAdminSetHousePriceMod_actionPerformed(ActionEvent e) {
        Object[] unitTypes = { "Mek", "Vehicles", "Infantry", "ProtoMek", "BattleArmor", "Aero" };
        Object[] unitClass = { "Light", "Medium", "Heavy", "Assault" };

        HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.length() == 0)) {
            return;
        }

        String unitTypestr = (String) JOptionPane.showInputDialog(mwclient.getMainFrame(), "Select Unit Type", "Unit Type", JOptionPane.INFORMATION_MESSAGE, null, unitTypes, unitTypes[0]);

        if ((unitTypestr == null) || (unitTypestr.length() == 0)) {
            return;
        }

        String unitClassstr = (String) JOptionPane.showInputDialog(mwclient.getMainFrame(), "Select Unit Class", "Unit Class", JOptionPane.INFORMATION_MESSAGE, null, unitClass, unitClass[0]);

        if ((unitClassstr == null) || (unitClassstr.length() == 0)) {
            return;
        }

        String priceMod = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Price Modifier:");

        if ((priceMod == null) || (priceMod.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsethousepricemod#" + factionName + "#" + unitTypestr + "#" + unitClassstr + "#" + priceMod);
    }

    public void jMenuAdminSetHouseFluMod_actionPerformed(ActionEvent e) {
        Object[] unitTypes = { "Mek", "Vehicles", "Infantry", "ProtoMek", "BattleArmor", "Aero" };
        Object[] unitClass = { "Light", "Medium", "Heavy", "Assault" };

        HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.length() == 0)) {
            return;
        }

        String unitTypestr = (String) JOptionPane.showInputDialog(mwclient.getMainFrame(), "Select Unit Type", "Unit Type", JOptionPane.INFORMATION_MESSAGE, null, unitTypes, unitTypes[0]);

        if ((unitTypestr == null) || (unitTypestr.length() == 0)) {
            return;
        }

        String unitClassstr = (String) JOptionPane.showInputDialog(mwclient.getMainFrame(), "Select Unit Class", "Unit Class", JOptionPane.INFORMATION_MESSAGE, null, unitClass, unitClass[0]);

        if ((unitClassstr == null) || (unitClassstr.length() == 0)) {
            return;
        }

        String fluMod = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Price Modifier:");

        if ((fluMod == null) || (fluMod.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsethouseflumod#" + factionName + "#" + unitTypestr + "#" + unitClassstr + "#" + fluMod);
    }

    public void jMenuAdminSetHouseTechLevel_actionPerformed(ActionEvent e) {

        HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.length() == 0)) {
            return;
        }

        JComboBox<String> techCombo = new JComboBox<String>(TechConstants.T_NAMES);
        techCombo.setEditable(false);

        JOptionPane jop = new JOptionPane(techCombo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dlg = jop.createDialog(mwclient.getMainFrame(), "Select Tech Level");
        techCombo.grabFocus();
        techCombo.getEditor().selectAll();

        dlg.setVisible(true);

        if ((Integer) jop.getValue() == JOptionPane.CANCEL_OPTION) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsethousetechlevel#" + factionName + "#" + techCombo.getSelectedIndex());
    }

    public void jMenuAdminSaveTheUniverse_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsave");
    }

    public void jMenuAdminSaveBlackMaketSettings_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsaveblackmarketconfigs");
    }

    public void jMenuAdminSavePlanetsToXML_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsaveplanetstoxml");
    }

    public void jMenuAdminOmniVariantMod_actionPerformed(ActionEvent e) {
        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(mwclient.getMainFrame());
        NewUnitViewerDialog unitSelector = new NewUnitViewerDialog(mwclient.getMainFrame(), unitLoadingDialog, mwclient,NewUnitViewerDialog.OMNI_VARIANT_SELECTOR);
        unitSelector.setName("Unit Selector");
        new Thread(unitSelector).start();
    }

    public void jMenuAdminServerAmmoBan_actionPerformed(ActionEvent e) {
        new BannedAmmoDialog(mwclient, null);
    }

    public void jMenuAdminServerTargetBan_actionPerformed(ActionEvent e) {
    	new BannedTargetingDialog(mwclient);
    }

    public void jMenuAdminListMuls_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c listMuls");
    }

    public void jMenuAdminRetrieveMul_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c listMuls#SMFD");
    }

    public void jMenuAdminRetrieveAllMuls_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c RetrieveAllMuls");
    }

    public void jMenuAdminUploadMul_actionPerformed(ActionEvent e) {

        JFileChooser chooser = new JFileChooser();

        File mulFolder = new File("./data/armies");
        if (!mulFolder.exists()) {
            mulFolder.mkdir();
        }

        chooser.setCurrentDirectory(mulFolder);

        int returnVal = chooser.showOpenDialog(chooser);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            StringBuilder line = new StringBuilder();
            line.append(MWClient.CAMPAIGN_PREFIX + "UploadMul ");
            line.append(file.getName());
            try {
                FileInputStream in = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                try {
                    while (br.ready()) {
                        line.append("#" + br.readLine());
                    }
                    br.close();
                    in.close();
                } catch (IOException ioex) {
                    MWLogger.errLog("IOException: " + line.toString());
                }
            } catch (FileNotFoundException fnfex) {
                MWLogger.errLog("FileNotFoundException: " + line.toString());
            }
            line.append("#");
            mwclient.sendChat(line.toString());

        }
    }

    public void jMenuAdminRequestBuildTable_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "AdminRequestBuildTable list");
    }

    public void jMenuAdminPruneBuildTable_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "AdminRequestBuildTable prune");
    }

    public void jMenuAdminUploadBuildTable_actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();

        chooser.setCurrentDirectory(new File("./data/buildtables"));

        int returnVal = chooser.showOpenDialog(chooser);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            StringBuilder line = new StringBuilder();
            line.append(MWClient.CAMPAIGN_PREFIX + "AdminUploadBuildTable ");
            String path = file.getPath();
            if (path.contains("rare")) {
                path = "rare/" + file.getName();
            } else if (path.contains("standard")) {
                path = "standard/" + file.getName();
            } else if (path.contains("reward")) {
                path = "reward/" + file.getName();
            }
            line.append(path);
            try {
                FileInputStream in = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                try {
                    while (br.ready()) {
                        line.append("#" + br.readLine());
                    }
                    br.close();
                    in.close();
                } catch (IOException ioex) {
                    MWLogger.errLog("IOException: " + line.toString());
                }
            } catch (FileNotFoundException fnfex) {
                MWLogger.errLog("FileNotFoundException: " + line.toString());
            }
            line.append("#");
            mwclient.sendChat(line.toString());

        }
    }

    public void jMenuAdminHouseAmmoBan_actionPerformed(ActionEvent e) {
        HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Select Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.length() == 0)) {
            return;
        }

        new BannedAmmoDialog(mwclient, mwclient.getData().getHouseByName(factionName));
    }

    public void jMenuAdminSetHouseBasePilotSkills_actionPerformed(ActionEvent e) {
        HouseNameDialog factionDialog = new HouseNameDialog(mwclient, "Select Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.length() == 0)) {
            return;
        }

        Object[] unitTypes = { "Mek", "Vehicles", "Infantry", "ProtoMeks", "BattleArmor", "Aero" };

        String unitTypestr = (String) JOptionPane.showInputDialog(mwclient.getMainFrame(), "Select Unit Type", "Unit Type", JOptionPane.INFORMATION_MESSAGE, null, unitTypes, unitTypes[0]);

        if ((unitTypestr == null) || (unitTypestr.length() == 0)) {
            return;
        }

        int unitTypeint = Unit.getTypeIDForName(unitTypestr);

        String gunnery = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Base Gunnery");

        if ((gunnery == null) || (gunnery.length() == 0)) {
            return;
        }

        String piloting = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Base Piloting");

        if ((piloting == null) || (piloting.length() == 0)) {
            return;
        }

        StringBuffer sendCommand = new StringBuffer();

        sendCommand.append(factionName);
        sendCommand.append("#");
        sendCommand.append(unitTypeint);
        sendCommand.append("#");
        sendCommand.append(gunnery);
        sendCommand.append("#");
        sendCommand.append(piloting);

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c sethousebasepilotskills#" + sendCommand.toString());
    }

    public void jMenuAdminSetCommandLevel_actionPerformed(ActionEvent e) {

        CommandNameDialog commandDialog = new CommandNameDialog(mwclient, "Select a Command");
        commandDialog.setVisible(true);
        String commandNamestr = commandDialog.getCommandName();
        commandDialog.dispose();

        if ((commandNamestr == null) || commandNamestr.equalsIgnoreCase("null")) {
            return;
        }

        String level = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Level");

        if ((level == null) || (level.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsetCommandLevel#" + commandNamestr + "#" + level);
    }

    public void jMenuAdminLockFactory_actionPerformed(ActionEvent e) {
        TreeSet<String> names = new TreeSet<String>();

        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        Planet planet = mwclient.getData().getPlanetByName(planetNamestr);

        for (UnitFactory UF : planet.getUnitFactories()) {
            names.add(UF.getName());
        }

        JComboBox<String> combo = new JComboBox<String>(names.toArray(new String[names.size()]));
        combo.setEditable(true);
        JOptionPane jop = new JOptionPane(combo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(mwclient.getMainFrame(), "Select factory to toggle the lock on.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        String factoryName = (String) combo.getSelectedItem();

        if ((factoryName == null) || (factoryName.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminlockfactory#" + planetNamestr + "#" + factoryName);
        mwclient.reloadData();

    }

    public void jMenuAdminSetPlanetMapSize_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        String xSize = JOptionPane.showInputDialog(mwclient.getMainFrame(), "X size");

        if ((xSize == null) || (xSize.length() == 0)) {
            return;
        }

        String ySize = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Y Size");

        if ((ySize == null) || (ySize.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsetplanetmapsize#" + planetNamestr + "#" + xSize + "#" + ySize);
        mwclient.reloadData();
    }

    public void jMenuAdminSetPlanetHomeWorld_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(null, "Set as HomeWorld?", "Set HomeWorld", JOptionPane.YES_NO_CANCEL_OPTION);

        if (result == JOptionPane.CANCEL_OPTION) {
            return;
        }

        boolean homeworld = false;
        if (result == JOptionPane.YES_OPTION) {
            homeworld = true;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsethomeworld#" + planetNamestr + "#" + homeworld);
        mwclient.reloadData();

    }

    public void jMenuAdminSetPlanetBoardSize_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        String xSize = JOptionPane.showInputDialog(mwclient.getMainFrame(), "X size");

        if ((xSize == null) || (xSize.length() == 0)) {
            return;
        }

        String ySize = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Y Size");

        if ((ySize == null) || (ySize.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsetplanetboardsize#" + planetNamestr + "#" + xSize + "#" + ySize);
        mwclient.reloadData();
    }

    public void jMenuAdminSetPlanetOriginalOwner_actionPerformed(ActionEvent ex) {

        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        HouseNameDialog hnd = new HouseNameDialog(mwclient, "Select Original Owner", false, false);
        hnd.setVisible(true);
        String owner = hnd.getHouseName();
        hnd.dispose();

        if ((owner == null) || (owner.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsetplanetoriginalowner#" + planetNamestr + "#" + owner);
        mwclient.reloadData();
    }

    public void jMenuAdminSetPlanetTemperature_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        String lowTemp = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Low Temp");

        if ((lowTemp == null) || (lowTemp.length() == 0)) {
            return;
        }

        String hiTemp = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Hi Temp");

        if ((hiTemp == null) || (hiTemp.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsetplanettemperature#" + planetNamestr + "#" + lowTemp + "#" + hiTemp);
        mwclient.reloadData();
    }

    public void jMenuAdminSetPlanetGravity_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        String grav = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Gravity");

        if ((grav == null) || (grav.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsetplanetgravity#" + planetNamestr + "#" + grav);
        mwclient.reloadData();
    }

    public void jMenuAdminCommandLists_actionPerformed(ActionEvent e) {
        CommandNameDialog commandDialog = new CommandNameDialog(mwclient, "Select a Command");
        commandDialog.setVisible(true);
        String commandNamestr = commandDialog.getCommandName();
        commandDialog.dispose();

        if (commandNamestr != null) {
            String input = MWClient.CAMPAIGN_PREFIX + commandNamestr;
            mwclient.getMainFrame().getMainPanel().getCommPanel().setInput(input);
            mwclient.getMainFrame().getMainPanel().getCommPanel().focusInputField();
        }
    }

    public void jMenuAdminComponentList_actionPerformed(ActionEvent e) {
        // ComponentDisplayDialog componentDialog =
        int type = (Integer) e.getSource();
        new ComponentDisplayDialog(mwclient, type);
        // componentDialog.setVisible(true);
    }

    public void jMenuAdminCreateMulArmy_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "listmuls CAFM");
    }

	private void jMenuAdminReloadSupportUnits_actionPerformed(ActionEvent e) {
		mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "adminReloadSupportUnits");
	}

	private void jMenuAdminReloadSanitizer_actionPerformed(ActionEvent e) {
		mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "adminReloadHTMLSanitizerConfigs");
	}

}// end AdminMenu class