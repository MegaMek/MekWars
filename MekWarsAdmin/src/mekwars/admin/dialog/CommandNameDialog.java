/*
 * MekWars - Copyright (C) 2005
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
 *
 * Portions of this dialog derived from work done by Imanuel Schultz. Original
 * part of MegaMekNET's client.gui.actions pacakge as SearchHouseActionListener.java.
 * See http://www.sourceforge.net/projects/megameknet for more info.
 */

package mekwars.admin.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import javax.swing.*;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;

/*
 * Base dialog, derived from MMNET's SearchHouseListener, allows players
 * to search for commands using partial strings. Eventually, I'd like to
 * expand this to allow searching in other modes (selectable via combo box),
 * like "Active Operations" and "Contested Worlds," w/ appropriate fields
 * for selection input.
 *
 * @urgru 5.2.05
 * used code that urgru started to make cookie cut dialog boxes for command
 * and planets for commands requiring that input.
 *
 * @Torren 5.6.05
 *
 * Created to list all of the Commands for the SO's
 *
 * @Torren 11.8.05
 */

public class CommandNameDialog extends JDialog implements ActionListener {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -1024120117465498506L;
    //variables
    private final Collection<String> commands;

    private final JList<String> matchingCommandList;
    private final JTextField nameField;//input field
    private final String okayCommand = "Okay";

    private String commandName = null;

    //constructor
    public CommandNameDialog(IClient client, String boxText) {

        /*
         * NOTE: variables are final to
         * allow access by caretUpdate()
         */

        //super, and variable saves
        super(client.getMainFrame(), boxText, true);//dummy frame as owner
        loadCommands(client);
        int accessLevel = client.getUser(client.getPlayer().getName()).getUserLevel();
        commands = client.getData().getCommandTable().keySet();
        //setup the a list of names to feed into a list
        TreeSet<String> commandNames = new TreeSet<>();//tree to alpha sort
        for (String command : commands) {

            if (command.equalsIgnoreCase("SendClientDataCommand")) {continue;}
            if (accessLevel >= client.getData().getCommandTable().get(command)) {
                commandNames.add(command.charAt(0) + command.substring(1).toLowerCase());
            }

        }
        final String[] allCommandNames = commandNames.toArray(new String[commandNames.size()]);

        //construct the command name list
        matchingCommandList = new JList<>(allCommandNames);
        matchingCommandList.setVisibleRowCount(10);
        matchingCommandList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        //the name field, for user input. caretUpdate
        //does most of the work to update list contents
        nameField = new JTextField();//field for user input
        nameField.addCaretListener(e -> new Thread() {
            @Override
            public void run() {
                String text = nameField.getText();
                if (text == null || text.isEmpty()) {
                    matchingCommandList.setListData(allCommandNames);
                    return;
                }
                ArrayList<String> possibleCommands = new ArrayList<>();
                text = text.toLowerCase();
                for (String curCommand : commands) {
                    if (curCommand.toLowerCase().contains(text)) {
                        possibleCommands.add(curCommand.charAt(0) +
                                                   curCommand.substring(1).toLowerCase());
                    }
                }
                matchingCommandList.setListData(possibleCommands.toArray(new String[possibleCommands.size()]));

                /*
                 * Try to select a command with a STARTING string which matched
                 * the seach index. If none is available, use the first command.
                 *
                 * Hacky, but functional. @urgru 5.2.05
                 */
                boolean shouldContinue = true;
                int element = 0;
                for (String name : possibleCommands) {
                    if (name.toLowerCase().startsWith(text)) {
                        matchingCommandList.setSelectedIndex(element);
                        shouldContinue = false;
                        break;
                    }
                    element++;
                }
                // MWLogger.errLog("7");

                //looped through without finding a starting match. set 0.
                if (shouldContinue) {
                    matchingCommandList.setSelectedIndex(0);
                }

            }
        }.start());

        //put the list in a scroll pane
        //holds the JList
        JScrollPane scrollPane = new JScrollPane(matchingCommandList);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        //set up listeners for the buttons
        JButton okayButton = new JButton("OK");
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        //do some formatting. rawr.
        JPanel springPanel = new JPanel(new SpringLayout());
        springPanel.add(nameField);
        springPanel.add(scrollPane);
        SpringLayoutHelper.setupSpringGrid(springPanel, 2, 1);

        JPanel buttonFlow = new JPanel();
        buttonFlow.add(okayButton);
        buttonFlow.add(cancelButton);

        JPanel generalLayout = new JPanel();
        generalLayout.setLayout(new BoxLayout(generalLayout, BoxLayout.Y_AXIS));
        generalLayout.add(springPanel);
        generalLayout.add(buttonFlow);
        this.getContentPane().add(generalLayout);
        this.pack();

        this.checkMinimumSize();
        this.setResizable(true);

        //set a default button
        this.getRootPane().setDefaultButton(okayButton);

        //center the dialog.
        this.setLocationRelativeTo(null);

    }


    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the dialog.
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals(okayCommand)) {
            String selectedCommand = matchingCommandList.getSelectedValue();
            if (selectedCommand == null) {selectedCommand = nameField.getText();}
            if (selectedCommand == null || selectedCommand.isEmpty()) {return;}
            if (matchingCommandList.getModel().getSize() == 1) {
                selectedCommand = matchingCommandList.getModel().getElementAt(0);
            }
            for (String commandName : commands) {
                if (selectedCommand.equalsIgnoreCase(commandName)) {
                    this.setCommandName(commandName);
                    this.setVisible(false);
                    //this.dispose();
                    return;
                }
            }
            JOptionPane.showMessageDialog(null, "Unknown Command");
        }

        //dispose of the dialog
        this.dispose();

    }//end actionPerformed

    private void checkMinimumSize() {

        Dimension curDim = this.getSize();

        int height;
        int width;
        boolean shouldRedraw = false;

        if (curDim.getWidth() < 300) {
            width = 300;
            shouldRedraw = true;
        } else {width = (int) curDim.getWidth();}

        if (curDim.getHeight() < 150) {
            height = 150;
            shouldRedraw = true;
        } else {height = (int) curDim.getHeight();}

        if (shouldRedraw) {
            this.setSize(new Dimension(width, height));
        }

    }//end checkMinimumSize

    private void setCommandName(String name) {
        this.commandName = name.charAt(0) + name.toLowerCase().substring(1);
    }

    public String getCommandName() {
        return this.commandName;
    }

    private void loadCommands(IClient client) {
        client.loadServerCommands();
    }
}
