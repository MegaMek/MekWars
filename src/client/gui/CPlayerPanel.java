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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;
import javax.swing.text.html.HTMLEditorKit;

import client.MWClient;
import client.campaign.CPlayer;
import common.campaign.clientutils.IClientConfig;
import common.util.MWLogger;
import common.util.UnitUtils;

/**
 * Player panel
 */

public class CPlayerPanel extends JScrollPane {

    /**
     *
     */
    private static final long serialVersionUID = -7036003412110367753L;
    MWClient mwclient;
    CPlayer player;
    IClientConfig config;

    private static final String PP_NAME = "Name";
    private static final String PP_STATUS = "Status:";
    private static final String PP_EXP = "Experience:";
    private static final String PP_ELO = "Rating:";
    private static String PP_REWARD;
    private static final String PP_TECHS = "Techs:";
    private static final String PP_PAIDTECHS = "Paid Techs:";
    private static final String PP_BAYS = "Bays:";
    private static final String PP_IDLETECHS = "Idle Techs:";
    private static final String PP_FREEUNITS = "Free Units:"; //@Salient for free build

    protected JPanel PlayerPanel = new JPanel();
    protected JEditorPane lblLogo = new JEditorPane("text/html", "");
    protected JPanel InfoPanel = new JPanel();
    protected JLabel lblName = new JLabel();
    protected JLabel lblStatus = new JLabel();
    protected JLabel lblExp = new JLabel();
    protected JLabel lblRating = new JLabel();
    protected JLabel lblMoney = new JLabel();
    protected JLabel lblInfluence = new JLabel();
    protected JLabel lblMekbay = new JLabel();
    protected JLabel lblTechs = new JLabel();
    protected JLabel lblRewardPoints = new JLabel();
    protected JLabel lblNextTick = new JLabel();
    protected JLabel lblFreeMeks = new JLabel(); //@Salient for free build
    protected long nextTick = System.currentTimeMillis();

