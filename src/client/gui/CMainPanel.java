/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

package client.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import client.MWClient;
import common.BMEquipment;
import common.util.MWLogger;

/**
 * Main panel
 */

public class CMainPanel extends JPanel implements ChangeListener, ComponentListener {
	/**
     *
     */
    private static final long serialVersionUID = -7817596095411018999L;
    JSplitPane MainSPane;
	JSplitPane TabSPane;
	JSplitPane SideSPane;
	CPlayerPanel PlayerPanel;
	CUserListPanel UserListPanel;
	JTabbedPane MainTPane = new JTabbedPane(SwingConstants.BOTTOM);
	CCommPanel CommPanel;
	CSelectTabAction CommSelect;
	CHQPanel HQPanel = null;
	CBMPanel BMPanel = null;
	CHSPanel HSPanel = null;
	CRulesPanel RulesPanel = null; //@salient
	JTabbedPane BMETabbed = null;
	CSelectTabAction HQSelect = null;
	CSelectTabAction RulesSelect = null; //@salient
	CSelectTabAction BMSelect = null;
	CSelectTabAction HSSelect = null;
	CSelectTabAction BMESelect = null;

	CBattlePanel BattlePanel = null;
	CSelectTabAction BattleSelect = null;
	CMapPanel MapPanel = null;
	CSelectTabAction MapSelect = null;

	MWClient mwclient;

	CTabForwardAction ForwardMainTab;
	CTabBackwardAction BackwardMainTab;
	int panelDivider;
    int playerPanelDivider;
    int verticalPanelDivider;
	int sPanelDivider = 0;

	public CMainPanel(MWClient client, CMainFrame mainFrame) {
		mwclient = client;
		setLayout(new BorderLayout());
		setMinimumSize(new Dimension(620, 400));
		addComponentListener(this);
		createMainTPane(mainFrame);
		PlayerPanel = new CPlayerPanel(mwclient);
		UserListPanel = new CUserListPanel(mwclient);
		add(PlayerPanel, BorderLayout.NORTH);
		panelDivider = mwclient.getConfig().getIntParam("PANELDIVIDER");
		//panelDivider = panelDividerInt;//(double)panelDividerInt/100;
		sPanelDivider = mwclient.getConfig().getIntParam("SPLITTERSIZE");
		playerPanelDivider = mwclient.getConfig().getIntParam("PLAYERPANELDIVIDER");
        verticalPanelDivider = mwclient.getConfig().getIntParam("VERTICALDIVIDER");

        TabSPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, MainTPane, CommPanel);
		TabSPane.setOneTouchExpandable(true);
		TabSPane.setDividerLocation(panelDivider);
		TabSPane.setDividerSize(sPanelDivider);

