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
 * The compact sidebar/summary panel that shows the logged-in player's key stats at a glance: name, status,
 * experience, rating (Elo), money, influence, tech bay/tech counts (in either "simple" or "advance repairs" style),
 * reward points, free-build units remaining, and a live countdown to the next campaign "tick". Optionally shows
 * the player's custom logo image/HTML above the stats. Values are populated on construction and refreshed on
 * demand via {@link #refresh()}; the tick countdown updates itself once per second via a background thread.
 */

public class CPlayerPanel extends JScrollPane {
    private static final MMLogger LOGGER = MMLogger.create(CPlayerPanel.class);

    @Serial
    private static final long serialVersionUID = -7036003412110367753L;
    private static final String PP_NAME = "Name";
    private static final String PP_STATUS = "Status:";
    private static final String PP_EXP = "Experience:";
    private static final String PP_ELO = "Rating:";
    /** Label used when advance-repairs mode is off: total paid-for tech slots. */
    private static final String PP_TECHS = "Techs:";
    /** Label used when advance-repairs mode is off: techs the player has paid to hire. */
    private static final String PP_PAID_TECHS = "Paid Techs:";
    /** Label used when advance-repairs mode is on: repair bay counts. */
    private static final String PP_BAYS = "Bays:";
    /** Label used when advance-repairs mode is on: techs available but not currently assigned to a repair. */
    private static final String PP_IDLE_TECHS = "Idle Techs:";
    private static final String PP_FREE_UNITS = "Free Units:"; //@Salient for free build
    /** Built in the constructor from the server-configured reward-point currency name (e.g. "Reward Points:"). */
    private static String PP_REWARD;
    /** Connection/session handle used to read server configuration and player-facing formatting helpers. */
    private final IClient client;
    /** The player whose stats this panel displays; captured once at construction time. */
    private final CPlayer player;
    /** The scrollable content panel (logo + info) set as this {@link JScrollPane}'s viewport view. */
    protected JPanel PlayerPanel = new JPanel();
    /** Displays the player's custom logo, if the "LOGO" config option is enabled. */
    protected JEditorPane lblLogo = new JEditorPane("text/html", "");
    /** Vertical stack of stat labels (name, status, exp, etc.). */
    protected JPanel InfoPanel = new JPanel();
    protected JLabel lblName = new JLabel();
    protected JLabel lblStatus = new JLabel();
    protected JLabel lblExp = new JLabel();
    protected JLabel lblRating = new JLabel();
    protected JLabel lblMoney = new JLabel();
    protected JLabel lblInfluence = new JLabel();
    /** Shows either "Techs:" (simple) or "Bays:" (advance repairs) counts, depending on server mode. */
    protected JLabel lblMekBay = new JLabel();
    /** Shows either "Paid Techs:" (simple) or "Idle Techs:" (advance repairs) counts, depending on server mode. */
    protected JLabel lblTechs = new JLabel();
    protected JLabel lblRewardPoints = new JLabel();
    /** Live countdown to the next campaign tick, updated once per second by {@link TThread}. */
    protected JLabel lblNextTick = new JLabel();
    protected JLabel lblFreeMeks = new JLabel(); //@Salient for free build
    /** Epoch-millis timestamp of the next campaign tick; updated externally via {@link #setNextTick(long)}. */
    protected long nextTick = System.currentTimeMillis();
    /** Client-side configuration (layout sizes, feature toggles) read during layout. */
    private IClientConfig config;

    /**
     * Lays out the panel: builds the (optional) logo area and the vertical stack of stat labels, sizes everything
     * according to the {@code PLAYER_PANEL_HEIGHT} and {@code LOGO} client-config values, and starts a background
     * {@link TThread} that ticks {@link #lblNextTick} once per second for the rest of the panel's (and JVM's)
     * lifetime — note this thread is never stopped, including when the panel itself is discarded.
     *
     * @param client the client providing player data and configuration for this panel
     */
    public CPlayerPanel(IClient client) {
        PP_REWARD = String.format("%s:", client.getServerConfigs("RPLongName"));

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
        lblMoney.setText(String.format("%s%s: %s", tag, client.getServerConfigs("MoneyLongName"), endTag));
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

    /**
     * Re-reads current player/client state and updates every stat label (name, status, exp, rating, money,
     * influence, tech/bay counts, reward points, free-build units) and, if enabled, reloads the player's logo
     * HTML. Money and influence labels special-case a value of exactly 0 (passing {@code -2} instead of the
     * negated value) to select an appropriate display message via {@link IClient#moneyOrFluMessage}. Tech/bay
     * counts are formatted differently depending on whether the server uses "advance repairs" mode; failures while
     * reading advance-repair tech-level counts are logged and swallowed (e.g. during initial client load before
     * data has arrived).
     */
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
        lblStatus.setText(String.format("%s %s", PP_STATUS, client.getStatus()));
        lblExp.setText(String.format("%s %s", PP_EXP, player.getExp()));
        DecimalFormat myFormatter = new DecimalFormat("###.##");
        String ratingStr = myFormatter.format(player.getRating());
        lblRating.setText(String.format("%s %s", PP_ELO, ratingStr));

        if (player.getMoney() == 0) {
            lblMoney.setText(String.format("%s: %s", client.moneyOrFluMessage(true, false, -2), player.getMoney()));
        } else {
            lblMoney.setText(String.format("%s: %s", client.moneyOrFluMessage(true,
                  false,
                  -player.getMoney()), NumberFormat.getInstance().format(player.getMoney())));
        }

        if (player.getInfluence() == 0) {
            lblInfluence.setText(String.format("%s: %s", client.moneyOrFluMessage(false, false, -2), player.getInfluence()));
        } else {
            lblInfluence.setText(String.format("%s: %s", client.moneyOrFluMessage(false,
                  false,
                  -player.getInfluence()), NumberFormat.getInstance().format(player.getInfluence())));
        }

        if (client.isUsingAdvanceRepairs()) {
            //when the client first loads, it doesn't have data in the vectors.
            try {
                lblMekBay.setText(String.format("%s %s/%s (%s)", PP_BAYS, player.getFreeBays(), player.getBays(), client.moneyOrFluMessage(
                      true,
                      true,
                      player.getTechCost())));
                lblTechs.setText(String.format("%s %s/%s/%s/%s", PP_IDLE_TECHS, player.getAvailableTechs()
                                                               .get(UnitUtils.TECH_GREEN), player.getAvailableTechs()
                                                                                                   .get(UnitUtils.TECH_REG), player.getAvailableTechs()
                                                                                                                                     .get(UnitUtils.TECH_VET), player.getAvailableTechs()
                                                                                                                                                                       .get(UnitUtils.TECH_ELITE)));
            } catch (Exception ex) {
                LOGGER.debug(ex, "Not sure why we're catching the error: {}", ex.getLocalizedMessage());
            }
        } else {
            lblMekBay.setText(String.format("%s %s/%s", PP_TECHS, player.getFreeBays(), player.getBays()));
            lblTechs.setText(String.format("%s %s (%s)", PP_PAID_TECHS, player.getTechs(), client.moneyOrFluMessage(true,
                  true,
                  player.getTechCost())));
        }

        lblRewardPoints.setText(String.format("%s %s/%s", PP_REWARD, player.getRewardPoints(), client.getServerConfigs("XPRewardCap")));
        lblFreeMeks.setText(String.format("%s %s Remain", PP_FREE_UNITS, Integer.parseInt(client.getServerConfigs("FreeBuild_Limit")) -
                                                          client.getPlayer()
                                                                .getMekToken())); //@Salient for free build
    }

    /**
     * Sets the epoch-millis timestamp of the next campaign tick, used by {@link #updateClock()} to compute the
     * displayed countdown. Called externally whenever the server informs the client of the next tick time.
     *
     * @param nextTick epoch-millis timestamp of the next tick
     */
    public void setNextTick(long nextTick) {
        this.nextTick = nextTick;
    }

    /** Recomputes and displays the seconds remaining until {@link #nextTick}; may show a negative value if overdue. */
    public void updateClock() {
        lblNextTick.setText(String.format("Next Tick: %s s", (nextTick - System.currentTimeMillis()) / 1000));
    }

    /**
     * Background thread that calls {@link CPlayerPanel#updateClock()} once per second for the life of the JVM.
     * NOTE: the loop has no exit condition and the thread is never stopped/interrupted, so one such thread leaks
     * for every {@link CPlayerPanel} constructed; {@code wait(1000)} is called without a corresponding
     * notify/lock contention scenario, so it functions purely as a sleep here (using the thread's own monitor).
     */
    private static class TThread extends Thread {
        CPlayerPanel myPanel;

        /**
         * @param panel the panel whose clock label this thread will keep updated
         */
        public TThread(CPlayerPanel panel) {
            myPanel = panel;
        }

        /** Updates the clock, then sleeps ~1 second (via {@code wait}), forever; logs and continues on interruption. */
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
