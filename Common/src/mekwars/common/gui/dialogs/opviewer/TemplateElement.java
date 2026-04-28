package mekwars.common.gui.dialogs.opviewer;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.operations.Operation;

public class TemplateElement {
    public final static int CONTROL_CAMPAIGN = 0;
    public final static int CONTROL_OP = 1;
    public final static int CONTROL_NAME = 2;

    private final String data;
    private final IClient client;
    private boolean isControl = false;
    private int controlType;

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

    private boolean requiresFormat(String s) {
        return s.equalsIgnoreCase("LegalAttackFactions")
                     || s.equalsIgnoreCase("IllegalAttackFactions")
                     || s.equalsIgnoreCase("LegalDefendFactions")
                     || s.equalsIgnoreCase("IllegalDefendFactions");
    }

    private String getData() {
        return data;
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
