package mekwars.common.gui.panels;

/**
 * Callback interface implemented by components that want to receive raw text submitted from an input widget
 * (for example {@link CChatField}) when the user presses Enter.
 */
public interface IInputReceiver {
    /**
     * Called with the text the user submitted.
     *
     * @param input the raw text entered by the user
     *
     * @return {@code true} if the input was accepted/handled, which signals the caller (e.g. {@link CChatField}) that
     *       it should clear its text; {@code false} to leave the text in place.
     */
    boolean processInput(String input);
}
