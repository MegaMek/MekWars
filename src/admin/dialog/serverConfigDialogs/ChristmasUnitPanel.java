package admin.dialog.serverConfigDialogs;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import common.VerticalLayout;
import common.util.SpringLayoutHelper;


/**
 * A JPanel implementation detailing the Christmas Units handed out by the server for Christmas Season.
 * 
 * @author Spork
 * @version 2016.10.26
 */
public class ChristmasUnitPanel extends JPanel {
	
	private static final long serialVersionUID = 3237635461980154996L;
	JTextField hiddenField;
	
	public ChristmasUnitPanel() {
		super(new SpringLayout());
		
		JPanel leftPanel = new JPanel(new VerticalLayout());
		JPanel rightPanel = new JPanel(new VerticalLayout());
		
		JTextArea unitList = new JTextArea(6, 40);
		unitList.setName("Christmas_List");
		unitList.setToolTipText("List of units, one per line, to be handed out as presents.");
		JScrollPane scrollPane = new JScrollPane(unitList);
		
		leftPanel.add(new JLabel("Christmas List"));
		leftPanel.add(scrollPane);
		
		JPanel buttonPanel = new JPanel(new VerticalLayout(5, VerticalLayout.LEFT));
		ButtonGroup buttonGroup = new ButtonGroup();
		
		buttonPanel.add(new JLabel("How many gifts?"));
		
		JRadioButton button = new JRadioButton("Give One of Each");
		button.setToolTipText("<html>One of each unit on the Christmas list "
							+ "will be given the first time a player logs on "
							+ "after the start of the Christmas season.</html>");
		button.setActionCommand("OneEach");
		button.setName("Christmas_Units_Method_OneOfEach");
		buttonGroup.add(button);
		buttonPanel.add(button);
		
		button = new JRadioButton("Give X of Each");
		button.setToolTipText("<html>X of each unit on the Christmas list "
							+ "will be given the first time a player logs on "
							+ "after the start of the Christmas season.</html>");
		button.setActionCommand("XEach");
		button.setName("Christmas_Units_Method_XOfEach");
		buttonGroup.add(button);
		buttonPanel.add(button);
		
		button = new JRadioButton("Give X total");
		button.setToolTipText("<html>X units will be selected randomly from the "
							+ "Christmas list. They will be given the first time "
							+ "a player logs on after the start of the Christmas season.</html>");
		button.setActionCommand("XTotal");
		button.setName("Christmas_Units_Method_XTotal");
		buttonGroup.add(button);
		buttonPanel.add(button);
		
		JPanel panel = new JPanel();
		JTextField textfield = new JTextField(5);
		textfield.setName("Christmas_Units_X");
		panel.add(new JLabel("X:", SwingConstants.TRAILING));
		panel.add(textfield);
		
		hiddenField = new JTextField();
		hiddenField.setName("Christmas_Units_Method");
		hiddenField.setVisible(false);
		panel.add(hiddenField);
		
		buttonPanel.add(panel);
		
		rightPanel.add(buttonPanel);
		
		this.add(leftPanel);
		this.add(rightPanel);
		
		SpringLayoutHelper.setupSpringGrid(this, 2);
	}
}
