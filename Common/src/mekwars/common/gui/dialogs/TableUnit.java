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
 * <p>
 * This class is the "model" element of the small unit-table viewer trio in this package, alongside
 * {@link MekTableModel} (an {@code AbstractTableModel} that is largely unrelated -- see {@code TableViewerModel}
 * for the model actually paired with this class) and {@link TableViewerRenderer} (the cell renderer that reads
 * {@code TableUnit} instances out of a {@code TableViewerModel} to build tooltips). A {@code TableUnit} does not
 * represent a single physical unit in an army/hangar; it represents one distinct unit design as it appears one or
 * more times across a set of "build tables" (e.g. random-encounter tables), together with the relative frequency
 * (weight) that design occurs with and the names of the tables it was pulled from.
 */
public class TableUnit extends CUnit {
    private final static MMLogger LOGGER = MMLogger.create(TableUnit.class);

    // IVARS
    /** Relative frequency/weight of this unit design across the table(s) it was loaded from; combined via {@link #addFrequencyFrom}. */
    double frequency;
    /** The original, unmodified unit filename (including extension) this TableUnit was constructed from or derived for. */
    String realFilename;
    /** Maps each build-table name this unit design appears on to that table's frequency value for the unit. */
    TreeMap<String, Double> tables;

    // CONSTRUCTOR
    /**
     * Creates a TableUnit by loading the {@link Entity} for the given unit file, preferring the shared
     * {@link MekSummaryCache} (fast path, works with unzipped data directories) and falling back to normal
     * server-style zip-based entity creation if the cache lookup fails for any reason. A generic "Autopilot"
     * pilot (skill 4/5) is assigned since TableUnit has no source data to pull a real pilot from.
     *
     * @param fileName  the unit's data file name (e.g. ending in .mtf or .blk); the trailing 4-character
     *                  extension is stripped before the cache lookup
     * @param frequency the initial relative frequency/weight for this unit design
     */
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

    /**
     * Fallback entity creation used by the {@link #TableUnit(String, double)} constructor when the summary-cache
     * lookup fails. Despite the "WithCache" name, this simply delegates to {@link UnitUtils#createEntity(String)}
     * (the normal server-style loader) rather than consulting a cache itself.
     *
     * @param fileName the unit's data file name
     */
    private void createEntityFromFileNameWithCache(String fileName) {
        unitEntity = UnitUtils.createEntity(fileName);
    }

    /**
     * Creates a TableUnit directly from an already-loaded {@link Entity}, skipping file/cache lookups entirely.
     * A generic "Autopilot" pilot is assigned, and the unit's filename is derived from the entity itself.
     *
     * @param entity    the already-constructed entity to wrap
     * @param frequency the initial relative frequency/weight for this unit design
     */
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

    /**
     * Merges another TableUnit's frequency into this one, used when the same unit design is encountered again
     * (e.g. on another build table) and its occurrences should be combined into a single running total rather
     * than tracked as a separate entry.
     *
     * @param tableUnit the other TableUnit whose frequency should be added into this one's
     */
    public void addFrequencyFrom(TableUnit tableUnit) {
        frequency += tableUnit.getFrequency();
    }

    // METHODS
    /** @return the current relative frequency/weight of this unit design. */
    public double getFrequency() {
        return frequency;
    }

    /** @return the original unit data file name this TableUnit was built from or derived for. */
    public String getRealFilename() {
        return realFilename;
    }

    /** @return the map of build-table name to per-table frequency value for this unit design. */
    public TreeMap<String, Double> getTables() {
        return tables;
    }

}// end TableUnit