    public CPlayerPanel(MWClient client)
    {
        PP_REWARD = client.getserverConfigs("RPLongName") + ":";

    	Insets insets = new Insets(0, 0, 0, 0);
        //int west = GridBagConstraints.WEST;
        int center = GridBagConstraints.CENTER;
        int none = GridBagConstraints.NONE;
        //String tag = "<HTML><font size=-1>";
        //String endtag = "</font></HTML>";
        String endtag = "";
        String tag = "";
        Dimension dimension;
        boolean logo;
        int height;

        this.mwclient = client;
        player = client.getPlayer();
        config = client.getConfig();
        logo = client.getConfig().isParam("LOGO");
        height = client.getConfig().getIntParam("PLAYERPANELHEIGHT");

        setMinimumSize(new Dimension(0, 0));

        if (logo) {dimension = new Dimension(140, height + 140);}
        else {dimension = new Dimension(140, height);}
        PlayerPanel.setLayout(new GridBagLayout());
        PlayerPanel.setMinimumSize(dimension);
        PlayerPanel.setPreferredSize(dimension);
        //PlayerPanel.addMouseListener(this);

        dimension = new Dimension(140, 150);
        //lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        //lblLogo.setVerticalAlignment(SwingConstants.CENTER);
        //lblLogo.setIcon(client.getPlayer().getLogo());
        MMNetHyperLinkListener chatHLL = new MMNetHyperLinkListener(mwclient);
        MyHTMLEditorKit kit = new MyHTMLEditorKit();

        lblLogo.setPreferredSize(dimension);
        lblLogo.setMinimumSize(dimension);
        lblLogo.setMaximumSize(dimension);
        lblLogo.setEditable(false);

        lblLogo.addHyperlinkListener(chatHLL);
        lblLogo.setEditorKit(kit);
        lblLogo.setEnabled(true);

        if (logo) {PlayerPanel.add(lblLogo);}

        dimension = new Dimension(140, height + 30);
        InfoPanel.setLayout(new BoxLayout(InfoPanel, BoxLayout.Y_AXIS));
        InfoPanel.setPreferredSize(dimension);
        InfoPanel.setMinimumSize(dimension);
        InfoPanel.setMaximumSize(dimension);
        lblName.setText(tag + PP_NAME + endtag);
        lblStatus.setText(tag + PP_STATUS + endtag);
        lblExp.setText(tag + PP_EXP + endtag);
        lblRating.setText(tag + PP_ELO + endtag);
        lblMoney.setText(tag + client.getserverConfigs("MoneyLongName")+": "+ endtag);
        lblInfluence.setText(tag + client.getserverConfigs("FluLongName")+ endtag);
        if ( mwclient.isUsingAdvanceRepairs() ){
            lblMekbay.setText(tag + PP_BAYS + endtag);
            lblTechs.setText(tag + PP_IDLETECHS + endtag);
        }
        else{
            lblMekbay.setText(tag + PP_TECHS + endtag);
            lblTechs.setText(tag + PP_PAIDTECHS + endtag);
        }
        lblRewardPoints.setText(tag + PP_REWARD + endtag);
        lblFreeMeks.setText(tag + PP_FREEUNITS + endtag); //@Salient for free build
        lblNextTick.setText("Next Tick: N/A");
        InfoPanel.add(lblName);

        //Comment out profession/rank until it actually means something.
        //Label is still being made and updated, just not added to the panel.
        //@urgru 12.1.04
        //InfoPanel.add(lblProfession);

        InfoPanel.add(lblStatus);
        InfoPanel.add(lblExp);
        if (!Boolean.parseBoolean(client.getserverConfigs("HideELO")))
            InfoPanel.add(lblRating);
        InfoPanel.add(lblMoney);
        InfoPanel.add(lblInfluence);
        InfoPanel.add(lblMekbay);
        InfoPanel.add(lblTechs);
        if (Boolean.parseBoolean(client.getserverConfigs("ShowReward")))
            InfoPanel.add(lblRewardPoints);
        if (Integer.parseInt(client.getserverConfigs("FreeBuild_Limit")) > 0) //@Salient for free build
        	InfoPanel.add(lblFreeMeks);
        InfoPanel.add(lblNextTick);
        if (logo) {PlayerPanel.add(InfoPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, center, none, insets, 0, 0));}
        else {PlayerPanel.add(InfoPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, center, none, insets, 0, 0));}

        if (logo) {
            setPreferredSize(new Dimension(130, height + 140));
            setMinimumSize(new Dimension(140, height + 140));
            setMaximumSize(new Dimension(140, height + 140));
        } else {
            setPreferredSize(new Dimension(130, height + 40));
            setMinimumSize(new Dimension(140, height + 40));
            setMaximumSize(new Dimension(140, height + 40));
        }

        setBorder(new LineBorder(Color.black));
        setViewportView(PlayerPanel);

        Thread clockT = new TThread(this);
        clockT.start();

    }

    public void refresh() {

        if (mwclient.getConfig().isParam("LOGO")) {
            try{
                lblLogo.setBackground(mwclient.getMainFrame().getBackground());
                lblLogo.getDocument().remove(0,lblLogo.getDocument().getLength());
                ((HTMLEditorKit) lblLogo.getEditorKit()).read(
                    new StringReader(mwclient.getPlayer().getLogo()), lblLogo.getDocument(),0);
                lblLogo.setCaretPosition(lblLogo.getDocument().getLength());
            }catch(Exception ex){
                MWLogger.errLog(ex);
            }
            //lblLogo.setIcon(mwclient.getPlayer().getLogo());
        }//lblLogo.setIcon(mwclient.getConfig().getImage("LOGO"));}
        lblName.setText(player.getName());
        lblStatus.setText(PP_STATUS + " " + mwclient.getStatus());
        lblExp.setText(PP_EXP + " " + player.getExp());
		DecimalFormat myFormatter = new DecimalFormat("###.##");
		String ratingStr = myFormatter.format(player.getRating());
		lblRating.setText(PP_ELO + " " + ratingStr);
        if ( player.getMoney() ==  0 )
            lblMoney.setText(mwclient.moneyOrFluMessage(true,false,-2)+": " + player.getMoney());
        else
            lblMoney.setText(mwclient.moneyOrFluMessage(true,false,-player.getMoney())+": " + NumberFormat.getInstance().format(player.getMoney()));
        if ( player.getInfluence() == 0)
            lblInfluence.setText(mwclient.moneyOrFluMessage(false,false,-2) + ": " + player.getInfluence());
        else
            lblInfluence.setText(mwclient.moneyOrFluMessage(false,false,-player.getInfluence()) + ": " + NumberFormat.getInstance().format(player.getInfluence()));

        if ( mwclient.isUsingAdvanceRepairs() ){
            //when the client first loads it doesn't have data in the vectors.
            try{
                lblMekbay.setText(PP_BAYS + " " + player.getFreeBays()+"/"+player.getBays()+ " (" +mwclient.moneyOrFluMessage(true,true,player.getTechCost())+")");
                lblTechs.setText(PP_IDLETECHS  + " " + player.getAvailableTechs().get(UnitUtils.TECH_GREEN) + "/" + player.getAvailableTechs().get(UnitUtils.TECH_REG) + "/" + player.getAvailableTechs().get(UnitUtils.TECH_VET) + "/" + player.getAvailableTechs().get(UnitUtils.TECH_ELITE));
            }
            catch(Exception ex){}
        }
        else{
            lblMekbay.setText(PP_TECHS + " " + player.getFreeBays() + "/" + player.getBays());
            lblTechs.setText(PP_PAIDTECHS + " " + player.getTechs() + " (" +mwclient.moneyOrFluMessage(true,true,player.getTechCost())+")");
        }

        lblRewardPoints.setText(PP_REWARD + " " + player.getRewardPoints() + "/" + mwclient.getserverConfigs("XPRewardCap"));
        lblFreeMeks.setText(PP_FREEUNITS + " " + (Integer.parseInt(mwclient.getserverConfigs("FreeBuild_Limit")) - mwclient.getPlayer().getMekToken()) + " Remain"); //@Salient for free build
    }

    public void setNextTick(long nextTick) {
        this.nextTick = nextTick;
    }

    public void updateClock() {
        lblNextTick.setText("Next Tick: " + ((nextTick - System.currentTimeMillis()) / 1000) + " s");
    }

    private static class TThread extends Thread {

        CPlayerPanel myPanel;
        public TThread(CPlayerPanel p) {
            myPanel = p;
        }

        @Override
		public synchronized void run(){
            while (true) {
                myPanel.updateClock();
                try {
                    wait(1000);
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            }//unendting while
        }//end run
    }

}
