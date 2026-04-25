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

package admin.dialog.serverConfigDialogs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import common.VerticalLayout;
import common.util.SpringLayoutHelper;

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
	private static final long serialVersionUID = -4014592405096904081L;

	private JCheckBox BaseCheckBox = new JCheckBox();
	
	public ChristmasPanel() {
		super();
		JPanel panel = new JPanel(new VerticalLayout());

        JPanel checkboxSpring = new JPanel(new SpringLayout());
        JPanel unitPanel = new ChristmasUnitPanel();
        
        BaseCheckBox = new JCheckBox("Celebrate Christmas");
        BaseCheckBox.setToolTipText("Give free units to players during the holidays.");
        BaseCheckBox.setName("Celebrate_Christmas");
        checkboxSpring.add(BaseCheckBox);
        checkboxSpring.add(new JLabel(""));
        
        BaseCheckBox = new JCheckBox("Allow Scrapping");
        BaseCheckBox.setToolTipText("Allow Christmas Units to be scrapped");
        BaseCheckBox.setName("Christmas_AllowScrap");
        checkboxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow BM Sales");
        BaseCheckBox.setToolTipText("Allow Christmas Units to be sold on the Black Market");
        BaseCheckBox.setName("Christmas_AllowBM");
        checkboxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Direct Sales");
        BaseCheckBox.setToolTipText("Allow Christmas Units to be sold to other players");
        BaseCheckBox.setName("Christmas_AllowDirectSell");
        checkboxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Transfer");
        BaseCheckBox.setToolTipText("Allow Christmas Units to be transferred to other players");
        BaseCheckBox.setName("Christmas_AllowTransfer");
        checkboxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Donate");
        BaseCheckBox.setToolTipText("Allow Christmas Units to be donated to faction bays");
        BaseCheckBox.setName("Christmas_AllowDonate");
        checkboxSpring.add(BaseCheckBox);

        UtilDateModel model = new UtilDateModel();
        Properties props = new Properties();
        props.put("text.today", "Today");
        props.put("text.month", "Month");
        props.put("text.year", "Year");
        JDatePanelImpl startDatePanel = new JDatePanelImpl(model, props);
        JDatePickerImpl startDatePicker = new JDatePickerImpl(startDatePanel, new DateLabelFormatter());
        startDatePicker.setName("Christmas_StartDate");

        model = new UtilDateModel();
        JDatePanelImpl endDatePanel = new JDatePanelImpl(model, props);
        JDatePickerImpl endDatePicker = new JDatePickerImpl(endDatePanel, new DateLabelFormatter());
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
	
	/**
	 * Format the panel JLabels 
	 * @author Spork
	 */
	private class DateLabelFormatter extends AbstractFormatter {

		private static final long serialVersionUID = -8200575816557834887L;

		private String datePattern = "yyyy-MM-dd";
		private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);
		
		@Override
		public Object stringToValue(String text) throws ParseException {
			return dateFormatter.parseObject(text);
		}

		@Override
		public String valueToString(Object value) throws ParseException {
			if (value != null) {
				Calendar cal = (Calendar) value;
				return dateFormatter.format(cal.getTime());
			}
			return "";
		}
		
	}
}