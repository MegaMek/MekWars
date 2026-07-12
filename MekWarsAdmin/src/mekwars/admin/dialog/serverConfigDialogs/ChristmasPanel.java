/*
 * MekWars - Copyright (C) 2016
 *
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

package mekwars.admin.dialog.serverConfigDialogs;

import java.io.Serial;
import java.util.Properties;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import mekwars.common.VerticalLayout;
import mekwars.common.util.SpringLayoutHelper;
import org.jdatepicker.JDatePicker;

/**
 * Configuration panel containing settings for the Christmas season.
 *
 * @author Spork
 * @version 2016.10.10
 */
public class ChristmasPanel extends JPanel {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -4014592405096904081L;

    public ChristmasPanel() {
        super();
        JPanel panel = new JPanel(new VerticalLayout());

        JPanel checkboxSpring = new JPanel(new SpringLayout());
        JPanel unitPanel = new ChristmasUnitPanel();

        JCheckBox baseCheckBox = new JCheckBox("Celebrate Christmas");
        baseCheckBox.setToolTipText("Give free units to players during the holidays.");
        baseCheckBox.setName("Celebrate_Christmas");
        checkboxSpring.add(baseCheckBox);
        checkboxSpring.add(new JLabel(""));

        baseCheckBox = new JCheckBox("Allow Scrapping");
        baseCheckBox.setToolTipText("Allow Christmas Units to be scrapped");
        baseCheckBox.setName("Christmas_AllowScrap");
        checkboxSpring.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Allow BM Sales");
        baseCheckBox.setToolTipText("Allow Christmas Units to be sold on the Black Market");
        baseCheckBox.setName("Christmas_AllowBM");
        checkboxSpring.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Allow Direct Sales");
        baseCheckBox.setToolTipText("Allow Christmas Units to be sold to other players");
        baseCheckBox.setName("Christmas_AllowDirectSell");
        checkboxSpring.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Allow Transfer");
        baseCheckBox.setToolTipText("Allow Christmas Units to be transferred to other players");
        baseCheckBox.setName("Christmas_AllowTransfer");
        checkboxSpring.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Allow Donate");
        baseCheckBox.setToolTipText("Allow Christmas Units to be donated to faction bays");
        baseCheckBox.setName("Christmas_AllowDonate");
        checkboxSpring.add(baseCheckBox);

        Properties props = new Properties();
        props.put("text.today", "Today");
        props.put("text.month", "Month");
        props.put("text.year", "Year");
        JDatePicker startDatePicker = new JDatePicker();
        startDatePicker.setName("Christmas_StartDate");

        JDatePicker endDatePicker = new JDatePicker();
        endDatePicker.setName("Christmas_EndDate");

        checkboxSpring.add(new JLabel(""));
        checkboxSpring.add(new JLabel("Start Date"));
        checkboxSpring.add(new JLabel("End Date"));
        checkboxSpring.add(startDatePicker);
        checkboxSpring.add(endDatePicker);

        SpringLayoutHelper.setupSpringGrid(checkboxSpring, 2);


        panel.add(checkboxSpring);
        panel.add(unitPanel);
        this.add(panel);
    }

}
