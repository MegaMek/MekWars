package mekwars.common.gui.panels;

import java.io.Serial;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.StringUtils;

// chat field class
public class CChatField extends javax.swing.JTextField
      implements java.awt.event.ActionListener, java.awt.event.KeyListener {

    @Serial
    private static final long serialVersionUID = -9140296134799032871L;

    private final CCommPanel cCommPanel;
    IClient Client;
    int ChatHistoryNumber = 0;
    int UserNumber = 0;

    java.util.ArrayList<String> ChatHistory = new java.util.ArrayList<String>();
    java.util.ArrayList<String> Users = new java.util.ArrayList<String>();

    IInputReceiver myReceiver;
    String textandnick = "";

    public CChatField(CCommPanel cCommPanel, IClient client) {
        this.cCommPanel = cCommPanel;
        Client = client;
        this.addActionListener(this);
        this.addKeyListener(this);
        this.setFocusTraversalKeysEnabled(false);
        this.setBackground(StringUtils.html2Color(cCommPanel.client.getConfigParam("BACKGROUNDCOLOR")));
        this.setForeground(StringUtils.html2Color(cCommPanel.client.getConfigParam("CHATFONTCOLOR")));
        this.setCaretColor(StringUtils.html2Color(cCommPanel.client.getConfigParam("CHATFONTCOLOR")));
    }

    public void setReceiver(IInputReceiver receiver) {
        this.myReceiver = receiver;
    }

    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        ChatHistory.add(getText());
        ChatHistoryNumber = 0;
        if (this.myReceiver != null) {
            if (myReceiver.processInput(getText())) {
                setText("");
            }
        }
    }

    public void keyTyped(java.awt.event.KeyEvent e) {
        // filler
    }

    public void keyPressed(java.awt.event.KeyEvent e) {

        if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
            UserNumber = 0;
            if (ChatHistoryNumber < ChatHistory.size()) {
                ChatHistoryNumber++;
                setText(ChatHistory.get(ChatHistory.size() - ChatHistoryNumber));
            }
        } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
            UserNumber = 0;
            if (ChatHistoryNumber > 0) {
                ChatHistoryNumber--;
                if (ChatHistoryNumber == 0) {
                    setText("");
                } else {
                    setText(ChatHistory.get(ChatHistory.size() - ChatHistoryNumber));
                }
            }
        } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_TAB) {
            String messageText = "";
            if (UserNumber == 0) {
                Users = Client.getPartialUser(getText());
                // No users where found keep the UserNumber at 0 and
                // wait for the next key press.
                if (Users == null || Users.isEmpty()) {return;}
                textandnick = parseOutUserName(getText());
                // Get rid of any leading spaces if the name is the first
                // thing typed.
                messageText = textandnick.trim() + " " + Users.get(UserNumber++).trim();
            } else if (UserNumber < Users.size()) {
                messageText = textandnick.trim() + " " + Users.get(UserNumber++).trim();
            } else {
                UserNumber = 0;
                messageText = textandnick.trim() + " " + Users.get(UserNumber++).trim();
            }
            setText(messageText.trim());
        } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {

            if (cCommPanel.CommTPane.getSelectedIndex() ==
                      cCommPanel.CommTPane.indexOfComponent(cCommPanel.ErrorLogPanel)) {
                cCommPanel.CommTPane.remove(cCommPanel.CommTPane.getSelectedComponent());
            } else if (cCommPanel.client.getConfig().isParam("USEMULTIPLEPM") &&
                             cCommPanel.CommTPane.getSelectedComponent() instanceof javax.swing.JPanel) {
                javax.swing.JPanel panel = (javax.swing.JPanel) cCommPanel.CommTPane.getSelectedComponent();
                if (panel.getName() != null && panel.getName().startsWith("Mail Tab ")) {
                    cCommPanel.CommTPane.remove(panel);
                    panel = null;
                }
            }
        } else {UserNumber = 0;}

    }

    public String parseOutUserName(String text) {

        // there are spaces in the text so get the last word
        if (text.trim().indexOf(" ") != -1) {text = text.substring(0, text.trim().lastIndexOf(" ")).trim();}

        // The name is the first word.
        else {text = "";}

        return text;
    }

    public void keyReleased(java.awt.event.KeyEvent e) {
        // filler
    }
}
