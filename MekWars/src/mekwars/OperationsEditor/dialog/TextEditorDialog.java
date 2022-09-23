
/*
 * MekWars - Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

/*
 * MechSelectorJDialog.java - Copyright (C) 2002,2004 Josh Yockey
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package OperationsEditor.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import common.util.SpringLayoutHelper;


/*
 * Allows a user to sort through a list of MechSummaries and select one
 */

public class TextEditorDialog extends JDialog implements ActionListener, KeyListener {

	/**
     *
     */
    private static final long serialVersionUID = -3851019509649287454L;


	private JButton bCancel = new JButton("Close");
	private JButton bSave = new JButton("Save");

	private JTextArea textField = new JTextArea();
    private JTextField textBox = null;
    private OperationsDialog opDialog = null;

    public TextEditorDialog(OperationsDialog opDialog, JTextField textBox) {
		//save params
        this.textBox = textBox;
        this.opDialog = opDialog;
        textField.setText(textBox.getText());


        Dimension maxSize = new Dimension(200, 200);
        textField.setMaximumSize(maxSize);
        textField.setMinimumSize(maxSize);
        textField.setPreferredSize(maxSize);
        textField.setWrapStyleWord(true);
        textField.setLineWrap(true);

		//set up a formatting holder for the cancel button
		JPanel buttonHolder = new JPanel();
		buttonHolder.add(bSave);
		buttonHolder.add(bCancel);

		bSave.addActionListener(this);
        bCancel.addActionListener(this);

        bCancel.setMnemonic(KeyEvent.VK_ESCAPE);
        bCancel.addKeyListener(this);
        addKeyListener(this);
        textField.addKeyListener(this);

		//set up the overall SpringLayout
		JPanel springHolder = new JPanel(new SpringLayout());
		springHolder.add(textField);
		springHolder.add(buttonHolder);
		SpringLayoutHelper.setupSpringGrid(springHolder,1);
		getContentPane().add(springHolder);
		getRootPane().setDefaultButton(bSave);

		setSize(785, 560);
		setResizable(false);

        setModal(false);
        pack();
        setVisible(true);
	}

	@Override
	public void setVisible(boolean show) {
		setLocationRelativeTo(null);
		super.setVisible(show);
		pack();
	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() == bCancel) {
            dispose();
		} else if (ae.getSource() == bSave) {
            textBox.setText(textField.getText());
            opDialog.keyPressed(new KeyEvent(textBox, 0, KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_0, '0'));
            dispose();
		}
	}

    public void keyPressed(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
            actionPerformed(new ActionEvent(bCancel, ActionEvent.ACTION_PERFORMED, ""));
        } else if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            actionPerformed(new ActionEvent(bSave, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    public void keyReleased(KeyEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void keyTyped(KeyEvent ke) {

    }
}
