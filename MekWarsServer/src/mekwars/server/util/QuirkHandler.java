package mekwars.server.util;

import java.util.Enumeration;
import java.util.StringJoiner;

import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.logging.MMLogger;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SUnit;

//@Salient this is a wrapper class for MM's QuirksHandler
public class QuirkHandler {
    private static final MMLogger LOGGER = MMLogger.create(QuirkHandler.class);
    private static QuirkHandler handler;

    protected QuirkHandler() {
    }

    public static QuirkHandler getInstance() {
        if (handler == null) {
            try {
                handler = new QuirkHandler();
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }

        return handler;
    }

    public void setQuirks(SUnit unit) {
        if (CampaignMain.campaignMain.getBooleanConfig("EnableQuirks")) {
            LOGGER.debug(String.format("%s %s Quirks: %s", unit.getModelName(), unit.getId(), returnQuirkList(unit)));
        }
    }

    /**
     * @return quirksList
     */
    public String returnQuirkList(SUnit unit) {
        StringJoiner quirksList = new StringJoiner("&");

        for (Enumeration<IOptionGroup> optionGroups = unit.getEntity().getQuirks().getGroups();
              optionGroups.hasMoreElements(); ) {
            IOptionGroup group = optionGroups.nextElement();
            if (unit.getEntity().getQuirks().count(group.getKey()) > 0) {
                for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                    IOption option = options.nextElement();

                    if (option != null && option.booleanValue()) {
                        quirksList.add(option.getName());
                    }
                }
            }
        }

        if (StringUtil.isNullOrEmpty(quirksList.toString())) {
            quirksList.add("None");
        }

        return quirksList.toString();
    }

    /**
     * @return quirksList
     */
    public String returnQuirkSave(SUnit unit) {
        if (CampaignMain.campaignMain.getBooleanConfig("EnableQuirks")) {
            StringJoiner quirksList = new StringJoiner("!");
            quirksList.add(returnHtmlQuirkList(unit));
            quirksList.add(returnQuirkList(unit));

            LOGGER.debug(String.format("%s: %s", unit.getVerboseModelName(), quirksList.toString()));

            return quirksList.toString(); // if a unit has no quirks, it will return a "!"
        }

        return " ";
    }

    /**
     * @return quirksList
     */
    public String returnHtmlQuirkList(SUnit unit) {
        StringJoiner quirksList = new StringJoiner("<br>*");

        for (Enumeration<IOptionGroup> optionGroups = unit.getEntity().getQuirks().getGroups();
              optionGroups.hasMoreElements(); ) {
            IOptionGroup group = optionGroups.nextElement();

            if (unit.getEntity().getQuirks().count(group.getKey()) > 0) {
                for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                    IOption option = options.nextElement();
                    if (option != null && option.booleanValue()) {
                        quirksList.add(option.getDisplayableNameWithValue());
                    }
                }
            }
        }

        if (StringUtil.isNullOrEmpty(quirksList.toString())) {
            quirksList.add("None");
        }

        return quirksList.toString();
    }

    public boolean hasQuirks(SUnit unit) {
        for (Enumeration<IOptionGroup> optionGroups = unit.getEntity().getQuirks().getGroups();
              optionGroups.hasMoreElements(); ) {
            IOptionGroup group = optionGroups.nextElement();

            if (unit.getEntity().getQuirks().count(group.getKey()) > 0) {
                for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                    IOption option = options.nextElement();

                    if (option != null && option.booleanValue()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

