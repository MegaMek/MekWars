/*
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekwars.common.gui.panels;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

import megamek.logging.MMLogger;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.clientutils.IClientConfig;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.MyHTMLEditorKit;
import mekwars.common.gui.listeners.MMNetHyperLinkListener;
import mekwars.common.util.UnitUtils;

/**
 * Player panel
 */

public class CPlayerPanel extends JScrollPane {
    private static final MMLogger LOGGER = MMLogger.create(CPlayerPanel.class);

    @Serial
    private static final long serialVersionUID = -7036003412110367753L;
    private static final String PP_NAME = "Name";
    private static final String PP_STATUS = "Status:";
    private static final String PP_EXP = "Experience:";
    private static final String PP_ELO = "Rating:";
    private static final String PP_TECHS = "Techs:";
    private static final String PP_PAID_TECHS = "Paid Techs:";
    private static final String PP_BAYS = "Bays:";
    private static final String PP_IDLE_TECHS = "Idle Techs:";
    private static final String PP_FREE_UNITS = "Free Units:"; //@Salient for free build
    private static String PP_REWARD;
    private final IClient client;
    private final CPlayer player;
    protected JPanel PlayerPanel = new JPanel();
    protected JEditorPane lblLogo = new JEditorPane("text/html", "");
    protected JPanel InfoPanel = new JPanel();
    protected JLabel lblName = new JLabel();
    protected JLabel lblStatus = new JLabel();
    protected JLabel lblExp = new JLabel();
    protected JLabel lblRating = new JLabel();
    protected JLabel lblMoney = new JLabel();
    protected JLabel lblInfluence = new JLabel();
    protected JLabel lblMekBay = new JLabel();
    protected JLabel lblTechs = new JLabel();
    protected JLabel lblRewardPoints = new JLabel();
    protected JLabel lblNextTick = new JLabel();
    protected JLabel lblFreeMeks = new JLabel(); //@Salient for free build
    protected long nextTick = System.currentTimeMillis();
    private IClientConfig config;

