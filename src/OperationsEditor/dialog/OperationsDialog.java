/*
 * MekWars - Copyright (C) 2004
 *
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

/**
 * @author jtighe
 *
 *         Server Configuration Page. All new Server Options need to be added To this page as well.
 */

package OperationsEditor.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import common.VerticalLayout;
import common.campaign.operations.DefaultOperation;
import common.flags.FlagSet;
import common.util.MWLogger;
import common.util.SpringLayoutHelper;

public class OperationsDialog extends JFrame implements ActionListener, KeyListener, MouseListener {

    /**
	 *
	 */
    private static final long serialVersionUID = -238767483230471330L;
    private final static String windowName = "MekWars Operations Editor";

    private final static int SHORT_OP = 0;
    private final static int LONG_OP = 0;
    private final static int SPECIAL_OP = 0;

    public final static int OP_VERSION = 2;
    private int currentOpType = SHORT_OP;

    private boolean shortOpScreenCreated = false;

    private String taskName = "";

    private DefaultOperation defaultOperationInfo = new DefaultOperation();

    private BackedTreeMap opValues;

    private JTextField BaseTextField = new JTextField(10);
    private JCheckBox BaseCheckBox = new JCheckBox();
    private JComboBox BaseComboBox = new JComboBox();

    private String filePathName = "./data/operations";
    private JOptionPane pane;
    private JScrollPane scrollPane;
    private Object mwclient = null;

    JPanel contentPane;

    private boolean changesMade = false;

    private Dimension textBoxSize = new Dimension(70, 22);

    JTabbedPane ConfigPane = new JTabbedPane(SwingConstants.TOP);

    FlagTable afTable;
    FlagTable dfTable;
    FlagTable wfTable;
    FlagTable lfTable;

