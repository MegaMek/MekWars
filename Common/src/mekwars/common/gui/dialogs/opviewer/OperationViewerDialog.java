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

/**
 * Non-modal dialog that lets a player browse "Operations" — MekWars campaign-level scenario/event
 * definitions loaded from XML files under {@code ./data/operations/xml} — as rendered HTML pages. The
 * page layout and boilerplate text come from a shared HTML template file
 * ({@code ./data/operations/OpTemplate.html}) into which each Operation's own properties (and select
 * server config values) are substituted; a combo box lets the player switch which Operation is shown,
 * and a column of anchor buttons lets them jump to named sections within the current page.
 *
 * <p>This class implements {@link Runnable} so that construction (cheap: just stores references) can be
 * separated from the actual UI build-out ({@link #run()}, which does file I/O and creates/shows the
 * dialog); callers are expected to invoke {@link #run()} themselves (e.g. off the constructing thread or
 * via {@code SwingUtilities.invokeLater}) once the dialog should actually appear.
 */
public class OperationViewerDialog extends JDialog implements Runnable {
    private static final MMLogger LOGGER = MMLogger.create(OperationViewerDialog.class);

    @Serial
    private static final long serialVersionUID = 1L;

    /** Directory scanned for per-operation XML definition files. */
    private final String xmlDir = "./data/operations/xml";
    /**
     * Maps each known operation's name to its pre-rendered display pane. A {@link LinkedHashMap} is used
     * so iteration/combo-box order matches insertion order; the synthetic "Defaults" entry is always
     * inserted first by {@link #loadOps()}, which is relied upon elsewhere to mean "index 0 is Defaults".
     */
    private final LinkedHashMap<String, OpViewerOpPane> ops = new LinkedHashMap<>();

    private final JPanel mainPanel = new JPanel();
    private final JPanel selectorPanel = new JPanel();
    private final JPanel contentPanel = new JPanel();
    /** Column of {@link OpViewerAnchorButton}s used to jump to named sections of the current page. */
    private final JPanel anchorPanel = new JPanel();
    /** Holds the single currently-visible {@link OpViewerOpPane} for the selected operation. */
    private final JPanel htmlPanel = new JPanel();
    /** Owning application frame; used only as the dialog's parent/owner for positioning. */
    private final JFrame mainframe;
    /**
     * The HTML template ({@code OpTemplate.html}) parsed into an ordered sequence of literal-text and
     * substitution fragments; shared by every operation's rendering pass ({@link #getOpHTML(Operation)}).
     */
    private final Vector<TemplateElement> templateElements = new Vector<>();
    /** Used to resolve {@code CC%} (server config) template placeholders. */
    private final IClient client;
    /** Combo box bound to the operation names in {@link #ops}; rebuilt in {@link #initComponents()}. */
    private JComboBox<String> selector = new JComboBox<>();

    /**
     * Stores the owning frame and client. Does not touch Swing components or the filesystem; call
     * {@link #run()} to actually build and display the dialog.
     */
    public OperationViewerDialog(JFrame mainframe, IClient client) {
        super(mainframe, "Operations Viewer", false);
        this.mainframe = mainframe;
        this.client = client;
    }

    /**
     * Builds the dialog in three ordered steps: parses the shared HTML template, loads every known
     * operation (rendering each against that template), then constructs and shows the Swing UI. The
     * order matters — {@link #loadOps()} needs {@link #templateElements} populated to render each
     * operation's HTML, and {@link #initComponents()} needs {@link #ops} populated.
     */
    @Override
    public void run() {
        parseTemplate();
        loadOps();
        initComponents();
    }

    /**
     * Reads {@code ./data/operations/OpTemplate.html} line by line and appends {@link TemplateElement}s
     * to {@link #templateElements}. Lines starting with {@code "%%ANCHOR"} are skipped entirely here (
     * anchor lines are instead handled separately by {@link #extractAnchorsFromTemplate(String)}); a
     * line containing {@code "%%"} is split on that delimiter into multiple fragments (each becoming its
     * own {@code TemplateElement}, so control tokens embedded inline with literal text are separated
     * out); a line without {@code "%%"} becomes a single literal {@code TemplateElement} (implicitly
     * without the newline, since {@code BufferedReader.readLine()} strips it). If the template file does
     * not exist, this silently leaves {@link #templateElements} empty — no error is surfaced to the user.
     */
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

