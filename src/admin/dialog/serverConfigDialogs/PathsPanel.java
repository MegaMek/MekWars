package admin.dialog.serverConfigDialogs;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import common.util.SpringLayoutHelper;

public class PathsPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5411927593664279713L;
    private JTextField baseTextField = new JTextField(5);
    private JCheckBox BaseCheckBox = new JCheckBox();
	
	public PathsPanel() {
		
        JPanel pathsBox = new JPanel();
        pathsBox.setLayout(new BoxLayout(pathsBox, BoxLayout.Y_AXIS));

        // and a sub panel to put a spring layout into.
        JPanel pathsSubPanel = new JPanel(new SpringLayout());

        baseTextField = new JTextField(5);

        pathsSubPanel.add(new JLabel("ELO Ranking Path:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Path to Ranking.htm");
        baseTextField.setName("RankingPath");
        pathsSubPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        pathsSubPanel.add(new JLabel("EXP Ranking Path:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Path to EXPRanking.htm");
        baseTextField.setName("EXPRankingPath");
        pathsSubPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        pathsSubPanel.add(new JLabel("News Path:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Path to News.rdf (News Feed)");
        baseTextField.setName("NewsPath");
        pathsSubPanel.add(baseTextField);
        
        baseTextField = new JTextField(5);
        pathsSubPanel.add(new JLabel("News URL:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("URL to News.rdf");
        baseTextField.setName("NewsURL");
        pathsSubPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        pathsSubPanel.add(new JLabel("House Rank Path:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Path to House Ranking file");
        baseTextField.setName("HouseRankPath");
        pathsSubPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        pathsSubPanel.add(new JLabel("XML Planet Path:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Path to up-to-date DynPlanets.xml for IS-Map-Generators");
        baseTextField.setName("XMLPlanetPath");
        pathsSubPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        pathsSubPanel.add(new JLabel("Mechstat Path:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Path to Mechstats.htm");
        baseTextField.setName("MechstatPath");
        pathsSubPanel.add(baseTextField);
        
        baseTextField = new JTextField(5);
        pathsSubPanel.add(new JLabel("HTML Who Path:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Path to who.html");
        baseTextField.setName("HTMLWhoPath");
        pathsSubPanel.add(baseTextField);

        
        
//        baseTextField = new JTextField();
//        pathsSubPanel.add(new JLabel("Discord Webhook Address", SwingConstants.TRAILING));
//        baseTextField.setToolTipText("Address for the webhook to which game results and such should be sent.");
//        baseTextField.setName("DiscordWebHookAddress");
//        pathsSubPanel.add(baseTextField);

        // do the spring layout.
        SpringLayoutHelper.setupSpringGrid(pathsSubPanel, 2);

        // add to the main panel.
        pathsBox.add(pathsSubPanel);

        // thats all the path naming options. put the HTML CBox here, as it
        // was in the old UI, for now. Should be moved eventually.
        BaseCheckBox = new JCheckBox("Enable HTML Output");

        BaseCheckBox.setToolTipText("Uncheck to disable html output [ranking, etc.]");
        BaseCheckBox.setName("HTMLOUTPUT");
        pathsBox.add(BaseCheckBox);
                
//        BaseCheckBox = new JCheckBox("Enable Discord Integration");
//        BaseCheckBox.setName("DiscordEnable");
//        BaseCheckBox.setToolTipText("Enable rankings and game output to be sent to a Discord channel");
//        pathsBox.add(BaseCheckBox);
        
        this.add(pathsBox);
	}
}