    /**
     * @author Torren (Jason Tighe) 01/04/2006
     *
     *         I've completely redone how the Operations dialog works There are 2 basic fields now baseTextField which is a JTextField and baseCheckBox which is
     *         a JCheckBox.
     *
     *         When you add a new config add the labels to the tab then use the base fields to add the ver. make sure to set the base field's name method this
     *         is used to populate and save.
     *
     *         ex: BaseTextField.setName("DefaultOperationsOptionsVariable");
     *
     *         Three recursive methods populate, load, and save the data to file
     *
     *         findAndPopulateTextAndCheckBoxes(JPanel) findAndPopulateTextAndCheckBoxes(JPanel, BackedTreeMap) findAndSaveConfigs(JPanel, PrintStream)
     *
     *         This change to the code removes the tediousness of having to add a new var to 3 locations when it is used. Now only 1 location needs to be added
     *         and that is the vars placement on the tab in the UI.
     */
    public OperationsDialog(Object o) {

        // super("Ops Editor");

        if (o != null) {
            mwclient = o;
        }

        String logFileName = "./logs/opeditorlog.txt";
        try {
            PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFileName), 64));
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu();
        JMenu createMenu = new JMenu();
        JMenu loadMenu = new JMenu();

        fileMenu.setText("File");
        fileMenu.setMnemonic('F');

        createMenu.setText("New");
        createMenu.setMnemonic('N');

        loadMenu.setText("Load");
        loadMenu.setMnemonic('L');

        fileMenu.add(createMenu);
        fileMenu.add(loadMenu);

        menuBar.add(fileMenu);
        menuBar.setVisible(true);

        JMenuItem item = new JMenuItem("Short Operation");
        item.setMnemonic('S');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuLoadShortOp_actionPerformed(e);
            }
        });
        loadMenu.add(item);

        item = new JMenuItem("Long Operation");
        item.setMnemonic('L');
        item.setEnabled(false);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuLoadLongOp_actionPerformed(e);
            }
        });
        loadMenu.add(item);

        item = new JMenuItem("Special Operation");
        item.setMnemonic('p');
        item.setEnabled(false);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuLoadSpecialOp_actionPerformed(e);
            }
        });
        loadMenu.add(item);

        item = new JMenuItem("Short Operation");
        item.setMnemonic('S');
        item.setEnabled(true);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuCreateShortOp_actionPerformed(e);
            }
        });
        createMenu.add(item);

        item = new JMenuItem("Long Operation");
        item.setMnemonic('L');
        item.setEnabled(false);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuCreateLongOp_actionPerformed(e);
            }
        });
        createMenu.add(item);

        item = new JMenuItem("Special Operation");
        item.setMnemonic('p');
        item.setEnabled(false);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuCreateSpecialOp_actionPerformed(e);
            }
        });
        createMenu.add(item);

        item = new JMenuItem("Save");
        item.setMnemonic('S');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (currentOpType == OperationsDialog.SHORT_OP) {
                    saveShortOperations();
                }
            }
        });
        fileMenu.add(item);

        item = new JMenuItem("Save as");
        item.setMnemonic('A');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuSaveAsOp_actionPerformed(e);
            }
        });
        fileMenu.add(item);

        fileMenu.addSeparator();
        item = new JMenuItem("Exit");
        item.setMnemonic('X');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (changesMade) {
                    int result = JOptionPane.showConfirmDialog(null, "Changes have been made\n\rDo you want to exit without saving?", "Exit Without Saving", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        if (mwclient == null) {
                            System.exit(0);
                        } else {
                            dispose();
                        }
                    }
                } else {
                    if (mwclient == null) {
                        System.exit(0);
                    } else {
                        dispose();
                    }
                }
            }
        });
        fileMenu.add(item);

        if (mwclient != null) {
            menuBar.add(createEditMenu());
        }

        setResizable(true);
        setSize(new Dimension(640, 480));
        setExtendedState(Frame.NORMAL);
        if (mwclient != null) {
            setTitle(windowName + "(Integrated)");
        } else {
            setTitle(windowName);
        }

        /*
         * Check for the operations directories.
         * If they're missing create them.
         */
        File shortDir = new File("./data/operations/short/");
        File longDir = new File("./data/operations/long/");
        File modDir = new File("./data/operations/modifiers/");
        try {
            if (!shortDir.exists()) {
                shortDir.mkdirs();
            }
            if (!longDir.exists()) {
                longDir.mkdir();
            }
            if (!modDir.exists()) {
                modDir.mkdir();
            }
        } catch (Exception e) {
            System.err.println("Error while creating operations directories.");
            System.exit(1);
        }

        // Set the actions to generate
        setJMenuBar(menuBar);

        // don't want any buttons have everything menu bar driven.
        Object[] options = {};

        // Create the pane containing the buttons
        pane = new JOptionPane(ConfigPane, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, null);

        pane.setVisible(false);
        scrollPane = new JScrollPane(pane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(scrollPane, BorderLayout.CENTER);
        repaint();
        setLocation(getLocation().x + 10, getLocation().y);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                boolean closeit = true;
                if (changesMade) {
                    int result = JOptionPane.showConfirmDialog(null, "Changes were been made!\n\rDo you want to exit without saving?", "Exit Without Saving", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        closeit = true;
                    } else {
                        closeit = false;
                    }
                }
                if (closeit) {
                    if (mwclient != null) {
                        dispose();
                    } else {
                        System.exit(0);
                    }
                }
            }
        });

        // this.pack();
        setVisible(true);

        /* // Create the main dialog and set the default button
         dialog = pane.createDialog(mainConfigPanel, windowName);
         dialog.getRootPane().setDefaultButton(cancelButton);
         dialog.setJMenuBar(menuBar);

         //Show the dialog and get the user's input
          dialog.setLocation(dialog.getLocation().x+10,dialog.getLocation().y);
          dialog.setModal(true);
          dialog.pack();
          dialog.setVisible(true);

          if (pane.getValue() == okayButton)
          {
          }
          else if ( pane.getValue() == cancelButton ){
          System.exit(0);
          }*/
    }

    public void actionPerformed(ActionEvent e) {
        changesMade = true;
        if (getTitle().indexOf("*") == -1) {
            setTitle(getTitle() + "*");
        }
    }

    public void jMenuLoadShortOp_actionPerformed(ActionEvent e) {
        currentOpType = OperationsDialog.SHORT_OP;
        if (!shortOpScreenCreated) {
            createShortOpPanel();
        }
        initShortOpVars();
        loadShortOp();
    }

    public void jMenuLoadLongOp_actionPerformed(ActionEvent e) {
        currentOpType = OperationsDialog.LONG_OP;
    }

    public void jMenuLoadSpecialOp_actionPerformed(ActionEvent e) {
        currentOpType = OperationsDialog.SPECIAL_OP;
    }

    public void jMenuCreateShortOp_actionPerformed(ActionEvent e) {
        taskName = JOptionPane.showInputDialog(null, "Operation Name", "New Operation Name", JOptionPane.OK_CANCEL_OPTION);

        if ((taskName == null) || (taskName.length() == 0)) {
            return;
        }

        if (!shortOpScreenCreated) {
            createShortOpPanel();
        }

        defaultOperationInfo = new DefaultOperation();
        initShortOpVars();
        currentOpType = OperationsDialog.SHORT_OP;
        setTitle(windowName + " (" + taskName + ")");
        filePathName = "./data/operations/short/" + taskName + ".txt";

    }

    public void jMenuCreateLongOp_actionPerformed(ActionEvent e) {
        currentOpType = OperationsDialog.LONG_OP;
    }

    public void jMenuCreateSpecialOp_actionPerformed(ActionEvent e) {
        currentOpType = OperationsDialog.SPECIAL_OP;
    }

    public void jMenuSaveAsOp_actionPerformed(ActionEvent e) {
        FileDialog fDialog = new FileDialog(this, "Save Short Op As", FileDialog.SAVE);

        fDialog.setDirectory(filePathName);
        fDialog.setVisible(true);

        if (fDialog.getFile() != null) {
            filePathName = fDialog.getDirectory() + fDialog.getFile();
            taskName = fDialog.getFile().substring(0, fDialog.getFile().indexOf(".txt"));

            if (currentOpType == OperationsDialog.SHORT_OP) {
                saveShortOperations();
            }

            setTitle(windowName + " (" + taskName + ")");
            changesMade = false;
        }
    }

    public void createShortOpPanel() {

        // TAB PANELS (these are added to the root pane as tabs)
        JPanel rangePanel = new JPanel();// Ranges for the Op
        JPanel factionPanel = new JPanel();// Faction exclusions
        JPanel unitsPanel = new JPanel();// Unit mins and maxes for the op
        JPanel costsPanel = new JPanel();// Cost to attack or defend an op
        JPanel flagsPanel = new JPanel(); // Player flags
        JPanel unitRatiosPanel = new JPanel();

        JPanel playerPropertiesPanel = new JPanel();// mins/maxes for players if they can attack/defend an op
        JPanel scenarioPanel = new JPanel();// Arty/mines anything given to an attacker/defender besides their own units
        JPanel opresultsPanel = new JPanel();// who gets payed what and how much
        JPanel salvagePanel = new JPanel();// how the units are divied up
        JPanel newbieOpsPanel = new JPanel();// how to treat the new player in your life
        JPanel metaSetupPanel = new JPanel();// Set what your faction gets for this op. Land Units Components
        JPanel chickenLeechPanel = new JPanel();// Set up what happens to those that flee and those that don't pay attention to an attack.
        JPanel pilotExpPanel = new JPanel();// Set up how unit pilots will be reward for surviving.
        JPanel buildingsPanel = new JPanel();// Set up buildings for speicific ops.
        JPanel victoryPanel = new JPanel();// Set up Victory Conditions.
        JPanel teamPanel = new JPanel();// Set up for Team games.
        JPanel deploymentPanel = new JPanel();// Set up game deployments.

        ConfigPane = new JTabbedPane();

        shortOpScreenCreated = true;

        /*
         * Operations Range
         *
         * Set up the Operations Range panel, which sets the max range for this operation
         */

        // give the path panel a box layout. its going to be smaller than some, so
        // we dont need flow-nested boxes
        JPanel rangesBox = new JPanel(new SpringLayout());
        JPanel rangesBox2 = new JPanel(new SpringLayout());
        JPanel rangesmasterBox = new JPanel();
        rangesmasterBox.setLayout(new BoxLayout(rangesmasterBox, BoxLayout.Y_AXIS));

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Operation Color:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("html or hex color for an op");
        BaseTextField.setName("OperationColor");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Operation Range:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("One Jump");
        BaseTextField.setName("OperationRange");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("% To Attack On World:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<Html>Min Percent needed by a faction to launch this operation aginst<br>this world from on the world itself.</html>");
        BaseTextField.setName("PercentageToAttackOnWorld");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("% to Attack Off World:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<Html>Min Percent needed by a faction to launch this operation aginst<br>another world from this world.</html>");
        BaseTextField.setName("PercentageToAttackOffWorld");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Min % Owned:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min % owned to use attack on a world.");
        BaseTextField.setName("MinPlanetOwnership");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Max % Owned:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max % owned to use attack on a world.");
        BaseTextField.setName("MaxPlanetOwnership");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Allowed Planet Flags:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Flags the planet must have to allow this op.<br>Delimit/Sperate with ^ Example AA^BB^CC</html>");
        BaseTextField.setName("AllowPlanetFlags");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Disallowed Planet Flags:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Flags the planet cannot have for this op to be allowed.<br>Delimit/Sperate with ^ Example AA^BB^CC</html>");
        BaseTextField.setName("DisallowPlanetFlags");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Max ELO Difference:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>The Max Difference in ELO between the attacker and the defender</html>");
        BaseTextField.setName("MaxELODifference");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Min SubFaction Access Level:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Your SubFactionAcessLevel must be this high to ride.<br>Default 0</html>");
        BaseTextField.setName("MinSubFactionAccessLevel");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Max BV Difference:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max BV difference between attacker and defender.<br>Default 150</html>");
        BaseTextField.setName("MaxBVDifference");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Max % BV Difference:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double Field. Max % BV diffence between Attacker and defneder.<br>Example .05 = 5% 1.5 = 150%. Default = 0%</html>");
        BaseTextField.setName("MaxBVPercent");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Night Chance:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>% chance operation will occur at night. Default 0</html>");
        BaseTextField.setName("NightChance");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Dusk Chance:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>% Chance operation will occur at dusk.  Default 0%</html>");
        BaseTextField.setName("DuskChance");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Attacker Briefing", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Message Attacker recieves after launching</html>");
        BaseTextField.setName("AttackerBriefing");
        rangesBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        rangesBox.add(new JLabel("Defender Briefing", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Message Defender recieves after launching</html>");
        BaseTextField.setName("DefenderBriefing");
        rangesBox.add(BaseTextField);

        BaseCheckBox = new JCheckBox("Must Have Fac");
        BaseCheckBox.setToolTipText("<html>If checked, attack type may only be used against<br>a world which has a production facility.</html>");
        BaseCheckBox.setName("OnlyAgainstFactoryWorlds");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("May Not Have Fac");
        BaseCheckBox.setToolTipText("<html>If checked, attack type may only be used against<br>a world which has NO production facilities.</html>");
        BaseCheckBox.setName("OnlyAgainstNonFactoryWorlds");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Must Have Home");
        BaseCheckBox.setToolTipText("<html>If checked, attack type may only be used against<br>a world that is set as a homeworld.</html>");
        BaseCheckBox.setName("OnlyAgainstHomeWorlds");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("May Not Have Home");
        BaseCheckBox.setToolTipText("<html>If checked, attack type may only be used against<br>a world which has not been labeled as a homeworld.</html>");
        BaseCheckBox.setName("OnlyAgainstNonHomeWorlds");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Real Blind Drop");
        BaseCheckBox.setToolTipText("<html>If checked the operation will play in<br>real blind drop and double blind mode</html>");
        BaseCheckBox.setName("RealBlindDrop");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Report Op To News Feed");
        BaseCheckBox.setToolTipText("<html>This will send the op info to the servers newsfeed file.</html>");
        BaseCheckBox.setName("ReportOpToNewsFeed");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Doesn't Count Toward PP");
        BaseCheckBox.setToolTipText("<html>Armies that are only legal for this op will not produce components.</html>");
        BaseCheckBox.setName("DoesNotCountForPP");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Forbid Counterattacks");
        BaseCheckBox.setToolTipText("<html>Check to stop players from using this attack if under attack themselves.</html>");
        BaseCheckBox.setName("ForbidCounterAttacks");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("AFR Only");
        BaseCheckBox.setToolTipText("Check to stop players on active duty from using this attack (AFR only).");
        BaseCheckBox.setName("OnlyAllowedFromReserve");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Active Only");
        BaseCheckBox.setToolTipText("Check to stop players on reserve duty from using this attack (No AFR).");
        BaseCheckBox.setName("OnlyAllowedFromActive");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Debug Op");
        BaseCheckBox.setToolTipText("<html>This will help send debug message to the Error logs for ops to debug ops.</html>");
        BaseCheckBox.setName("DebugOp");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Individual Initiative");
        BaseCheckBox.setToolTipText("<html>Set Individual Initiative for MegaMek Games</html>");
        BaseCheckBox.setName("IndividualInit");
        rangesBox2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Autoresolve Battles");
        BaseCheckBox.setToolTipText("<html>If set, the battles will be autoresolved</html>");
        BaseCheckBox.setName("AutoresolveBattle");
        rangesBox2.add(BaseCheckBox);

        //Baruk Khazad! - 20151003 - new checkbox
        BaseCheckBox = new JCheckBox("Defender ignores Min % Owned");
        BaseCheckBox.setToolTipText("<html>If defender owns some of the planet but less than the Min%Owned, defender is eligible to be attacked.</html>");
        BaseCheckBox.setName("MinPlanetOwnershipIgnoredByDefender");
        rangesBox2.add(BaseCheckBox);

        // finalize layout.
        SpringLayoutHelper.setupSpringGrid(rangesBox, 4);
        SpringLayoutHelper.setupSpringGrid(rangesBox2, 3);

        rangesmasterBox.add(rangesBox);
        rangesmasterBox.add(rangesBox2);
        rangePanel.add(rangesmasterBox);

        /*
         * Operations FACTIONS
         *
         * Faction panel, which allows Admins to limit which
         * factions have access to certain attack and defense
         * types.
         */
        JPanel factionsBox = new JPanel(new SpringLayout());

        JPanel factionsmasterBox = new JPanel();
        factionsmasterBox.setLayout(new BoxLayout(factionsmasterBox, BoxLayout.Y_AXIS));

        BaseTextField = new JTextField(5);
        factionsBox.add(new JLabel("Legal Attack Factions:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Factions which may use this attack. Separate with $'s<br>Example: Liao$Davion$<br>Only add $ if you have more then one faction listed.</html>");
        BaseTextField.setName("LegalAttackFactions");
        factionsBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        factionsBox.add(new JLabel("Illegal Attack Factions:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Factions which may NOT use this attack. Separate with $'s.<br>Only used if Legal is blank.<br>Example: Pirates$Bandits<br>Only add $ if you have more then one faction listed.</html>");
        BaseTextField.setName("IllegalAttackFactions");
        factionsBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        factionsBox.add(new JLabel("Legal Defense Factions:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Factions which may defend against this attack. Separate with $'s<br>Example: Marik$Kurita$Steiner<br>Only add $ if you have more then one faction listed.</html>");
        BaseTextField.setName("LegalDefendFactions");
        factionsBox.add(BaseTextField);

        BaseTextField = new JTextField(5);
        factionsBox.add(new JLabel("Illegal Defense Factions:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Factions which may NOT defend this attack. Separate with $'s.<br>Only used if Legal is blank.<br>Example: Solaris Training Company$Periphery<br>Only add $ if you have more then one faction listed.</html>");
        BaseTextField.setName("IllegalDefendFactions");
        factionsBox.add(BaseTextField);

        // finalize layout. 2 label/box pairs - 2 rows
        SpringLayoutHelper.setupSpringGrid(factionsBox, 4);

        factionsmasterBox.add(factionsBox);
        factionPanel.add(factionsmasterBox);

        /*
         * Operations Units
         *
         * Set up the Operations Unit panel, Sets the min/max BV speed Movement and
         * other types for the Units allow in an operation.
         *
         */

        // give the path panel a box layout. its going to be smaller than some, so
        // we dont need flow-nested boxes
        JPanel unitBox = new JPanel();
        JPanel masterBox = new JPanel();

        unitBox.setLayout(new BoxLayout(unitBox, BoxLayout.X_AXIS));
        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.Y_AXIS));

        JPanel attackerBox = new JPanel();
        attackerBox.setLayout(new BoxLayout(attackerBox, BoxLayout.Y_AXIS));
        JPanel defenderBox = new JPanel();
        defenderBox.setLayout(new BoxLayout(defenderBox, BoxLayout.Y_AXIS));

        JPanel spreadPanel = new JPanel(new SpringLayout());

        JPanel attackerPanel = new JPanel(new SpringLayout());
        JPanel attackerCBoxPanel = new JPanel(new SpringLayout());

        JPanel defenderPanel = new JPanel(new SpringLayout());
        JPanel defenderCBoxPanel = new JPanel(new SpringLayout());

        // universal options
        BaseCheckBox = new JCheckBox("Proto Grouping");
        BaseCheckBox.setToolTipText("<html>If enabled, protos must be moved in movement groups. For example, an<br> army w/ 4 protos will be ilelgal if they move in groups of 5</html>");
        BaseCheckBox.setName("ProtosMustbeGrouped");
        spreadPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Mul Armies Only");
        BaseCheckBox.setToolTipText("<html>If enabled players can go active without armies as they will be provided with muls.</html>");
        BaseCheckBox.setName("MULArmiesOnly");
        spreadPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Count Vehs in Spread");
        BaseCheckBox.setToolTipText("If true, vehicles are included when checking BV spreads between high/low units.");
        BaseCheckBox.setName("CountVehsForSpread");
        spreadPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Count Aeros in Spread");
        BaseCheckBox.setToolTipText("If true, aeros are included when checking BV spreads between high/low units.");
        BaseCheckBox.setName("CountAerosForSpread");
        spreadPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Count Protos in Spread");
        BaseCheckBox.setToolTipText("If true, protos are included when checking BV spreads between high/low units.");
        BaseCheckBox.setName("CountProtosForSpread");
        spreadPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Count Inf in Spread");
        BaseCheckBox.setToolTipText("If true, infantry and BA are included when checking BV spreads between high/low units.");
        BaseCheckBox.setName("CountInfForSpread");
        spreadPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Ignore pilot for BV Spread");
        BaseCheckBox.setToolTipText("If true, pilot levels will be ignored for the purposes of spread-checking");
        BaseCheckBox.setName("IgnorePilotsForBVSpread");
        spreadPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Enforce Tech Base Ratios");
        BaseCheckBox.setToolTipText("If true, limited clan tech per army will be enforced.");
        BaseCheckBox.setName("UseClanEquipmentRatios");
        spreadPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Ignore Support Unit Designation");
        BaseCheckBox.setToolTipText("<html>If true, support units will be counted as normal units for purposes of unit count.<br>If false, a support unit will not count towards it unit type count.</html>");
        BaseCheckBox.setName("CountSupportUnits");
        spreadPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Count support units in Spread");
        BaseCheckBox.setToolTipText("If true, protos are included when checking BV spreads between high/low units.");
        BaseCheckBox.setName("CountSupportUnitsForSpread");
        spreadPanel.add(BaseCheckBox);

        //spreadPanel.add(new JLabel(" "));


        BaseTextField = new JTextField(5);
        spreadPanel.add(new JLabel("Repod Omni to Base:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<HTML>String. Omni's that where used in this op are repodded<br>back to this base configuration.<br>Leave blank to disable this option </HTML>");
        BaseTextField.setName("RepodOmniUnitsToBase");
        spreadPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(spreadPanel, 2);

        // attacker box stuff
        BaseCheckBox = new JCheckBox("Allow Meks");
        BaseCheckBox.setToolTipText("Allow meks in this operation for the attacker");
        BaseCheckBox.setName("AttackerAllowedMeks");
        attackerCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Veh");
        BaseCheckBox.setToolTipText("Allow Vehciles in this operation for the attacker");
        BaseCheckBox.setName("AttackerAllowedVehs");
        attackerCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Aero");
        BaseCheckBox.setToolTipText("Allow Aeros in this operation for the attacker");
        BaseCheckBox.setName("AttackerAllowedAeros");
        attackerCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Inf");
        BaseCheckBox.setToolTipText("Allow Infantry in this operation for the attacker");
        BaseCheckBox.setName("AttackerAllowedInf");
        attackerCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Powered Inf");
        BaseCheckBox.setToolTipText("<html>Overrides Allow Infantry and allows BA and Protos.<br>Set inf allowed to False and this to True to ban<br>foot/jump/moto inf but allow BA and Protos.</html>");
        BaseCheckBox.setName("AttackerPoweredInfAllowed");
        attackerCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Standard Inf");
        BaseCheckBox.setToolTipText("<html>Overrides Allow Infantry and allows foot/moto/jump units.<br>Set inf allowed to False and this to True to BA/Protos<br>but allow foot/jump/moto inf.</html>");
        BaseCheckBox.setName("AttackerStandardInfAllowed");
        attackerCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("OmniMeks Only");
        BaseCheckBox.setToolTipText("Attackers mechs must be OmniMeks");
        BaseCheckBox.setName("AttackerOmniMeksOnly");
        attackerCBoxPanel.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(attackerCBoxPanel, 2);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max BV for the Attackers Army");
        BaseTextField.setName("MaxAttackerBV");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min BV for the Attackers Army");
        BaseTextField.setName("MinAttackerBV");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Meks:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max number of Meks in the attackers army");
        BaseTextField.setName("MaxAttackerMeks");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Meks:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min number of Meks in the attackers army");
        BaseTextField.setName("MinAttackerMeks");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Vehicles:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max number of Vehicles in the attackers army");
        BaseTextField.setName("MaxAttackerVehicles");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Vehicles:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min number of Vehicles in the attackers army");
        BaseTextField.setName("MinAttackerVehicles");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Aero:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max number of Aero in the attackers army");
        BaseTextField.setName("MaxAttackerAero");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Aero:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min number of Aero in the attackers army");
        BaseTextField.setName("MinAttackerAero");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Non-Infantry:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of Non-Infantry in the attackers army<br>Includes Meks and Vehicles</html>");
        BaseTextField.setName("MaxAttackerNonInfantry");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Non-Infantry:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of Non-Infantry in the attackers army<br>Includes Meks and Vehicles</html>");
        BaseTextField.setName("MinAttackerNonInfantry");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Infantry:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of Infantry in the attackers army<br>Includes non-Powered, BA, and ProtoMeks</html>");
        BaseTextField.setName("MaxAttackerInfantry");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Infantry:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of Infantry in the attackers army<br>Includes non-Powered, BA, and ProtoMeks</html>");
        BaseTextField.setName("MinAttackerInfantry");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Walk:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of movement points, i.e. Walking,<br>that any unit in the attackers army can have<br>to participate in this operation</html>");
        BaseTextField.setName("MinAttackerWalk");
        attackerPanel.add(BaseTextField);

        /* padding to get both jump fields on the same line. Can be replaced with content */
        attackerPanel.add(new JLabel(" "));
        attackerPanel.add(new JLabel(" "));

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Jump:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of jump points<br>that any unit in the attackers army can have<br>to participate in this operation</html>");
        BaseTextField.setName("MinAttackerJump");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Jump:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of jump points<br>that any unit in the attackers army can have<br>to participate in this operation</html>");
        BaseTextField.setName("MaxAttackerJump");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Unit Tons:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Tons any unit in the attackers army can have for this op");
        BaseTextField.setName("MaxAttackerUnitTonnage");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Unit Tons:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Tons any unit in the attackers army maybe for this op");
        BaseTextField.setName("MinAttackerUnitTonnage");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Total Tons:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Total Tonnage for an army to run this op");
        BaseTextField.setName("MaxTotalAttackerTonnage");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Total Tons:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Total Tonnage for an army to run this op");
        BaseTextField.setName("MinTotalAttackerTonnage");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Unit BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Tons any unit in the attackers army maybe for this op");
        BaseTextField.setName("MaxAttackerUnitBV");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Unit BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Tons any unit in the attackers army maybe for this op");
        BaseTextField.setName("MinAttackerUnitBV");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max BV Spread:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max BV difference between the highest and lowest counted units.<br>If % BV spread is used, this will be an added value to the calculated percent</html>");
        BaseTextField.setName("MaxAttackerUnitBVSpread");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min BV Spread:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min BV difference between the highest and lowest counted units.</html>");
        BaseTextField.setName("MinAttackerUnitBVSpread");
        attackerPanel.add(BaseTextField);

        attackerPanel.add(new JLabel("Use % BV Spread", SwingConstants.TRAILING));
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setToolTipText("<html>BV Spread will be a % of the army's BV.<br>Float value, so 0.2 = 20%</html>");
        BaseCheckBox.setName("AttackerUsePercentageBVSpread");
        attackerPanel.add(BaseCheckBox);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("BV Spread %", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("% of army's BV allowed");
        BaseTextField.setName("AttackerBVSpreadPercent");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Highest Pilot Skill:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Highest total skill, Gunnery + Piloting, that an<br>attacking Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("HighestAttackerPilotSkillTotal");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Lowest Pilot Skill:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Lowest total skill, Gunnery + Piloting, that an<br>attacking Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("LowestAttackerPilotSkillTotal");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Highest Piloting:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Highest Piloting that an<br>attacking Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("HighestAttackerPiloting");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Lowest Piloting:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Lowest Piloting that an<br>attacking Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("LowestAttackerPiloting");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Highest Gunnery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Highest Gunnery that an<br>attacking Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("HighestAttackerGunnery");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Lowest Gunnery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Lowest Gunnery that an<br>attacking Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("LowestAttackerGunnery");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Average Army Skill Max:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max Average piloting skills a attacking army can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("AttackerAverageArmySkillMax");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Average Army Skill Min:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min Average piloting skills a attacking army can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("AttackerAverageArmySkillMin");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Clantech Percent:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Minimum percent of an army that may be Clantech</html>");
        BaseTextField.setName("AttackerMinClanEquipmentPercent");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Clantech Percent:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Maximum percent of an army that may be Clantech</html>");
        BaseTextField.setName("AttackerMaxClanEquipmentPercent");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Support Units:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Minimum number of support units");
        BaseTextField.setName("MinAttackerSupportUnits");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Support Units:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Maximum number of support units");
        BaseTextField.setName("MaxAttackerSupportUnits");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Non-Support Units:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Minimum number of non-support units");
        BaseTextField.setName("MinAttackerNonSupportUnits");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Non-Support Units:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Maximum number of non-support units");
        BaseTextField.setName("MaxAttackerNonSupportUnits");
        attackerPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(attackerPanel, 4);

        BaseCheckBox = new JCheckBox("Allow Meks");
        BaseCheckBox.setToolTipText("Allow Defender to use Meks in this op");
        BaseCheckBox.setName("DefenderAllowedMeks");
        defenderCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Veh");
        BaseCheckBox.setToolTipText("Allow Defender to use Vehicles in this Op");
        BaseCheckBox.setName("DefenderAllowedVehs");
        defenderCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Aero");
        BaseCheckBox.setToolTipText("Allow Defender to use Aeros in this Op");
        BaseCheckBox.setName("DefenderAllowedAeros");
        defenderCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Inf");
        BaseCheckBox.setToolTipText("Allow Defender to use infantry in this Op");
        BaseCheckBox.setName("DefenderAllowedInf");
        defenderCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Powered Inf");
        BaseCheckBox.setToolTipText("<html>Overrides Allow Infantry and allows BA and Protos.<br>Set inf allowed to False and this to True to ban<br>foot/jump/moto inf but allow BA and Protos.</html>");
        BaseCheckBox.setName("DefenderPoweredInfAllowed");
        defenderCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Standard Inf");
        BaseCheckBox.setToolTipText("<html>Overrides Allow Infantry and allows foot/moto/jump units.<br>Set inf allowed to False and this to True to BA/Protos<br>but allow foot/jump/moto inf.</html>");
        BaseCheckBox.setName("DefenderStandardInfAllowed");
        defenderCBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("OmniMeks Only");
        BaseCheckBox.setToolTipText("Defenders mechs must be OmniMeks");
        BaseCheckBox.setName("DefenderOmniMeksOnly");
        defenderCBoxPanel.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(defenderCBoxPanel, 2);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max BV the defenders army can be for this Op");
        BaseTextField.setName("MaxDefenderBV");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min BV the defenders army can be for this Op");
        BaseTextField.setName("MinDefenderBV");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Meks:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Number of Meks a Defender my use in this op");
        BaseTextField.setName("MaxDefenderMeks");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Meks:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min number of Meks a defender must use in this op");
        BaseTextField.setName("MinDefenderMeks");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Vehicles:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Number of Vehicles a Defender my use in this op");
        BaseTextField.setName("MaxDefenderVehicles");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Vehicles:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min number of Vehicles a defender must use in this op");
        BaseTextField.setName("MinDefenderVehicles");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Aero:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Number of Aero a Defender my use in this op");
        BaseTextField.setName("MaxDefenderAero");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Aero:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min number of Aero a defender must use in this op");
        BaseTextField.setName("MinDefenderAero");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Non-Infantry:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max Number of Non-Infantry a Defender my use in this op<br>This includes Meks and Vehicles</html>");
        BaseTextField.setName("MaxDefenderNonInfantry");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Non-Infantry:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of Non-Infantry a defender must use in this op<br>This includes Meks and Vehicles</html>");
        BaseTextField.setName("MinDefenderNonInfantry");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Infantry:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max Number of Infantry a Defender my use in this op<br>This includes non-Powered Infantry, BA, and ProtoMeks</html>");
        BaseTextField.setName("MaxDefenderInfantry");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Infantry:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of Infantry a defender must use in this op<br>This includes non-Powered Infantry, BA, and ProtoMeks</html>");
        BaseTextField.setName("MinDefenderInfantry");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Walk:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of movement points, i.e. Walking,<br>that any unit in the defenders army can have<br>to participate in this operation</html>");
        BaseTextField.setName("MinDefenderWalk");
        defenderPanel.add(BaseTextField);

        /* padding to get both jump fields on the same line. Can be replaced with content */
        defenderPanel.add(new JLabel(" "));
        defenderPanel.add(new JLabel(" "));

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Jump:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of jump points<br>that any unit in the defenders army can have<br>to participate in this operation</html>");
        BaseTextField.setName("MinDefenderJump");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Jump:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of jump points<br>that any unit in the defenders army can have<br>to participate in this operation</html>");
        BaseTextField.setName("MaxDefenderJump");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Unit Tons:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Tonnage any unit in the defenders army may have for this op");
        BaseTextField.setName("MaxDefenderUnitTonnage");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Unit Tons", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min tonnage any unit in the defeneders army may have for this op");
        BaseTextField.setName("MinDefenderUnitTonnage");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Total Tons", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Tonnage for the defenders army for this op");
        BaseTextField.setName("MaxTotalDefenderTonnage");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Total Tons", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Total the defenders army may have for this op");
        BaseTextField.setName("MinTotalDefenderTonnage");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Unit BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Tons any unit in the attackers army maybe for this op");
        BaseTextField.setName("MaxDefenderUnitBV");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Unit BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Tons any unit in the attackers army maybe for this op");
        BaseTextField.setName("MinDefenderUnitBV");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max BV Spread:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max BV difference between the highest and lowest counted units.<br>If % BV spread is used, this will be an added value to the calculated percent</html>");
        BaseTextField.setName("MaxDefenderUnitBVSpread");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min BV Spread:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min BV difference between the highest and lowest counted units.</html>");
        BaseTextField.setName("MinDefenderUnitBVSpread");
        defenderPanel.add(BaseTextField);

        defenderPanel.add(new JLabel("Use % BV Spread", SwingConstants.TRAILING));
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setToolTipText("BV Spread will be a % of the army's BV");
        BaseCheckBox.setName("DefenderUsePercentBVSpread");
        defenderPanel.add(BaseCheckBox);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("BV Spread %", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("% of army's BV allowed.  Float value, so 0.20 = 20%");
        BaseTextField.setName("DefenderBVSpreadPercent");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Highest Pilot Skill:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Highest total skill, Gunnery + Piloting, that a<br>defending Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("HighestDefenderPilotSkillTotal");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Lowest Pilot Skill:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Lowest total skill, Gunnery + Piloting, that a<br>defending Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("LowestDefenderPilotSkillTotal");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Highest Piloting:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Highest Piloting that a<br>defending Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("HighestDefenderPiloting");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Lowest Piloting:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Lowest Piloting that a<br>defending Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("LowestDefenderPiloting");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Highest Gunnery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Highest Gunnery that a<br>defending Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("HighestDefenderGunnery");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Lowest Gunnery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Lowest Gunnery that a<br>defending Units pilot can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("LowestDefenderGunnery");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Average Army Skill Max:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max Average piloting skills a defending army can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("DefenderAverageArmySkillMax");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Average Army Skill Min:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min Average piloting skills a defending army can have<br>NOTE: Units will not be checked if they<br>are not counted for the BV Spread</html>");
        BaseTextField.setName("DefenderAverageArmySkillMin");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Clantech Percent:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Minimum percent of an army that may be Clantech</html>");
        BaseTextField.setName("DefenderMinClanEquipmentPercent");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Clantech Percent:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Maximum percent of an army that may be Clantech</html>");
        BaseTextField.setName("DefenderMaxClanEquipmentPercent");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Support Units:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Minimum number of support units");
        BaseTextField.setName("MinDefenderSupportUnits");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Support Units:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Maximum number of support units");
        BaseTextField.setName("MaxDefenderSupportUnits");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Non-Support Units:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Minimum number of non-support units");
        BaseTextField.setName("MinDefenderNonSupportUnits");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Non-Support Units:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Maximum number of non-support units");
        BaseTextField.setName("MaxDefenderNonSupportUnits");
        defenderPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(defenderPanel, 4);

        attackerBox.add(new JLabel("Attacker"));
        attackerBox.add(attackerCBoxPanel);
        attackerBox.add(attackerPanel);

        attackerBox.setBorder(BorderFactory.createLineBorder(Color.black));

        defenderBox.add(new JLabel("Defender"));
        defenderBox.add(defenderCBoxPanel);
        defenderBox.add(defenderPanel);

        defenderBox.setBorder(BorderFactory.createLineBorder(Color.black));

        unitBox.add(attackerBox);
        unitBox.add(defenderBox);

        masterBox.add(spreadPanel);
        masterBox.add(unitBox);

        unitsPanel.add(masterBox);

        /*
         * Operations Cost
         *
         * Set up the Operations Cost panel,Set what it costs to attack or defend an op
         * in Money Flu and/or RP
         *
         */

        JPanel costBox = new JPanel();
        costBox.setLayout(new BoxLayout(costBox, BoxLayout.X_AXIS));

        attackerBox = new JPanel();
        attackerBox.setLayout(new BoxLayout(attackerBox, BoxLayout.Y_AXIS));
        defenderBox = new JPanel();
        defenderBox.setLayout(new BoxLayout(defenderBox, BoxLayout.Y_AXIS));

        attackerPanel = new JPanel(new SpringLayout());
        defenderPanel = new JPanel(new SpringLayout());

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Cost Money:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("What it costs the attacker, in money, to preform this op");
        BaseTextField.setName("AttackerCostMoney");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Cost Flu:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("What it costs the attacker, in flu, to preform this op");
        BaseTextField.setName("AttackerCostInfluence");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Cost Rewards:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("What it costs the attacker, in reward points, to preform this op");
        BaseTextField.setName("AttackerCostReward");
        attackerPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(attackerPanel, 3, 2);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Cost Money:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("What it costs the defender, in money, to preform this op");
        BaseTextField.setName("DefenderCostMoney");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Cost Flu:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("What it costs the defender, in flu, to preform this op");
        BaseTextField.setName("DefenderCostInfluence");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Cost Rewards:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("What it costs the defender, in reward points, to preform this op");
        BaseTextField.setName("DefenderCostReward");
        defenderPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(defenderPanel, 3, 2);

        attackerBox.add(new JLabel("Attacker"));
        attackerBox.add(attackerPanel);

        attackerBox.setBorder(BorderFactory.createLineBorder(Color.black));

        defenderBox.add(new JLabel("Defender"));
        defenderBox.add(defenderPanel);

        defenderBox.setBorder(BorderFactory.createLineBorder(Color.black));

        costBox.add(attackerBox);
        costBox.add(defenderBox);

        costsPanel.add(costBox);

        /*
         * Player Properties
         *
         * Set up the Player Properties panel,mins/maxes for players if
         * they can attack/defend an op
         *
         */

        JPanel playerPropertiesBox = new JPanel();
        playerPropertiesBox.setLayout(new BoxLayout(playerPropertiesBox, BoxLayout.X_AXIS));

        attackerBox = new JPanel();
        attackerBox.setLayout(new BoxLayout(attackerBox, BoxLayout.Y_AXIS));
        defenderBox = new JPanel();
        defenderBox.setLayout(new BoxLayout(defenderBox, BoxLayout.Y_AXIS));

        attackerPanel = new JPanel(new SpringLayout());
        defenderPanel = new JPanel(new SpringLayout());

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Rating:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Rating Attacker must have for this op");
        BaseTextField.setName("MinAttackerRating");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Rating:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Rating Attacker must have for this op");
        BaseTextField.setName("MaxAttackerRating");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Exp Attacker must have for this op");
        BaseTextField.setName("MinAttackerXP");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Exp Attacker must have for this op");
        BaseTextField.setName("MaxAttackerXP");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Games:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Games Attacker must have played for this op");
        BaseTextField.setName("MinAttackerGamesPlayed");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max Games:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Games Attacker must have played for this op");
        BaseTextField.setName("MaxAttackerGamesPlayed");
        attackerPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(attackerPanel, 6, 2);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Rating:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Rating Defender must have for this op");
        BaseTextField.setName("MinDefenderRating");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Rating:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Rating Defender must have for this op");
        BaseTextField.setName("MaxDefenderRating");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Exp Defender must have for this op");
        BaseTextField.setName("MinDefenderXP");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Exp Defender must have for this op");
        BaseTextField.setName("MaxDefenderXP");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min Games:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Min Games Defender must have played for this op");
        BaseTextField.setName("MinDefenderGamesPlayed");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max Games:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max Games Defender must have played for this op");
        BaseTextField.setName("MaxDefenderGamesPlayed");
        defenderPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(defenderPanel, 6, 2);

        attackerBox.add(new JLabel("Attacker"));
        attackerBox.add(attackerPanel);

        attackerBox.setBorder(BorderFactory.createLineBorder(Color.black));

        defenderBox.add(new JLabel("Defender"));
        defenderBox.add(defenderPanel);

        defenderBox.setBorder(BorderFactory.createLineBorder(Color.black));

        playerPropertiesBox.add(attackerBox);
        playerPropertiesBox.add(defenderBox);

        playerPropertiesPanel.add(playerPropertiesBox);

        /*
         * Scenario Addons
         *
         * Set up the Scenario Addons panel,Arty/mines anything given to an
         * attacker/defender besides their own units
         *
         */

        Dimension boxSize = new Dimension(199, 110);
        masterBox = new JPanel();
        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.Y_AXIS));

        JPanel scenarioBox = new JPanel();
        scenarioBox.setLayout(new BoxLayout(scenarioBox, BoxLayout.X_AXIS));

        JPanel scenarioBox2 = new JPanel();
        scenarioBox2.setLayout(new BoxLayout(scenarioBox2, BoxLayout.X_AXIS));

        JPanel scenarioBox3 = new JPanel();
        scenarioBox3.setLayout(new BoxLayout(scenarioBox3, BoxLayout.X_AXIS));

        JPanel scenarioBox4 = new JPanel();
        scenarioBox4.setLayout(new BoxLayout(scenarioBox4, BoxLayout.X_AXIS));

        JPanel scenarioBox5 = new JPanel();
        scenarioBox5.setLayout(new BoxLayout(scenarioBox5, BoxLayout.X_AXIS));

        attackerBox = new JPanel();
        attackerBox.setLayout(new BoxLayout(attackerBox, BoxLayout.Y_AXIS));
        defenderBox = new JPanel();
        defenderBox.setLayout(new BoxLayout(defenderBox, BoxLayout.Y_AXIS));
        attackerPanel = new JPanel(new SpringLayout());
        defenderPanel = new JPanel(new SpringLayout());
        attackerPanel.setPreferredSize(boxSize);
        attackerPanel.setMaximumSize(boxSize);
        attackerPanel.setMinimumSize(boxSize);
        defenderPanel.setPreferredSize(boxSize);
        defenderPanel.setMaximumSize(boxSize);
        defenderPanel.setMinimumSize(boxSize);

        JPanel attackerTurretBox = new JPanel();
        attackerTurretBox.setLayout(new BoxLayout(attackerTurretBox, BoxLayout.Y_AXIS));
        JPanel defenderTurretBox = new JPanel();
        defenderTurretBox.setLayout(new BoxLayout(defenderTurretBox, BoxLayout.Y_AXIS));

        JPanel attackerTurretPanel = new JPanel(new SpringLayout());
        JPanel defenderTurretPanel = new JPanel(new SpringLayout());
        attackerTurretPanel.setPreferredSize(boxSize);
        attackerTurretPanel.setMaximumSize(boxSize);
        attackerTurretPanel.setMinimumSize(boxSize);
        defenderTurretPanel.setPreferredSize(boxSize);
        defenderTurretPanel.setMaximumSize(boxSize);
        defenderTurretPanel.setMinimumSize(boxSize);

        JPanel attackerMineBox = new JPanel();

        attackerMineBox.setLayout(new BoxLayout(attackerMineBox, BoxLayout.Y_AXIS));
        JPanel defenderMineBox = new JPanel();

        defenderMineBox.setLayout(new BoxLayout(defenderMineBox, BoxLayout.Y_AXIS));

        JPanel BotsBox = new JPanel();
        BotsBox.setLayout(new BoxLayout(BotsBox, BoxLayout.Y_AXIS));
        JPanel BotsPanel = new JPanel(new SpringLayout());

        JPanel attackerMinePanel = new JPanel(new SpringLayout());
        JPanel defenderMinePanel = new JPanel(new SpringLayout());
        attackerMinePanel.setPreferredSize(boxSize);
        attackerMinePanel.setMaximumSize(boxSize);
        attackerMinePanel.setMinimumSize(boxSize);
        defenderMinePanel.setPreferredSize(boxSize);
        defenderMinePanel.setMaximumSize(boxSize);
        defenderMinePanel.setMinimumSize(boxSize);

        JPanel attackerMULBox = new JPanel();
        attackerMULBox.setLayout(new BoxLayout(attackerMULBox, BoxLayout.Y_AXIS));

        JPanel defenderMULBox = new JPanel();
        defenderMULBox.setLayout(new BoxLayout(defenderMULBox, BoxLayout.Y_AXIS));

        JPanel attackerMULPanel = new JPanel(new SpringLayout());
        JPanel defenderMULPanel = new JPanel(new SpringLayout());
        // attackerMULPanel.setPreferredSize(boxSize);
        // attackerMULPanel.setMaximumSize(boxSize);
        // attackerMULPanel.setMinimumSize(boxSize);
        // defenderMULPanel.setPreferredSize(boxSize);
        // defenderMULPanel.setMaximumSize(boxSize);
        // defenderMULPanel.setMinimumSize(boxSize);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Flat Artillery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of BV to shift attacker");
        BaseTextField.setName("AttackerFlatArtilleryModifier");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Perecent Artillery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>% adjustment to relative assignment BV<br>NOTE: this is a double field and percent<br>1 would be normal bv .75 would be 75% of normal<br>1.15 would be a 15% increase of normal bv</html>");
        BaseTextField.setName("AttackerPercentArtilleryModifier");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min BV Artillery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min BV to be presumed for attack/defender player<br>hen assigning autoartillery. Floor for modifiers.</html>");
        BaseTextField.setName("MinAttackerArtilleryBV");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max BV Artillery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max BV to be presumed for attack/def player when<br>assigning artillery. Ceiling for modifiers.</html>");
        BaseTextField.setName("MaxAttackerArtilleryBV");
        attackerPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(attackerPanel, 4, 2);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Flat Artillery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of BV to shift defender");
        BaseTextField.setName("DefenderFlatArtilleryModifier");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Perecent Artillery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("% adjustment to relative assignment BV");
        BaseTextField.setName("DefenderPercentArtilleryModifier");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Min BV Artillery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min BV to be presumed for defend/defender player<br>hen assigning autoartillery. Floor for modifiers.</html>");
        BaseTextField.setName("MinDefenderArtilleryBV");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Max BV Artillery:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max BV to be presumed for defend/def player when<br>assigning artillery. Ceiling for modifiers.</html>");
        BaseTextField.setName("MaxDefenderArtilleryBV");
        defenderPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(defenderPanel, 4, 2);

        attackerBox.add(new JLabel("Attacker"));

        BaseCheckBox = new JCheckBox("Artillery");
        BaseCheckBox.setToolTipText("<html>If true, attack players will receive<br>normal autoartillery. Set false to make the<br>grant lopsided.</html>");
        BaseCheckBox.setName("AttackerReceivesAutoArtillery");
        attackerBox.add(BaseCheckBox);
        attackerBox.add(attackerPanel);

        attackerBox.setBorder(BorderFactory.createLineBorder(Color.black));

        defenderBox.add(new JLabel("Defender"));

        BaseCheckBox = new JCheckBox("Artillery");
        BaseCheckBox.setToolTipText("<html>If true, defend players will receive<br>normal autoartillery. Set false to make the<br>grant lopsided.</html>");
        BaseCheckBox.setName("DefenderReceivesAutoArtillery");
        defenderBox.add(BaseCheckBox);
        defenderBox.add(defenderPanel);

        defenderBox.setBorder(BorderFactory.createLineBorder(Color.black));

        scenarioBox.add(attackerBox);
        scenarioBox.add(defenderBox);

        BaseTextField = new JTextField(5);
        attackerTurretPanel.add(new JLabel("Flat Turret:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of BV to shift attacker");
        BaseTextField.setName("AttackerFlatGunEmplacementModifier");
        attackerTurretPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerTurretPanel.add(new JLabel("Perecent Turret:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("% adjustment to relative assignment BV");
        BaseTextField.setName("AttackerPercentGunEmplacementModifier");
        attackerTurretPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerTurretPanel.add(new JLabel("Min BV Turret:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min BV to be presumed for attack/defender player<br>when assigning gun emplacements. Floor for modifiers.</html>");
        BaseTextField.setName("MinAttackerGunEmplacementBV");
        attackerTurretPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerTurretPanel.add(new JLabel("Max BV Turret:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max BV to be presumed for attack/def player when<br>assigning gun emplacements. Ceiling for modifiers.</html>");
        BaseTextField.setName("MaxAttackerGunEmplacementBV");
        attackerTurretPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(attackerTurretPanel, 4, 2);

        BaseTextField = new JTextField(5);
        defenderTurretPanel.add(new JLabel("Flat Turret:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of BV to shift defender");
        BaseTextField.setName("DefenderFlatGunEmplacementModifier");
        defenderTurretPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderTurretPanel.add(new JLabel("Perecent Turret:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>% adjustment to relative assignment BV<br>NOTE: this is a double field and percent<br>1 would be normal bv .75 would be 75% of normal<br>1.15 would be a 15% increase of normal bv</html>");
        BaseTextField.setName("DefenderPercentGunEmplacementModifier");
        defenderTurretPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderTurretPanel.add(new JLabel("Min BV Turret:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min BV to be presumed for defend/defender player<br>when assigning gun emplacements. Floor for modifiers.</html>");
        BaseTextField.setName("MinDefenderGunEmplacementBV");
        defenderTurretPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderTurretPanel.add(new JLabel("Max BV Turret:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max BV to be presumed for defend/def player when<br>assigning gun emplacements. Ceiling for modifiers.</html>");
        BaseTextField.setName("MaxDefenderGunEmplacementBV");
        defenderTurretPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(defenderTurretPanel, 4, 2);

        attackerTurretBox.add(new JLabel("Attacker"));

        BaseCheckBox = new JCheckBox("Turret");
        BaseCheckBox.setToolTipText("<html>If true, attack players will receive<br>normal gun emplacements. Set false to make the<br>grant lopsided.</html>");
        BaseCheckBox.setName("AttackerReceivesGunEmplacement");
        attackerTurretBox.add(BaseCheckBox);
        attackerTurretBox.add(attackerTurretPanel);

        attackerTurretBox.setBorder(BorderFactory.createLineBorder(Color.black));

        defenderTurretBox.add(new JLabel("Defender"));

        BaseCheckBox = new JCheckBox("Turret");
        BaseCheckBox.setToolTipText("<html>If true, defend players will receive<br>normal gun emplacements. Set false to make the<br>grant lopsided.</html>");
        BaseCheckBox.setName("DefenderReceivesGunEmplacement");
        defenderTurretBox.add(BaseCheckBox);
        defenderTurretBox.add(defenderTurretPanel);

        defenderTurretBox.setBorder(BorderFactory.createLineBorder(Color.black));

        scenarioBox2.add(attackerTurretBox);
        scenarioBox2.add(defenderTurretBox);

        BaseTextField = new JTextField(5);
        attackerMinePanel.add(new JLabel("BV Per Conventional:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Set the BV amount for 1 conventional mine i.e set to 100<br>and the total of both armies bv is 10k you get 100 mines</html>");
        BaseTextField.setName("AttackerBVPerConventional");
        attackerMinePanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMinePanel.add(new JLabel("Ton Per Conventional:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Set the Ton amount for 1 conventional mine i.e set to 100<br>and the total of both armies ton is 500 you get 5 mines</html>");
        BaseTextField.setName("AttackerTonPerConventional");
        attackerMinePanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMinePanel.add(new JLabel("BV Per Vibra:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Set the BV amount for 1 vibra mine i.e set to 100<br>and the total of both armies bv is 10k you get 100 mines</html>");
        BaseTextField.setName("AttackerBVPerVibra");
        attackerMinePanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMinePanel.add(new JLabel("Ton Per Vibra:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Set the Ton amount for 1 vibra mine i.e set to 100<br>and the total of both armies ton is 500 you get 5 mines</html>");
        BaseTextField.setName("AttackerTonPerVibra");
        attackerMinePanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(attackerMinePanel, 4, 2);

        BaseTextField = new JTextField(5);
        defenderMinePanel.add(new JLabel("BV Per Conventional:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Set the BV amount for 1 conventional mine i.e set to 100<br>and the total of both armies bv is 10k you get 100 mines</html>");
        BaseTextField.setName("DefenderBVPerConventional");
        defenderMinePanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMinePanel.add(new JLabel("Ton Per Conventional:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Set the Ton amount for 1 conventional mine i.e set to 100<br>and the total of both armies ton is 500 you get 5 mines</html>");
        BaseTextField.setName("DefenderTonPerConventional");
        defenderMinePanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMinePanel.add(new JLabel("BV Per Vibra:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Set the BV amount for 1 vibra mine i.e set to 100<br>and the total of both armies bv is 10k you get 100 mines</html>");
        BaseTextField.setName("DefenderBVPerVibra");
        defenderMinePanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMinePanel.add(new JLabel("Ton Per Vibra:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Set the Ton amount for 1 vibra mine i.e set to 100<br>and the total of both armies ton is 500 you get 5 mines</html>");
        BaseTextField.setName("DefenderTonPerVibra");
        defenderMinePanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(defenderMinePanel, 4, 2);

        attackerMineBox.add(new JLabel("Attacker"));

        BaseCheckBox = new JCheckBox("Mine");
        BaseCheckBox.setToolTipText("<html>If true, attacking players will receive mines.<br>Set false to make the grant lopsided.</html>");
        BaseCheckBox.setName("AttackerReceivesMines");
        attackerMineBox.add(BaseCheckBox);
        attackerMineBox.add(attackerMinePanel);

        attackerMineBox.setBorder(BorderFactory.createLineBorder(Color.black));

        defenderMineBox.add(new JLabel("Defender"));

        BaseCheckBox = new JCheckBox("Mine");
        BaseCheckBox.setToolTipText("<html>If true, defending players will receive mines.<br>Set false to make the grant lopsided.</html>");
        BaseCheckBox.setName("DefenderReceivesMines");
        defenderMineBox.add(BaseCheckBox);
        defenderMineBox.add(defenderMinePanel);

        defenderMineBox.setBorder(BorderFactory.createLineBorder(Color.black));

        scenarioBox3.add(attackerMineBox);
        scenarioBox3.add(defenderMineBox);

        BotsBox.add(new JLabel("Bots"));
        BotsBox.setBorder(BorderFactory.createLineBorder(Color.black));

        BaseCheckBox = new JCheckBox("Bots Control all Support Units");
        BaseCheckBox.setToolTipText("<html>If true any support units giving to the task" + "<br>are given to a bot to be controlled by instead." + "<br>this includes arty gun emplacements and mines" + "<br>all players will be given a bot and any support" + "<br>that would have gone to that player will goto the bot" + "<br>instead." + "<br>NOTE: bots will fire upon all players and other bots" + "<br>if not on the same team!</html>");
        BaseCheckBox.setName("BotControlsAll");
        BotsPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Bots are all on the same team.");
        BaseCheckBox.setToolTipText("<html>If set all bots will be added to the same team" + "<br>This way all bots will attack the players and not" + "<br>other bots. Bots unite!</html>");
        BaseCheckBox.setName("BotsAllOnSameTeam");
        BotsPanel.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(BotsPanel, 2);

        BotsBox.add(BotsPanel);

        scenarioBox4.add(BotsBox);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Min Armies:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL armies a Defender can receive</html>");
        BaseTextField.setName("MinDefenderMulArmies");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Max Armies:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL armies a Defender can receive</html>");
        BaseTextField.setName("MaxDefenderMulArmies");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Mul Army List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("DefenderMulArmyList");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Min Meks:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL Meks a Defender can receive</html>");
        BaseTextField.setName("MinDefenderMulMeks");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Max Meks:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL Meks a Defender can receive</html>");
        BaseTextField.setName("MaxDefenderMulMeks");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Mul Mek List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("DefenderMulMekList");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Min Vee:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL Vee a Defender can receive</html>");
        BaseTextField.setName("MinDefenderMulVehicles");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Max Vee:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL vee a Defender can receive</html>");
        BaseTextField.setName("MaxDefenderMulVehicles");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Mul Vee List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("DefenderMulVehicleList");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Min Inf:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL Infantry a Defender can receive</html>");
        BaseTextField.setName("MinDefenderMulInf");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Max Inf:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL Infantry a Defender can receive</html>");
        BaseTextField.setName("MaxDefenderMulInf");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Mul Inf List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("DefenderMulInfList");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Min BA:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL BA a Defender can receive</html>");
        BaseTextField.setName("MinDefenderMulBA");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Max BA:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL BA a Defender can receive</html>");
        BaseTextField.setName("MaxDefenderMulBA");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Mul BA List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("DefenderMulBAList");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Min Aero:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL Aero a Defender can receive</html>");
        BaseTextField.setName("MinDefenderMulAero");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Max Aero:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL Aero a Defender can receive</html>");
        BaseTextField.setName("MaxDefenderMulAero");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Mul Army List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("DefenderMulAeroList");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Min Proto:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL Proto a Defender can receive</html>");
        BaseTextField.setName("MinDefenderMulProto");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Max Proto:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL Proto a Defender can receive</html>");
        BaseTextField.setName("MaxDefenderMulProto");
        defenderMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderMULPanel.add(new JLabel("Mul Proto List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("DefenderMulProtoList");
        defenderMULPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(defenderMULPanel, 6);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Min Armies:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL armies a Attacker can receive</html>");
        BaseTextField.setName("MinAttackerMulArmies");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Max Armies:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL armies a Attacker can receive</html>");
        BaseTextField.setName("MaxAttackerMulArmies");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Mul Army List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("AttackerMulArmyList");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Min Meks:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL Meks a Attacker can receive</html>");
        BaseTextField.setName("MinAttackerMulMeks");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Max Meks:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL Meks a Attacker can receive</html>");
        BaseTextField.setName("MaxAttackerMulMeks");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Mul Mek List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("AttackerMulMekList");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Min Vee:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL Vee a Attacker can receive</html>");
        BaseTextField.setName("MinAttackerMulVehicles");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Max Vee:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL vee a Attacker can receive</html>");
        BaseTextField.setName("MaxAttackerMulVehicles");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Mul Vee List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("AttackerMulVehicleList");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Min Inf:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL Infantry a Attacker can receive</html>");
        BaseTextField.setName("MinAttackerMulInf");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Max Inf:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL Infantry a Attacker can receive</html>");
        BaseTextField.setName("MaxAttackerMulInf");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Mul Inf List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("AttackerMulInfList");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Min BA:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL BA a Attacker can receive</html>");
        BaseTextField.setName("MinAttackerMulBA");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Max BA:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL BA a Attacker can receive</html>");
        BaseTextField.setName("MaxAttackerMulBA");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Mul BA List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("AttackerMulBAList");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Min Aero:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL Aero a Attacker can receive</html>");
        BaseTextField.setName("MinAttackerMulAero");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Max Aero:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL Aero a Attacker can receive</html>");
        BaseTextField.setName("MaxAttackerMulAero");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Mul Army List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("AttackerMulAeroList");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Min Proto:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Min number of MUL Proto a Attacker can receive</html>");
        BaseTextField.setName("MinAttackerMulProto");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Max Proto:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Max number of MUL Proto a Attacker can receive</html>");
        BaseTextField.setName("MaxAttackerMulProto");
        attackerMULPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerMULPanel.add(new JLabel("Mul Proto List:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>List of the MUL files to choose from separated by ; <br>These files exist in the servers data\\armies folder</html>");
        BaseTextField.setName("AttackerMulProtoList");
        attackerMULPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(attackerMULPanel, 6);

        attackerMULBox.add(new JLabel("Attacker"));

        BaseCheckBox = new JCheckBox("MUL Armies");
        BaseCheckBox.setToolTipText("<html>If true, attacking players will receive Mul Armies.</html>");
        BaseCheckBox.setName("AttackerReceivesMULArmy");
        attackerMULBox.add(BaseCheckBox);
        attackerMULBox.add(attackerMULPanel);

        attackerMULBox.setBorder(BorderFactory.createLineBorder(Color.black));

        defenderMULBox.add(new JLabel("Defender"));

        BaseCheckBox = new JCheckBox("MUL Armies");
        BaseCheckBox.setToolTipText("<html>If true, defending players will receive Mul Armies.</html>");
        BaseCheckBox.setName("DefenderReceivesMULArmy");
        defenderMULBox.add(BaseCheckBox);
        defenderMULBox.add(defenderMULPanel);

        defenderMULBox.setBorder(BorderFactory.createLineBorder(Color.black));

        scenarioBox5.add(attackerMULBox);
        scenarioBox5.add(defenderMULBox);

        JPanel groupBox1 = new JPanel();
        groupBox1.setLayout(new BoxLayout(groupBox1, BoxLayout.X_AXIS));

        JPanel groupBox2 = new JPanel();
        groupBox2.setLayout(new BoxLayout(groupBox2, BoxLayout.X_AXIS));

        JPanel groupBox3 = new JPanel();
        groupBox3.setLayout(new BoxLayout(groupBox3, BoxLayout.X_AXIS));

        groupBox1.add(scenarioBox);
        groupBox1.add(scenarioBox2);
        groupBox2.add(scenarioBox3);
        groupBox3.add(scenarioBox5);

        masterBox.add(groupBox1);
        masterBox.add(groupBox2);
        masterBox.add(groupBox3);
        masterBox.add(scenarioBox4);

        scenarioPanel.add(masterBox);

        /*
         * Operations Results
         *
         * Set up the Operations results panel,who gets payed what and how much
         *
         */

        JPanel opResultsBox = new JPanel();
        opResultsBox.setLayout(new BoxLayout(opResultsBox, BoxLayout.X_AXIS));

        masterBox = new JPanel();
        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.Y_AXIS));

        attackerBox = new JPanel();
        attackerBox.setLayout(new BoxLayout(attackerBox, BoxLayout.Y_AXIS));
        //attackerBox.setPreferredSize(new Dimension(350, 260));
        attackerBox.setMinimumSize(new Dimension(350, 260));
        //attackerBox.setMaximumSize(new Dimension(350, 260));

        defenderBox = new JPanel();
        defenderBox.setLayout(new BoxLayout(defenderBox, BoxLayout.Y_AXIS));
        //defenderBox.setPreferredSize(new Dimension(350, 260));
        defenderBox.setMinimumSize(new Dimension(350, 260));
        //defenderBox.setMaximumSize(new Dimension(350, 260));

        JPanel outcomeBox = new JPanel();
        outcomeBox.setLayout(new BoxLayout(outcomeBox, BoxLayout.Y_AXIS));
        //outcomeBox.setPreferredSize(new Dimension(697, 100));
        outcomeBox.setMinimumSize(new Dimension(697, 100));
        //outcomeBox.setMaximumSize(new Dimension(697, 100));

        JPanel penaltyBox = new JPanel();
        penaltyBox.setLayout(new BoxLayout(penaltyBox, BoxLayout.Y_AXIS));
        penaltyBox.setPreferredSize(new Dimension(700, 50));
        penaltyBox.setMinimumSize(new Dimension(700, 50));
        //penaltyBox.setMaximumSize(new Dimension(700, 50));

        JPanel fleeingBox = new JPanel();
        Dimension dim = new Dimension(700, 170);
        fleeingBox.setLayout(new BoxLayout(fleeingBox, BoxLayout.Y_AXIS));
        fleeingBox.setPreferredSize(dim);
        fleeingBox.setMinimumSize(dim);
        //fleeingBox.setMaximumSize(dim);

        attackerPanel = new JPanel(new SpringLayout());
        defenderPanel = new JPanel(new SpringLayout());

        JPanel outcomePanel = new JPanel(new SpringLayout());
        JPanel penaltyPanel = new JPanel(new SpringLayout());
        JPanel fleeingPanel = new JPanel(new SpringLayout());

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Money Paid:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat amount attacker is paid for participating");
        BaseTextField.setName("BaseAttackerPayCBills");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Flu Paid:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat amount attacker is paid for participating");
        BaseTextField.setName("BaseAttackerPayInfluence");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Exp Paid:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat amount attacker is paid for participating");
        BaseTextField.setName("BaseAttackerPayExperience");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Money per BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>1 CBill added to Base pay for every complete<br>increment. ie - if 2, will add BV/2 Cbills to pay</html>");
        BaseTextField.setName("AttackerPayBVforCBill");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Flu per BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>1 flu added to Base pay for every complete<br>increment. ie - if 2, will add BV/2 flu to pay</html>");
        BaseTextField.setName("AttackerPayBVforInfluence");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Exp per BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>1 Exp added to Base pay for every complete<br>increment. ie - if 2, will add BV/2 Exp to pay</html>");
        BaseTextField.setName("AttackerPayBVforExperience");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("RP per BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>1 RP added to Base pay for every complete<br>increment. ie - if 2, will add BV/2 RP to pay</html>");
        BaseTextField.setName("AttackerPayBVforRP");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Win Money:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat boost to pay for winning attack");
        BaseTextField.setName("AttackerWinModifierCBillsFlat");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Loss Money:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat drop in pay for losing attack");
        BaseTextField.setName("AttackerLossModifierCBillsFlat");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Win %Mod Money:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified upward by this percent.</html>");
        BaseTextField.setName("AttackerWinModifierCBillsPercent");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Loss %Mod Money:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified downward by this percent.</html>");
        BaseTextField.setName("AttackerLossModifierCBillsPercent");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Win Flu:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat boost to pay for winning attack");
        BaseTextField.setName("AttackerWinModifierInfluenceFlat");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Loss Flu:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat drop in pay for losing attack");
        BaseTextField.setName("AttackerLossModifierInfluenceFlat");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Win %Mod Flu:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified upward by this percent.</html>");
        BaseTextField.setName("AttackerWinModifierInfluencePercent");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Loss %Mod Flu:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified downward by this percent.</html>");
        BaseTextField.setName("AttackerLossModifierInfluencePercent");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Win Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat boost to pay for winning attack");
        BaseTextField.setName("AttackerWinModifierExperienceFlat");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Loss Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat drop in pay for losing attack");
        BaseTextField.setName("AttackerLossModifierExperienceFlat");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Win %Mod Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified upward by this percent.</html>");
        BaseTextField.setName("AttackerWinModifierExperiencePercent");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Loss %Mod Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified downward by this percent.</html>");
        BaseTextField.setName("AttackerLossModifierExperiencePercent");
        attackerPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(attackerPanel, 10, 4);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Money Paid:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat amount defender is paid for participating");
        BaseTextField.setName("BaseDefenderPayCBills");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Flu Paid:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat amount defender is paid for participating");
        BaseTextField.setName("BaseDefenderPayInfluence");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Exp Paid:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat amount defender is paid for participating");
        BaseTextField.setName("BaseDefenderPayExperience");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Money per BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>1 CBill added to Base pay for every complete<br>increment. ie - if 2, will add BV/2 Cbills to pay</html>");
        BaseTextField.setName("DefenderPayBVforCBill");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Flu per BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>1 flu added to Base pay for every complete<br>increment. ie - if 2, will add BV/2 flu to pay</html>");
        BaseTextField.setName("DefenderPayBVforInfluence");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Exp per BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>1 Exp added to Base pay for every complete<br>increment. ie - if 2, will add BV/2 Exp to pay</html>");
        BaseTextField.setName("DefenderPayBVforExperience");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("RP per BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>1 RP added to Base pay for every complete<br>increment. ie - if 2, will add BV/2 RP to pay</html>");
        BaseTextField.setName("DefenderPayBVforRP");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Win Money:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat boost to pay for winning defend");
        BaseTextField.setName("DefenderWinModifierCBillsFlat");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Loss Money:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat drop in pay for losing defend");
        BaseTextField.setName("DefenderLossModifierCBillsFlat");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Win %Mod Money:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified upward by this percent.</html>");
        BaseTextField.setName("DefenderWinModifierCBillsPercent");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Loss %Mod Money:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified downward by this percent.</html>");
        BaseTextField.setName("DefenderLossModifierCBillsPercent");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Win Flu:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat boost to pay for winning defend");
        BaseTextField.setName("DefenderWinModifierInfluenceFlat");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Loss Flu:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat drop in pay for losing defend");
        BaseTextField.setName("DefenderLossModifierInfluenceFlat");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Win %Mod Flu:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified upward by this percent.</html>");
        BaseTextField.setName("DefenderWinModifierInfluencePercent");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Loss %Mod Flu:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified downward by this percent.</html>");
        BaseTextField.setName("DefenderLossModifierInfluencePercent");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Win Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat boost to pay for winning defend");
        BaseTextField.setName("DefenderWinModifierExperienceFlat");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Loss Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("flat drop in pay for losing defend");
        BaseTextField.setName("DefenderLossModifierExperienceFlat");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Win %Mod Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified upward by this percent.</html>");
        BaseTextField.setName("DefenderWinModifierExperiencePercent");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Loss %Mod Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Double value. The base amounts + bv adjustments +<br>flat adjustments are modified downward by this percent.</html>");
        BaseTextField.setName("DefenderLossModifierExperiencePercent");
        defenderPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(defenderPanel, 10, 4);

        BaseTextField = new JTextField(5);
        outcomePanel.add(new JLabel("Attacker RP:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("number of RP to give Attacker(s)");
        BaseTextField.setName("RPForAttacker");
        outcomePanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        outcomePanel.add(new JLabel("Defender RP:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("num RP to give Defender(s)");
        BaseTextField.setName("RPForDefender");
        outcomePanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        outcomePanel.add(new JLabel("Winner RP:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("num RP to give Winner");
        BaseTextField.setName("RPForWinner");
        outcomePanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        outcomePanel.add(new JLabel("Loser RP:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("num RP to give Loser");
        BaseTextField.setName("RPForLoser");
        outcomePanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(outcomePanel, 2, 4);

        outcomeBox.add(new JLabel("Reward Points"));

        BaseCheckBox = new JCheckBox("Winner Only Gets RP");
        BaseCheckBox.setToolTipText("use to restrict attack/def RP to winning players");
        BaseCheckBox.setName("OnlyGiveRPtoWinners");
        outcomeBox.add(BaseCheckBox);

        outcomeBox.add(outcomePanel);
        outcomeBox.setBorder(BorderFactory.createLineBorder(Color.black));

        BaseTextField = new JTextField(5);
        penaltyPanel.add(new JLabel("Min BV Difference:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html><b>NOTE:</b>This is a double field. 0.25 = 25%<br>This is the min difference between the starting BV of the losers army<br>and his ending BV. if the difference is less then this<br>the player will not get a full payout</html>");
        BaseTextField.setName("MinBVDifferenceForFullPay");
        penaltyPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        penaltyPanel.add(new JLabel("BV Failure Payment Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html><b>NOTE:</b>This is an Integer Field. 25 = 25%<br>This is precentage the payment is reduced to when players<br>fail to meet the min BV difference</html>");
        BaseTextField.setName("BVFailurePaymentModifier");
        penaltyPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(penaltyPanel, 4);
        penaltyBox.add(new JLabel("BV Payment Penalties"));
        penaltyBox.add(penaltyPanel);
        penaltyBox.setBorder(BorderFactory.createLineBorder(Color.black));

        BaseTextField = new JTextField(5);
        fleeingPanel.add(new JLabel("Fled Slavage Chance:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html><b>NOTE:</b>This is an Integer Field. 25 = 25%<br>Chance that a unit that flees the field is put into the salvage pool. Default 0</html>");
        BaseTextField.setName("FledUnitSalvageChance");
        fleeingPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        fleeingPanel.add(new JLabel("Fled Scrapped Chance:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html><b>NOTE:</b>This is an Integer Field. 25 = 25%<br>Chance, out of 100, that a unit that flees the field is scrapped. Default 0.</html>");
        BaseTextField.setName("FledUnitScrappedChance");
        fleeingPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        fleeingPanel.add(new JLabel("Pushed Slavage Chance:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html><b>NOTE:</b>This is an Integer Field. 25 = 25%<br>Chance that a unit that is pushed off the field is put into the salvage pool. Default 0</html>");
        BaseTextField.setName("PushedUnitSalvageChance");
        fleeingPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        fleeingPanel.add(new JLabel("Pushed Scrapped Chance:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html><b>NOTE:</b>This is an Integer Field. 25 = 25%<br>Chance, out of 100, that a unit that is pushed off the field is scrapped. Default 0.</html>");
        BaseTextField.setName("PushedUnitScrappedChance");
        fleeingPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        fleeingPanel.add(new JLabel("Engined Units Scrapped:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html><b>NOTE:</b>This is an Integer Field. 25 = 25%<br>Chance, out of 100, that an engined unit is utterly<br>destroyed while trying to be salvaged. Default 0</html>");
        BaseTextField.setName("EnginedUnitsScrappedChance");
        fleeingPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        fleeingPanel.add(new JLabel("Forced Salvaged Units Scrapped:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html><html><b>NOTE:</b>This is an Integer Field. 25 = 25%<br>Chance, out of 100, that a legged/gyroed unit is utterly destroyed while trying to be salvaged. Default 0</html>");
        BaseTextField.setName("ForcedSalvageUnitsScrappedChance");
        fleeingPanel.add(BaseTextField);

        JPanel separateSalvagePanel = new JPanel();
        separateSalvagePanel.setLayout(new BoxLayout(separateSalvagePanel, BoxLayout.Y_AXIS));
        JPanel checkBoxPanel = new JPanel();
        JPanel salvageOptionsPanel = new JPanel();

        BaseCheckBox = new JCheckBox("Use Separate Legged/Gyroed Salvage Chance");
        BaseCheckBox.setName("UseSeparateLegAndGyroScrappedChance");
        checkBoxPanel.add(BaseCheckBox);

        BaseTextField = new JTextField(5);
        salvageOptionsPanel.add(new JLabel("Gyroed Units Scrapped:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html><b>NOTE:</b>This is an Integer Field. 25 = 25%<br> Chance, out of 100, that a gyroed unit is utterly destroyed while trying to be salvaged.  Default 0</html>");
        BaseTextField.setName("GyroedUnitsScrappedChance");
        salvageOptionsPanel.add(BaseTextField);

        salvageOptionsPanel.add(new JLabel("    "));

        BaseTextField = new JTextField(5);
        salvageOptionsPanel.add(new JLabel("Legged Units Scrapped:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html><b>NOTE:</b>This is an Integer Field. 25 = 25%<br> Chance, out of 100, that a legged unit is utterly destroyed while trying to be salvaged.  Default 0</html>");
        BaseTextField.setName("LeggedUnitsScrappedChance");
        salvageOptionsPanel.add(BaseTextField);

        separateSalvagePanel.add(checkBoxPanel);
        separateSalvagePanel.add(salvageOptionsPanel);

        SpringLayoutHelper.setupSpringGrid(fleeingPanel, 4);
        fleeingBox.add(new JLabel("Fleeing Penalties"));
        fleeingBox.add(fleeingPanel);
        fleeingBox.add(separateSalvagePanel);
        fleeingBox.setBorder(BorderFactory.createLineBorder(Color.black));

        attackerBox.add(new JLabel("Attacker"));
        attackerBox.add(attackerPanel);

        attackerBox.setBorder(BorderFactory.createLineBorder(Color.black));

        defenderBox.add(new JLabel("Defender"));
        defenderBox.add(defenderPanel);

        defenderBox.setBorder(BorderFactory.createLineBorder(Color.black));

        opResultsBox.add(attackerBox);
        opResultsBox.add(defenderBox);
        masterBox.add(opResultsBox);
        masterBox.add(outcomeBox);
        masterBox.add(penaltyBox);
        masterBox.add(fleeingBox);

        opresultsPanel.add(masterBox);

        /*
         * Salvage
         *
         * Set up the Salvage panel,who gets payed what and how much
         *
         */

        JPanel salvageBox = new JPanel();
        salvageBox.setLayout(new BoxLayout(salvageBox, BoxLayout.X_AXIS));

        masterBox = new JPanel();
        masterBox.setLayout(new SpringLayout());

        attackerBox = new JPanel();
        attackerBox.setLayout(new BoxLayout(attackerBox, BoxLayout.Y_AXIS));
        defenderBox = new JPanel();
        defenderBox.setLayout(new BoxLayout(defenderBox, BoxLayout.Y_AXIS));

        attackerPanel = new JPanel(new SpringLayout());
        defenderPanel = new JPanel(new SpringLayout());

        BaseTextField = new JTextField();
        attackerPanel.add(new JLabel("Base Salvage%:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText(" attacker starting salvage rate if he wins");
        BaseTextField.setName("BaseAttackerSalvagePercent");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField();
        attackerPanel.add(new JLabel("Salvage Adjustment:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Adjustment after each salvage attempt. IE - if base<br>is 50, and adjust is 20, 2nd attempt will be 30 or 70<br>30 if salvage attempt was successful 70 if not.</html>");
        BaseTextField.setName("AttackerSalvageAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField();
        attackerPanel.add(new JLabel("BV To Boost Salvage:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>UnitBV/BVToBoost is the starting cost of salvage. If<br>set to 0, salvage will be *FREE*. IMPORTANT!</html>");
        BaseTextField.setName("BVToBoostAttackerSalvageCost");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField();
        attackerPanel.add(new JLabel("Salvage Cost Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>salvage cost multiplier. is a double. entry of .75 will<br>reduct cost by 25%, entry of 2.00 will double the cost<br>to salvage a unit.</html>");
        BaseTextField.setName("AttackerSalvageCostModifier");
        attackerPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(attackerPanel, 4, 2);

        BaseTextField = new JTextField();
        defenderPanel.add(new JLabel("Base Salvage%:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText(" defender starting salvage rate if he wins");
        BaseTextField.setName("BaseDefenderSalvagePercent");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField();
        defenderPanel.add(new JLabel("Salvage Adjustment:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Adjustment after each salvage attempt. IE - if base<br>is 50, and adjust is 20, 2nd attempt will be 30 or 70<br>30 if salvage attempt was successful 70 if not.</html>");
        BaseTextField.setName("DefenderSalvageAdjustment");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField();
        defenderPanel.add(new JLabel("BV To Boost Salvage:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>UnitBV/BVToBoost is the starting cost of salvage. If<br>set to 0, salvage will be *FREE*. IMPORTANT!</html>");
        BaseTextField.setName("BVToBoostDefenderSalvageCost");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField();
        defenderPanel.add(new JLabel("Salvage Cost Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>salvage cost multiplier. is a double. entry of .75 will<br>reduct cost by 25%, entry of 2.00 will double the cost<br>to salvage a unit.</html>");
        BaseTextField.setName("DefenderSalvageCostModifier");
        defenderPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(defenderPanel, 4, 2);

        attackerBox.add(new JLabel("Attacker"));
        attackerBox.add(attackerPanel);

        attackerBox.setBorder(BorderFactory.createLineBorder(Color.black));

        defenderBox.add(new JLabel("Defender"));
        defenderBox.add(defenderPanel);

        defenderBox.setBorder(BorderFactory.createLineBorder(Color.black));

        salvageBox.add(attackerBox);
        salvageBox.add(defenderBox);
        //masterBox.add(salvageBox);

        BaseCheckBox = new JCheckBox("Attacker Salvages Own Units");
        BaseCheckBox.setToolTipText("if true attacker gets all his salvageables");
        BaseCheckBox.setName("AttackerAlwaysSalvagesOwnUnits");
        masterBox.add(BaseCheckBox);


        BaseCheckBox = new JCheckBox("Winner Salvages Own Units");
        BaseCheckBox.setToolTipText("if true winner gets all his salvageables");
        BaseCheckBox.setName("WinnerAlwaysSalvagesOwnUnits");
        masterBox.add(BaseCheckBox);
        //masterBox.add(salvageBox);

        BaseCheckBox = new JCheckBox("Defender Salvages Own Units");
        BaseCheckBox.setToolTipText("if true defender gets all his salvageables");
        BaseCheckBox.setName("DefenderAlwaysSalvagesOwnUnits");
        masterBox.add(BaseCheckBox);
        //masterBox.add(salvageBox);

        BaseCheckBox = new JCheckBox("Support Units Go To Salvage Pool");
        BaseCheckBox.setToolTipText("if true support units, arty, mul armies, and others go to the salvage pool instead of going away");
        BaseCheckBox.setName("SupportUnitsAreSalvageable");
        masterBox.add(BaseCheckBox);
        //masterBox.add(salvageBox);

        BaseCheckBox = new JCheckBox("Destroy All Salvage");
        BaseCheckBox.setToolTipText("move salvage list to the destroyed list, effectively causing all salvage to be destroyed");
        BaseCheckBox.setName("DestroyAllSalvage");
        masterBox.add(BaseCheckBox);
        //masterBox.add(salvageBox);

        SpringLayoutHelper.setupSpringGrid(masterBox, 2);
        salvagePanel.setLayout(new VerticalLayout(5, VerticalLayout.CENTER, VerticalLayout.TOP));
        salvagePanel.add(masterBox);
        salvagePanel.add(salvageBox);

        /*
         * Newbie Operations
         *
         * Set up the Newbie Operations panel,How you want to let the noobs play
         *
         */

        JPanel newbieBox = new JPanel();
        newbieBox.setLayout(new BoxLayout(newbieBox, BoxLayout.Y_AXIS));

        JPanel newbiePanel1 = new JPanel(new SpringLayout());
        JPanel newbiePanel2 = new JPanel(new SpringLayout());
        JPanel newbiePanel3 = new JPanel(new SpringLayout());
        JPanel newbiePanel4 = new JPanel(new SpringLayout());

        BaseCheckBox = new JCheckBox("Allow SOL To Use");
        BaseCheckBox.setToolTipText("set true to allow a newbie to initiate");
        BaseCheckBox.setName("AllowSOLToUse");
        newbiePanel1.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Against SOL");
        BaseCheckBox.setToolTipText("set true to allow a player to launch this task against SOL");
        BaseCheckBox.setName("AllowAgainstSOL");
        newbiePanel1.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow For NonConquer");
        BaseCheckBox.setToolTipText("set false to forbid non-conquer players from using this attack");
        BaseCheckBox.setName("AllowNonConqToUse");
        newbiePanel1.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Against NonConquer");
        BaseCheckBox.setToolTipText("set false to forbid players from using this attack against a non-conq player");
        BaseCheckBox.setName("AllowAgainstNonConq");
        newbiePanel1.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(newbiePanel1, 1, 4);

        BaseCheckBox = new JCheckBox("SOL Pilots Gain XP");
        BaseCheckBox.setToolTipText("set true to allow SOL player's units to gain XP");
        BaseCheckBox.setName("SOLPilotsGainXP");
        newbiePanel2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("House Pilots Gain XP");
        BaseCheckBox.setToolTipText("set true to allow faction units to gain XP vs SOL");
        BaseCheckBox.setName("HousePilotsGainXP");
        newbiePanel2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("SOL Pilots Can Level");
        BaseCheckBox.setToolTipText("set true to allow SOL player's units to level after game");
        BaseCheckBox.setName("SOLPilotsCheckLevelUp");
        newbiePanel2.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("House Pilots Can Level");
        BaseCheckBox.setToolTipText("set true to allow faction units to level after game vs SOL");
        BaseCheckBox.setName("HousePilotsCheckLevelUp");
        newbiePanel2.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(newbiePanel2, 1, 4);

        BaseCheckBox = new JCheckBox("Pay all As Winners");
        BaseCheckBox.setToolTipText("<html>enable to play all players in a task involving a SOL as game <br> winners, regardless of outcome. <br><br> NOTEs: for a training task, AllowSOLtoUse and AllowAgainstSOL should be <br> set true simultaneously, otherwise player would be able to LAUNCH <br> attack, but other SOLs would not be able to defend. <br> It is NOT recommended that PayAllAsWinners be used unless NoStats <br> and NoDestruction are also enabled.</html>");
        BaseCheckBox.setName("PayAllAsWinners");
        newbiePanel3.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Count Game For Ranking");
        BaseCheckBox.setToolTipText("<html>set to true in order to stop ELO changes.</html>");
        BaseCheckBox.setName("CountGameForRanking");
        newbiePanel3.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("No Statisitcs Mode");
        BaseCheckBox.setToolTipText("<html>if enabled, stats will not be kept in games involving SOLs. <br> Pilot/Unit kills will not be counted or published.</html>");
        BaseCheckBox.setName("NoStatisticsMode");
        newbiePanel3.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("No Destruction Mode");
        BaseCheckBox.setToolTipText("<html>if enabled, no units will be destroyed and no units will<br>be salvaged (essentially sets up a sim-task).</html>");
        BaseCheckBox.setName("NoDestructionMode");
        newbiePanel3.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Pay Techs");
        BaseCheckBox.setToolTipText("set false to turn off technician payments.");
        BaseCheckBox.setName("PayTechsForGame");
        newbiePanel3.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow In Faction");
        BaseCheckBox.setToolTipText("<html>If enabled then players in the same faction<br>will be able to attack each other.</html>");
        BaseCheckBox.setName("AllowInFaction");
        newbiePanel3.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(newbiePanel3, 4);

        newbiePanel4.add(new JLabel("Count Game For Production:", SwingConstants.TRAILING));

        BaseTextField = new JTextField(5);
        BaseTextField.setToolTipText("<html>Double. Controls amount of production players generate<br>Set to 0 to disable production. Defaults to 1.0, the<br>same amount that a solitary army would generate.</html>");
        BaseTextField.setName("CountGameForProduction");
        newbiePanel4.add(BaseTextField);
        SpringLayoutHelper.setupSpringGrid(newbiePanel4, 1, 2);

        newbieBox.add(newbiePanel1);
        newbieBox.add(newbiePanel2);
        newbieBox.add(newbiePanel3);
        newbieBox.add(newbiePanel4);

        newbieOpsPanel.add(newbieBox);

        /*
         * Meta Awards
         *
         * Set up the Meta Awards panel,Set what your faction gets for this op.
         * Land, Units, Components
         *
         */

        JPanel metaBox = new JPanel();
        metaBox.setLayout(new BoxLayout(metaBox, BoxLayout.X_AXIS));

        JPanel capsBox = new JPanel();
        capsBox.setLayout(new BoxLayout(capsBox, BoxLayout.Y_AXIS));

        masterBox = new JPanel();
        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.Y_AXIS));

        JPanel destructionBox = new JPanel();
        destructionBox.setLayout(new BoxLayout(destructionBox, BoxLayout.Y_AXIS));
        destructionBox.setPreferredSize(new Dimension(746, 104));
        destructionBox.setMaximumSize(new Dimension(746, 104));
        destructionBox.setMinimumSize(new Dimension(746, 104));

        attackerBox = new JPanel();
        attackerBox.setLayout(new BoxLayout(attackerBox, BoxLayout.Y_AXIS));
        defenderBox = new JPanel();
        defenderBox.setLayout(new BoxLayout(defenderBox, BoxLayout.Y_AXIS));
        defenderBox.setPreferredSize(new Dimension(372, 204));
        defenderBox.setMaximumSize(new Dimension(372, 204));
        defenderBox.setMinimumSize(new Dimension(372, 204));

        JPanel capsPanel = new JPanel(new SpringLayout());
        JPanel destructionPanel = new JPanel(new SpringLayout());
        JPanel destructionCapPanel = new JPanel(new SpringLayout());
        attackerPanel = new JPanel(new SpringLayout());
        defenderPanel = new JPanel(new SpringLayout());

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Base Conquer:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Base points of a planet taken for winning attacker");
        BaseTextField.setName("AttackerBaseConquestAmount");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Conquer BV Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of BV needed for extra points");
        BaseTextField.setName("AttackerConquestBVAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Conquer Unit Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Number of units needed for extra points");
        BaseTextField.setName("AttackerConquestUnitAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Base Delay Amount:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Base miniticks delay caused by winning attacker");
        BaseTextField.setName("AttackerBaseDelayAmount");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Delay BV Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of BV needed for extra minitick of delay");
        BaseTextField.setName("AttackerDelayBVAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Delay Unit Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Number of units needed for extra minitick of delay");
        BaseTextField.setName("AttackerDelayUnitAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Base Components:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Base components taken by winning attacker");
        BaseTextField.setName("AttackerBasePPAmount");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Component BV Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of BV needed for extra batch of components");
        BaseTextField.setName("AttackerPPBVAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Component Unit Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Number of units needed for extra batch of components");
        BaseTextField.setName("AttackerPPUnitAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Units Taken:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("base number of units taken by winning attacker");
        BaseTextField.setName("AttackerBaseUnitsTaken");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Units BV Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("BV to take an additonal unit");
        BaseTextField.setName("AttackerUnitsBVAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Units Unit Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Units to take an additional unit (confusing!)");
        BaseTextField.setName("AttackerUnitsUnitAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Factory Units Taken:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Base number of units taken by winning attacker<br>from factories on the planet</html>");
        BaseTextField.setName("AttackerBaseFactoryUnitsTaken");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Factory Units BV Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("BV to take an additonal factory unit");
        BaseTextField.setName("AttackerFactoryUnitsBVAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Factory Units Unit Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("units to take an additional factory unit (confusing!)");
        BaseTextField.setName("AttackerFactoryUnitsUnitAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Target Op Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Op Adjustments are used to increase or decrease victory THRESHOLDS for<br>targetted long-ops. Not recommended for individual games. Generally, better<br>for use as a long-op (w/ fewer games than target) set up as a counter-assault<br>or spoling attack.</html>");
        BaseTextField.setName("AttackerTargetOpAdjustment");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Factory Units to Player Max:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Maximum number of units to award to a player<br>instead of the faction</html>");
        BaseTextField.setName("AttackerAwardFactoryUnitsTakenToPlayerMax");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max award BV percent:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Maximum BV of single unit to award to a player<br>based on a percentage of the BV of<br>their initial force.<br><br>This stops players from recovering a WarShip<br>with a single ASF.</html>");
        BaseTextField.setName("AttackerAwardFactoryUnitsTakenToPlayerBVPercent");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Max total award BV percent:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Maximum total BV of units to award to a player<br>based on a percentage of the BV of<br>their initial force.</html>");
        BaseTextField.setName("AttackerAwardFactoryUnitsTakenToPlayerMaxBVPercent");
        attackerPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(attackerPanel, 4);
        // *** START new valid factory panel
        JPanel validAttackerFactoryPanel = new JPanel(new SpringLayout());

        validAttackerFactoryPanel.add(new JLabel("Valid factory types to capture from:"));
        BaseCheckBox = new JCheckBox("Mek Factories");
        BaseCheckBox.setToolTipText("<html>Only capture components and force produce units from Mek factories</html>");
        BaseCheckBox.setName("ForceProduceAndCaptureMeks");
        validAttackerFactoryPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Vee Factories");
        BaseCheckBox.setToolTipText("<html>Only capture components and force produce units from Vee factories</html>");
        BaseCheckBox.setName("ForceProduceAndCaptureVees");
        validAttackerFactoryPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Inf Factories");
        BaseCheckBox.setToolTipText("<html>Only capture components and force produce units from Inf factories</html>");
        BaseCheckBox.setName("ForceProduceAndCaptureInfs");
        validAttackerFactoryPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("BA Factories");
        BaseCheckBox.setToolTipText("<html>Only capture components and force produce units from BA factories</html>");
        BaseCheckBox.setName("ForceProduceAndCaptureBAs");
        validAttackerFactoryPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Aero Factories");
        BaseCheckBox.setToolTipText("<html>Only capture components and force produce units from Aero factories</html>");
        BaseCheckBox.setName("ForceProduceAndCaptureAeros");
        validAttackerFactoryPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Proto Factories");
        BaseCheckBox.setToolTipText("<html>Only capture components and force produce units from Proto factories</html>");
        BaseCheckBox.setName("ForceProduceAndCaptureProtos");
        validAttackerFactoryPanel.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(validAttackerFactoryPanel, 1);
        // *** END new valid factory panel

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Base Conquer:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Base points of a planet taken for winning defender");
        BaseTextField.setName("DefenderBaseConquestAmount");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Conquer BV Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of BV needed for extra points");
        BaseTextField.setName("DefenderConquestBVAdjustment");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Conquer Unit Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Number of units needed for extra points");
        BaseTextField.setName("DefenderConquestUnitAdjustment");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Base Delay Amount:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Base miniticks REPAIR caused by winning defender");
        BaseTextField.setName("DefenderBaseDelayAmount");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Delay BV Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of BV needed for extra minitick of REPAIR");
        BaseTextField.setName("DefenderDelayBVAdjustment");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Delay Unit Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Number of units needed for extra minitick of REPAIR");
        BaseTextField.setName("DefenderDelayUnitAdjustment");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Base Components:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Base components taken by winning defender");
        BaseTextField.setName("DefenderBasePPAmount");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Component BV Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of BV needed for extra batch of components");
        BaseTextField.setName("DefenderPPBVAdjustment");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Component Unit Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Number of units needed for extra batch of components");
        BaseTextField.setName("DefenderPPUnitAdjustment");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Target Op Mod:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Op Adjustments are used to increase or decrease victory THRESHOLDS for<br>targetted long-ops. Not recommended for individual games. Generlly, better<br>for use as a long-op (w/ fewer games than target) set up as a counter-assault<br>or spoling defend.</html>");
        BaseTextField.setName("DefenderTargetOpAdjustment");
        defenderPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(defenderPanel, 4);

        BaseTextField = new JTextField(5);
        capsPanel.add(new JLabel("Conqer Cap:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max amount of % to take, regardless of BV/units involved.");
        BaseTextField.setName("ConquestAmountCap");
        capsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        capsPanel.add(new JLabel("Delay Cap:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max amount of delay to apply, regardless of BV/units involved.");
        BaseTextField.setName("DelayAmountCap");
        capsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        capsPanel.add(new JLabel("Component Cap:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Max # of PP to take/generate");
        BaseTextField.setName("PPCapptureCap");
        capsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        capsPanel.add(new JLabel("Unit Cap:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Ceiling on number of units which may be raided");
        BaseTextField.setName("UnitCaptureCap");
        capsPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(capsPanel, 1, 8);

        BaseTextField = new JTextField(5);
        destructionCapPanel.add(new JLabel("Component Cap:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Ceiling on number of components which may be destroyed");
        BaseTextField.setName("PPDestructionCap");
        destructionCapPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        destructionCapPanel.add(new JLabel("Unit Cap:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Ceiling on number of Units which may be destroyed");
        BaseTextField.setName("UnitDestructionCap");
        destructionCapPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        destructionPanel.add(new JLabel("Base Units:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Base number of units to destroy");
        BaseTextField.setName("BaseUnitsDestroyed");
        destructionPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        destructionPanel.add(new JLabel("Units BV adjust:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Number of units to destroy based on BV");
        BaseTextField.setName("DestroyedUnitsBVAdjustment");
        destructionPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        destructionPanel.add(new JLabel("Unit Unit adjust:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Number of units to destroy based on units used in the op.");
        BaseTextField.setName("DestroyedUnitsUnitAdjustment");
        destructionPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        destructionPanel.add(new JLabel("Base Components:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Base number of components to destroy");
        BaseTextField.setName("BasePPDestroyed");
        destructionPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        destructionPanel.add(new JLabel("Component BV adjust:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Number of components to destroy based on BV");
        BaseTextField.setName("DestroyedPPBVAdjustment");
        destructionPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        destructionPanel.add(new JLabel("Unit Component Adjust:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Number of components to destroy based on units used in the op");
        BaseTextField.setName("DestroyedPPUnitAdjustment");
        destructionPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(destructionPanel, 6);
        SpringLayoutHelper.setupSpringGrid(destructionCapPanel, 4);

        destructionBox.add(new JLabel("Destruction"));
        destructionBox.add(destructionCapPanel);
        destructionBox.add(destructionPanel);
        destructionBox.setBorder(BorderFactory.createLineBorder(Color.black));

        attackerBox.add(new JLabel("Attacker"));
        BaseCheckBox = new JCheckBox("Units Taken First");
        BaseCheckBox.setToolTipText("<html>If enabled then the attacker will start the game with<br>the stolen units and must get them off the battle field.</html>");
        BaseCheckBox.setName("AttackerUnitsTakenBeforeFightStarts");
        attackerBox.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Unclaimed Land");
        BaseCheckBox.setToolTipText("<html>If enabled then the attacker can target a planet without claimed land and will automatically capture CP.</html>");
        BaseCheckBox.setName("AttackerAllowAgainstUnclaimedLand");
        attackerBox.add(BaseCheckBox);

        attackerBox.add(attackerPanel);

        attackerBox.setBorder(BorderFactory.createLineBorder(Color.black));

        defenderBox.add(new JLabel("Defender"));
        defenderBox.add(defenderPanel);

        defenderBox.setBorder(BorderFactory.createLineBorder(Color.black));

        capsBox.add(new JLabel("Max Operation Winnings"));
        capsBox.add(capsPanel);

        attackerBox.add(validAttackerFactoryPanel);

        metaBox.add(attackerBox);
        metaBox.add(defenderBox);

        masterBox.add(capsBox);
        masterBox.add(metaBox);
        masterBox.add(destructionBox);

        metaSetupPanel.add(masterBox);

        /*
         * Chicken/Leech
         *
         * Set up the Chicken/Leech panel,Set up what happens to those that flee and
         * those that don't pay attention to an attack.
         *
         */

        JPanel chickenLeechBox = new JPanel();
        chickenLeechBox.setLayout(new BoxLayout(metaBox, BoxLayout.X_AXIS));

        JPanel leechBox1 = new JPanel();
        leechBox1.setLayout(new BoxLayout(leechBox1, BoxLayout.Y_AXIS));

        JPanel leechPanel1 = new JPanel(new SpringLayout());
        JPanel leechPanel2 = new JPanel(new SpringLayout());
        JPanel leechPanel3 = new JPanel(new SpringLayout());
        JPanel leechPanel4 = new JPanel(new SpringLayout());

        BaseTextField = new JTextField(5);
        leechPanel1.add(new JLabel("Chicken Time:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("time, in SECONDS, from attack init to penalty.");
        BaseTextField.setName("TimeToNondefensePenalty");
        leechPanel1.add(BaseTextField);

        BaseTextField = new JTextField(5);
        leechPanel1.add(new JLabel("Total Allowed Leeches:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("num leeches before a player is moved into reserve.");
        BaseTextField.setName("LeechesToDeactivate");
        leechPanel1.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(leechPanel1, 1, 4);

        BaseTextField = new JTextField(5);
        leechPanel2.add(new JLabel("RP Penalty:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("# of RP to take.");
        BaseTextField.setName("FlatRPChickenPenalty");
        leechPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        leechPanel2.add(new JLabel("Exp Penalty:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("# of EXP to take.");
        BaseTextField.setName("FlatExpChickenPenalty");
        leechPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        leechPanel2.add(new JLabel("Flu Penalty:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("# of Inf to take.");
        BaseTextField.setName("FlatInfluenceChickenPenalty");
        leechPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        leechPanel2.add(new JLabel("Money Penalty:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("# of CBills to take.");
        BaseTextField.setName("FlatCBillChickenPenalty");
        leechPanel2.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(leechPanel2, 2, 4);

        BaseTextField = new JTextField(5);
        leechPanel3.add(new JLabel("% RP Penalty:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("% of RP to take. i.e. .10 for 10%");
        BaseTextField.setName("PercentRPChickenPenalty");
        leechPanel3.add(BaseTextField);

        BaseTextField = new JTextField(5);
        leechPanel3.add(new JLabel("% Exp Penalty:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("% of EXP to take. i.e. .10 for 10%");
        BaseTextField.setName("PercentExpChickenPenalty");
        leechPanel3.add(BaseTextField);

        BaseTextField = new JTextField(5);
        leechPanel3.add(new JLabel("% Flu Penalty:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("% of Flu to take. i.e. .10 for 10%");
        BaseTextField.setName("PercentInfluenceChickenPenalty");
        leechPanel3.add(BaseTextField);

        BaseTextField = new JTextField(5);
        leechPanel3.add(new JLabel("% Money Penalty:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("% of Money to take. i.e. .10 for 10%");
        BaseTextField.setName("PercentCBillChickenPenalty");
        leechPanel3.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(leechPanel3, 2, 4);

        BaseTextField = new JTextField(5);
        leechPanel4.add(new JLabel("Land Taken:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("% control transferred on each leech");
        BaseTextField.setName("ConquestPerLeech");
        leechPanel4.add(BaseTextField);

        BaseTextField = new JTextField(5);
        leechPanel4.add(new JLabel("Factory Delays (Ticks):", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("delay to on-target facilities w/ each leech");
        BaseTextField.setName("DelayPerLeech");
        leechPanel4.add(BaseTextField);

        BaseTextField = new JTextField(5);
        leechPanel4.add(new JLabel("Components Taken:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Components taken with each leech");
        BaseTextField.setName("ProdPointsPerLeech");
        leechPanel4.add(BaseTextField);

        BaseTextField = new JTextField(5);
        leechPanel4.add(new JLabel("Units Taken:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Units taken with each leech");
        BaseTextField.setName("UnitsPerLeech");
        leechPanel4.add(BaseTextField);

        BaseTextField = new JTextField(5);
        leechPanel4.add(new JLabel("Failure Penalty:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Components taken if no other penalty is applied. Use it<br>to ensure that a penalty is always given, even<br>if there is no % to yeild or factory to delay.</html>");
        BaseTextField.setName("FailurePenalty");
        leechPanel4.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(leechPanel4, 4);

        leechBox1.add(leechPanel1);
        leechBox1.add(leechPanel2);
        leechBox1.add(leechPanel3);
        leechBox1.add(leechPanel4);

        chickenLeechPanel.add(leechBox1);

        /*
         * Pilot Panel
         *
         * Set up the Pilot Panel,Exp and leveling for pilots
         *
         */

        JPanel pilotExpBox = new JPanel();
        pilotExpBox.setLayout(new BoxLayout(pilotExpBox, BoxLayout.X_AXIS));

        JPanel pilotPanel = new JPanel(new SpringLayout());

        BaseTextField = new JTextField(5);
        pilotPanel.add(new JLabel("Base Unit Exp:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("XP earned by all surviving units for playing the game");
        BaseTextField.setName("BaseUnitXP");
        pilotPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        pilotPanel.add(new JLabel("Surviving Unit Adjustment:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>bonus XP given to all surviving units, based on the starting size of the game<br>if there are 10 units in game, and Adjust is set to 10, all units will get +1<br>XP. if there are 10 in game and Adjust is 5, all units will get +2 XP, etc.</html>");
        BaseTextField.setName("UnitXPUnitsAdjustment");
        pilotPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        pilotPanel.add(new JLabel("BV Adjustment:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>bonus XP given to all surviving units, based on the starting size of the game. if<br>total BV in game is 30,000 and Adjust is 10,000, all units will get +3 XP.</html>");
        BaseTextField.setName("UnitXPBVAdjustment");
        pilotPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        pilotPanel.add(new JLabel("Winner Bonus:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>bonux XP given to all surviving units owned by a winning player</html>");
        BaseTextField.setName("WinnerBonusUnitXP");
        pilotPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        pilotPanel.add(new JLabel("Defender Bonus:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("bonus XP given to all surviving units owned by a defending player");
        BaseTextField.setName("DefenderBonusUnitXP");
        pilotPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        pilotPanel.add(new JLabel("Units Killed Bonus:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>flat amount of XP given to a unit for each kill it earned. this is granted only<br>to the killing unit, and only if it survives or is salvaged by original owner<br>with original pilot.</html>");
        BaseTextField.setName("KillBonusUnitXP");
        pilotPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        pilotPanel.add(new JLabel("BV Killed Bonus:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>KilledUnitsBV/KillBonusXPforBV. bonus XP based on the BV of units killed. added<br>only to the killing unit, and only if the unit survives or is salvaged by its<br>original owner with original pilot.</html>");
        BaseTextField.setName("KillBonusXPforBV");
        pilotPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(pilotPanel, 7, 2);

        pilotExpBox.add(pilotPanel);

        pilotExpPanel.add(pilotExpBox);

        /*
         * Building Panel
         *
         * Set up the Building Panel,Bildings for building raids and ops
         *
         */

        JPanel buildingsBox = new JPanel();
        buildingsBox.setLayout(new BoxLayout(buildingsBox, BoxLayout.X_AXIS));

        JPanel constructionPanel = new JPanel(new SpringLayout());

        masterBox = new JPanel();
        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.Y_AXIS));

        attackerBox = new JPanel();
        attackerBox.setLayout(new BoxLayout(attackerBox, BoxLayout.Y_AXIS));

        defenderBox = new JPanel();
        defenderBox.setLayout(new BoxLayout(defenderBox, BoxLayout.Y_AXIS));
        defenderBox.setPreferredSize(new Dimension(210, 178));
        defenderBox.setMaximumSize(new Dimension(210, 178));
        defenderBox.setMinimumSize(new Dimension(210, 178));

        attackerPanel = new JPanel(new SpringLayout());

        defenderPanel = new JPanel(new SpringLayout());

        BaseTextField = new JTextField(5);
        constructionPanel.add(new JLabel("Total Buildings:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Total number of buildings to place on the map");
        BaseTextField.setName("TotalBuildings");
        constructionPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        constructionPanel.add(new JLabel("Min Buildings:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Minimum number of buildings the Attack needs to destroy for the op to be a sucess<br>I.E. 10 buildings 5 min. if the attacker kills only 4 they don't get any of the b</html>");
        BaseTextField.setName("MinBuildingsForOp");
        constructionPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        constructionPanel.add(new JLabel("Min Floors:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Minimum number of floors in each building.");
        BaseTextField.setName("MinFloors");
        constructionPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        constructionPanel.add(new JLabel("Max Floors:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Maximum number of floors in each building.");
        BaseTextField.setName("MaxFloors");
        constructionPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        constructionPanel.add(new JLabel("Min CF:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Minimum Construction Factor each building can have (How many points of damage it'll take)");
        BaseTextField.setName("MinCF");
        constructionPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        constructionPanel.add(new JLabel("Max CF:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Maximum Construction Factor each building can have (How many points of damage it'll take)");
        BaseTextField.setName("MaxCF");
        constructionPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        constructionPanel.add(new JLabel("Building Type:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("1: Light, 2: Medium, 3: Heavy, 4: Hardend");
        BaseTextField.setName("BuildingType");
        constructionPanel.add(BaseTextField);

        BaseCheckBox = new JCheckBox("Buildings Start on map edge");
        BaseCheckBox.setToolTipText("<html>defaults to true.<br>If selected a random edge is selected<br>and the defender is set to that<br>and the attacker to the opposite edge.<br>If not selected the buildings<br>will be placed randomly around the map.</html>");
        BaseCheckBox.setName("BuildingsStartOnMapEdge");
        constructionPanel.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(constructionPanel, 4);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Min Buildings if Attacker wins:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>If The attacket wins the ops and the number of buildings destroyed is blow this it gets set to this.<br>I.E. Keep the defender from screwing the Attacker by quiting early.</html>");
        BaseTextField.setName("AttackerMinBuildingsIfAttackerWins");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Delay Per Building:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Number of Delay ticks set to the planet factories for each building destroyed,<br>if they meet the min amount of buildings destroyed.</html>");
        BaseTextField.setName("DelayPerBuilding");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Money Per Building:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Amount of Money the attacker gets for each building destroyed,<br>if they meet the min amount of buildings destroyed.</html>");
        BaseTextField.setName("AttackerMoneyPerBuilding");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Exp Per Building:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Amount of Exp the attacker gets for each building destroyed,<br>if they meet the min amount of buildings destroyed.</html>");
        BaseTextField.setName("AttackerExpPerBuilding");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("Flu Per Building:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Amount of Flu the attacker gets for each building destroyed,<br>if they meet the min amount of buildings destroyed.</html>");
        BaseTextField.setName("AttackerFluPerBuilding");
        attackerPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        attackerPanel.add(new JLabel("RP Per Building:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Amount of RP the attacker gets for each building destroyed,<br>if they meet the min amount of buildings destroyed.</html>");
        BaseTextField.setName("AttackerRPPerBuilding");
        attackerPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(attackerPanel, 6, 2);

        defenderPanel.add(new JLabel("  ", SwingConstants.TRAILING));
        defenderPanel.add(new JLabel("  ", SwingConstants.TRAILING));

        defenderPanel.add(new JLabel("  ", SwingConstants.TRAILING));
        defenderPanel.add(new JLabel("  ", SwingConstants.TRAILING));

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Money Per Building:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of Money the defender gets for each building left standing.");
        BaseTextField.setName("DefenderMoneyPerBuilding");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Exp Per Building:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of Exp the defender gets for each building left standing.");
        BaseTextField.setName("DefenderExpPerBuilding");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("Flu Per Building:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of Flu the defender gets for each building left standing.");
        BaseTextField.setName("DefenderFluPerBuilding");
        defenderPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        defenderPanel.add(new JLabel("RP Per Building:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Amount of RP the defender gets for each building left standing.");
        BaseTextField.setName("DefenderRPPerBuilding");
        defenderPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(defenderPanel, 6, 2);

        attackerBox.add(new JLabel("Attacker"));
        attackerBox.add(attackerPanel);

        attackerBox.setBorder(BorderFactory.createLineBorder(Color.black));

        defenderBox.add(new JLabel("Defender"));
        defenderBox.add(defenderPanel);

        defenderBox.setBorder(BorderFactory.createLineBorder(Color.black));

        buildingsBox.add(attackerBox);
        buildingsBox.add(defenderBox);

        masterBox.add(constructionPanel);
        masterBox.add(buildingsBox);

        /*
         * City Panel
         *
         * Set up the City Panel,City Generator Configs.
         *
         */

        JPanel checkBoxBox = new JPanel(new SpringLayout());

        JPanel cityParamsPanel = new JPanel(new SpringLayout());

        BaseCheckBox = new JCheckBox("Use City Generator for this Op?");
        BaseCheckBox.setToolTipText("<html>Check if you want MM to generate a city for this op<br>Using the settings below</html>");
        BaseCheckBox.setName("RCGUseCityGenerator");
        checkBoxBox.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(checkBoxBox, 1);

        BaseTextField = new JTextField(5);
        cityParamsPanel.add(new JLabel("City Type:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Set a city type<br>GRID, METRO, HUB</html>");
        BaseTextField.setName("RCGCityType");
        cityParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        cityParamsPanel.add(new JLabel("City Blocks:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Number of city blocks for this city. This goes west to east and north to south");
        BaseTextField.setName("RCGCityBlocks");
        cityParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        cityParamsPanel.add(new JLabel("Min CF:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Minimum Construction Factor each building can have (How many points of damage it'll take)");
        BaseTextField.setName("RCGMinCF");
        cityParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        cityParamsPanel.add(new JLabel("Max CF:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Maximum Construction Factor each building can have (How many points of damage it'll take)");
        BaseTextField.setName("RCGMaxCF");
        cityParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        cityParamsPanel.add(new JLabel("Min Floors:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Minimum number of floors in each building.");
        BaseTextField.setName("RCGMinFloors");
        cityParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        cityParamsPanel.add(new JLabel("Max Floors:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("Maximum number of floors in each building.");
        BaseTextField.setName("RCGMaxFloors");
        cityParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        cityParamsPanel.add(new JLabel("City Density:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>1-100 This is the % chance that a hex will contain a building the higher the number<br>the more buildings will be generated.</html>");
        BaseTextField.setName("RCGCityDensity");
        cityParamsPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(cityParamsPanel, 4);

        masterBox.add(checkBoxBox);
        masterBox.add(cityParamsPanel);

        buildingsPanel.add(masterBox);

        /*
         * Victory Conditions Panel
         *
         * Set up the Victory Conditions that will be sent to MegaMek
         *
         */

        JPanel victoryParamsPanel = new JPanel(new SpringLayout());
        checkBoxBox = new JPanel(new SpringLayout());

        masterBox = new JPanel();
        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.Y_AXIS));

        BaseCheckBox = new JCheckBox("Use Destroy Enemy BV?");
        BaseCheckBox.setToolTipText("<html>If this is turned on DestroyEnemyBV field well be sent to MM</html>");
        BaseCheckBox.setName("UseDestroyEnemyBV");
        checkBoxBox.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Use BV Ratio Percent?");
        BaseCheckBox.setToolTipText("<html>If this is turned on UseBVRatioPercent field will be sent to MM.</html>");
        BaseCheckBox.setName("UseBVRatioPercent");
        checkBoxBox.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Use Game Turn Limit?");
        BaseCheckBox.setToolTipText("<html>If this is turned on UseGameTurnLimit field will be sent to MM.</html>");
        BaseCheckBox.setName("UseGameTurnLimit");
        checkBoxBox.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Use Kill Count?");
        BaseCheckBox.setToolTipText("<html>If this is turned on UseKillCount field will be sent to MM.</html>");
        BaseCheckBox.setName("UseKillCount");
        checkBoxBox.add(BaseCheckBox);


        BaseCheckBox = new JCheckBox("Use Unit Commanders?");
        BaseCheckBox.setToolTipText("<html>If this is turned on UseUnitCommanders field will be sent to MM.</html>");
        BaseCheckBox.setName("UseUnitCommander");
        checkBoxBox.add(BaseCheckBox);

        BaseTextField = new JTextField(5);
        victoryParamsPanel.add(new JLabel("Victory Conditions:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>The number of victory conditions that must<br>be achieved by the winning team.<br>Default 0 (off)</html>");
        BaseTextField.setName("NumberOfVictoryConditions");
        victoryParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        victoryParamsPanel.add(new JLabel("Destroy Enemy BV:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Destroy/damage a certain percentage<br>of the enemy force to win,<br>measured by current BV / original BV</html>");
        BaseTextField.setName("DestroyEnemyBV");
        victoryParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        victoryParamsPanel.add(new JLabel("BV Ratio Percent:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Friendly forces outnumber enemy forces by a percentage ratio.<br>Measured by current BV.<br>E.G. 300 means you have 3x the surviving BV of the enemy.</html>");
        BaseTextField.setName("BVRatioPercent");
        victoryParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        victoryParamsPanel.add(new JLabel("Game Turn Limit:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Limit of game turns.</html>");
        BaseTextField.setName("GameTurnLimit");
        victoryParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        victoryParamsPanel.add(new JLabel("Game Kill Count:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Number of kills needed to achieve victory.</html>");
        BaseTextField.setName("KillCount");
        victoryParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        victoryParamsPanel.add(new JLabel("Minimum Commanders:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Minimum number of unit commanders this op needs for players to launch/defend it.</html>");
        BaseTextField.setName("MinimumUnitCommanders");
        victoryParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        victoryParamsPanel.add(new JLabel("Maximum Commanders:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Maximum number of unit commanders this op needs for players to launch/defend it.</html>");
        BaseTextField.setName("MaximumUnitCommanders");
        victoryParamsPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(checkBoxBox, 3);
        SpringLayoutHelper.setupSpringGrid(victoryParamsPanel, 2);

        masterBox.add(checkBoxBox);
        masterBox.add(victoryParamsPanel);

        victoryPanel.add(masterBox);

        /*
         * Team Panel
         *
         * Set up the Team settings that will be sent to MegaMek
         *
         */

        JPanel teamParamsPanel = new JPanel(new SpringLayout());
        checkBoxBox = new JPanel(new SpringLayout());

        masterBox = new JPanel();
        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.Y_AXIS));

        BaseCheckBox = new JCheckBox("Free For All Op");
        BaseCheckBox.setToolTipText("<html>If checked this op become a free for all.<br>Unlimited number of defenders can join<br>The op will start when the attacker clicks on the commence link<br>or the leech time has timed out<br>This of note you will want to set Min planet ownership to 0<br>This way any player can join no matter what planet is hosting</html>");
        BaseCheckBox.setName("FreeForAllOperation");
        checkBoxBox.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Team Operation");
        BaseCheckBox.setToolTipText("<html>If checked this operation is set up for teams</html>");
        BaseCheckBox.setName("TeamOperation");
        checkBoxBox.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Random Teams");
        BaseCheckBox.setToolTipText("<html>If checked Players are randomly placed on teams</html>");
        BaseCheckBox.setName("RandomTeamDetermination");
        checkBoxBox.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Same Factions");
        BaseCheckBox.setToolTipText("<html>If checked players are divided up based on factions</html>");
        BaseCheckBox.setName("TeamsMustBeSameFaction");
        checkBoxBox.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(checkBoxBox, 3);

        BaseTextField = new JTextField(5);
        teamParamsPanel.add(new JLabel("Min Number Of Players:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Minimum number of players needed for<br>the Free For All to launch.</html>");
        BaseTextField.setName("MinNumberOfPlayers");
        teamParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        teamParamsPanel.add(new JLabel("Team Size:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Maximum number of players per team.</html>");
        BaseTextField.setName("TeamSize");
        teamParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        teamParamsPanel.add(new JLabel("Team Numbers:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Maximum number teams.</html>");
        BaseTextField.setName("NumberOfTeams");
        teamParamsPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(teamParamsPanel, 4);

        masterBox.add(checkBoxBox);
        masterBox.add(teamParamsPanel);
        teamPanel.add(masterBox);

        /*
         * Deployment Panel
         *
         * Set up the Deployment settings that will be sent to MegaMek
         *
         */

        JPanel deploymentParamsPanel = new JPanel(new SpringLayout());
        JPanel deploymentParamsPanel2 = new JPanel(new SpringLayout());
        checkBoxBox = new JPanel(new SpringLayout());

        masterBox = new JPanel();
        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.Y_AXIS));
        JPanel deploymentBox = new JPanel();
        deploymentBox.setLayout(new BoxLayout(deploymentBox, BoxLayout.X_AXIS));

        BaseCheckBox = new JCheckBox("Random Deployment");
        BaseCheckBox.setToolTipText("<html>If checked the operation picks what sides<br>the attacker and defender start on.</html>");
        BaseCheckBox.setName("RandomDeployment");
        checkBoxBox.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(checkBoxBox, 3);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel.add(new JLabel("Northwest:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy Northwest</html>");
        BaseTextField.setName("DeployNorthwest");
        deploymentParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel.add(new JLabel("North:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy North</html>");
        BaseTextField.setName("DeployNorth");
        deploymentParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel.add(new JLabel("Northeast:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy Northeast</html>");
        BaseTextField.setName("DeployNortheast");
        deploymentParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel.add(new JLabel("East:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy East</html>");
        BaseTextField.setName("DeployEast");
        deploymentParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel.add(new JLabel("Southeast:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy Southeast</html>");
        BaseTextField.setName("DeploySoutheast");
        deploymentParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel.add(new JLabel("South:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy South</html>");
        BaseTextField.setName("DeploySouth");
        deploymentParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel.add(new JLabel("Southwest:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy Southwest</html>");
        BaseTextField.setName("DeploySouthwest");
        deploymentParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel.add(new JLabel("West:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy West</html>");
        BaseTextField.setName("DeployWest");
        deploymentParamsPanel.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel.add(new JLabel("Edge:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy Edge</html>");
        BaseTextField.setName("DeployEdge");
        deploymentParamsPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(deploymentParamsPanel, 2);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel2.add(new JLabel("Northwest (Deep):", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy Northwest (Deep)</html>");
        BaseTextField.setName("DeployNorthwestdeep");
        deploymentParamsPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel2.add(new JLabel("North (Deep):", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy North (Deep)</html>");
        BaseTextField.setName("DeployNorthdeep");
        deploymentParamsPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel2.add(new JLabel("Northeast (Deep):", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy Northeast (Deep)</html>");
        BaseTextField.setName("DeployNortheastdeep");
        deploymentParamsPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel2.add(new JLabel("East (Deep):", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy East (Deep)</html>");
        BaseTextField.setName("DeployEastdeep");
        deploymentParamsPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel2.add(new JLabel("Southeast (Deep):", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy Southeast (Deep)</html>");
        BaseTextField.setName("DeploySoutheastdeep");
        deploymentParamsPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel2.add(new JLabel("South (Deep):", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy South (Deep)</html>");
        BaseTextField.setName("DeploySouthdeep");
        deploymentParamsPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel2.add(new JLabel("Southwest (Deep):", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy Southwest (Deep)</html>");
        BaseTextField.setName("DeploySouthwestdeep");
        deploymentParamsPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel2.add(new JLabel("West (deep):", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy West (Deep)</html>");
        BaseTextField.setName("DeployWestdeep");
        deploymentParamsPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        deploymentParamsPanel2.add(new JLabel("Center:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Chances for the attacker to deploy Center</html>");
        BaseTextField.setName("DeployCenter");
        deploymentParamsPanel2.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(deploymentParamsPanel2, 2);

        deploymentBox.add(deploymentParamsPanel);
        deploymentBox.add(deploymentParamsPanel2);
        masterBox.add(checkBoxBox);
        masterBox.add(deploymentBox);

        /*
         * Map Settings Panel
         *
         * Set up the map settings that will be sent to MegaMek
         *
         */

        JPanel mapParamsPanel = new JPanel(new SpringLayout());
        JPanel mapParamsPanel2 = new JPanel(new SpringLayout());
        checkBoxBox = new JPanel(new SpringLayout());

        BaseCheckBox = new JCheckBox("Use Operation Map");
        BaseCheckBox.setToolTipText("<html>If checked use the Operation map instead of the terrain on the planet.</html>");
        BaseCheckBox.setName("UseOperationMap");
        checkBoxBox.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(checkBoxBox, 1);

        BaseTextField = new JTextField(5);
        mapParamsPanel.add(new JLabel("Map Name:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Name of the map to use or surprise/generated if using sizes</html>");
        BaseTextField.setName("MapName");
        mapParamsPanel.add(BaseTextField);

        SpringLayoutHelper.setupSpringGrid(mapParamsPanel, 2);

        BaseTextField = new JTextField(5);
        mapParamsPanel2.add(new JLabel("Board Size X:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Number of maps along the X axis of the Board</html>");
        BaseTextField.setName("BoardSizeX");
        mapParamsPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        mapParamsPanel2.add(new JLabel("Board Size Y:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Number of maps along the Y axis of the board</html>");
        BaseTextField.setName("BoardSizeY");
        mapParamsPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        mapParamsPanel2.add(new JLabel("Map Size X:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>X Size of the map<br>Also can be used to set the base X size of a generated map.</html>");
        BaseTextField.setName("MapSizeX");
        mapParamsPanel2.add(BaseTextField);

        BaseTextField = new JTextField(5);
        mapParamsPanel2.add(new JLabel("Map Size Y:", SwingConstants.TRAILING));
        BaseTextField.setToolTipText("<html>Y Size of the map<br>Also can be used to set the base Y size of a generated map.</html>");
        BaseTextField.setName("MapSizeY");
        mapParamsPanel2.add(BaseTextField);

        String[] mediumNames = { "Ground", "Atmosphere", "Space" };
        BaseComboBox = new JComboBox<String>(mediumNames);
        mapParamsPanel2.add(new JLabel("Map Medium:", SwingConstants.TRAILING));
        BaseComboBox.setToolTipText("<html>Ground, Space, Atmosphere</html>");
        BaseComboBox.setName("MapMedium");
        mapParamsPanel2.add(BaseComboBox);

        SpringLayoutHelper.setupSpringGrid(mapParamsPanel2, 4);

        masterBox.add(checkBoxBox);
        masterBox.add(mapParamsPanel);
        masterBox.add(mapParamsPanel2);
        deploymentPanel.add(masterBox);

        /*
         * Set up Player Flag panel
         */
        // We're going to use 4 JTables for this

        JPanel afPanel = new JPanel(new VerticalLayout());
        //afPanel.setBorder(BorderFactory.createEtchedBorder());
        JPanel dfPanel = new JPanel(new VerticalLayout());
        //dfPanel.setBorder(BorderFactory.createEtchedBorder());
        JPanel wfPanel = new JPanel(new VerticalLayout());
        //wfPanel.setBorder(BorderFactory.createEtchedBorder());
        JPanel lfPanel = new JPanel(new VerticalLayout());
        //lfPanel.setBorder(BorderFactory.createEtchedBorder());

        afTable = new FlagTable(this, FlagSet.FLAGTYPE_PLAYER);
        afTable.setName("AttackerFlags");
        dfTable = new FlagTable(this, FlagSet.FLAGTYPE_PLAYER);
        dfTable.setName("DefenderFlags");
        wfTable = new FlagTable(this, FlagSet.FLAGTYPE_RESULTS);
        wfTable.setName("WinnerFlags");
        lfTable = new FlagTable(this, FlagSet.FLAGTYPE_RESULTS);
        lfTable.setName("LoserFlags");

        afPanel.add(new JLabel("Required Attacker Flags"));
        afPanel.add(afTable.getTableHeader());
        afPanel.add(afTable);
        dfPanel.add(new JLabel("Required Defender Flags"));
        dfPanel.add(dfTable.getTableHeader());
        dfPanel.add(dfTable);
        wfPanel.add(new JLabel("Winner Flags to Set"));
        wfPanel.add(wfTable.getTableHeader());
        wfPanel.add(wfTable);
        lfPanel.add(new JLabel("Loser Flags to Set"));
        lfPanel.add(lfTable.getTableHeader());
        lfPanel.add(lfTable);

        JPanel mainFlagPanel = new JPanel();

        //mainFlagPanel.setLayout(new VerticalLayout());
        mainFlagPanel.setLayout(new GridLayout(2,2,10,10));

        //mainFlagPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        mainFlagPanel.add(afPanel);
        mainFlagPanel.add(dfPanel);
        mainFlagPanel.add(wfPanel);
        mainFlagPanel.add(lfPanel);

        flagsPanel.add(mainFlagPanel);

        // Unit Ratios Tab
        unitRatiosPanel.setLayout(new VerticalLayout());
        JPanel aeroRatioPanel = new JPanel();
        JPanel supportRatioPanel = new JPanel();
        JPanel attackerRatioPanel = new JPanel();
        JPanel defenderRatioPanel = new JPanel();

        BaseCheckBox = new JCheckBox("Count Support Aero");
        BaseCheckBox.setName("CountSupportUnitsInAeroRatio");
        BaseCheckBox.setToolTipText("<html>If checked, support Aero will count as normal aero for purposes of the ratio<br>If unchecked, support aero will not count toward the ratio</html>");
        supportRatioPanel.add(BaseCheckBox);

        attackerRatioPanel.setLayout(new VerticalLayout());
        attackerRatioPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Attacker"));
        BaseCheckBox = new JCheckBox("Enforce Attacker Ratio");
        BaseCheckBox.setName("EnforceAttackerAeroRatio");
        BaseCheckBox.setToolTipText("If checked, ratios will be enforced for the attacker");
        attackerRatioPanel.add(BaseCheckBox);
        BaseTextField = new JTextField(5);
        BaseTextField.setName("MaxAttackerAeroPercent");
        JPanel maxPanel = new JPanel();
        maxPanel.add(new JLabel("Max aero percent:"));
        maxPanel.add(BaseTextField);
        attackerRatioPanel.add(maxPanel);
        BaseTextField = new JTextField(5);
        BaseTextField.setName("MinAttackerAeroPercent");
        JPanel minPanel = new JPanel();
        minPanel.add(new JLabel("Min aero percent:"));
        minPanel.add(BaseTextField);
        attackerRatioPanel.add(minPanel);

        defenderRatioPanel.setLayout(new VerticalLayout());
        defenderRatioPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Defender"));
        BaseCheckBox = new JCheckBox("Enforce Defender Ratio");
        BaseCheckBox.setName("EnforceDefenderAeroRatio");
        BaseCheckBox.setToolTipText("If checked, ratios will be enforced for the Defender");
        defenderRatioPanel.add(BaseCheckBox);
        BaseTextField = new JTextField(5);
        BaseTextField.setName("MaxDefenderAeroPercent");
        maxPanel = new JPanel();
        maxPanel.add(new JLabel("Max aero percent:"));
        maxPanel.add(BaseTextField);
        defenderRatioPanel.add(maxPanel);
        BaseTextField = new JTextField(5);
        BaseTextField.setName("MinDefenderAeroPercent");
        minPanel = new JPanel();
        minPanel.add(new JLabel("Min aero percent:"));
        minPanel.add(BaseTextField);
        defenderRatioPanel.add(minPanel);


        aeroRatioPanel.add(supportRatioPanel);
        aeroRatioPanel.add(attackerRatioPanel);
        aeroRatioPanel.add(defenderRatioPanel);

        aeroRatioPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Aero Ratios"));

        unitRatiosPanel.add(aeroRatioPanel);

        ConfigPane.addTab("Buildings", null, buildingsPanel, "Set up buildings for operations.");
        ConfigPane.addTab("Chicken/Leech", null, chickenLeechPanel, "<html>Set up what happens to those that flee and<br>those that don't pay attention to an attack.</html>");
        ConfigPane.addTab("Deployment", null, deploymentPanel, "Set Army Deployment Chances.");
        ConfigPane.addTab("Faction Limits", null, factionPanel, "Exlude factions from using or defending the op.");
        ConfigPane.addTab("Meta Awards", null, metaSetupPanel, "<html>Set what your faction gets for this op.<br>Land, Units, Components</html>");
        ConfigPane.addTab("Newbie Ops", null, newbieOpsPanel, "how to treat the new player in your life");
        ConfigPane.addTab("Operation Costs", null, costsPanel, "Cost to attack or defend an op");
        ConfigPane.addTab("Operation Results", null, opresultsPanel, "who gets payed what and how much");
        ConfigPane.addTab("Player Properties", null, playerPropertiesPanel, "<html>mins/maxes for players if<br>they can attack/defend an op</html>");
        ConfigPane.addTab("Pilot Exp", null, pilotExpPanel, "Set up how unit pilots will be reward for surviving.");
        ConfigPane.addTab("Ranges & Misc", null, rangePanel, "Ranges & Targets for the Op. Includes some Misc. values.");
        ConfigPane.addTab("Salvage", null, salvagePanel, "how the units are divied up");
        ConfigPane.addTab("Scenario Addons", null, scenarioPanel, "<html>Arty/mines anything given to an attacker/<br>defender besides their own units</HTML>");
        ConfigPane.addTab("Teams", null, teamPanel, "Team Settings For MegaMek");
        ConfigPane.addTab("Units", null, unitsPanel, "Unit mins and maxes for the op");
        ConfigPane.addTab("Victory Conditions", null, victoryPanel, "Victory Conditions For MegaMek");
        ConfigPane.addTab("Player Flags", null, flagsPanel, "Player Flag settings");
        ConfigPane.addTab("Unit Ratios", null, unitRatiosPanel, "Limit access to op by unit ratios");


        // Remove the old configpane and add the newly created one!
        pane.remove(0);
        pane.add(ConfigPane, 0);
        pane.setVisible(true);
        repaint();

    }

    public void initShortOpVars() {

        for (int pos = ConfigPane.getComponentCount() - 1; pos >= 0; pos--) {
            JPanel panel = (JPanel) ConfigPane.getComponent(pos);
            findAndPopulateTextAndCheckBoxes(panel);

        }

    }

    public void saveShortOperations() {

        try {
            FileOutputStream out = new FileOutputStream(filePathName);
            PrintStream p = new PrintStream(out);

            for (int pos = ConfigPane.getComponentCount() - 1; pos >= 0; pos--) {
                JPanel panel = (JPanel) ConfigPane.getComponent(pos);
                findAndSaveConfigs(panel, p);
            }

            p.close();
            out.close();

            JOptionPane.showMessageDialog(null, taskName + " saved to " + filePathName, "File Saved", JOptionPane.INFORMATION_MESSAGE);
            changesMade = false;
            setTitle(windowName + " (" + taskName + ")");
        } catch (Exception ex) {
        	System.err.println(ex.getMessage());
            System.err.println("Unable to save file");
            MWLogger.errLog(ex);
        }

    }

    public void loadShortOp() {

        opValues = new BackedTreeMap(defaultOperationInfo);
        FileDialog fDialog = new FileDialog(this, "Load Short Op File", FileDialog.LOAD);

        fDialog.setDirectory(System.getProperty("user.dir") + "/data/operations/short");
        fDialog.setVisible(true);

        if (fDialog.getFile() == null) {
            return;
        }

        File shortOP = new File(fDialog.getDirectory(), fDialog.getFile());

        filePathName = fDialog.getDirectory() + fDialog.getFile();
        taskName = fDialog.getFile().substring(0, fDialog.getFile().indexOf(".txt"));

        setTitle(windowName + " (" + taskName + ")");

        // clear out the Flag Tables, just in case
        afTable.clear();
        dfTable.clear();
        wfTable.clear();
        lfTable.clear();
        BufferedReader dis = null;
        try {
            FileInputStream fis = new FileInputStream(shortOP);
            dis = new BufferedReader(new InputStreamReader(fis));
            while (dis.ready()) {
                String values = "";
                try {
                    values = dis.readLine();
                    StringTokenizer OperationOption = new StringTokenizer(values, "=");
                    String opVar = OperationOption.nextToken();
                    String value = OperationOption.nextToken();
                    opValues.put(opVar, value);
                } catch (Exception ex) {
                    System.err.println("Error reading file " + filePathName);
                    System.err.println("Bad value " + values);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error loading file " + filePathName);
            MWLogger.errLog(ex);
        } finally {
        	try {
				dis.close();
			} catch (IOException e) {
				MWLogger.errLog(e);
			}
        }

        for (int pos = ConfigPane.getComponentCount() - 1; pos >= 0; pos--) {
            JPanel panel = (JPanel) ConfigPane.getComponent(pos);
            findAndPopulateTextAndCheckBoxes(panel, opValues);

        }



    }

    /*
     * Inner class which backs a treemap with
     * a set of default ops values.
     */
    private class BackedTreeMap extends TreeMap<String, String> {

        private static final long serialVersionUID = 1L;
        DefaultOperation defaults;

        public BackedTreeMap(DefaultOperation dop) {
            defaults = dop;
        }

        public String getV(String key) {
            Object toReturn = super.get(key);
            if (toReturn == null) {
                toReturn = defaults.getDefault(key);
            }
            return (String) toReturn;
        }
    }// end BackedTreeMap

    public void keyTyped(KeyEvent arg0) {

        if ((arg0.getKeyCode() >= 32) && (arg0.getKeyCode() <= 126)) {
            changesMade = true;
            if (getTitle().indexOf("*") == -1) {
                setTitle(getTitle() + "*");
            }
        }
    }

    public void keyPressed(KeyEvent arg0) {
        if ((arg0.getKeyCode() >= 32) && (arg0.getKeyCode() <= 126)) {
            changesMade = true;
            if (getTitle().indexOf("*") == -1) {
                setTitle(getTitle() + "*");
            }
        }
    }

    public void keyReleased(KeyEvent arg0) {
        if ((arg0.getKeyCode() >= 32) && (arg0.getKeyCode() <= 126)) {
            changesMade = true;
            if (getTitle().indexOf("*") == -1) {
                setTitle(getTitle() + "*");
            }
        }
    }

    /**
     * This Method tunnels through all of the panels to find the textfields and checkboxes. Once it find one it grabs the Name() param of the object and uses
     * that to find out what the setting should be from the mwclient.getserverConfigs() method.
     *
     * @param panel
     */
    public void findAndPopulateTextAndCheckBoxes(JPanel panel) {
        String key = null;

        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            if (field instanceof JPanel) {
                findAndPopulateTextAndCheckBoxes((JPanel) field);
            } else if (field instanceof JTextField) {
                JTextField textBox = (JTextField) field;

                key = textBox.getName();
                if (key == null) {
                    continue;
                }

                // bad hack need to format the message for the last time the backup happened
                textBox.setText(defaultOperationInfo.getDefault(key));
                textBox.addKeyListener(this);
                textBox.setPreferredSize(textBoxSize);
                textBox.setMaximumSize(textBoxSize);
                textBox.setMinimumSize(textBoxSize);
                textBox.removeMouseListener(this);
                textBox.addMouseListener(this);
            } else if (field instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) field;

                key = checkBox.getName();
                if (key == null) {
                    System.err.println("Null Checkbox: " + checkBox.getToolTipText());
                    continue;
                }
                checkBox.setSelected(Boolean.parseBoolean(defaultOperationInfo.getDefault(key)));
                checkBox.addKeyListener(this);
            } else if (field instanceof JComboBox) {
                JComboBox<String> combo = (JComboBox<String>) field;

                key = combo.getName();

                if (key == null) {
                    System.err.println("Null Checkbox: " + combo.getToolTipText());
                    continue;
                }
                combo.setSelectedIndex(Integer.parseInt(defaultOperationInfo.getDefault(key)));

            } else if (field instanceof FlagTable) {
            	FlagTable table = (FlagTable) field;

            	key = table.getName();
            	table.importFlagString(defaultOperationInfo.getDefault(key));
            }// else continue
        }
    }

    /**
     * This Method tunnels through all of the panels to find the textfields and checkboxes. Once it find one it grabs the Name() param of the object and uses
     * that to find out what the setting should be from the mwclient.getserverConfigs() method.
     *
     * @param panel
     */
    public void findAndPopulateTextAndCheckBoxes(JPanel panel, BackedTreeMap OperationInfo) {
        String key = null;

        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            if (field instanceof JPanel) {
                findAndPopulateTextAndCheckBoxes((JPanel) field, OperationInfo);
            } else if (field instanceof JTextField) {
                JTextField textBox = (JTextField) field;

                key = textBox.getName();

                if (key == null) {
                    continue;
                }

                textBox.setText(OperationInfo.getV(key));
                textBox.setPreferredSize(textBoxSize);
                textBox.setMaximumSize(textBoxSize);
                textBox.setMinimumSize(textBoxSize);
            } else if (field instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) field;

                key = checkBox.getName();

                if (key == null) {
                    System.err.println("Null Checkbox: " + checkBox.getToolTipText());
                    continue;
                }
                checkBox.setSelected(Boolean.parseBoolean(OperationInfo.getV(key)));

            } else if (field instanceof JComboBox) {
                JComboBox<String> combo = (JComboBox) field;

                key = combo.getName();

                if (key == null) {
                    System.err.println("Null Checkbox: " + combo.getToolTipText());
                    continue;
                }
                combo.setSelectedIndex(Integer.parseInt(OperationInfo.getV(key)));

            } else if (field instanceof FlagTable) {
            	FlagTable table = (FlagTable) field;
            	key = table.getName();

            	if (key == null) {
            		System.err.println("Null Flagtable");
            		continue;
            	}
            	table.importFlagString(OperationInfo.getV(key));

            } // else continue
        }
    }

    /**
     * This method will tunnel through all of the panels of the config UI to find any changed text fields or checkboxes. Then it will send the new configs to
     * the server.
     *
     * @param panel
     */
    public void findAndSaveConfigs(JPanel panel, PrintStream p) {
        String key = null;
        String value = null;
        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            // found another JPanel keep digging!
            if (field instanceof JPanel) {
                findAndSaveConfigs((JPanel) field, p);
            } else if (field instanceof JTextField) {
                JTextField textBox = (JTextField) field;

                value = textBox.getText();
                key = textBox.getName();

                if ((key == null) || (value == null)) {
                    continue;
                }

                // only save to file if the key does not match default.
                if (!value.equals(defaultOperationInfo.getDefault(key)) && (value.length() > 0)) {
                    p.println(key + "=" + value);
                }
            } else if (field instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) field;

                value = Boolean.toString(checkBox.isSelected());
                key = checkBox.getName();

                if ((key == null) || (value == null)) {
                    continue;
                }
                if (Boolean.parseBoolean(value) != Boolean.parseBoolean(defaultOperationInfo.getDefault(key))) {
                    p.println(key + "=" + value);
                }
            } else if (field instanceof JComboBox) {
                JComboBox<String> combo = (JComboBox) field;

                value = Integer.toString(combo.getSelectedIndex());
                key = combo.getName();

                if ((key == null) || (value == null)) {
                    continue;
                }
                if (Integer.parseInt(value) != Integer.parseInt(defaultOperationInfo.getDefault(key))) {
                    p.println(key + "=" + value);
                }
            } else if (field instanceof FlagTable) {
            	FlagTable table = (FlagTable) field;
            	value = table.exportFlagString();
            	key = table.getName();
            	if (!value.equalsIgnoreCase(defaultOperationInfo.getDefault(key))) {
            		p.println(key + "=" + value);
            	}
            }// else continue
        }

    }

    private JMenu createEditMenu() {
        JMenu jMenuOperations = new JMenu();

        jMenuOperations.setText("Edit");

        JMenuItem jMenuSendCurrentOperationFile = new JMenuItem();
        JMenuItem jMenuRetrieveOperationFile = new JMenuItem();
        JMenuItem jMenuSetOperationFile = new JMenuItem();
        JMenuItem jMenuSetNewOperationFile = new JMenuItem();
        JMenuItem jMenuSendAllOperationFiles = new JMenuItem();
        JMenuItem jMenuUpdateOperations = new JMenuItem();

        jMenuSendCurrentOperationFile.setText("Send Current File");
        jMenuSendCurrentOperationFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuSendCurrentFile_actionPerformed(e);
            }
        });

        jMenuRetrieveOperationFile.setText("Retrieve Operation File");
        jMenuRetrieveOperationFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((client.MWClient) mwclient).getMainFrame().jMenuRetrieveOperationFile_actionPerformed(e);
            }
        });

        jMenuSetOperationFile.setText("Set Operation File");
        jMenuSetOperationFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((client.MWClient) mwclient).getMainFrame().jMenuSetOperationFile_actionPerformed(e);
            }
        });

        jMenuSetNewOperationFile.setText("Set New Operation File");
        jMenuSetNewOperationFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((client.MWClient) mwclient).getMainFrame().jMenuSetNewOperationFile_actionPerformed(e);
            }
        });

        jMenuSendAllOperationFiles.setText("Send All Local Op Files");
        jMenuSendAllOperationFiles.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((client.MWClient) mwclient).getMainFrame().jMenuSendAllOperationFiles_actionPerformed(e);
            }
        });

        jMenuUpdateOperations.setText("Update Operations");
        jMenuUpdateOperations.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((client.MWClient) mwclient).getMainFrame().jMenuUpdateOperations_actionPerformed(e);
            }
        });

        int userLevel = ((client.MWClient) mwclient).getUser(((client.MWClient) mwclient).getUsername()).getUserlevel();
        if (userLevel >= ((client.MWClient) mwclient).getData().getAccessLevel("RetrieveOperation")) {
            jMenuOperations.add(jMenuRetrieveOperationFile);
        }
        if (userLevel >= ((client.MWClient) mwclient).getData().getAccessLevel("SetOperation")) {
            jMenuOperations.add(jMenuSendCurrentOperationFile);
            jMenuOperations.add(jMenuSetOperationFile);
            jMenuOperations.add(jMenuSetNewOperationFile);
            jMenuOperations.add(jMenuSendAllOperationFiles);
        }
        if (userLevel >= ((client.MWClient) mwclient).getData().getAccessLevel("UpdateOperations")) {
            jMenuOperations.add(jMenuUpdateOperations);
        }

        return jMenuOperations;

    }

    private void jMenuSendCurrentFile_actionPerformed(ActionEvent e) {

        if (changesMade) {
            saveShortOperations();
        }

        File opFile = new File("./data/operations/short/" + taskName + ".txt");
        if (!opFile.exists()) {
            return;
        }

        StringBuilder opData = new StringBuilder();

        try {
            FileInputStream fis = new FileInputStream(opFile);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
            opData.append(taskName + "#");
            while (dis.ready()) {
                opData.append(dis.readLine().replaceAll("#", "(pound)") + "#");
            }
            dis.close();
            fis.close();

        } catch (Exception ex) {
            MWLogger.errLog("Unable to read " + opFile);
            return;
        }

        ((client.MWClient) mwclient).sendChat(client.MWClient.CAMPAIGN_PREFIX + "c setoperation#short#" + opData.toString());
    }

    public void mouseClicked(MouseEvent arg0) {

        if (arg0.getButton() == MouseEvent.BUTTON3) {
            JTextField text = (JTextField) arg0.getSource();
            new TextEditorDialog(this, text);
        }

    }

    public void mouseEntered(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent arg0) {
        // TODO Auto-generated method stub

    }
}