    /**
     * Populates {@link #ops} with one rendered pane per known operation.
     *
     * <p>A synthetic "Defaults" operation (backed by {@link DefaultOperation} and empty properties) is
     * always added first, guaranteeing {@link #ops} is never empty even if no XML files are found; its
     * being first is what {@link #initComponents()} relies on when it defaults the selector to index 0.
     *
     * <p>Every {@code .xml} file directly under {@link #xmlDir} (subdirectories are skipped) is then
     * deserialized via {@link MMNetXStream} into a {@link Properties} bag and wrapped in an
     * {@link Operation} named after the filename (minus the {@code .xml} extension).
     *
     * <p><b>Quirk:</b> if deserializing a given XML file throws {@link FileNotFoundException}, the
     * exception is only logged — the loop does not {@code continue}/skip that file. It falls through and
     * still builds and registers an {@code Operation} for that filename, reusing whatever {@code
     * properties} value is left over from the previous successful iteration (or the initial empty
     * "Defaults" properties, if it's the very first file). That operation's page therefore silently
     * displays stale/wrong property values instead of being skipped or showing an error.
     */
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

    /**
     * Builds and displays the full dialog UI: the operation-selector combo box, the anchor navigation
     * column (always seeded with a "Top" button plus one button per {@code %%ANCHOR%} found in the
     * template), the scrollable content area showing the currently selected operation's pane, and
     * window sizing/behavior (resizable, sized to at most half the screen, packed, centered on
     * {@link #mainframe}, disposed on close). Also wires a resize listener that forces the html pane to
     * revalidate/repaint, and defaults the selector to index 0 (the "Defaults" operation, per the
     * insertion-order guarantee documented on {@link #loadOps()}).
     */
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

    /**
     * Renders the complete HTML page for one operation by concatenating the output of every parsed
     * {@link #templateElements} fragment against it (literal text passed through, control tokens
     * substituted with values from {@code operation} or server config). This is how every operation's
     * page shares the same template layout while differing in content.
     */
    private String getOpHTML(Operation operation) {
        StringBuilder stringBuilder = new StringBuilder();

        for (TemplateElement templateElement : templateElements) {
            stringBuilder.append(templateElement.getHTMLData(operation));
        }

        return stringBuilder.toString();
    }

    /**
     * Swaps the single child of {@link #htmlPanel} to the pane for whichever operation is now selected
     * in {@link #selector}. The previously-shown pane is simply removed from display (it remains cached
     * in {@link #ops}, not disposed) and the new one is added and explicitly re-shown.
     */
    private void changeSelectedPanel() {
        htmlPanel.removeAll();
        OpViewerOpPane pane = ops.get(selector.getSelectedItem());
        pane.setVisible(true);
        htmlPanel.add(pane);
        htmlPanel.revalidate();
        htmlPanel.repaint();
    }

    /**
     * Builds the list of anchor navigation buttons for {@link #anchorPanel} by re-reading the template
     * file at {@code fileName} (note: this is a second, separate read of the same file already consumed
     * by {@link #parseTemplate()}) looking for lines containing {@code "%%ANCHOR%"}. A "Top" button
     * (targeting anchor id {@code "top"}) is always added first, unconditionally — even if the template
     * contains no actual {@code %%ANCHOR%top%...} line defining that anchor, so clicking it may be a
     * no-op if the template never defines a "top" anchor. If the file does not exist, only that "Top"
     * button is returned.
     */
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

    /**
     * Scrolls the currently displayed operation pane to the given HTML named anchor. Assumes
     * {@link #htmlPanel} always has exactly one child of type {@link OpViewerOpPane}; if the panel is
     * ever empty this throws {@link ArrayIndexOutOfBoundsException}, and if the child were some other
     * component type it would throw {@link ClassCastException}.
     *
     * @param loc the HTML named-anchor id to scroll to
     */
    private void setHTMLLocation(String loc) {
        OpViewerOpPane pane = (OpViewerOpPane) htmlPanel.getComponent(0);
        pane.scrollToReference(loc);
    }

    /**
     * Parses one {@code %%ANCHOR%<id>%<label>%%...} template line into an {@link OpViewerAnchorButton}.
     * Strips the {@code "%%ANCHOR%"} marker and all remaining {@code "%%"} pairs, then splits the
     * remainder on single {@code "%"} characters, taking the first token as the anchor id and the
     * second as the button label. This is a fragile, position-based parse: it assumes exactly one
     * anchor definition per line and no other {@code "%"} characters in the line.
     */
    private OpViewerAnchorButton buildAnchorButton(String line) {
        line = line.replace("%%ANCHOR%", "");
        line = line.replace("%%", "");
        String[] lines = line.split("%");
        return new OpViewerAnchorButton(lines[0], lines[1]);
    }

}
