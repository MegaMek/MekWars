package mekwars.common.gui.dialogs;

import java.util.TreeMap;

import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.util.UnitUtils;

/**
 * TableUnit is a CUnit with added stat-tracking for ongoing frequency calculations. Much like the BMUnit; however, the
 * TableUnit is less complex.
 */
public class TableUnit extends CUnit {
    private final static MMLogger LOGGER = MMLogger.create(TableUnit.class);

    // IVARS
    double frequency;
    String realFilename;
    TreeMap<String, Double> tables;

    // CONSTRUCTOR
    public TableUnit(String fileName, double frequency) {
        super();

        /*
         * Since the TableUnit has no data fileName to set things with,
         * hard flag necessary values.
         */
        setUnitFilename(fileName.trim());
        setPilot(new Pilot("Autopilot", 4, 5));

        /*
         * Try to get an entity from the unit cache, given a filename. This
         * makes it possible to use the build table viewer with unzipped
         * file structures (like that in use on the new MMNET). If this
         * fails, use normal server-style zip loading.
         */
        try {

            // remove .MTF, .blk, etc.
            String modfn = fileName.trim();
            modfn = modfn.substring(0, modfn.length() - 4);

            // get the unit from the summary cache
            MekSummary mekSummary = MekSummaryCache.getInstance().getMek(modfn);
            unitEntity = new MekFileParser(mekSummary.getSourceFile(), mekSummary.getEntryName()).getEntity();
        } catch (Exception e) {
            createEntityFromFileNameWithCache(fileName.trim());// make the
            // entity
        }

        realFilename = fileName;
        this.frequency = frequency;

        tables = new TreeMap<>();
    }

    private void createEntityFromFileNameWithCache(String fileName) {
        unitEntity = UnitUtils.createEntity(fileName);
    }

    public TableUnit(Entity entity, double frequency) {
        super();

        realFilename = UnitUtils.getEntityFileName(entity);

        setUnitFilename(realFilename);
        setPilot(new Pilot("Autopilot", 4, 5));

        // get the unit from the summary cache
        unitEntity = entity;

        this.frequency = frequency;

        tables = new TreeMap<>();
    }

    public void addFrequencyFrom(TableUnit tableUnit) {
        frequency += tableUnit.getFrequency();
    }

    // METHODS
    public double getFrequency() {
        return frequency;
    }

    public String getRealFilename() {
        return realFilename;
    }

    public TreeMap<String, Double> getTables() {
        return tables;
    }

}// end TableUnit
