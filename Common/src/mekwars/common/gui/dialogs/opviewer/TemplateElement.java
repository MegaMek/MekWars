package mekwars.common.gui.dialogs.opviewer;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.operations.Operation;

/**
 * One parsed fragment/token of the Operation Viewer's HTML template file ({@code OpTemplate.html}).
 * {@link OperationViewerDialog#parseTemplate()} splits the template on {@code "%%"} delimiters into a
 * sequence of these elements; rendering an Operation's page ({@link OperationViewerDialog#getOpHTML})
 * concatenates each element's {@link #getHTMLData(Operation)} output in order.
 *
 * <p>Each fragment is classified as either literal HTML (passed through unchanged) or a placeholder
 * "control" token to be substituted at render time:
 * <ul>
 *     <li>{@code opname} (case-insensitive) — replaced with the Operation's display name.</li>
 *     <li>{@code CC%<key>} — "Campaign Config": replaced with the value of server-wide config
 *         {@code <key>}, fetched via {@link IClient#getServerConfigs(String)}.</li>
 *     <li>{@code OP%<key>} — replaced with the value of property {@code <key>} on the specific
 *         Operation being rendered, via {@link Operation#getValue(String)}.</li>
 * </ul>
 * Anything not matching these patterns is treated as literal text.
 */
public class TemplateElement {
    /** Control type for a {@code CC%} fragment (Campaign Config: uses server-wide config values). */
    public final static int CONTROL_CAMPAIGN = 0;
    /** Control type for an {@code OP%} fragment (uses the current {@link Operation}'s own properties). */
    public final static int CONTROL_OP = 1;
    /** Control type for the {@code opname} fragment (substitutes the operation's display name). */
    public final static int CONTROL_NAME = 2;

    /**
     * For literal (non-control) fragments, the literal text itself. For {@code CC%}/{@code OP%}
     * fragments, the lookup key with its prefix stripped. Empty for {@code CONTROL_NAME} fragments.
     */
    private final String data;
    /** Used only for {@link #CONTROL_CAMPAIGN} lookups, to fetch server-wide config values. */
    private final IClient client;
    /** Whether this fragment is a substitution placeholder rather than literal text. */
    private boolean isControl = false;
    /** Which of the three substitution kinds applies; only meaningful when {@link #isControl} is true. */
    private int controlType;

    /**
     * Classifies the raw template fragment {@code s} as literal text or one of the three control types:
     * {@code "CC%"}-prefixed fragments become {@link #CONTROL_CAMPAIGN}, {@code "OP%"}-prefixed fragments
     * become {@link #CONTROL_OP}, and the literal token {@code "opname"} becomes {@link #CONTROL_NAME}.
     *
     * @param s the raw fragment text (already split on {@code "%%"} by the caller)
     * @param client used later to resolve {@code CC%} lookups against server config
     */
    public TemplateElement(String s, IClient client) {
        this.client = client;
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

    /**
     * Renders this fragment's contribution to the HTML for the given operation: literal fragments are
     * returned unchanged; {@code CONTROL_NAME} returns the operation's name; {@code CONTROL_OP}
     * ({@code OP%} fragments) looks up the key in the operation's own properties, reformatting the
     * value first if the key is one of the faction-list keys (see {@link #requiresFormat(String)});
     * {@code CONTROL_CAMPAIGN} ({@code CC%} fragments) looks the key up in server-wide config verbatim,
     * with no formatting applied even for the same faction-list key names.
     *
     * @param op the Operation currently being rendered, used to resolve {@code OP%} lookups
     * @return the HTML text this fragment contributes
     */
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
                return client.getServerConfigs(getData());
            }
        } else {
            return getData();
        }
    }

    /**
     * @return true if {@code s} names one of the four faction-list properties whose raw value is a
     *       {@code "$"}-delimited list that needs reformatting into a human-readable, comma-separated list
     */
    private boolean requiresFormat(String s) {
        return s.equalsIgnoreCase("LegalAttackFactions")
                     || s.equalsIgnoreCase("IllegalAttackFactions")
                     || s.equalsIgnoreCase("LegalDefendFactions")
                     || s.equalsIgnoreCase("IllegalDefendFactions");
    }

    private String getData() {
        return data;
    }

    /**
     * Reformats a faction-list value by replacing its {@code "$"} delimiters with {@code ", "} for
     * display. For any other key this returns {@code value} unchanged; in practice that fallback is
     * never reached via {@link #getHTMLData(Operation)} since it only calls this method after
     * {@link #requiresFormat(String)} has already confirmed {@code key} is one of the four faction-list keys.
     */
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
