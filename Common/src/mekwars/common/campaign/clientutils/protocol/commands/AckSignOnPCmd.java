package mekwars.common.campaign.clientutils.protocol.commands;

import java.util.StringTokenizer;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.MWLogger;

/**
 * AckSignOn command
 */

public class AckSignOnPCmd extends CProtCommand {

    public AckSignOnPCmd(IClient mwclient) {
        super(mwclient);
        name = "ack_sign_on";
    }

    // execute command
    @Override
    public boolean execute(String input) {
        StringTokenizer ST = new StringTokenizer(input, delimiter);
        if (check(ST.nextToken()) && ST.hasMoreTokens()) {
            input = decompose(input);
            ST = new StringTokenizer(input, delimiter);
            client.setUsername(ST.nextToken());
            echo(input);
            if (client.isDedicated()) {

                try {Thread.sleep(5000);} catch (Exception ex) {MWLogger.errLog(ex);}

                try {
                    client.startHost(true, false, false);
                } catch (Exception ex) {
                    MWLogger.errLog("AckSignOnPCmd: Error attempting to start host on signon.");
                    MWLogger.errLog(ex);
                }
            }

            return true;
        }
        //else
        return false;
    }

    // echo command in GUI
    @Override
    protected void echo(String input) {
        MWLogger.infoLog("Signon acknowledged");
        MWLogger.errLog("Signon acknowledged");
    }

}
