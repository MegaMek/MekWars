package admin.dialog.serverConfigDialogs;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import common.VerticalLayout;

public class DiscordAndDjangoPanel extends JPanel 
{

	private static final long serialVersionUID = -4629994177197999929L;

	private JTextField baseTextField = new JTextField(5);
	private JCheckBox baseCheckBox = new JCheckBox();
	private JLabel baseLabel = new JLabel();

	private void init() 
	{

		setLayout(new VerticalLayout(5, VerticalLayout.CENTER, VerticalLayout.TOP));

		String desc = "<HTML>Discord comes with functionality allowing for creation of webhooks and bots. This section<br>"
				+ "contains options regarding bots and django projects not included with the mekwars repo.<br>"
				+ "django battle board repo: "
				+ "<a href=\"https://github.com/salient-opensource/battle-board/\"> "
				+ "https://github.com/salient-opensource/battle-board/</a></HTML>";
		
		String desc2 = "<HTML>!NOTE: Django battle board also requires Enable Player Data Capture (JSON) to be <br>"
				+ "enabled. See previous section above.</HTML>";
		
		JPanel panel0 = new JPanel();
		JPanel panel1 = new JPanel();
		JPanel panel2 = new JPanel();
		JPanel panel2a = new JPanel();
		JPanel panel2b = new JPanel();
		JPanel panel3 = new JPanel();


		panel0.add(new JLabel(desc));

		panel1.setBorder(BorderFactory.createTitledBorder("Discord Webhooks"));
		
        baseCheckBox = new JCheckBox("Enable Discord Webhooks");
        baseCheckBox.setName("DiscordEnable");
        baseCheckBox.setToolTipText("Enable rankings and game output to be sent to a Discord channel");
        panel1.add(baseCheckBox);
        
        baseTextField = new JTextField(25);
        panel1.add(new JLabel("Discord Webhook Address", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Address for the webhook to which game results and such should be sent.");
        baseTextField.setName("DiscordWebHookAddress");
        panel1.add(baseTextField);
        
		panel2.setLayout(new VerticalLayout(5, VerticalLayout.CENTER, VerticalLayout.TOP));
		panel2.setBorder(BorderFactory.createTitledBorder("Discord Bot Options"));
		
		baseCheckBox = new JCheckBox("Enable Player Data Capture (JSON)");
		baseCheckBox.setToolTipText("<HTML>This allows the saving of player data to a JSON file for the discord bot to read from</HTML>");
		baseCheckBox.setName("Enable_BotPlayerInfo");
		panel2a.add(baseCheckBox);

		
		baseCheckBox = new JCheckBox("Enable Bot Chat Capture (TXT)");
		baseCheckBox.setToolTipText("<HTML>Logs all chat to a file which the discord bot can read from</HTML>");
		baseCheckBox.setName("Enable_Bot_Chat");
		panel2a.add(baseCheckBox);


		baseLabel = new JLabel("Chat Buffer Location:");
		baseLabel.setHorizontalAlignment(JLabel.RIGHT);
		baseTextField = new JTextField(30);
		baseTextField.setToolTipText("<HTML>Location of file to use with bot, the code 'should' generate this file if it does not exit </HTML>");
		baseTextField.setName("Bot_Buffer_Location");
		panel2b.add(baseLabel);
		panel2b.add(baseTextField);
		
		panel2.add(panel2a);
		panel2.add(panel2b);
        
        panel3.setBorder(BorderFactory.createTitledBorder("DJANGO Battle Board Options"));
        panel3.setLayout(new VerticalLayout(5, VerticalLayout.CENTER, VerticalLayout.TOP));
        
        panel3.add(new JLabel(desc2));
        
		baseCheckBox = new JCheckBox("Battle Save (JSON)");
		baseCheckBox.setToolTipText("<HTML>Saves battles to json for django battle board at data/django/battles/ </HTML>");
		baseCheckBox.setName("Django_CaptureBattleAsJson");
		panel3.add(baseCheckBox);
		
		add(panel0);
		add(panel1);
		add(panel2);
		add(panel3);
		//add(panel4);
		
	}
	
	public DiscordAndDjangoPanel() 
	{
		super();
		init();
	}

}