		//    UserListPanel.setMinimumSize(new Dimension(100, 100));
		//    PlayerPanel.setMinimumSize(new Dimension(100, 100));
		SideSPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, PlayerPanel, UserListPanel);
		//    SideSPane.add(PlayerPanel);
		SideSPane.setOneTouchExpandable(true);
		SideSPane.setDividerLocation(playerPanelDivider);
		SideSPane.setDividerSize(sPanelDivider);
		MainSPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, TabSPane, SideSPane);
		MainSPane.setOneTouchExpandable(true);
		MainSPane.setDividerLocation(verticalPanelDivider);
		MainSPane.setDividerSize(sPanelDivider);
		add(MainSPane, BorderLayout.CENTER);

		ForwardMainTab = new CTabForwardAction();
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt X"), "TabForward");
		getActionMap().put("TabForward", ForwardMainTab);
		BackwardMainTab = new CTabBackwardAction();
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("shift alt X"), "TabBackward");
		getActionMap().put("TabBackward", BackwardMainTab);
		revalidate();
	}

	private void addPanelMain(
			JPanel panel,
			CSelectTabAction select,
			String name,
			String tooltip,
			String mnemostr,
			String commandStr) {
		MainTPane.addTab(name, null, panel, tooltip);
		int index = MainTPane.indexOfComponent(panel);
		int mnemo = MainTPane.getTitleAt(index).indexOf(mnemostr.toUpperCase());
		if (mnemo == -1)
			mnemo = MainTPane.getTitleAt(index).indexOf(mnemostr.toLowerCase());
		MainTPane.setDisplayedMnemonicIndexAt(index, mnemo);
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt "+mnemostr), commandStr);
		getActionMap().put(commandStr, select);
	}

	private void addPanelCComm(
			JPanel panel,
			CSelectTabAction select,
			String name,
			String tooltip,
			String mnemostr,
			String commandStr,
			CCommPanel CommPanel) {
		CommPanel.CommTPane.addTab(name, null, panel, tooltip);
		int index = CommPanel.CommTPane.indexOfComponent(panel);
		int mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemostr.toUpperCase());
		if (mnemo == -1)
			mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemostr.toLowerCase());
		CommPanel.CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
		CommPanel.CommTPane.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt "+mnemostr), commandStr);
		CommPanel.CommTPane.getActionMap().put(commandStr, select);
	}

    //Why is this defined twice? - salient
	private void addPanelMain(
			JTabbedPane panel,
			CSelectTabAction select,
			String name,
			String tooltip,
			String mnemostr,
			String commandStr) {
		MainTPane.addTab(name, null, panel, tooltip);
		int index = MainTPane.indexOfComponent(panel);
		int mnemo = MainTPane.getTitleAt(index).indexOf(mnemostr.toUpperCase());
		if (mnemo == -1)
			mnemo = MainTPane.getTitleAt(index).indexOf(mnemostr.toLowerCase());
		MainTPane.setDisplayedMnemonicIndexAt(index, mnemo);
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt "+mnemostr), commandStr);
		getActionMap().put(commandStr, select);
	}

	private void addPanelCComm(
			JTabbedPane panel,
			CSelectTabAction select,
			String name,
			String tooltip,
			String mnemostr,
			String commandStr,
			CCommPanel CommPanel) {
		CommPanel.CommTPane.addTab(name, null, panel, tooltip);
		int index = CommPanel.CommTPane.indexOfComponent(panel);
		int mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemostr.toUpperCase());
		if (mnemo == -1)
			mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemostr.toLowerCase());
		CommPanel.CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
		CommPanel.CommTPane.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt "+mnemostr), commandStr);
		CommPanel.CommTPane.getActionMap().put(commandStr, select);
	}

	private void createMainTPane(CMainFrame mainFrame)
	{
		//int index;
		//int mnemo;

		CommPanel = new CCommPanel(mwclient);

        String tabText="";
		String mnemonicText="";
		if ( mwclient.getConfig().isParam("HQTABVISIBLE"))
		{
			HQPanel = new CHQPanel(mwclient);
			HQSelect = new CSelectTabAction(HQPanel);
			tabText = mwclient.getConfig().getParam("HQTABNAME");
			mnemonicText = mwclient.getConfig().getParam("HQMNEMONIC");
			if ( mwclient.getConfig().isParam("HQINTOPROW"))
				addPanelMain(HQPanel, HQSelect, tabText, "Command Center and Hangars (Alt + "+mnemonicText+")",mnemonicText, "HQSelect");
			else
				addPanelCComm(HQPanel, HQSelect, tabText, "Command Center and Hangars (Alt + "+mnemonicText+")", mnemonicText, "HQSelect",CommPanel);
		}

		if ( mwclient.getConfig().isParam("BMTABVISIBLE"))
		{
			BMPanel = new CBMPanel(mwclient);
			BMSelect = new CSelectTabAction(BMPanel);
			tabText = mwclient.getConfig().getParam("BMTABNAME");
			mnemonicText = mwclient.getConfig().getParam("BMMNEMONIC");
			if ( mwclient.getConfig().isParam("BMINTOPROW"))
				addPanelMain(BMPanel, BMSelect, tabText, "Buy and Sell Units (Alt + "+mnemonicText+")", mnemonicText, "BMSelect");
			else
				addPanelCComm(BMPanel, BMSelect, tabText, "Buy and Sell Units (Alt + "+mnemonicText+")", mnemonicText, "BMSelect",CommPanel);
		}

		if ( mwclient.getConfig().isParam("BMETABVISIBLE") && Boolean.parseBoolean(mwclient.getserverConfigs("UsePartsBlackMarket")) )
		{
			BMETabbed = new JTabbedPane(SwingConstants.BOTTOM);
			BMETabbed.addTab("Ammo", new CBMPartsPanel(mwclient,BMEquipment.PART_AMMO));
			BMETabbed.addTab("Armor", new CBMPartsPanel(mwclient,BMEquipment.PART_ARMOR));
			BMETabbed.addTab("Weapons", new CBMPartsPanel(mwclient,BMEquipment.PART_WEAPON));
			BMETabbed.addTab("Misc", new CBMPartsPanel(mwclient,BMEquipment.PART_MISC));
			BMESelect = new CSelectTabAction(BMETabbed);
			tabText = mwclient.getConfig().getParam("BMETABNAME");
			mnemonicText = mwclient.getConfig().getParam("BMEMNEMONIC");
			if ( mwclient.getConfig().isParam("BMEINTOPROW"))
				addPanelMain(BMETabbed, BMESelect, tabText, "Buy and Sell Parts (Alt + "+mnemonicText+")", mnemonicText, "BMESelect");
			else
				addPanelCComm(BMETabbed, BMESelect, tabText, "Buy and Sell Parts (Alt + "+mnemonicText+")", mnemonicText, "BMESelect",CommPanel);
		}

		HSPanel = new CHSPanel(mwclient);
		if ( mwclient.getConfig().isParam("HSTATUSTABVISIBLE")) {
			HSSelect = new CSelectTabAction(HSPanel);
			tabText = mwclient.getConfig().getParam("HSTATUSTABNAME");
			mnemonicText = mwclient.getConfig().getParam("HSTATUSMNEMONIC");
			if ( mwclient.getConfig().isParam("HSTATUSINTOPROW"))
				addPanelMain(HSPanel, HSSelect, tabText, "Show current House Status (Alt + "+mnemonicText+")", mnemonicText, "HSSelect");
			else
				addPanelCComm(HSPanel, HSSelect, tabText, "Show current House Status (Alt + "+mnemonicText+")", mnemonicText, "HSSelect",CommPanel);
		}

		if ( mwclient.getConfig().isParam("BATTLETABVISIBLE"))
		{
			BattlePanel = new CBattlePanel(mwclient);
			BattleSelect = new CSelectTabAction(BattlePanel);
			tabText = mwclient.getConfig().getParam("BATTLETABNAME");
			mnemonicText = mwclient.getConfig().getParam("BATTLEMNEMONIC");
			if ( mwclient.getConfig().isParam("BATTLEINTOPROW"))
				addPanelMain(BattlePanel, BattleSelect, tabText, "Battles Intelligence Data (Alt + "+mnemonicText+")", mnemonicText, "BattleSelect");
			else
				addPanelCComm(BattlePanel, BattleSelect, tabText, "Battles Intelligence Data (Alt + "+mnemonicText+")", mnemonicText, "BattleSelect",CommPanel);
		}

		MapPanel = new CMapPanel(mwclient, mainFrame, CommPanel.getWidth(), CommPanel.getHeight());
		if ( mwclient.getConfig().isParam("MAPTABVISIBLE")) {
			MapSelect = new CSelectTabAction(MapPanel);
			tabText = mwclient.getConfig().getParam("MAPTABNAME");
			mnemonicText = mwclient.getConfig().getParam("MAPMNEMONIC");
			if ( mwclient.getConfig().isParam("MAPINTOPROW"))
				addPanelMain(MapPanel, MapSelect, tabText, "Star Map (Alt + "+mnemonicText+")", mnemonicText, "MapSelect");
			else
				addPanelCComm(MapPanel, MapSelect, tabText, "Star Map (Alt + "+mnemonicText+")", mnemonicText, "MapSelect",CommPanel);
		}

		if ( mwclient.getConfig().isParam("RULESTABVISIBLE")) //@salient
		{
			RulesPanel = new CRulesPanel(mwclient);
			RulesSelect = new CSelectTabAction(RulesPanel);
			tabText = mwclient.getConfig().getParam("RULESTABNAME");
			mnemonicText = mwclient.getConfig().getParam("RULESMNEMONIC");
			if ( mwclient.getConfig().isParam("RULESINTOPROW"))
				addPanelMain(RulesPanel, RulesSelect, tabText, "Rules Tab (Alt + "+mnemonicText+")",mnemonicText, "RulesSelect");
	        else
	        	addPanelCComm(RulesPanel, RulesSelect, tabText, "Rules Tab (Alt + "+mnemonicText+")",mnemonicText, "RulesSelect",CommPanel);
		}

		MainTPane.addChangeListener(this);
	}

	public void changeStatus(int status, int laststatus) {

		if (status == MWClient.STATUS_RESERVE) {
			if (laststatus == MWClient.STATUS_LOGGEDOUT) {
				UserListPanel.setLoggedIn(true);
				UserListPanel.getUsers().getRenderer().setLoggedIn(true);
				if (mwclient.getConfig().isParam("PLAYERPANEL")) {PlayerPanel.setVisible(true);}
				TabSPane.setDividerLocation(panelDivider);
				SideSPane.setDividerLocation(playerPanelDivider);
			}
		}

		if (status == MWClient.STATUS_DISCONNECTED || status == MWClient.STATUS_LOGGEDOUT) {
			UserListPanel.setLoggedIn(false);
			UserListPanel.getUsers().getRenderer().setLoggedIn(false);
			PlayerPanel.setVisible(false);
		}

		if (status == MWClient.STATUS_DISCONNECTED) {MainTPane.setVisible(false);}
		else {MainTPane.setVisible(true);}
	}

	/**
	 * A method which selects the FIRST tab, whatever it may be.
	 */
	public void selectFirstTab() {
		try {
			MainTPane.setSelectedIndex(0);
		} catch (Exception e) {
			//do nothing. just means no upper-level tabs.
		}
	}

	/**
	 * A method which selects the map tab, whether it is in
	 * the main or comm panel, and sends it to the front.
	 *
	 * Used by MMNETHyperLinkListener if MAPTABONCLICK is set.
	 */
	public void selectMapTab() {

		//get the map name
		String nameToFind = mwclient.getConfigParam("MAPTABNAME");

		//look for map in main/top
		for (int i = MainTPane.getTabCount() - 1; 0 <= i ; i--) {
			String currTitle = MainTPane.getTitleAt(i);
			if (currTitle.equals(nameToFind)) {
				MainTPane.setSelectedIndex(i);
				return;
			}
		}

		//look for map in chat/bottom
		for (int i = CommPanel.CommTPane.getTabCount() - 1; 0 <= i ; i--) {
			String currTitle = CommPanel.CommTPane.getTitleAt(i);
			if (currTitle.equals(nameToFind)) {
				CommPanel.CommTPane.setSelectedIndex(i);
				return;
			}
		}
	}//end selectMapTab

	public CPlayerPanel getPlayerPanel() {return PlayerPanel;}
    public void setPlayerPanel(CPlayerPanel panel) {PlayerPanel = panel;}

    public CMapPanel getMapPanel() {return MapPanel;}

    public JSplitPane getTabSPane(){return TabSPane;}
    public JSplitPane getMainSPane(){return MainSPane;}
    public JSplitPane getSideSPane(){return SideSPane;}

	public CUserListPanel getUserListPanel() {return UserListPanel;}

	public CCommPanel getCommPanel() {return CommPanel;}

	public CHQPanel getHQPanel() {return HQPanel;}
	public CBMPanel getBMPanel() {return BMPanel;}
	public CHSPanel getHSPanel() {return HSPanel;}
	public void refreshBME() {
		if ( BMETabbed == null )
			return;
		try{
		((CBMPartsPanel)BMETabbed.getComponentAt(0)).refresh();
		((CBMPartsPanel)BMETabbed.getComponentAt(1)).refresh();
		((CBMPartsPanel)BMETabbed.getComponentAt(2)).refresh();
		((CBMPartsPanel)BMETabbed.getComponentAt(3)).refresh();
		}catch (Exception ex){
			MWLogger.errLog(ex);
		}
	}

	public JTable getBattleTable() {return BattlePanel.getBattleTable();}

	public void refreshBattleTable() {BattlePanel.getBattleTableModel().refreshModel();}


	// change listener
	public void stateChanged(ChangeEvent e) {
		if (MainTPane.getSelectedIndex() == -1) {return;}
		if (MainTPane.getSelectedComponent() == CommPanel) {CommPanel.getInputField().requestFocusInWindow();}
	}

	// component listener
	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}

	public void componentResized(ComponentEvent e) {
		TabSPane.setDividerLocation(panelDivider);
		SideSPane.setDividerLocation(playerPanelDivider);
		MainSPane.setDividerLocation(verticalPanelDivider);
	}

	public void componentShown(ComponentEvent e) {
		TabSPane.setDividerLocation(panelDivider);
		SideSPane.setDividerLocation(playerPanelDivider);
		MainSPane.setDividerLocation(verticalPanelDivider);
	}

	// component listener
	// actions

	private class CTabForwardAction extends AbstractAction {

		/**
         *
         */
        private static final long serialVersionUID = -6816947698919825957L;
        public CTabForwardAction() {}
		public void actionPerformed(ActionEvent e) {

			int count = MainTPane.getTabCount();
			if (count < 2) {return;}
			int index = MainTPane.getSelectedIndex();
			index++;
			if (index == count) {index = 0;}
			while (!MainTPane.isEnabledAt(index)) {
				index++;
				if (index == count) {index = 0;}
			}

			MainTPane.setSelectedIndex(index);
		}
	}

	private class CTabBackwardAction extends AbstractAction {

		/**
         *
         */
        private static final long serialVersionUID = -507793785645622171L;
        public CTabBackwardAction() {}
		public void actionPerformed(ActionEvent e) {
			int count = MainTPane.getTabCount();
			if (count < 2) {return;}
			int index = MainTPane.getSelectedIndex();
			index--;
			if (index == -1) {index = count - 1;}
			while (!MainTPane.isEnabledAt(index)){
				index--;
				if (index == -1) {index = count - 1;}
			}

			MainTPane.setSelectedIndex(index);
		}
	}

	private class CSelectTabAction extends AbstractAction {

		/**
         *
         */
        private static final long serialVersionUID = -1191343876143323182L;
        Component Tab = null;
		public CSelectTabAction(Component tab) {Tab = tab;}
		public void actionPerformed(ActionEvent e) {
			try{
				if (MainTPane.isEnabledAt(MainTPane.indexOfComponent(Tab))) {MainTPane.setSelectedComponent(Tab);}
			} catch (Exception ex) {
				if (CommPanel.CommTPane.isEnabledAt(CommPanel.CommTPane.indexOfComponent(Tab)))
					CommPanel.CommTPane.setSelectedComponent(Tab);
			}
		}
	}

	// actions

    public void recreateMainTPane(CMainFrame mainFrame)  {
        //int index;
        //int mnemo;

        MainTPane.removeAll();
        CommPanel.CommTPane.removeAll();
        CommPanel.reload();

        String tabText="";
        String mnemonicText="";
        if ( mwclient.getConfig().isParam("HQTABVISIBLE"))
        {
            if ( HQPanel == null ){
                HQPanel = new CHQPanel(mwclient);
                HQSelect = new CSelectTabAction(HQPanel);
            }
            tabText = mwclient.getConfig().getParam("HQTABNAME");
            mnemonicText = mwclient.getConfig().getParam("HQMNEMONIC");
            if ( mwclient.getConfig().isParam("HQINTOPROW"))
                addPanelMain(HQPanel, HQSelect, tabText, "Command Center and Hangars (Alt + "+mnemonicText+")",mnemonicText, "HQSelect");
            else
                addPanelCComm(HQPanel, HQSelect, tabText, "Command Center and Hangars (Alt + "+mnemonicText+")", mnemonicText, "HQSelect",CommPanel);
        }

        if ( mwclient.getConfig().isParam("BMTABVISIBLE"))
        {
            if ( BMPanel == null ){
                BMPanel = new CBMPanel(mwclient);
                BMSelect = new CSelectTabAction(BMPanel);
            }
            tabText = mwclient.getConfig().getParam("BMTABNAME");
            mnemonicText = mwclient.getConfig().getParam("BMMNEMONIC");
            if ( mwclient.getConfig().isParam("BMINTOPROW"))
                addPanelMain(BMPanel, BMSelect, tabText, "Buy and Sell Units (Alt + "+mnemonicText+")", mnemonicText, "BMSelect");
            else
                addPanelCComm(BMPanel, BMSelect, tabText, "Buy and Sell Units (Alt + "+mnemonicText+")", mnemonicText, "BMSelect",CommPanel);
        }

        if ( mwclient.getConfig().isParam("BMETABVISIBLE") && Boolean.parseBoolean(mwclient.getserverConfigs("UsePartsBlackMarket")) )
        {
            if ( BMETabbed == null ){
            	BMETabbed = new JTabbedPane(SwingConstants.BOTTOM);
    			BMETabbed.addTab("Ammo", new CBMPartsPanel(mwclient,BMEquipment.PART_AMMO));
    			BMETabbed.addTab("Armor", new CBMPartsPanel(mwclient,BMEquipment.PART_ARMOR));
    			BMETabbed.addTab("Weapons", new CBMPartsPanel(mwclient,BMEquipment.PART_WEAPON));
    			BMETabbed.addTab("Misc", new CBMPartsPanel(mwclient,BMEquipment.PART_MISC));
    			BMESelect = new CSelectTabAction(BMETabbed);
            }
            tabText = mwclient.getConfig().getParam("BMETABNAME");
            mnemonicText = mwclient.getConfig().getParam("BMEMNEMONIC");
            if ( mwclient.getConfig().isParam("BMEINTOPROW"))
                addPanelMain(BMETabbed, BMSelect, tabText, "Buy and Sell Parts (Alt + "+mnemonicText+")", mnemonicText, "BMESelect");
            else
                addPanelCComm(BMETabbed, BMSelect, tabText, "Buy and Sell Parts (Alt + "+mnemonicText+")", mnemonicText, "BMESelect",CommPanel);
        }

        if ( mwclient.getConfig().isParam("HSTATUSTABVISIBLE"))
        {
            if (HSPanel == null){
                HSPanel = new CHSPanel(mwclient);
                HSSelect = new CSelectTabAction(HSPanel);
            } else if (HSSelect == null) {
            	 HSSelect = new CSelectTabAction(HSPanel);
            }
            tabText = mwclient.getConfig().getParam("HSTATUSTABNAME");
            mnemonicText = mwclient.getConfig().getParam("HSTATUSMNEMONIC");
            if ( mwclient.getConfig().isParam("HSTATUSINTOPROW"))
                addPanelMain(HSPanel, HSSelect, tabText, "Show current House Status (Alt + "+mnemonicText+")", mnemonicText, "HSSelect");
            else
                addPanelCComm(HSPanel, HSSelect, tabText, "Show current House Status (Alt + "+mnemonicText+")", mnemonicText, "HSSelect",CommPanel);
        }

        if ( mwclient.getConfig().isParam("BATTLETABVISIBLE"))
        {
            if ( BattlePanel == null){
                BattlePanel = new CBattlePanel(mwclient);
                BattleSelect = new CSelectTabAction(BattlePanel);
            }
            tabText = mwclient.getConfig().getParam("BATTLETABNAME");
            mnemonicText = mwclient.getConfig().getParam("BATTLEMNEMONIC");
            if ( mwclient.getConfig().isParam("BATTLEINTOPROW"))
                addPanelMain(BattlePanel, BattleSelect, tabText, "Battles Intelligence Data (Alt + "+mnemonicText+")", mnemonicText, "BattleSelect");
            else
                addPanelCComm(BattlePanel, BattleSelect, tabText, "Battles Intelligence Data (Alt + "+mnemonicText+")", mnemonicText, "BattleSelect",CommPanel);
        }

        if ( MapPanel == null )
            MapPanel = new CMapPanel(mwclient, mainFrame, CommPanel.getWidth(), CommPanel.getHeight());
        if ( mwclient.getConfig().isParam("MAPTABVISIBLE")) {
            if ( MapSelect == null )
                MapSelect = new CSelectTabAction(MapPanel);
            tabText = mwclient.getConfig().getParam("MAPTABNAME");
            mnemonicText = mwclient.getConfig().getParam("MAPMNEMONIC");
            if ( mwclient.getConfig().isParam("MAPINTOPROW"))
                addPanelMain(MapPanel, MapSelect, tabText, "Star Map (Alt + "+mnemonicText+")", mnemonicText, "MapSelect");
            else
                addPanelCComm(MapPanel, MapSelect, tabText, "Star Map (Alt + "+mnemonicText+")", mnemonicText, "MapSelect",CommPanel);
        }

		if ( mwclient.getConfig().isParam("RULESTABVISIBLE"))
		{
			if(RulesPanel == null)
			{
				RulesPanel = new CRulesPanel(mwclient);
				RulesSelect = new CSelectTabAction(RulesPanel);
			}
			tabText = mwclient.getConfig().getParam("RULESTABNAME");
			mnemonicText = mwclient.getConfig().getParam("RULESMNEMONIC");
			if ( mwclient.getConfig().isParam("RULESINTOPROW"))
				addPanelMain(RulesPanel, RulesSelect, tabText, "Rules Tab (Alt + "+mnemonicText+")",mnemonicText, "RulesSelect");
	        else
	        	addPanelCComm(RulesPanel, RulesSelect, tabText, "Rules Tab (Alt + "+mnemonicText+")",mnemonicText, "RulesSelect",CommPanel);		
		}

        MainTPane.addChangeListener(this);
    }

}
