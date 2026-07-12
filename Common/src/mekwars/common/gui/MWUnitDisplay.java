package mekwars.common.gui;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.util.MegaMekController;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * MekWars-specific subclass of MegaMek's {@link UnitDisplayPanel} (the panel that shows a unit's armor diagram,
 * weapons, equipment, etc.). It exists mainly to make sure any {@link Entity} shown by the client's unit-display
 * dialogs/panels is attached to a {@link megamek.common.game.Game} and has an owning {@link Player} before being
 * handed off to the underlying MegaMek display code, which otherwise expects entities to already be part of a
 * running game.
 */
public class MWUnitDisplay extends UnitDisplayPanel {
    /** The MekWars client this display is associated with; used to obtain the current game/player context. */
    IClient client;

    /**
     * Creates a unit display bound to a MegaMek {@link ClientGUI}.
     *
     * @param clientgui the MegaMek client GUI to attach to (may be {@code null})
     * @param client    the MekWars client providing game/player context
     */
    public MWUnitDisplay(@Nullable ClientGUI clientgui, IClient client) {
        super(clientgui);
        this.client = client;
    }

    /**
     * Creates a unit display bound to a MegaMek {@link ClientGUI} and input controller.
     *
     * @param clientgui  the MegaMek client GUI to attach to (may be {@code null})
     * @param controller the MegaMek input/hotkey controller to attach to (may be {@code null})
     * @param client     the MekWars client providing game/player context
     */
    public MWUnitDisplay(@Nullable ClientGUI clientgui, @Nullable MegaMekController controller, IClient client) {
        super(clientgui, controller);
        this.client = client;
    }

    /**
     * Creates a standalone unit display with no MegaMek {@link ClientGUI} attached.
     *
     * @param client the MekWars client providing game/player context
     */
    public MWUnitDisplay(IClient client) {
        super(null);
        this.client = client;
    }

    /**
     * Replaces the client reference used to look up game/player context for subsequent
     * {@link #displayEntity(Entity)} calls.
     */
    public void setClient(IClient client) {
        this.client = client;
    }

    /**
     * Displays the given entity, first ensuring it is attached to a game. If the entity has no game set yet, it is
     * assigned the client's current game; additionally, if that game currently has no players, a placeholder
     * {@link Player} (team 0, named after the client's own username) is created and set as the entity's owner so
     * that the display code (which expects an owning player) does not fail. Delegates the actual rendering to
     * {@link UnitDisplayPanel#displayEntity(Entity)}.
     *
     * @param theEntity the unit to display
     */
    public void displayEntity(Entity theEntity) {
        if (theEntity.getGame() == null) {
            if (client.getGame().getNoOfPlayers() == 0) {
                Player p = new Player(0, client.getUsername());
                p.setTeam(0);
                theEntity.setOwner(p);
            }
            theEntity.setGame(client.getGame());
        }


        super.displayEntity(theEntity);
    }
}
