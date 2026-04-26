package mekwars.admin.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.targetsystems.TargetSystem;
import mekwars.common.campaign.targetsystems.TargetTypeNotImplementedException;
import mekwars.common.campaign.targetsystems.TargetTypeOutOfBoundsException;
import mekwars.common.util.SpringLayoutHelper;

public class BannedTargetingDialog implements ActionListener {

    private final JDialog dialog;
    private final IClient client;
    private final static String okayCommand = "Add";
    private final static String cancelCommand = "Close";

    private final String windowName = "Server Banned Target System Editor";

    private final HashMap<Integer, JCheckBox> newBans = new HashMap<>();

    public BannedTargetingDialog(IClient client) {
        this.client = client;

        JButton okayButton = new JButton("Save");
        okayButton.setActionCommand(okayCommand);
        JButton cancelButton = new JButton("Close");
        cancelButton.setActionCommand(cancelCommand);

        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        okayButton.setToolTipText("Save");
        cancelButton.setToolTipText("Exit without saving changes");

        JPanel banPanel = new JPanel();//player name, etc

        banPanel.setLayout(new BoxLayout(banPanel, BoxLayout.Y_AXIS));

        JPanel tsPanel = new JPanel(new SpringLayout());

        JCheckBox baseCheckBox;

        // Add the target systems
        int max = TargetSystem.TS_TYPE_MAX;
        for (int i = 0; i <= max; i++) {
            try {
                TargetSystem targetSystem = new TargetSystem();
                baseCheckBox = new JCheckBox(targetSystem.getTypeName(i));
                baseCheckBox.setSelected(getTargetSystemBanStatus(i));
                tsPanel.add(baseCheckBox);
                newBans.put(i, baseCheckBox);

            } catch (TargetTypeOutOfBoundsException | TargetTypeNotImplementedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        SpringLayoutHelper.setupSpringGrid(tsPanel, 2);
        banPanel.add(tsPanel);

        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        JOptionPane pane = new JOptionPane(banPanel,
              JOptionPane.PLAIN_MESSAGE,
              JOptionPane.DEFAULT_OPTION,
              null,
              options,
              null);

        //if ( house != null  )
        //   windowName = this.house.getName() +" Banned Ammo Dialog";
        // Create the main dialog and set the default button
        dialog = pane.createDialog(tsPanel, windowName);
        dialog.getRootPane().setDefaultButton(cancelButton);


        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals(okayCommand)) {
            // Send it
            int max = TargetSystem.TS_TYPE_MAX;
            StringBuilder bans = new StringBuilder("/adminsetservertargetban ");
            for (int i = 0; i <= max; i++) {
                if (newBans.get(i).isSelected()) {
                    bans.append(i).append("#");
                }
            }
            client.sendChat(bans.toString());
            dialog.dispose();
        } else {
            // Kill it
            dialog.dispose();
        }

    }

    private boolean getTargetSystemBanStatus(int type) {
        return client.getTargetSystemBanStatus(type);
    }
}
