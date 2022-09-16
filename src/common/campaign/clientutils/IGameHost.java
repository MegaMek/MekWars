package common.campaign.clientutils;

import megamek.common.event.GameCFREvent;

public interface IGameHost {
	public void changeStatus(int newStatus);
    public boolean isAdmin();
    public boolean isMod();
    public String getUsername();
	void gameClientFeedbackRquest(GameCFREvent arg0);
}
