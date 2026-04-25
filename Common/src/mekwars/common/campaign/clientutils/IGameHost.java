package mekwars.common.campaign.clientutils;

import megamek.common.event.GameCFREvent;

public interface IGameHost {
    void changeStatus(int newStatus);

    boolean isAdmin();

    boolean isMod();

    String getUsername();

    void gameClientFeedbackRequest(GameCFREvent arg0);
}
