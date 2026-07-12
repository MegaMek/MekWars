/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package mekwars.common.commands;

import java.util.StringTokenizer;

import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.RePodSelectorDialog;

/**
 * Client-side handler for the {@code UnitRePodDialogCommand} protocol message. The server sends this in response
 * to a re-pod request for a unit, telling the client either that no re-pod options exist for that unit, or which
 * chassis options are available; executing it either prints an informational chat message or opens the
 * {@link RePodSelectorDialog} so the player can choose a replacement chassis ("re-pod").
 *
 * @author jtighe
 */
public class UnitRePodDialogCommand extends Command {
    private static final MMLogger LOGGER = MMLogger.create(UnitRePodDialogCommand.class);

    /**
     *
     */
    public UnitRePodDialogCommand(IClient client) {
        super(client);
    }

    /**
     * Parses the unit ID and, if present, a chassis list from the command payload.
     * <p>
     * If no chassis-list token follows the unit ID, a "no re-pod options" chat message is synthesized (using the
     * {@code CH|} chat-command prefix) and fed back into the client via {@link IClient#doParseDataInput(String)}
     * as if it were an incoming server chat line. Otherwise, a {@link UnitLoadingDialog} is created and a
     * {@link RePodSelectorDialog} is built around it, then launched on its own {@link Thread}.
     * <p>
     * Quirk: this method calls {@code sleep(125)} (inherited from {@link Thread}, since {@link Command} itself
     * extends {@code Thread}) between constructing the dialog and starting the worker thread, unconditionally
     * blocking whatever thread is executing this command for 125ms. Since {@code Command.execute} is typically
     * invoked directly from network/dispatch code rather than from this object's own run loop, this sleep likely
     * blocks the caller (e.g. the network read thread), not a dedicated thread for this command.
     * <p>
     * Any exception thrown while parsing or building the dialog is caught and only logged; it is never
     * rethrown or surfaced to the user.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);

        try {
            String unitId = stringTokenizer.nextToken();

            if (!stringTokenizer.hasMoreTokens()) {
                String toUser = String.format("CH|CLIENT: Your faction has no re-pod options for Unit %s.", unitId);
                client.doParseDataInput(toUser);
            } else {
                String chassisList = stringTokenizer.nextToken();
                UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(client.getMainFrame());
                RePodSelectorDialog rePodSelector = new RePodSelectorDialog(client.getMainFrame(),
                      unitLoadingDialog,
                      client,
                      chassisList,
                      unitId);
                sleep(125);
                new Thread(rePodSelector).start();
            }

        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to run RePod Dialog");
        }
    }

    /**
     * Unused on the client side; this command has no reply-argument parsing behavior.
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * Unused on the client side; this command is never parsed as server-bound arguments.
     */
    @Override
    public void parseArguments(String s) {

    }
}
