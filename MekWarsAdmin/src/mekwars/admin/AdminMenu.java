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

package mekwars.admin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serial;
import java.util.TreeSet;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.common.TechConstants;
import mekwars.admin.dialog.BannedAmmoDialog;
import mekwars.admin.dialog.BannedTargetingDialog;
import mekwars.admin.dialog.CommandNameDialog;
import mekwars.admin.dialog.ComponentDisplayDialog;
import mekwars.admin.dialog.FactionConfigurationDialog;
import mekwars.admin.dialog.FactionToFactionRewardPointMultiplierDialog;
import mekwars.admin.dialog.PlanetEditorDialog;
import mekwars.admin.dialog.ServerConfigurationDialog;
import mekwars.admin.dialog.SubFactionConfigurationDialog;
import mekwars.admin.dialog.playerFlags.DefaultPlayerFlagListDialog;
import mekwars.common.CampaignData;
import mekwars.common.Planet;
import mekwars.common.Terrain;
import mekwars.common.Unit;
import mekwars.common.UnitFactory;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.HouseNameDialog;
import mekwars.common.gui.dialogs.PlanetNameDialog;
import mekwars.common.gui.dialogs.TraitDialog;
import mekwars.common.util.MWLogger;

public class AdminMenu extends JMenu {
    @Serial
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

    IClient client;

    // constructor
    public AdminMenu() {
        super("Server Configs");
    }

    public void createMenu(IClient client) {
        this.client = client;

        int userLevel = this.client.getUser(this.client.getUsername()).getUserLevel();
        /*
         * Code to create the actual menu, add action listeners, etc. This is
         * extracted from CMainFrame and could be improved dramatically ...
         */
        jMenuAdminSubSave.setText("Save");
        jMenuAdminSubSet.setText("Set");
        jMenuAdminSubCreate.setText("Create");
        jMenuAdminSubDestroy.setText("Destroy");

        jMenuAdminCreatePlanet.setText("Create Planet");
        jMenuAdminCreatePlanet.addActionListener(this::jMenuAdminCreatePlanet_actionPerformed);

        jMenuAdminDestroyPlanet.setText("Destroy Planet");
        jMenuAdminDestroyPlanet.addActionListener(this::jMenuAdminDestroyPlanet_actionPerformed);
        jMenuAdminCreateFactory.setText("Create Factory");
        jMenuAdminCreateFactory.addActionListener(this::jMenuAdminCreateFactory_actionPerformed);
        jMenuAdminDestroyFactory.setText("Destroy Factory");
        jMenuAdminDestroyFactory.addActionListener(this::jMenuAdminDestroyFactory_actionPerformed);

        jMenuAdminCreateTerrain.setText("Create Terrain");
        jMenuAdminCreateTerrain.addActionListener(this::jMenuAdminCreateTerrain_actionPerformed);

        jMenuAdminDestroyTerrain.setText("Destroy Terrain");
        jMenuAdminDestroyTerrain.addActionListener(this::jMenuAdminDestroyTerrain_actionPerformed);

        jMenuAdminHouseAmmoBan.setText("Set Banned Ammo");
        jMenuAdminHouseAmmoBan.addActionListener(this::jMenuAdminHouseAmmoBan_actionPerformed);

        jMenuAdminChangePlanetOwner.setText("Change Planet Owner");
        jMenuAdminChangePlanetOwner.addActionListener(this::jMenuAdminChangePlanetOwner_actionPerformed);

        jMenuAdminServerConfig.setText("Server Configuration");
        jMenuAdminServerConfig.addActionListener(e -> {
            this.client.getServerConfigData();
            new ServerConfigurationDialog(this.client);
        });

        jMenuAdminFactionConfig.setText("Faction Configuration");
        jMenuAdminFactionConfig.addActionListener(e -> {
            HouseNameDialog factionDialog = new HouseNameDialog(
                  AdminMenu.this.client,
                  "Faction",
                  false,
                  false);
            factionDialog.setVisible(true);
            String faction = factionDialog.getHouseName();
            factionDialog.dispose();
            if ((faction == null) || (faction.isEmpty())) {
                return;
            }

            try {
                AdminMenu.this.client.getServerConfigData();
                // Give the server configs a head start.
                Thread.sleep(1000);
                AdminMenu.this.client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c GetFactionConfigs#-1#\{faction}");
                AdminMenu.this.client.setWaiting(true);

                while (AdminMenu.this.client.isWaiting()) {
                    Thread.sleep(120);
                }
            } catch (Exception ex) {
                MWLogger.errLog(ex);
                AdminMenu.this.client.setWaiting(false);
            }

            new FactionConfigurationDialog(AdminMenu.this.client, faction);
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
                new FactionToFactionRewardPointMultiplierDialog(AdminMenu.this.client);
            }
        });

        jMenuAdminSetSubFactionConfigs.setText("Sub Faction Configs");
        jMenuAdminSetSubFactionConfigs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                HouseNameDialog factionDialog = new HouseNameDialog(
                      AdminMenu.this.client,
                      "Faction",
                      false,
                      false);
                factionDialog.setVisible(true);
                String faction = factionDialog.getHouseName();
                factionDialog.dispose();
                if ((faction == null) || (faction.isEmpty())) {
                    return;
                }

