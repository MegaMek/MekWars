package mekwars.common.gui;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.unitDisplay.UnitDisplayPanel;
import megamek.client.ui.util.MegaMekController;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import mekwars.common.campaign.clientutils.protocol.IClient;

public class MWUnitDisplay extends UnitDisplayPanel {
    IClient client;

    public MWUnitDisplay(@Nullable ClientGUI clientgui, IClient client) {
        super(clientgui);
        this.client = client;
    }

    public MWUnitDisplay(@Nullable ClientGUI clientgui, @Nullable MegaMekController controller, IClient client) {
        super(clientgui, controller);
        this.client = client;
    }

    public MWUnitDisplay(IClient client) {
        super(null);
        this.client = client;
    }

    public void setClient(IClient client) {
        this.client = client;
    }

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
