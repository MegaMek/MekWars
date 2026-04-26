package mekwars.common.gui.dialogs;

import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import mekwars.common.campaign.CUnit;

/**
 * TableUnit is a CUnit with added stat-tracking for ongoing frequency calculations. Much like the BMUnit; however, the
 * TableUnit is less complex.
 */
public class TableUnit extends CUnit {

    // IVARS
    double frequency;
    String realFilename;
    java.util.TreeMap<String, Double> tables;

    // CONSTRUCTOR
    public TableUnit(String fn, double f) {
        super();

        /*
         * Since the TableUnit has no data string to set things with,
         * hardflag necessary values.
         */
        setUnitFilename(fn.trim());
        setPilot(new mekwars.common.campaign.pilot.Pilot("Autopilot", 4, 5));

        /*
         * Try to get an entity from the unit cache, given a filename. This
         * makes it possible to use the build table viewer with unzipped
         * file structures (like that in use on the new MMNET). If this
         * fails, use normal server-style zip loading.
         */
        try {

            // remove .MTF, .blk, etc.
            String modfn = fn.trim();
            modfn = modfn.substring(0, modfn.length() - 4);

            // get the unit from the summary cache
            MekSummary ms = MekSummaryCache.getInstance().getMek(modfn);
            unitEntity = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();

        } catch (Exception e) {
            createEntityFromFileNameWithCache(fn.trim());// make the
            // entity
        }

        realFilename = fn;
        frequency = f;

        tables = new java.util.TreeMap<>();
    }

    public TableUnit(Entity en, double f) {
        super();

        realFilename = mekwars.common.util.UnitUtils.getEntityFileName(en);


        setUnitFilename(realFilename);
        setPilot(new mekwars.common.campaign.pilot.Pilot("Autopilot", 4, 5));

        // get the unit from the summary cache
        unitEntity = en;


        frequency = f;

        tables = new java.util.TreeMap<>();
    }

    // METHODS
    public double getFrequency() {
        return frequency;
    }

    public void addFrequencyFrom(mekwars.common.gui.dialogs.TableUnit u) {
        frequency += u.getFrequency();
    }

    public String getRealFilename() {
        return realFilename;
    }

    public java.util.TreeMap<String, Double> getTables() {
        return tables;
    }

    private void createEntityFromFileNameWithCache(String fn) {

        unitEntity = mekwars.common.util.UnitUtils.createEntity(fn);

        if (unitEntity == null) {
            createEntityFromFilename(fn);
        }
    }

    /**
     * Tries to setUnitEntity from a filename w/ extension. This used to be the default way of getting units, but CUnit
     * was changed to use the MegaMek summary cache. Because the table viewer reads the tables the same way the server
     * does, it needs a server-style loading cascade, ugly as it may be :-(
     */
    private void createEntityFromFilename(String fn) {

        unitEntity = null;
        try {
            unitEntity = new MekFileParser(new java.io.File("./data/mechfiles/Meks.zip"), fn).getEntity();
        } catch (Exception e) {
            try {
                unitEntity = new MekFileParser(new java.io.File("./data/mechfiles/Vehicles.zip"), fn).getEntity();
            } catch (Exception ex) {
                try {
                    unitEntity = new MekFileParser(new java.io.File("./data/mechfiles/Infantry.zip"),
                          fn).getEntity();
                } catch (Exception exc) {
                    try {
                        mekwars.common.util.MWLogger.errLog(STR."Error loading unit: \{fn}. Try replacing with OMG.");
                        unitEntity = mekwars.common.util.UnitUtils.createOMG();// new
                    } catch (Exception exepe) {
                        mekwars.common.util.MWLogger.errLog("Error unit failed to load. Exiting.");
                        System.exit(1);
                    }
                }
            }
        }

        setType(getEntityType(unitEntity));
        getC3Type(unitEntity);
    }

}// end TableUnit