                try {
                    AdminMenu.this.client.refreshData();
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
                mekwars.common.gui.dialogs.SubFactionNameDialog subFactionDialog = new mekwars.common.gui.dialogs.SubFactionNameDialog(
                      AdminMenu.this.client,
                      "SubFaction",
                      faction);
                subFactionDialog.setVisible(true);
                String subFactionName = subFactionDialog.getSubFactionName();
                subFactionDialog.dispose();
                if ((subFactionName == null) || (subFactionName.isEmpty())) {
                    return;
                }

                new SubFactionConfigurationDialog(AdminMenu.this.client, faction, subFactionName);
            }
        });

        jMenuAdminSetFactionTraits.setText("Faction Traits");
        jMenuAdminSetFactionTraits.addActionListener(e -> new TraitDialog(AdminMenu.this.client, false));

        jMenuAdminSetFactionTechPoints.setText("Grant Faction Tech Points");
        jMenuAdminSetFactionTechPoints.addActionListener(this::jMenuAdminSetFactionTechPoints_actionPerformed);

        jMenuAdminSetHouseFluFile.setText("Set House Flu File");
        jMenuAdminSetHouseFluFile.addActionListener(this::jMenuAdminSetHouseFluFile_actionPerformed);

        jMenuAdminSetHouseTechLevel.setText("Set House Tech Level");
        jMenuAdminSetHouseTechLevel.addActionListener(this::jMenuAdminSetHouseTechLevel_actionPerformed);

        jMenuAdminSaveTheUniverse.setText("Save The Universe");
        jMenuAdminSaveTheUniverse.addActionListener(this::jMenuAdminSaveTheUniverse_actionPerformed);

        jMenuAdminSaveBlackMaketSettings.setText("Save Black Market Settings");
        jMenuAdminSaveBlackMaketSettings.addActionListener(this::jMenuAdminSaveBlackMarketSettings_actionPerformed);

        jMenuAdminSavePlanetsToXML.setText("Save Planets to XML");
        jMenuAdminSavePlanetsToXML.addActionListener(this::jMenuAdminSavePlanetsToXML_actionPerformed);

        jMenuAdminRemoveOMG.setText("List and Remove OMG Units");
        jMenuAdminRemoveOMG.addActionListener(e -> AdminMenu.this.client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminlistandremoveomg"));

        jMenuAdminGrantComponents.setText("Grant Components");
        jMenuAdminGrantComponents.addActionListener(this::jMenuAdminGrantComponents_actionPerformed);

        jMenuAdminExchangePlanetOwnership.setText("Exchange Planet Ownership");
        jMenuAdminExchangePlanetOwnership.addActionListener(this::jMenuAdminExchangePlanetOwnership_actionPerformed);

        jMenuAdminLockFactory.setText("Lock Factory");
        jMenuAdminLockFactory.addActionListener(this::jMenuAdminLockFactory_actionPerformed);

        jMenuAdminSaveServerConfigs.setText("Save Server Configuration");
        jMenuAdminSaveServerConfigs.addActionListener(e -> {
            AdminMenu.this.client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c AdminSaveServerConfigs");
            AdminMenu.this.client.reloadData();
        });

        jMenuAdminSaveCommandLevels.setText("Save Command Levels");
        jMenuAdminSaveCommandLevels.addActionListener(e -> AdminMenu.this.client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c AdminSaveCommandLevels"));

        jMenuAdminSetPlanetMapSize.setText("Set Planet Map Size");
        jMenuAdminSetPlanetMapSize.addActionListener(this::jMenuAdminSetPlanetMapSize_actionPerformed);

        jMenuAdminSetPlanetBoardSize.setText("Set Planet Board Size");
        jMenuAdminSetPlanetBoardSize.addActionListener(this::jMenuAdminSetPlanetBoardSize_actionPerformed);

        jMenuAdminSetPlanetTemperature.setText("Set Planet Temperature");
        jMenuAdminSetPlanetTemperature.addActionListener(this::jMenuAdminSetPlanetTemperature_actionPerformed);

        jMenuAdminSetPlanetGravity.setText("Set Planet Gravity");
        jMenuAdminSetPlanetGravity.addActionListener(this::jMenuAdminSetPlanetGravity_actionPerformed);

        jMenuAdminSetPlanetVacuum.setText("Set Planet Vacuum");
        jMenuAdminSetPlanetVacuum.addActionListener(e -> {
            AdminMenu.this.client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c AdminSetPlanetVacuum");
            AdminMenu.this.client.reloadData();
        });

        jMenuAdminSetPlanetHomeWorld.setText("Set Planet Home World");
        jMenuAdminSetPlanetHomeWorld.addActionListener(this::jMenuAdminSetPlanetHomeWorld_actionPerformed);

        jMenuAdminSetPlanetOriginalOwner.setText("Set Planet Original Owner");
        jMenuAdminSetPlanetOriginalOwner.addActionListener(this::jMenuAdminSetPlanetOriginalOwner_actionPerformed);

        jMenuAdminSetServerAmmoBan.setText("Set Server Ammo Ban");
        jMenuAdminSetServerAmmoBan.addActionListener(this::jMenuAdminServerAmmoBan_actionPerformed);

        jMenuAdminSetServerTargetBan.setText("Set Server Target System Ban");
        jMenuAdminSetServerTargetBan.addActionListener(this::jMenuAdminServerTargetBan_actionPerformed);

        jMenuAdminReloadSupportUnits.setText("Reload supportunits.txt");
        jMenuAdminReloadSupportUnits.addActionListener(this::jMenuAdminReloadSupportUnits_actionPerformed);

        jMenuAdminReloadSanitizerConfigs.setText("Reload HTML Sanitizer");
        jMenuAdminReloadSanitizerConfigs.addActionListener(this::jMenuAdminReloadSanitizer_actionPerformed);

        jMenuAdminUploadBuildTable.setText("Upload a build table");
        jMenuAdminUploadBuildTable.addActionListener(this::jMenuAdminUploadBuildTable_actionPerformed);

        jMenuAdminSynchBuildTables.setText("Sync local build tables");
        jMenuAdminSynchBuildTables.addActionListener(this::jMenuAdminRequestBuildTable_actionPerformed);

        jMenuAdminPruneBuildTables.setText("Prune server backups");
        jMenuAdminPruneBuildTables.addActionListener(this::jMenuAdminPruneBuildTable_actionPerformed);

        jMenuAdminUploadMul.setText("Upload a Mul File");
        jMenuAdminUploadMul.addActionListener(this::jMenuAdminUploadMul_actionPerformed);

        jMenuAdminListMuls.setText("List Muls");
        jMenuAdminListMuls.addActionListener(this::jMenuAdminListMuls_actionPerformed);

        jMenuAdminRetrieveMul.setText("Retrieve Mul File");
        jMenuAdminRetrieveMul.addActionListener(this::jMenuAdminRetrieveMul_actionPerformed);

        jMenuAdminRetrieveAllMuls.setText("Retrieve All Muls");
        jMenuAdminRetrieveAllMuls.addActionListener(this::jMenuAdminRetrieveAllMuls_actionPerformed);

        jMenuAdminCreateMulArmy.setText("Create Mul Army");
        jMenuAdminCreateMulArmy.addActionListener(this::jMenuAdminCreateMulArmy_actionPerformed);

        jMenuAdminSetAmmoCost.setText("Set Ammo Cost");
        jMenuAdminSetAmmoCost.addActionListener(event -> {
            event.setSource(ComponentDisplayDialog.AMMO_COSTS_TYPE);
            jMenuAdminComponentList_actionPerformed(event);
        });

        jMenuAdminOmniVariantMod.setText("Set Omni Variant Mod");
        jMenuAdminOmniVariantMod.addActionListener(this::jMenuAdminOmniVariantMod_actionPerformed);

        jMenuAdminSetCommandLevel.setText("Set Command Level");
        jMenuAdminSetCommandLevel.addActionListener(this::jMenuAdminSetCommandLevel_actionPerformed);

        jMenuAdminSetHouseBasePilotSkill.setText("Set House Base Pilot Skills");
        jMenuAdminSetHouseBasePilotSkill.addActionListener(this::jMenuAdminSetHouseBasePilotSkills_actionPerformed);

        jMenuAdminCommandLists.setText("List Commands");
        jMenuAdminCommandLists.setMnemonic('L');
        jMenuAdminCommandLists.addActionListener(this::jMenuAdminCommandLists_actionPerformed);

        jMenuAdminComponentWeaponList.setText("List Weapon Components");
        jMenuAdminComponentWeaponList.setMnemonic('W');
        jMenuAdminComponentWeaponList.addActionListener(event -> {
            event.setSource(ComponentDisplayDialog.WEAPON_TYPE);
            jMenuAdminComponentList_actionPerformed(event);
        });

        jMenuAdminComponentAmmoList.setText("List Ammo Components");
        jMenuAdminComponentAmmoList.setMnemonic('A');
        jMenuAdminComponentAmmoList.addActionListener(event -> {
            event.setSource(ComponentDisplayDialog.AMMO_TYPE);
            jMenuAdminComponentList_actionPerformed(event);
        });

        jMenuAdminComponentMiscList.setText("List Misc Components");
        jMenuAdminComponentMiscList.setMnemonic('M');
        jMenuAdminComponentMiscList.addActionListener(e -> {
            e.setSource(ComponentDisplayDialog.MISC_TYPE);
            jMenuAdminComponentList_actionPerformed(e);
        });

        jMenuAdminSetMegaMekGameOptions.setText("Set MegaMek Game Options");
        jMenuAdminSetMegaMekGameOptions.addActionListener(e -> AdminMenu.this.client.loadMegaMekClient());

        jMenuAdminPlayerFlags.setText("Player Flags");
        jMenuAdminPlayerFlags.addActionListener(e -> new DefaultPlayerFlagListDialog(AdminMenu.this.client));

        // clear the entire menu, in case this is a reconstruction call
        removeAll();

        // then, set the menu up
        if (userLevel >= this.client.getData().getAccessLevel("AdminChangeServerConfig")) {
            this.add(jMenuAdminServerConfig);
        }

        if (userLevel >= this.client.getData().getAccessLevel("AdminChangeFactionConfig")) {
            this.add(jMenuAdminFactionConfig);
        }

        if (userLevel >= this.client.getData().getAccessLevel("AdminTerminateAll")) {
            this.add(jMenuAdminTerminateAll);
        }

        if (userLevel >= this.client.getData().getAccessLevel("AdminReloadSupportUnits")) {
            this.add(jMenuAdminReloadSupportUnits);
        }

        if (userLevel >= this.client.getData().getAccessLevel("AdminReloadHTMLSanitizerConfig")) {
            this.add(jMenuAdminReloadSanitizerConfigs);
        }

        if (getItemCount() > 0) {
            addSeparator();
        }

        if (userLevel >= this.client.getData().getAccessLevel("AdminCreatePlanet")) {
            jMenuAdminSubCreate.add(jMenuAdminCreatePlanet);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminCreateFactory")) {
            jMenuAdminSubCreate.add(jMenuAdminCreateFactory);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminCreateTerrain")) {
            jMenuAdminSubCreate.add(jMenuAdminCreateTerrain);
        }
        if (jMenuAdminSubCreate.getItemCount() > 0) {
            this.add(jMenuAdminSubCreate);
        }

        if (userLevel >= this.client.getData().getAccessLevel("AdminDestroyPlanet")) {
            jMenuAdminSubDestroy.add(jMenuAdminDestroyPlanet);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminDestroyFactory")) {
            jMenuAdminSubDestroy.add(jMenuAdminDestroyFactory);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminDestroyTerrain")) {
            jMenuAdminSubDestroy.add(jMenuAdminDestroyTerrain);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminListAndRemoveOMG")) {
            jMenuAdminSubDestroy.add(jMenuAdminRemoveOMG);
        }
        if (jMenuAdminSubDestroy.getItemCount() > 0) {
            this.add(jMenuAdminSubDestroy);
        }

        JMenu jMenuAdminSubSetHouse = new JMenu();
        JMenu jMenuAdminSubSetPlanet = new JMenu();

        if (userLevel >= this.client.getData().getAccessLevel("AdminChangePlanetOwner")) {
            jMenuAdminSubSetHouse.add(jMenuAdminChangePlanetOwner);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetHouseFluFile")) {
            jMenuAdminSubSetHouse.add(jMenuAdminSetHouseFluFile);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSsetHouseTechLevel")) {
            jMenuAdminSubSetHouse.add(jMenuAdminSetHouseTechLevel);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminGrantComponents")) {
            jMenuAdminSubSetHouse.add(jMenuAdminGrantComponents);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminExchangePlanetOwnership")) {
            jMenuAdminSubSetHouse.add(jMenuAdminExchangePlanetOwnership);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetHouseAmmoBan")) {
            jMenuAdminSubSetHouse.add(jMenuAdminHouseAmmoBan);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AddTrait")) {
            jMenuAdminSubSetHouse.add(jMenuAdminSetFactionTraits);
        }
        if ((userLevel >= this.client.getData().getAccessLevel("CreateSubFaction")) &&
                  (userLevel >= this.client.getData().getAccessLevel("SetSubFactionConfig"))) {
            jMenuAdminSubSetHouse.add(jMenuAdminSetSubFactionConfigs);
        }
        if (userLevel >= this.client.getData().getAccessLevel("SetFactionToFactionRewardPointMultiplier")) {
            jMenuAdminSubSetHouse.add(jMenuAdminSetFactionToFactionRewardPointMultiplier);
        }

        jMenuAdminSubSetHouse.setText("Factions");
        if (jMenuAdminSubSetHouse.getItemCount() > 0) {
            jMenuAdminSubSet.add(jMenuAdminSubSetHouse);
        }

        if (userLevel >= this.client.getData().getAccessLevel("AdminLockFactory")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminLockFactory);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetMapSize")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetMapSize);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetBoardSize")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetBoardSize);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetTemperature")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetTemperature);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetGravity")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetGravity);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetVacuum")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetVacuum);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetHomeWorld")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetHomeWorld);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetOriginalOwner")) {
            jMenuAdminSubSetPlanet.add(jMenuAdminSetPlanetOriginalOwner);
        }

        jMenuAdminSubSetPlanet.setText("Planets");
        if (jMenuAdminSubSetPlanet.getItemCount() > 0) {
            jMenuAdminSubSet.add(jMenuAdminSubSetPlanet);
        }

        if (userLevel >= this.client.getData().getAccessLevel("AdminSetServerAmmoBan")) {
            jMenuAdminSubSet.add(jMenuAdminSetServerAmmoBan);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetServerTargetBan")) {
            jMenuAdminSubSet.add(jMenuAdminSetServerTargetBan);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetCommandLevel")) {
            jMenuAdminSubSet.add(jMenuAdminSetCommandLevel);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AddOmniVariantMod")) {
            jMenuAdminSubSet.add(jMenuAdminOmniVariantMod);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetAmmoCost")) {
            jMenuAdminSubSet.add(jMenuAdminSetAmmoCost);
        }
        if (userLevel >= this.client.getData().getAccessLevel("SetHouseBasePilotSkills")) {
            jMenuAdminSubSet.add(jMenuAdminSetHouseBasePilotSkill);
        }
        if (userLevel >= 200) {
            jMenuAdminSubSet.addSeparator();
            jMenuAdminSubSet.add(jMenuAdminSetMegaMekGameOptions);
        }

        if (jMenuAdminSubSet.getItemCount() > 0) {
            this.add(jMenuAdminSubSet);
        }

        if (userLevel >= this.client.getData().getAccessLevel("AdminUploadBuildTable")) {
            jMenuAdminBuildTables.add(jMenuAdminUploadBuildTable);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminRequestBuildTable")) {
            jMenuAdminBuildTables.add(jMenuAdminSynchBuildTables);
            jMenuAdminBuildTables.add(jMenuAdminPruneBuildTables);
        }

        if (jMenuAdminBuildTables.getItemCount() > 0) {
            this.add(jMenuAdminBuildTables);
        }

        if (userLevel >= this.client.getData().getAccessLevel("ListMuls")) {
            jMenuAdminMuls.add(jMenuAdminListMuls);
        }
        if (userLevel >= this.client.getData().getAccessLevel("RetrieveMul")) {
            jMenuAdminMuls.add(jMenuAdminRetrieveMul);
            jMenuAdminMuls.add(jMenuAdminRetrieveAllMuls);
        }
        if (userLevel >= this.client.getData().getAccessLevel("UploadMul")) {
            jMenuAdminMuls.add(jMenuAdminUploadMul);
        }
        if (userLevel >= this.client.getData().getAccessLevel("CreateArmyFromMul")) {
            jMenuAdminMuls.add(jMenuAdminCreateMulArmy);
        }

        if (jMenuAdminMuls.getItemCount() > 0) {
            this.add(jMenuAdminMuls);
        }

        if (userLevel >= this.client.getData().getAccessLevel("AdminSave")) {
            jMenuAdminSubSave.add(jMenuAdminSaveTheUniverse);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSavePlanetsToXML")) {
            jMenuAdminSubSave.add(jMenuAdminSavePlanetsToXML);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSaveServerConfigs")) {
            jMenuAdminSubSave.add(jMenuAdminSaveServerConfigs);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSaveCommandLevels")) {
            jMenuAdminSubSave.add(jMenuAdminSaveCommandLevels);
        }
        if (userLevel >= this.client.getData().getAccessLevel("AdminSaveBlackMarketConfigs")) {
            jMenuAdminSubSave.add(jMenuAdminSaveBlackMaketSettings);
        }
        if (jMenuAdminSubSave.getItemCount() > 0) {
            this.add(jMenuAdminSubSave);
            addSeparator();
        }

        if (userLevel >= 101) {
            this.add(jMenuAdminCommandLists);
        }

        if (userLevel >= this.client.getData().getAccessLevel("AdminSetBlackMarketSetting")) {
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
        String planetName = JOptionPane.showInputDialog(client.getMainFrame(), "Planet Name?");
        if ((planetName == null) || (planetName.isEmpty())) {
            return;
        }

        String xCord = JOptionPane.showInputDialog(client.getMainFrame(), "Planet x coord");
        if ((xCord == null) || (xCord.isEmpty())) {
            return;
        }

        String yCord = JOptionPane.showInputDialog(client.getMainFrame(), "Planet y coord?");
        if ((yCord == null) || (yCord.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c admincreateplanet#\{planetName}#\{xCord}#\{yCord}#");
        client.reloadData();
        int id = CampaignData.cd.getPlanetByName(planetName).getId();
        new PlanetEditorDialog(client, planetName, id);

    }

    public void jMenuAdminDestroyPlanet_actionPerformed(ActionEvent e) {

        PlanetNameDialog planetDialog = new PlanetNameDialog(client, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetName = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetName == null) || (planetName.isEmpty())) {
            return;
        }
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c admindestroyplanet#\{planetName}");
        client.reloadData();
    }

    public void jMenuAdminCreateFactory_actionPerformed(ActionEvent e) {
        Object[] factoryTypes = { "All", "Mek", "Vehicles", "Mek & Vehicles", "Infantry", "Mek & Infantry",
                                  "Vehicles & Infantry", "Mek & Infantry & Vehicles", "ProtoMeks", "Mek & ProtoMeks",
                                  "Vehicles & ProtoMek", "Mek & Vehicles & ProtoMek", "Infantry & ProtoMek",
                                  "Mek & Infantry & ProtoMek", "Vehicles & Infantry & ProtoMek",
                                  "Mek & Vehicles & Infantry & ProtoMek", "BattleArmor", "Mek & BattleArmor",
                                  "Vehicles & BattleArmor", "Mek & Vehicles & BattleArmor", "Infantry & BattleArmor",
                                  "Mek & Infantry & BattleArmor", "Vehicles & Infantry & BattleArmor",
                                  "Mek & Vehicles & Infantry & BattleArmor", "ProtoMeks & BattleArmor",
                                  "Mek & ProtoMeks & BattleArmor", "Vehicles & ProtoMek & BattleArmor",
                                  "Mek & Vehicles & ProtoMek & BattleArmor", "Infantry & ProtoMek & BattleArmor",
                                  "Mek & Infantry & ProtoMek & BattleArmor",
                                  "Vehicles & Infantry & ProtoMek & BattleArmor",
                                  "Mek & Vehicles & Infantry & ProtoMek & BattleArmor", "VTOL", "Aero" };

        Object[] factorySize = { "Light", "Medium", "Heavy", "Assault" };
        int i;

        PlanetNameDialog planetDialog = new PlanetNameDialog(client, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetName = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetName == null) || (planetName.isEmpty())) {
            return;
        }

        String factoryName = JOptionPane.showInputDialog(client.getMainFrame(), "Factory Name");

        if ((factoryName == null) || (factoryName.isEmpty())) {
            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.isEmpty())) {
            return;
        }

        String factoryType = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select factory production",
              "Factory Production",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              factoryTypes,
              factoryTypes[0]);

        if ((factoryType == null) || (factoryType.isEmpty())) {
            return;
        }

        for (i = 0; i < factoryTypes.length; i++) {
            if (factoryType.equals(factoryTypes[i])) {
                break;
            }
        }

        int factoryTypeId = i;

        String factorySizestr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select a factory size",
              "FactorySize",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              factorySize,
              factorySize[0]);

        if ((factorySizestr == null) || (factorySizestr.isEmpty())) {
            return;
        }

        String factoryBuildTable = JOptionPane.showInputDialog(client.getMainFrame(), "Factory Build Table");

        if (factoryBuildTable == null) {
            return;
        }

        String factoryAccessLevel = JOptionPane.showInputDialog(client.getMainFrame(), "Factory Access Level", 0);

        if ((factoryAccessLevel == null) || (factoryAccessLevel.isEmpty())) {
            return;
        }

        String sendCommand = STR."\{planetName.trim()}#\{factoryName.trim()}#\{factorySizestr.trim()}#\{factionName.trim()}#\{factoryTypeId}#\{factoryBuildTable}#\{factoryAccessLevel}";

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c admincreatefactory#\{sendCommand}");
        client.reloadData();

    }

    public void jMenuAdminDestroyFactory_actionPerformed(ActionEvent e) {
        TreeSet<String> names = new TreeSet<>();

        PlanetNameDialog planetDialog = new PlanetNameDialog(client, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.isEmpty())) {
            return;
        }

        Planet planet = client.getData().getPlanetByName(planetNamestr);

        names.clear();

        for (UnitFactory UF : planet.getUnitFactories()) {
            names.add(UF.getName());
        }

        JComboBox<String> combo = new JComboBox<>(names.toArray(new String[names.size()]));
        combo.setEditable(true);
        JOptionPane jop = new JOptionPane(combo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select factory to destroy.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        String factoryName = (String) combo.getSelectedItem();

        if ((factoryName == null) || (factoryName.isEmpty())) {
            return;
        }

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c admindestroyfactory#\{planetNamestr}#\{factoryName}");
        client.reloadData();

    }

    public void jMenuAdminCreateTerrain_actionPerformed(ActionEvent e) {
        TreeSet<String> names = new TreeSet<>();
        PlanetNameDialog planetDialog = new PlanetNameDialog(client, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.isEmpty())) {
            return;
        }

        for (Terrain pe : client.getData().getAllTerrains()) {
            names.add(pe.getName());
        }

        JComboBox<String> combo = new JComboBox<>(names.toArray(new String[names.size()]));
        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select a Terrain Type.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);
        String terrainType = (String) combo.getSelectedItem();

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        if ((terrainType == null) || (terrainType.isEmpty())) {
            return;
        }

        String terrainChance = JOptionPane.showInputDialog(client.getMainFrame(), "Terrain Chance", 100);

        if ((terrainChance == null) || (terrainChance.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c admincreateterrain#\{planetNamestr}#\{terrainType}#\{terrainChance}");
        client.reloadData();

    }

    public void jMenuAdminDestroyTerrain_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(client, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.isEmpty())) {
            return;
        }

        String terrainType = JOptionPane.showInputDialog(client.getMainFrame(),
              "Select the Terrain position: start with 0 for the top most terrain in the information box");

        if ((terrainType == null) || (terrainType.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c admindestroyterrain#\{planetNamestr}#\{terrainType}");
        client.reloadData();

    }

    public void jMenuAdminChangePlanetOwner_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(client, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.isEmpty())) {
            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(client, "New Owner", false, false);
        factionDialog.setVisible(true);
        String newOwner = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((newOwner == null) || (newOwner.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminchangeplanetowner#\{planetNamestr}#\{newOwner}");
        client.reloadData();

    }

    public void jMenuAdminSetFactionTechPoints_actionPerformed(ActionEvent e) {
        HouseNameDialog factionDialog = new HouseNameDialog(client, "Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.isEmpty())) {
            return;
        }

        String points = JOptionPane.showInputDialog(client.getMainFrame(),
              "Number of Points(Negative to remove Points)",
              "0");

        if ((points == null) || (points.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c GrantTechPoints#\{factionName}#\{points}");

    }

    public void jMenuAdminTerminateAll_actionPerformed(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(client.getMainFrame(),
              "Are you sure you want to terminate all waiting/running games?");
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminterminateall");
    }

    public void jMenuAdminSetHouseFluFile_actionPerformed(ActionEvent e) {

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Faction", false, false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.isEmpty())) {
            return;
        }

        String fluFilePrefix = JOptionPane.showInputDialog(client.getMainFrame(),
              STR."\{client.moneyOrFluMessage(false, true, -1)} File Prefix:");

        if ((fluFilePrefix == null) || (fluFilePrefix.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsethouseflufile#\{factionName}#\{fluFilePrefix}");
    }

    public void jMenuAdminGrantComponents_actionPerformed(ActionEvent e) {
        Object[] Types = { "Mek", "Vehicles", "Infantry", "ProtoMek", "BattleArmor", "Aero" };

        Object[] Size = { "Light", "Medium", "Heavy", "Assault" };

        HouseNameDialog factionDialog = new HouseNameDialog(client,
              "Faction",
              false,
              false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.isEmpty())) {
            return;
        }

        String Typestr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select component type",
              "Component Type",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              Types,
              Types[0]);

        if ((Typestr == null) || (Typestr.isEmpty())) {
            return;
        }

        String Sizestr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select a component size",
              "Component Size",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              Size,
              Size[0]);
        if ((Sizestr == null) || (Sizestr.isEmpty())) {
            return;
        }

        String components = JOptionPane.showInputDialog(client.getMainFrame(),
              "Amount of Components to add(negative number to subtract)");
        if ((components == null) || (components.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c admingrantcomponents#\{factionName}#\{Typestr}#\{Sizestr}#\{components}");
    }

    public void jMenuAdminExchangePlanetOwnership_actionPerformed(ActionEvent e) {

        PlanetNameDialog planetDialog = new PlanetNameDialog(client, "Planet", null);
        planetDialog.setVisible(true);
        String planetName = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetName == null) || (planetName.isEmpty())) {
            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(client,
              "Gaining Faction",
              false,
              false);
        factionDialog.setVisible(true);
        String winningHouseName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((winningHouseName == null) || (winningHouseName.isEmpty())) {
            return;
        }

        factionDialog = new HouseNameDialog(client, "Losing Faction", false, false);
        factionDialog.setVisible(true);
        String losingHouseName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((losingHouseName == null) || (losingHouseName.isEmpty())) {
            return;
        }

        String amount = JOptionPane.showInputDialog(client.getMainFrame(), "Amount");
        if ((amount == null) || (amount.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminexchangeplanetownership#\{planetName}#\{winningHouseName}#\{losingHouseName}#\{amount}");
    }

    public void jMenuAdminSetHousePriceMod_actionPerformed(ActionEvent e) {
        Object[] unitTypes = { "Mek", "Vehicles", "Infantry", "ProtoMek", "BattleArmor", "Aero" };
        Object[] unitClass = { "Light", "Medium", "Heavy", "Assault" };

        HouseNameDialog factionDialog = new HouseNameDialog(client,
              "Faction",
              false,
              false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.isEmpty())) {
            return;
        }

        String unitTypestr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select Unit Type",
              "Unit Type",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              unitTypes,
              unitTypes[0]);

        if ((unitTypestr == null) || (unitTypestr.isEmpty())) {
            return;
        }

        String unitClassstr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select Unit Class",
              "Unit Class",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              unitClass,
              unitClass[0]);

        if ((unitClassstr == null) || (unitClassstr.isEmpty())) {
            return;
        }

        String priceMod = JOptionPane.showInputDialog(client.getMainFrame(), "Price Modifier:");

        if ((priceMod == null) || (priceMod.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsethousepricemod#\{factionName}#\{unitTypestr}#\{unitClassstr}#\{priceMod}");
    }

    public void jMenuAdminSetHouseFluMod_actionPerformed(ActionEvent e) {
        Object[] unitTypes = { "Mek", "Vehicles", "Infantry", "ProtoMek", "BattleArmor", "Aero" };
        Object[] unitClass = { "Light", "Medium", "Heavy", "Assault" };

        HouseNameDialog factionDialog = new HouseNameDialog(client,
              "Faction",
              false,
              false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.isEmpty())) {
            return;
        }

        String unitTypestr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select Unit Type",
              "Unit Type",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              unitTypes,
              unitTypes[0]);

        if ((unitTypestr == null) || (unitTypestr.isEmpty())) {
            return;
        }

        String unitClassstr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select Unit Class",
              "Unit Class",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              unitClass,
              unitClass[0]);

        if ((unitClassstr == null) || (unitClassstr.isEmpty())) {
            return;
        }

        String fluMod = JOptionPane.showInputDialog(client.getMainFrame(), "Price Modifier:");

        if ((fluMod == null) || (fluMod.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsethouseflumod#\{factionName}#\{unitTypestr}#\{unitClassstr}#\{fluMod}");
    }

    public void jMenuAdminSetHouseTechLevel_actionPerformed(ActionEvent e) {

        HouseNameDialog factionDialog = new HouseNameDialog(client,
              "Faction",
              false,
              false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.isEmpty())) {
            return;
        }

        JComboBox<String> techCombo = new JComboBox<>(TechConstants.T_NAMES);
        techCombo.setEditable(false);

        JOptionPane jop = new JOptionPane(techCombo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select Tech Level");
        techCombo.grabFocus();
        techCombo.getEditor().selectAll();

        dlg.setVisible(true);

        if ((Integer) jop.getValue() == JOptionPane.CANCEL_OPTION) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsethousetechlevel#\{factionName}#\{techCombo.getSelectedIndex()}");
    }

    public void jMenuAdminSaveTheUniverse_actionPerformed(ActionEvent e) {
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsave");
    }

    public void jMenuAdminSaveBlackMarketSettings_actionPerformed(ActionEvent e) {
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsaveblackmarketconfigs");
    }

    public void jMenuAdminSavePlanetsToXML_actionPerformed(ActionEvent e) {
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsaveplanetstoxml");
    }

    public void jMenuAdminOmniVariantMod_actionPerformed(ActionEvent e) {
        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(client.getMainFrame());
        mekwars.common.gui.dialogs.NewUnitViewerDialog unitSelector = new mekwars.common.gui.dialogs.NewUnitViewerDialog(
              client.getMainFrame(),
              unitLoadingDialog,
              client,
              mekwars.common.gui.dialogs.NewUnitViewerDialog.OMNI_VARIANT_SELECTOR);
        unitSelector.setName("Unit Selector");
        new Thread(unitSelector).start();
    }

    public void jMenuAdminServerAmmoBan_actionPerformed(ActionEvent e) {
        new BannedAmmoDialog(client, null);
    }

    public void jMenuAdminServerTargetBan_actionPerformed(ActionEvent e) {
        new BannedTargetingDialog(client);
    }

    public void jMenuAdminListMuls_actionPerformed(ActionEvent e) {
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c listMuls");
    }

    public void jMenuAdminRetrieveMul_actionPerformed(ActionEvent e) {
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c listMuls#SMFD");
    }

    public void jMenuAdminRetrieveAllMuls_actionPerformed(ActionEvent e) {
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c RetrieveAllMuls");
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
            line.append(STR."\{IClient.CAMPAIGN_PREFIX}UploadMul ");
            line.append(file.getName());
            try {
                FileInputStream in = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                try {
                    while (br.ready()) {
                        line.append("#").append(br.readLine());
                    }
                    br.close();
                    in.close();
                } catch (IOException ioex) {
                    MWLogger.errLog(STR."IOException: \{line.toString()}");
                }
            } catch (FileNotFoundException fnfex) {
                MWLogger.errLog(STR."FileNotFoundException: \{line.toString()}");
            }
            line.append("#");
            client.sendChat(line.toString());

        }
    }

    public void jMenuAdminRequestBuildTable_actionPerformed(ActionEvent e) {
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}AdminRequestBuildTable list");
    }

    public void jMenuAdminPruneBuildTable_actionPerformed(ActionEvent e) {
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}AdminRequestBuildTable prune");
    }

    public void jMenuAdminUploadBuildTable_actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();

        chooser.setCurrentDirectory(new File("./data/buildtables"));

        int returnVal = chooser.showOpenDialog(chooser);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            StringBuilder line = new StringBuilder();
            line.append(STR."\{IClient.CAMPAIGN_PREFIX}AdminUploadBuildTable ");
            String path = file.getPath();
            if (path.contains("rare")) {
                path = STR."rare/\{file.getName()}";
            } else if (path.contains("standard")) {
                path = STR."standard/\{file.getName()}";
            } else if (path.contains("reward")) {
                path = STR."reward/\{file.getName()}";
            }
            line.append(path);
            try {
                FileInputStream in = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                try {
                    while (br.ready()) {
                        line.append("#").append(br.readLine());
                    }
                    br.close();
                    in.close();
                } catch (IOException ioex) {
                    MWLogger.errLog(STR."IOException: \{line.toString()}");
                }
            } catch (FileNotFoundException fnfex) {
                MWLogger.errLog(STR."FileNotFoundException: \{line.toString()}");
            }
            line.append("#");
            client.sendChat(line.toString());

        }
    }

    public void jMenuAdminHouseAmmoBan_actionPerformed(ActionEvent e) {
        HouseNameDialog factionDialog = new HouseNameDialog(client,
              "Select Faction",
              false,
              false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.isEmpty())) {
            return;
        }

        new BannedAmmoDialog(client, client.getData().getHouseByName(factionName));
    }

    public void jMenuAdminSetHouseBasePilotSkills_actionPerformed(ActionEvent e) {
        HouseNameDialog factionDialog = new HouseNameDialog(client,
              "Select Faction",
              false,
              false);
        factionDialog.setVisible(true);
        String factionName = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((factionName == null) || (factionName.isEmpty())) {
            return;
        }

        Object[] unitTypes = { "Mek", "Vehicles", "Infantry", "ProtoMeks", "BattleArmor", "Aero" };

        String unitTypestr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select Unit Type",
              "Unit Type",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              unitTypes,
              unitTypes[0]);

        if ((unitTypestr == null) || (unitTypestr.isEmpty())) {
            return;
        }

        int unitTypeint = Unit.getTypeIDForName(unitTypestr);

        String gunnery = JOptionPane.showInputDialog(client.getMainFrame(), "Base Gunnery");

        if ((gunnery == null) || (gunnery.isEmpty())) {
            return;
        }

        String piloting = JOptionPane.showInputDialog(client.getMainFrame(), "Base Piloting");

        if ((piloting == null) || (piloting.isEmpty())) {
            return;
        }

        String sendCommand = STR."\{factionName}#\{unitTypeint}#\{gunnery}#\{piloting}";

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c sethousebasepilotskills#\{sendCommand}");
    }

    public void jMenuAdminSetCommandLevel_actionPerformed(ActionEvent e) {

        CommandNameDialog commandDialog = new CommandNameDialog(client, "Select a Command");
        commandDialog.setVisible(true);
        String commandNamestr = commandDialog.getCommandName();
        commandDialog.dispose();

        if ((commandNamestr == null) || commandNamestr.equalsIgnoreCase("null")) {
            return;
        }

        String level = JOptionPane.showInputDialog(client.getMainFrame(), "Level");

        if ((level == null) || (level.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsetCommandLevel#\{commandNamestr}#\{level}");
    }

    public void jMenuAdminLockFactory_actionPerformed(ActionEvent e) {
        TreeSet<String> names = new TreeSet<>();

        PlanetNameDialog planetDialog = new PlanetNameDialog(
              client,
              "Select a Planet",
              null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.isEmpty())) {
            return;
        }

        Planet planet = client.getData().getPlanetByName(planetNamestr);

        for (UnitFactory UF : planet.getUnitFactories()) {
            names.add(UF.getName());
        }

        JComboBox<String> combo = new JComboBox<>(names.toArray(new String[names.size()]));
        combo.setEditable(true);
        JOptionPane jop = new JOptionPane(combo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select factory to toggle the lock on.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        String factoryName = (String) combo.getSelectedItem();

        if ((factoryName == null) || (factoryName.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminlockfactory#\{planetNamestr}#\{factoryName}");
        client.reloadData();

    }

    public void jMenuAdminSetPlanetMapSize_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(
              client,
              "Select a Planet",
              null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.isEmpty())) {
            return;
        }

        String xSize = JOptionPane.showInputDialog(client.getMainFrame(), "X size");

        if ((xSize == null) || (xSize.isEmpty())) {
            return;
        }

        String ySize = JOptionPane.showInputDialog(client.getMainFrame(), "Y Size");

        if ((ySize == null) || (ySize.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsetplanetmapsize#\{planetNamestr}#\{xSize}#\{ySize}");
        client.reloadData();
    }

    public void jMenuAdminSetPlanetHomeWorld_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(
              client,
              "Select a Planet",
              null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.isEmpty())) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(null,
              "Set as HomeWorld?",
              "Set HomeWorld",
              JOptionPane.YES_NO_CANCEL_OPTION);

        if (result == JOptionPane.CANCEL_OPTION) {
            return;
        }

        boolean homeworld = false;
        if (result == JOptionPane.YES_OPTION) {
            homeworld = true;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsethomeworld#\{planetNamestr}#\{homeworld}");
        client.reloadData();

    }

    public void jMenuAdminSetPlanetBoardSize_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(
              client,
              "Select a Planet",
              null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.isEmpty())) {
            return;
        }

        String xSize = JOptionPane.showInputDialog(client.getMainFrame(), "X size");

        if ((xSize == null) || (xSize.isEmpty())) {
            return;
        }

        String ySize = JOptionPane.showInputDialog(client.getMainFrame(), "Y Size");

        if ((ySize == null) || (ySize.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsetplanetboardsize#\{planetNamestr}#\{xSize}#\{ySize}");
        client.reloadData();
    }

    public void jMenuAdminSetPlanetOriginalOwner_actionPerformed(ActionEvent ex) {

        PlanetNameDialog planetDialog = new PlanetNameDialog(
              client,
              "Select a Planet",
              null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.isEmpty())) {
            return;
        }

        HouseNameDialog hnd = new HouseNameDialog(client,
              "Select Original Owner",
              false,
              false);
        hnd.setVisible(true);
        String owner = hnd.getHouseName();
        hnd.dispose();

        if ((owner == null) || (owner.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsetplanetoriginalowner#\{planetNamestr}#\{owner}");
        client.reloadData();
    }

    public void jMenuAdminSetPlanetTemperature_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(
              client,
              "Select a Planet",
              null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.isEmpty())) {
            return;
        }

        String lowTemp = JOptionPane.showInputDialog(client.getMainFrame(), "Low Temp");

        if ((lowTemp == null) || (lowTemp.isEmpty())) {
            return;
        }

        String hiTemp = JOptionPane.showInputDialog(client.getMainFrame(), "Hi Temp");

        if ((hiTemp == null) || (hiTemp.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsetplanettemperature#\{planetNamestr}#\{lowTemp}#\{hiTemp}");
        client.reloadData();
    }

    public void jMenuAdminSetPlanetGravity_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(
              client,
              "Select a Planet",
              null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.isEmpty())) {
            return;
        }

        String grav = JOptionPane.showInputDialog(client.getMainFrame(), "Gravity");

        if ((grav == null) || (grav.isEmpty())) {
            return;
        }

        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminsetplanetgravity#\{planetNamestr}#\{grav}");
        client.reloadData();
    }

    public void jMenuAdminCommandLists_actionPerformed(ActionEvent e) {
        CommandNameDialog commandDialog = new CommandNameDialog(client, "Select a Command");
        commandDialog.setVisible(true);
        String commandNamestr = commandDialog.getCommandName();
        commandDialog.dispose();

        if (commandNamestr != null) {
            String input = IClient.CAMPAIGN_PREFIX + commandNamestr;
            client.getMainFrame().getMainPanel().getCommPanel().setInput(input);
            client.getMainFrame().getMainPanel().getCommPanel().focusInputField();
        }
    }

    public void jMenuAdminComponentList_actionPerformed(ActionEvent e) {
        // ComponentDisplayDialog componentDialog =
        int type = (Integer) e.getSource();
        new ComponentDisplayDialog(client, type);
        // componentDialog.setVisible(true);
    }

    public void jMenuAdminCreateMulArmy_actionPerformed(ActionEvent e) {
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}listmuls CAFM");
    }

    private void jMenuAdminReloadSupportUnits_actionPerformed(ActionEvent e) {
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}adminReloadSupportUnits");
    }

    private void jMenuAdminReloadSanitizer_actionPerformed(ActionEvent e) {
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}adminReloadHTMLSanitizerConfigs");
    }

}// end AdminMenu class
