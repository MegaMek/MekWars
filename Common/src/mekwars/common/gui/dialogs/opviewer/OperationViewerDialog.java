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

package mekwars.common.gui.dialogs.opviewer;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serial;
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

import megamek.logging.MMLogger;
import mekwars.common.VerticalLayout;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.operations.DefaultOperation;
import mekwars.common.campaign.operations.Operation;
import mekwars.common.util.MMNetXStream;

public class OperationViewerDialog extends JDialog implements Runnable {
    private static final MMLogger LOGGER = MMLogger.create(OperationViewerDialog.class);

    @Serial
    private static final long serialVersionUID = 1L;

    private final String xmlDir = "./data/operations/xml";
    private final LinkedHashMap<String, OpViewerOpPane> ops = new LinkedHashMap<>();

    private final JPanel mainPanel = new JPanel();
    private final JPanel selectorPanel = new JPanel();
    private final JPanel contentPanel = new JPanel();
    private final JPanel anchorPanel = new JPanel();
    private final JPanel htmlPanel = new JPanel();
    private final JFrame mainframe;
    private final Vector<TemplateElement> templateElements = new Vector<>();
    private final IClient client;
    private JComboBox<String> selector = new JComboBox<>();

    public OperationViewerDialog(JFrame mainframe, IClient client) {
        super(mainframe, "Operations Viewer", false);
        this.mainframe = mainframe;
        this.client = client;
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
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                for (String line; (line = bufferedReader.readLine()) != null; ) {
                    // Here, we will parse out the entire thing into a vector of phrases.
                    if (line.startsWith("%%ANCHOR")) {
                        continue;
                    }

                    if (line.contains("%%")) {
                        String[] arr = line.split("%%");
                        for (String s : arr) {
                            templateElements.add(new TemplateElement(s, client));
                        }
                    } else {
                        templateElements.add(new TemplateElement(line, client));
                    }
                }
            } catch (IOException e) {
                LOGGER.error(e, "IO Exception trying to read lines: {}", e.getLocalizedMessage());
            }
        }
    }

    // Load operations
    private void loadOps() {
        Properties properties = new Properties();
        Operation operation = new Operation("Defaults", new DefaultOperation(), properties);
        ops.put(operation.getName(), new OpViewerOpPane(getOpHTML(operation)));
        File dir = new File(xmlDir);

        File[] fileList = dir.listFiles();

        if (fileList != null) {
            for (final File fileEntry : fileList) {
                if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".xml")) {
                    MMNetXStream xml = new MMNetXStream();

                    try {
                        properties = (Properties) xml.fromXML(new FileReader(fileEntry));
                    } catch (FileNotFoundException e) {
                        LOGGER.error(e, "File not found: {}", e.getLocalizedMessage());
                    }

                    operation = new Operation(fileEntry.getName().replace(".xml", ""),
                          new DefaultOperation(),
                          properties);
                    ops.put(operation.getName(), new OpViewerOpPane(getOpHTML(operation)));
                }
            }
        }
    }

    private void initComponents() {
        // Set up the selector panel
        Vector<String> opNames = new Vector<>(ops.keySet());
        final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(opNames);
        selector = new JComboBox<>(model);
        selector.addActionListener(e -> changeSelectedPanel());
        selectorPanel.add(selector);
        selector.setSelectedIndex(0); // 0 is Default Op

        // Set up the anchor panel
        anchorPanel.setLayout(new VerticalLayout(5, VerticalLayout.LEFT, VerticalLayout.TOP));

        Dimension size = new java.awt.Dimension();
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
            button.addActionListener(e -> setHTMLLocation(url));
            anchorPanel.add(button);
        }

        // Set up the content panel
        htmlPanel.add(ops.get(selector.getSelectedItem()));
        JScrollPane scrollPane = new JScrollPane(htmlPanel);
        // Let's set the info panels to all the same size
        size = new java.awt.Dimension();

        for (OpViewerOpPane pane : ops.values()) {
            size.height = Math.max(size.height, pane.getPreferredSize().height);
            size.width = Math.max(size.width, pane.getPreferredSize().width);
        }

        for (String string : ops.keySet()) {
            ops.get(string).setPreferredSize(size);
        }

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));

        contentPanel.add(anchorPanel);
        contentPanel.add(scrollPane);

        Dimension maxSize = Toolkit.getDefaultToolkit().getScreenSize();
        maxSize.height /= 2;
        maxSize.width /= 2;

        Dimension prefSize = new Dimension();
        prefSize.width = Math.min(maxSize.width, (int) (htmlPanel.getPreferredSize().width * 1.1));
        prefSize.height = Math.min(maxSize.height, htmlPanel.getPreferredSize().height);
        scrollPane.setPreferredSize(prefSize);
        scrollPane.setMaximumSize(maxSize);

        mainPanel.setLayout(new VerticalLayout());
        mainPanel.add(selectorPanel);
        mainPanel.add(contentPanel);
        add(mainPanel);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
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

    private String getOpHTML(Operation operation) {
        StringBuilder stringBuilder = new StringBuilder();

        for (TemplateElement templateElement : templateElements) {
            stringBuilder.append(templateElement.getHTMLData(operation));
        }

        return stringBuilder.toString();
    }

    private void changeSelectedPanel() {
        htmlPanel.removeAll();
        OpViewerOpPane pane = ops.get(selector.getSelectedItem());
        pane.setVisible(true);
        htmlPanel.add(pane);
        htmlPanel.revalidate();
        htmlPanel.repaint();
    }

    private Vector<OpViewerAnchorButton> extractAnchorsFromTemplate(String fileName) {
        Vector<OpViewerAnchorButton> buttons = new Vector<>();

        // First, always put in the top button
        buttons.add(new OpViewerAnchorButton("top", "Top"));
        // Now, read in the template and find Anchors

        File file = new File(fileName);

        if (file.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                for (String line; (line = bufferedReader.readLine()) != null; ) {
                    if (line.contains("%%ANCHOR%")) {
                        buttons.add(buildAnchorButton(line));
                    }
                }
            } catch (IOException e) {
                LOGGER.error(e, "IO Error: {}", e.getLocalizedMessage());
            }
        }

        return buttons;
    }

    private void setHTMLLocation(String loc) {
        OpViewerOpPane pane = (OpViewerOpPane) htmlPanel.getComponent(0);
        pane.scrollToReference(loc);
    }

    private OpViewerAnchorButton buildAnchorButton(String line) {
        line = line.replace("%%ANCHOR%", "");
        line = line.replace("%%", "");
        String[] lines = line.split("%");
        return new OpViewerAnchorButton(lines[0], lines[1]);
    }

}
