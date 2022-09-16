package admin.dialog.serverConfigDialogs;

import java.awt.Dimension;

import javax.swing.JTextField;
import javax.swing.text.Document;

public class PilotSkillTextField extends JTextField {

	private static final long serialVersionUID = -312611296349397491L;

	public PilotSkillTextField() {
		// TODO Auto-generated constructor stub
	}

	public PilotSkillTextField(String text) {
		super(text);
		// TODO Auto-generated constructor stub
	}

	public PilotSkillTextField(int columns) {
		super(columns);
		Dimension fieldSize = new Dimension(5, 10);
		this.setMaximumSize(fieldSize);
		this.setPreferredSize(fieldSize);
	}

	public PilotSkillTextField(String text, int columns) {
		super(text, columns);
		// TODO Auto-generated constructor stub
	}

	public PilotSkillTextField(Document doc, String text, int columns) {
		super(doc, text, columns);
		// TODO Auto-generated constructor stub
	}

	public PilotSkillTextField(int columns, Dimension fieldSize) {
		super(columns);
		this.setMaximumSize(fieldSize);
		this.setPreferredSize(fieldSize);
	}
}
