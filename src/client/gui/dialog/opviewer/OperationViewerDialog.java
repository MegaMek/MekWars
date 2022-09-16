/*
 * MekWars - Copyright (C) 2014 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
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
 */

package client.gui.dialog.opviewer;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import client.MWClient;
import common.VerticalLayout;
import common.campaign.operations.DefaultOperation;
import common.campaign.operations.Operation;
import common.util.MMNetXStream;
import common.util.MWLogger;

public class OperationViewerDialog extends JDialog implements Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public class TemplateElement {
		public final static int CONTROL_CAMPAIGN = 0;
		public final static int CONTROL_OP = 1;
		public final static int CONTROL_NAME = 2;
		
		private String data;
		private boolean isControl = false;
		private int controlType;
		
		public TemplateElement(String s) {
			if (s.equalsIgnoreCase("opname")) {
				data = "";
				isControl = true;
				controlType = CONTROL_NAME;
			} else if (s.startsWith("CC%")) {
				// Campaign Config
				data = s.substring(3);
				isControl = true;
				controlType = CONTROL_CAMPAIGN;
			} else if (s.startsWith("OP%")) {
				data = s.substring(3);
				isControl = true;
				controlType = CONTROL_OP;
			} else {
				data = s;
			}
		}
		
		private String getData() {
			return data;
		}
		
		public String getHTMLData(Operation op) {
			if (isControl) {
				if (controlType == CONTROL_NAME) {
					return op.getName();
				} else if (controlType == CONTROL_OP) {
					if(requiresFormat(getData())) {
						return format(getData(), op.getValue(getData()));
					}
					return op.getValue(getData());
				} else {
					return mwclient.getserverConfigs(getData());
				}
			} else {
				return getData();
			}
		}
		
		private boolean requiresFormat(String s) {
			if(s.equalsIgnoreCase("LegalAttackFactions") 
					|| s.equalsIgnoreCase("IllegalAttackFactions")
					|| s.equalsIgnoreCase("LegalDefendFactions")
					|| s.equalsIgnoreCase("IllegalDefendFactions")
					) {
				return true;
			}
			return false;
		}
		
