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

package mekwars.client.gui.dialog.opviewer;

import common.VerticalLayout;
import common.campaign.operations.DefaultOperation;
import common.campaign.operations.Operation;
import common.util.MMNetXStream;
import common.util.MWLogger;

public class OperationViewerDialog extends javax.swing.JDialog implements Runnable {
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
                    if (requiresFormat(getData())) {
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
            if (s.equalsIgnoreCase("LegalAttackFactions")
                      || s.equalsIgnoreCase("IllegalAttackFactions")
                      || s.equalsIgnoreCase("LegalDefendFactions")
                      || s.equalsIgnoreCase("IllegalDefendFactions")
            ) {
                return true;
            }
            return false;
        }

        private String format(String key, String value) {
            if (key.equalsIgnoreCase("LegalAttackFactions")
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
    private java.util.LinkedHashMap<String, mekwars.client.gui.dialog.opviewer.OpViewerOpPane> ops = new java.util.LinkedHashMap<String, mekwars.client.gui.dialog.opviewer.OpViewerOpPane>();

    private client.MWClient mwclient;

    private javax.swing.JPanel mainPanel = new javax.swing.JPanel();
    private javax.swing.JPanel selectorPanel = new javax.swing.JPanel();
    private javax.swing.JPanel contentPanel = new javax.swing.JPanel();
    private javax.swing.JPanel anchorPanel = new javax.swing.JPanel();
    private javax.swing.JPanel htmlPanel = new javax.swing.JPanel();
    private javax.swing.JScrollPane scrollpane = new javax.swing.JScrollPane();
    private javax.swing.JComboBox<String> selector = new javax.swing.JComboBox<String>();
    private javax.swing.JFrame mainframe;

    private java.util.Vector<mekwars.client.gui.dialog.opviewer.OperationViewerDialog.TemplateElement> templateElements = new java.util.Vector<mekwars.client.gui.dialog.opviewer.OperationViewerDialog.TemplateElement>();

    private String getOpHTML(Operation o) {
        StringBuilder sb = new StringBuilder();
        for (mekwars.client.gui.dialog.opviewer.OperationViewerDialog.TemplateElement te : templateElements) {
            sb.append(te.getHTMLData(o));
        }
        return sb.toString();
    }

    // Load operations
    private void loadOps() {
        java.util.Properties p = new java.util.Properties();
        Operation o = new Operation("Defaults", new DefaultOperation(), p);
        ops.put(o.getName(), new OpViewerOpPane(getOpHTML(o)));
        java.io.File dir = new java.io.File(xmldir);
        for (final java.io.File fileEntry : dir.listFiles()) {
            if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".xml")) {
                MMNetXStream xml = new MMNetXStream();
                try {
                    p = (java.util.Properties) xml.fromXML(new java.io.FileReader(fileEntry));
                } catch (java.io.FileNotFoundException e) {
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
        OpViewerOpPane pane = (OpViewerOpPane) htmlPanel.getComponent(0);
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
        java.util.Vector<String> opNames = new java.util.Vector<String>();
        for (String s : ops.keySet()) {
            opNames.add(s);
        }
        final javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<String>(opNames);
        selector = new javax.swing.JComboBox<String>(model);
        selector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                changeSelectedPanel();
            }
        });
        selectorPanel.add(selector);
        selector.setSelectedIndex(0); // 0 is Default Op

        // Set up the anchor panel
        anchorPanel.setLayout(new VerticalLayout(5, VerticalLayout.LEFT, VerticalLayout.TOP));

        java.awt.Dimension size = new java.awt.Dimension();
        java.util.Vector<mekwars.client.gui.dialog.opviewer.OpViewerAnchorButton> buttons = extractAnchorsFromTemplate(
              "./data/operations/OpTemplate.html");

        for (OpViewerAnchorButton button : buttons) {
            size.height = Math.max(size.height, button.getPreferredSize().height);
            size.width = Math.max(size.width, button.getPreferredSize().width);
        }

        for (OpViewerAnchorButton button : buttons) {
            button.setPreferredSize(size);
            button.setMaximumSize(size);
            button.setMinimumSize(size);
            final String url = button.getUrl();
            button.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    setHTMLLocation(url);
                }
            });
            anchorPanel.add(button);
        }

        // Set up the content panel
        htmlPanel.add(ops.get(selector.getSelectedItem()));
        scrollpane = new javax.swing.JScrollPane(htmlPanel);
        // Let's set the info panels to all the same size
        size = new java.awt.Dimension();
        for (OpViewerOpPane pane : ops.values()) {
            size.height = Math.max(size.height, pane.getPreferredSize().height);
            size.width = Math.max(size.width, pane.getPreferredSize().width);
        }
        for (String s : ops.keySet()) {
            ops.get(s).setPreferredSize(size);
        }

        contentPanel.setLayout(new javax.swing.BoxLayout(contentPanel, javax.swing.BoxLayout.X_AXIS));

        contentPanel.add(anchorPanel);
        contentPanel.add(scrollpane);

        java.awt.Dimension maxSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        maxSize.height /= 2;
        maxSize.width /= 2;

        java.awt.Dimension prefSize = new java.awt.Dimension();
        prefSize.width = Math.min(maxSize.width, (int) (htmlPanel.getPreferredSize().width * 1.1));
        prefSize.height = Math.min(maxSize.height, htmlPanel.getPreferredSize().height);
        scrollpane.setPreferredSize(prefSize);
        scrollpane.setMaximumSize(maxSize);

        mainPanel.setLayout(new VerticalLayout());
        mainPanel.add(selectorPanel);
        mainPanel.add(contentPanel);
        add(mainPanel);
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                htmlPanel.revalidate();
                htmlPanel.repaint();
            }
        });
        this.setResizable(true);
        this.pack();
        this.setLocationRelativeTo(mainframe);
        this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        this.setVisible(true);
    }

    private java.util.Vector<mekwars.client.gui.dialog.opviewer.OpViewerAnchorButton> extractAnchorsFromTemplate(
          String fileName) {
        java.util.Vector<mekwars.client.gui.dialog.opviewer.OpViewerAnchorButton> buttons = new java.util.Vector<mekwars.client.gui.dialog.opviewer.OpViewerAnchorButton>();

        // First, always put in the top button
        buttons.add(new OpViewerAnchorButton("top", "Top"));
        // Now, read in the template and find Anchors

        java.io.File file = new java.io.File(fileName);

        if (file.exists()) {
            try {
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
                for (String line; (line = br.readLine()) != null; ) {
                    if (line.contains("%%ANCHOR%")) {
                        buttons.add(buildAnchorButton(line));
                    }
                }
                br.close();
            } catch (java.io.FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                MWLogger.errLog(e);
            } catch (java.io.IOException e) {
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

    public OperationViewerDialog(javax.swing.JFrame mainframe, client.MWClient c) {
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
        java.io.File file = new java.io.File("./data/operations/OpTemplate.html");
        if (file.exists()) {
            java.io.BufferedReader br = null;
            try {
                br = new java.io.BufferedReader(new java.io.FileReader(file));
            } catch (java.io.FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                for (String line; (line = br.readLine()) != null; ) {
                    // Here, we will parse out the entire thing into a vector of phrases.
                    if (line.startsWith("%%ANCHOR")) {
                        continue;
                    }
                    if (line.contains("%%")) {
                        String[] arr = line.split("%%");
                        for (String s : arr) {
                            templateElements.add(new mekwars.client.gui.dialog.opviewer.OperationViewerDialog.TemplateElement(
                                  s));
                        }
                    } else {
                        templateElements.add(new mekwars.client.gui.dialog.opviewer.OperationViewerDialog.TemplateElement(
                              line));
                    }

                }
            } catch (java.io.IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                br.close();
            } catch (java.io.IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
