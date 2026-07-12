package mekwars.common.gui.panels;

import java.io.Serial;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.StringUtils;

/**
 * The single-line text field the player types chat messages into (used by {@link CCommPanel}). On top of being a
 * plain {@link javax.swing.JTextField}, it adds three behaviors on key/action events:
 * <ul>
 *   <li>Enter submits the current text to a registered {@link IInputReceiver}, clearing the field only if the
 *       receiver reports it handled the input; the submitted text is always appended to an in-memory chat
 *       history.</li>
 *   <li>Up/Down arrows cycle backwards/forwards through that chat history (like a shell history), replacing the
 *       field's current contents.</li>
 *   <li>Tab cycles through user names that match the last "word" typed, for nick autocompletion; Escape closes the
 *       currently focused error-log or private-message tab in the owning {@link CCommPanel}.</li>
 * </ul>
 */
public class CChatField extends javax.swing.JTextField
      implements java.awt.event.ActionListener, java.awt.event.KeyListener {

    @Serial
    private static final long serialVersionUID = -9140296134799032871L;

    /** The chat tab/panel this field belongs to; used to reach sibling components (tabs, error log, mail tabs). */
    private final CCommPanel cCommPanel;
    /** Connection/session handle, used for partial-username lookup during Tab-completion. */
    IClient Client;
    /**
     * Position within {@link #ChatHistory} while navigating with Up/Down, counted back from the end of the list.
     * 0 means "not currently navigating history" (i.e. the live/newest entry).
     */
    int ChatHistoryNumber = 0;
    /** Index into {@link #Users} of the current Tab-completion candidate; reset to 0 whenever completion restarts. */
    int UserNumber = 0;

    /** Every message the player has previously submitted, oldest first, used for Up/Down history recall. */
    java.util.ArrayList<String> ChatHistory = new java.util.ArrayList<String>();
    /** Candidate usernames matching the current partial nick, populated when Tab-completion starts. */
    java.util.ArrayList<String> Users = new java.util.ArrayList<String>();

    /** Callback invoked with the submitted text when Enter is pressed; set via {@link #setReceiver(IInputReceiver)}. */
    IInputReceiver myReceiver;
    /** The text typed before the nick being completed, cached across successive Tab presses of one completion cycle. */
    String textandnick = "";

    /**
     * Wires this field to its owning chat panel and applies the configured chat colors (background, font, and
     * caret color all read from client config).
     *
     * @param cCommPanel the chat panel that owns this field (used to reach its tabs on Escape)
     * @param client     used to read chat color configuration
     */
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

    /**
     * Registers the callback that will receive submitted chat text (typically the {@link CCommPanel} itself or a
     * command dispatcher).
     *
     * @param receiver callback to invoke on Enter; may be {@code null} to disable submission
     */
    public void setReceiver(IInputReceiver receiver) {
        this.myReceiver = receiver;
    }

    /**
     * Invoked when the user presses Enter in the field. The current text is unconditionally appended to
     * {@link #ChatHistory} (even if the receiver rejects it) and history navigation is reset. The field is cleared
     * only if a receiver is registered and {@link IInputReceiver#processInput(String)} returns {@code true}.
     *
     * @param actionEvent the Enter key event (unused; current text is read via {@link #getText()})
     */
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        ChatHistory.add(getText());
        ChatHistoryNumber = 0;
        if (this.myReceiver != null) {
            if (myReceiver.processInput(getText())) {
                setText("");
            }
        }
    }

    /** Unused; required by {@link java.awt.event.KeyListener} but intentionally does nothing. */
    public void keyTyped(java.awt.event.KeyEvent e) {
        // filler
    }

    /**
     * Handles the field's special key bindings:
     * <ul>
     *   <li>{@code VK_UP}/{@code VK_DOWN} - step backward/forward through {@link #ChatHistory}, replacing the
     *       field text; reaching index 0 while moving down clears the field.</li>
     *   <li>{@code VK_TAB} - nickname autocompletion: on the first press, looks up usernames matching the last
     *       word of the current text via {@link IClient#getPartialUser(String)} and appends the first match;
     *       subsequent presses (while still in the same completion cycle) cycle to the next match, wrapping
     *       around.</li>
     *   <li>{@code VK_ESCAPE} - closes the currently selected tab in the owning {@link CCommPanel} if it is the
     *       error log tab, or (when multiple-PM mode is enabled) a private-message tab whose name starts with
     *       {@code "Mail Tab "}.</li>
     *   <li>any other key - resets {@link #UserNumber}, ending any in-progress Tab-completion cycle.</li>
     * </ul>
     *
     * @param e the key-press event
     */
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

    /**
     * Strips the trailing partial-nickname word off the given text, returning everything typed before it. Used
     * during Tab-completion so the completed username can be re-appended after this prefix. If the text has no
     * spaces (the whole field is the partial nick), the prefix is the empty string.
     *
     * @param text the current field text, ending in the partial nickname being completed
     *
     * @return the text before the last word, trimmed; empty string if {@code text} is a single word
     */
    public String parseOutUserName(String text) {

        // there are spaces in the text so get the last word
        if (text.trim().indexOf(" ") != -1) {text = text.substring(0, text.trim().lastIndexOf(" ")).trim();}

        // The name is the first word.
        else {text = "";}

        return text;
    }

    /** Unused; required by {@link java.awt.event.KeyListener} but intentionally does nothing. */
    public void keyReleased(java.awt.event.KeyEvent e) {
        // filler
    }
}