		private String format(String key, String value) {
			if(key.equalsIgnoreCase("LegalAttackFactions") 
					|| key.equalsIgnoreCase("IllegalAttackFactions")
					|| key.equalsIgnoreCase("LegalDefendFactions")
					|| key.equalsIgnoreCase("IllegalDefendFactions")
					) {
				return value.replace("$", ", ");
			}
			return value;
		}
	}

	private String xmldir = "./data/operations/xml";
	private LinkedHashMap<String, OpViewerOpPane> ops = new LinkedHashMap<String, OpViewerOpPane>();
	
	private MWClient mwclient;
	
	private JPanel mainPanel = new JPanel();
	private JPanel selectorPanel = new JPanel();
	private JPanel contentPanel = new JPanel();
	private JPanel anchorPanel = new JPanel();
	private JPanel htmlPanel = new JPanel();
	private JScrollPane scrollpane = new JScrollPane();
	private JComboBox<String> selector = new JComboBox<String>();
	private JFrame mainframe;
	
	private Vector<TemplateElement> templateElements = new Vector<TemplateElement>();
	
	private String getOpHTML(Operation o) {
		StringBuilder sb = new StringBuilder();
		for (TemplateElement te : templateElements) {
			sb.append(te.getHTMLData(o));
		}
		return sb.toString();
	}
	
	// Load operations
	private void loadOps() {
		Properties p = new Properties();
		Operation o = new Operation("Defaults", new DefaultOperation(), p);
		ops.put(o.getName(), new OpViewerOpPane(getOpHTML(o)));
		File dir = new File(xmldir);
		for (final File fileEntry : dir.listFiles()) {
			if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".xml")) {
				MMNetXStream xml = new MMNetXStream();
				try {
					p = (Properties)xml.fromXML(new FileReader(fileEntry));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					MWLogger.errLog(e);
				}
				o = new Operation(fileEntry.getName().replace(".xml", ""), new DefaultOperation(), p);
				ops.put(o.getName(), new OpViewerOpPane(getOpHTML(o)));
			}
		}
	}

	private void setHTMLLocation(String loc) {
		OpViewerOpPane pane = (OpViewerOpPane)htmlPanel.getComponent(0);
		pane.scrollToReference(loc);
	}
	
	private void changeSelectedPanel() {
		htmlPanel.removeAll();
		OpViewerOpPane pane = ops.get(selector.getSelectedItem());
		pane.setVisible(true);		
		htmlPanel.add(pane);
		htmlPanel.revalidate();
		htmlPanel.repaint();
	}
	
	private void initComponents() {
		// Set up the selector panel
		Vector<String> opNames = new Vector<String>();
		for (String s : ops.keySet()) {
			opNames.add(s);
		}
		final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>(opNames);
		selector = new JComboBox<String>(model);
		selector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				changeSelectedPanel();
			}
		});
		selectorPanel.add(selector);
		selector.setSelectedIndex(0); // 0 is Default Op
		
		// Set up the anchor panel
		anchorPanel.setLayout(new VerticalLayout(5, VerticalLayout.LEFT, VerticalLayout.TOP));
		
		Dimension size = new Dimension();
		Vector<OpViewerAnchorButton> buttons = extractAnchorsFromTemplate("./data/operations/OpTemplate.html");
		
		for (OpViewerAnchorButton button : buttons) {
			size.height = Math.max(size.height, button.getPreferredSize().height);
			size.width = Math.max(size.width, button.getPreferredSize().width);
		}
		
		for (OpViewerAnchorButton button : buttons) {
			button.setPreferredSize(size);
			button.setMaximumSize(size);
			button.setMinimumSize(size);
			final String url = button.getUrl();
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setHTMLLocation(url);
				}
			});
			anchorPanel.add(button);
		}
		
		// Set up the content panel
		htmlPanel.add(ops.get(selector.getSelectedItem()));
		scrollpane = new JScrollPane(htmlPanel);
		// Let's set the info panels to all the same size
		size = new Dimension();
		for (OpViewerOpPane pane : ops.values()) {
			size.height = Math.max(size.height, pane.getPreferredSize().height);
			size.width = Math.max(size.width, pane.getPreferredSize().width);
		}
		for (String s : ops.keySet()) {
			ops.get(s).setPreferredSize(size);
		}
		
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));
		
		contentPanel.add(anchorPanel);
		contentPanel.add(scrollpane);
		
		Dimension maxSize = Toolkit.getDefaultToolkit().getScreenSize();
		maxSize.height /= 2;
		maxSize.width /= 2;

		Dimension prefSize = new Dimension();
		prefSize.width = Math.min(maxSize.width, (int)(htmlPanel.getPreferredSize().width*1.1));
		prefSize.height = Math.min(maxSize.height, htmlPanel.getPreferredSize().height);
		scrollpane.setPreferredSize(prefSize);
		scrollpane.setMaximumSize(maxSize);
		
		mainPanel.setLayout(new VerticalLayout());
		mainPanel.add(selectorPanel);
		mainPanel.add(contentPanel);
		add(mainPanel);
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				htmlPanel.revalidate();
				htmlPanel.repaint();
			}
		});
		this.setResizable(true);
		this.pack();
		this.setLocationRelativeTo(mainframe);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}
	
	private Vector<OpViewerAnchorButton> extractAnchorsFromTemplate(String fileName) {
		Vector<OpViewerAnchorButton> buttons = new Vector<OpViewerAnchorButton>();
		
		// First, always put in the top button
		buttons.add(new OpViewerAnchorButton("top", "Top"));		
		// Now, read in the template and find Anchors
		
		File file = new File(fileName);
		
		if (file.exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				for (String line; (line = br.readLine()) != null; ) {
					if (line.contains("%%ANCHOR%")) {
						buttons.add(buildAnchorButton(line));
					}
				}
				br.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				MWLogger.errLog(e);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				MWLogger.errLog(e);
			} 
			
		}
		
		return buttons;
	}
	
	private OpViewerAnchorButton buildAnchorButton(String line) {
		line = line.replace("%%ANCHOR%", "");
		line = line.replace("%%", "");
		String[] lines = line.split("%");
		return new OpViewerAnchorButton(lines[0], lines[1]);
	}
	
	public OperationViewerDialog(JFrame mainframe, MWClient c) {
		super(mainframe, "Operations Viewer", false);
		this.mainframe = mainframe;
		mwclient = c;
	}

	@Override
	public void run() {
		parseTemplate();
		loadOps();
		initComponents();
	}
	
	private void parseTemplate() {
		File file = new File("./data/operations/OpTemplate.html");
		if (file.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(file));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				for (String line; (line = br.readLine()) != null; ) {
					// Here, we will parse out the entire thing into a vector of phrases.
					if(line.startsWith("%%ANCHOR")) {
						continue;
					}
					if(line.contains("%%")) {
						String[] arr = line.split("%%");
						for (String s : arr) {
							templateElements.add(new TemplateElement(s));
						}
					} else {
						templateElements.add(new TemplateElement(line));
					}
					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
