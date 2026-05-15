package mekwars.server.util;


import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import mekwars.server.campaign.CampaignMain;

//@Salient this is a wrapper class for MM's QuirksHandler
public class QuirkHandler {
    private static mekwars.server.util.QuirkHandler handler;

    protected QuirkHandler() throws Exception {
        if (CampaignMain.campaignMain.getBooleanConfig("EnableQuirks")) {
            QuirksHandler.initQuirksList();
        }

    }

    public static mekwars.server.util.QuirkHandler getInstance() {
        if (handler == null) {
            try {
                handler = new mekwars.server.util.QuirkHandler();
            } catch (Exception e) {
                MWLogger.errLog(e);
            }
        }
        return handler;
    }

    public void setQuirks(server.campaign.SUnit unit) {
        if (CampaignMain.campaignMain.getBooleanConfig("EnableQuirks")) {
            unit.getEntity().loadDefaultQuirks();
            MWLogger.debugLog(unit.getModelName() + " " + unit.getId() + " Quirks: " + returnQuirkList(unit));
        }
    }

    /**
     * @param unit
     *
     * @return quirksList
     */
    public String returnQuirkList(server.campaign.SUnit unit) {
        java.util.StringJoiner quirksList = new java.util.StringJoiner("&");

        for (java.util.Enumeration<IOptionGroup> optionGroups = unit.getEntity().getQuirks().getGroups();
              optionGroups.hasMoreElements(); ) {
            IOptionGroup group = optionGroups.nextElement();
            if (unit.getEntity().getQuirks().count(group.getKey()) > 0) {
                for (java.util.Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
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
     * @param unit
     *
     * @return quirksList
     */
    public String returnQuirkSave(server.campaign.SUnit unit) {
        if (CampaignMain.campaignMain.getBooleanConfig("EnableQuirks")) {
            java.util.StringJoiner quirksList = new java.util.StringJoiner("!");
            quirksList.add(returnHtmlQuirkList(unit));
            quirksList.add(returnQuirkList(unit));

            MWLogger.debugLog(unit.getVerboseModelName() + ": " + quirksList.toString());

            return quirksList.toString(); // if a unit has no quirks, it will return a "!"
        }

        return " ";
    }

    /**
     * @param unit
     *
     * @return quirksList
     */
    public String returnHtmlQuirkList(server.campaign.SUnit unit) {
        java.util.StringJoiner quirksList = new java.util.StringJoiner("<br>*");

        for (java.util.Enumeration<IOptionGroup> optionGroups = unit.getEntity().getQuirks().getGroups();
              optionGroups.hasMoreElements(); ) {
            IOptionGroup group = optionGroups.nextElement();
            if (unit.getEntity().getQuirks().count(group.getKey()) > 0) {
                for (java.util.Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
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

    public boolean hasQuirks(server.campaign.SUnit unit) {
        for (java.util.Enumeration<IOptionGroup> optionGroups = unit.getEntity().getQuirks().getGroups();
              optionGroups.hasMoreElements(); ) {
            IOptionGroup group = optionGroups.nextElement();
            if (unit.getEntity().getQuirks().count(group.getKey()) > 0) {
                for (java.util.Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
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