    public CPlayerPanel(IClient client) {
        PP_REWARD = STR."\{client.getServerConfigs("RPLongName")}:";

        Insets insets = new Insets(0, 0, 0, 0);
        int center = GridBagConstraints.CENTER;
        int none = GridBagConstraints.NONE;
        String endTag = "";
        String tag = "";
        Dimension dimension;
        boolean logo;
        int height;

        this.client = client;
        player = client.getPlayer();
        config = client.getConfig();
        logo = client.getConfig().isParam("LOGO");
        height = client.getConfig().getIntParam("PLAYER_PANEL_HEIGHT");

        setMinimumSize(new Dimension(0, 0));

        if (logo) {
            dimension = new Dimension(140, height + 140);
        } else {
            dimension = new Dimension(140, height);
        }

        PlayerPanel.setLayout(new GridBagLayout());
        PlayerPanel.setMinimumSize(dimension);
        PlayerPanel.setPreferredSize(dimension);

        dimension = new Dimension(140, 150);
        MMNetHyperLinkListener chatHLL = new MMNetHyperLinkListener(this.client);
        MyHTMLEditorKit kit = new MyHTMLEditorKit();

        lblLogo.setPreferredSize(dimension);
        lblLogo.setMinimumSize(dimension);
        lblLogo.setMaximumSize(dimension);
        lblLogo.setEditable(false);

        lblLogo.addHyperlinkListener(chatHLL);
        lblLogo.setEditorKit(kit);
        lblLogo.setEnabled(true);

        if (logo) {
            PlayerPanel.add(lblLogo);
        }

        dimension = new Dimension(140, height + 30);
        InfoPanel.setLayout(new BoxLayout(InfoPanel, BoxLayout.Y_AXIS));
        InfoPanel.setPreferredSize(dimension);
        InfoPanel.setMinimumSize(dimension);
        InfoPanel.setMaximumSize(dimension);
        lblName.setText(tag + PP_NAME + endTag);
        lblStatus.setText(tag + PP_STATUS + endTag);
        lblExp.setText(tag + PP_EXP + endTag);
        lblRating.setText(tag + PP_ELO + endTag);
        lblMoney.setText(STR."\{tag}\{client.getServerConfigs("MoneyLongName")}: \{endTag}");
        lblInfluence.setText(tag + client.getServerConfigs("FluLongName") + endTag);

        if (this.client.isUsingAdvanceRepairs()) {
            lblMekBay.setText(tag + PP_BAYS + endTag);
            lblTechs.setText(tag + PP_IDLE_TECHS + endTag);
        } else {
            lblMekBay.setText(tag + PP_TECHS + endTag);
            lblTechs.setText(tag + PP_PAID_TECHS + endTag);
        }

        lblRewardPoints.setText(tag + PP_REWARD + endTag);
        lblFreeMeks.setText(tag + PP_FREE_UNITS + endTag); //@Salient for free build
        lblNextTick.setText("Next Tick: N/A");
        InfoPanel.add(lblName);

        InfoPanel.add(lblStatus);
        InfoPanel.add(lblExp);

        if (!Boolean.parseBoolean(client.getServerConfigs("HideELO"))) {
            InfoPanel.add(lblRating);
        }

        InfoPanel.add(lblMoney);
        InfoPanel.add(lblInfluence);
        InfoPanel.add(lblMekBay);
        InfoPanel.add(lblTechs);

        if (Boolean.parseBoolean(client.getServerConfigs("ShowReward"))) {
            InfoPanel.add(lblRewardPoints);
        }

        if (Integer.parseInt(client.getServerConfigs("FreeBuild_Limit")) > 0) {
            InfoPanel.add(lblFreeMeks);
        }

        InfoPanel.add(lblNextTick);

        if (logo) {
            PlayerPanel.add(InfoPanel,
                  new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, center, none, insets, 0, 0));
        } else {
            PlayerPanel.add(InfoPanel,
                  new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, center, none, insets, 0, 0));
        }

        if (logo) {
            setPreferredSize(new Dimension(130, height + 140));
            setMinimumSize(new Dimension(140, height + 140));
            setMaximumSize(new Dimension(140, height + 140));
        } else {
            setPreferredSize(new Dimension(130, height + 40));
            setMinimumSize(new Dimension(140, height + 40));
            setMaximumSize(new Dimension(140, height + 40));
        }

        setBorder(new LineBorder(java.awt.Color.black));
        setViewportView(PlayerPanel);

        Thread clockT = new CPlayerPanel.TThread(this);
        clockT.start();
    }

    public void refresh() {
        if (client.getConfig().isParam("LOGO")) {
            try {
                lblLogo.setBackground(client.getMainFrame().getBackground());
                lblLogo.getDocument().remove(0, lblLogo.getDocument().getLength());
                lblLogo.getEditorKit().read(new StringReader(client.getPlayer().getLogo()), lblLogo.getDocument(), 0);
                lblLogo.setCaretPosition(lblLogo.getDocument().getLength());
            } catch (Exception ex) {
                LOGGER.error(ex, "Unable to refresh: {}", ex.getLocalizedMessage());
            }
        }

        lblName.setText(player.getName());
        lblStatus.setText(STR."\{PP_STATUS} \{client.getStatus()}");
        lblExp.setText(STR."\{PP_EXP} \{player.getExp()}");
        DecimalFormat myFormatter = new DecimalFormat("###.##");
        String ratingStr = myFormatter.format(player.getRating());
        lblRating.setText(STR."\{PP_ELO} \{ratingStr}");

        if (player.getMoney() == 0) {
            lblMoney.setText(STR."\{client.moneyOrFluMessage(true, false, -2)}: \{player.getMoney()}");
        } else {
            lblMoney.setText(STR."\{client.moneyOrFluMessage(true,
                  false,
                  -player.getMoney())}: \{NumberFormat.getInstance().format(player.getMoney())}");
        }

        if (player.getInfluence() == 0) {
            lblInfluence.setText(STR."\{client.moneyOrFluMessage(false, false, -2)}: \{player.getInfluence()}");
        } else {
            lblInfluence.setText(STR."\{client.moneyOrFluMessage(false,
                  false,
                  -player.getInfluence())}: \{NumberFormat.getInstance().format(player.getInfluence())}");
        }

        if (client.isUsingAdvanceRepairs()) {
            //when the client first loads, it doesn't have data in the vectors.
            try {
                lblMekBay.setText(STR."\{PP_BAYS} \{player.getFreeBays()}/\{player.getBays()} (\{client.moneyOrFluMessage(
                      true,
                      true,
                      player.getTechCost())})");
                lblTechs.setText(STR."\{PP_IDLE_TECHS} \{player.getAvailableTechs()
                                                               .get(UnitUtils.TECH_GREEN)}/\{player.getAvailableTechs()
                                                                                                   .get(UnitUtils.TECH_REG)}/\{player.getAvailableTechs()
                                                                                                                                     .get(UnitUtils.TECH_VET)}/\{player.getAvailableTechs()
                                                                                                                                                                       .get(UnitUtils.TECH_ELITE)}");
            } catch (Exception ex) {
                LOGGER.debug(ex, "Not sure why we're catching the error: {}", ex.getLocalizedMessage());
            }
        } else {
            lblMekBay.setText(STR."\{PP_TECHS} \{player.getFreeBays()}/\{player.getBays()}");
            lblTechs.setText(STR."\{PP_PAID_TECHS} \{player.getTechs()} (\{client.moneyOrFluMessage(true,
                  true,
                  player.getTechCost())})");
        }

        lblRewardPoints.setText(STR."\{PP_REWARD} \{player.getRewardPoints()}/\{client.getServerConfigs("XPRewardCap")}");
        lblFreeMeks.setText(STR."\{PP_FREE_UNITS} \{Integer.parseInt(client.getServerConfigs("FreeBuild_Limit")) -
                                                          client.getPlayer()
                                                                .getMekToken()} Remain"); //@Salient for free build
    }

    public void setNextTick(long nextTick) {
        this.nextTick = nextTick;
    }

    public void updateClock() {
        lblNextTick.setText(STR."Next Tick: \{(nextTick - System.currentTimeMillis()) / 1000} s");
    }

    private static class TThread extends Thread {
        CPlayerPanel myPanel;

        public TThread(CPlayerPanel panel) {
            myPanel = panel;
        }

        @Override
        public synchronized void run() {
            while (true) {
                myPanel.updateClock();
                try {
                    wait(1000);
                } catch (Exception ex) {
                    LOGGER.error(ex, "Thread Escaped! {}", ex.getLocalizedMessage());
                }
            }
        }
    }

}
