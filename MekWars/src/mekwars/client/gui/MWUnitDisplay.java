package client.gui;

import client.FakePlayer;
import client.MWClient;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.annotations.Nullable;

public class MWUnitDisplay extends UnitDisplay {
	MWClient client;
	
	public MWUnitDisplay(@Nullable ClientGUI clientgui, MWClient client) {
		super(clientgui);
		this.client = client;
	}
	
	public MWUnitDisplay(@Nullable ClientGUI clientgui,
            @Nullable MegaMekController controller, MWClient client) {
		super(clientgui, controller);
		this.client = client;
	}
	
	public MWUnitDisplay(MWClient client) {
		super(null);
		this.client = client;
	}
	
	public void setClient(MWClient client) {
		this.client = client;
	}
	
	public void displayEntity(Entity theEntity) {
        if(theEntity.getGame() == null) {
        	if(client.getGame().getNoOfPlayers() == 0) {
        		Player p = new Player(0,client.getUsername());
        		p.setTeam(0);
        		theEntity.setOwner(p);
        	}
        	theEntity.setGame(client.getGame());
        }

        
        super.displayEntity(theEntity);
	}
}
