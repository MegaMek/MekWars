package common.campaign.clientutils.protocol.commands;

import java.util.StringTokenizer;

import common.campaign.clientutils.protocol.IClient;
import common.util.MWLogger;

/**
 * Ping command
 */
public class PingPCmd extends CProtCommand {

	public PingPCmd(IClient client) {
		super(client);
		setName("ping");
	}

	// execute command
	@Override
	public boolean execute(String input) {

		StringTokenizer ST = new StringTokenizer(input, getDelimiter());
		if (check(ST.nextToken()) && ST.hasMoreTokens()) {
			input = decompose(input);
			ST = new StringTokenizer(input, getDelimiter());
			String sender = ST.nextToken();
			String stamp = ST.nextToken();

			MWLogger.infoLog("Received server ping.");

			getConnector().send(getPrefix() + "pong" + getDelimiter() + sender + getDelimiter() + stamp);
			if (!sender.equals("server")) {echo(input);}
			else {getClient().setLastPing(System.currentTimeMillis() / 1000);}
			return true;
		}
		//else
		return false; 
	}

	// echo command in GUI
	@Override
	protected void echo(String input) {

		StringTokenizer ST = new StringTokenizer(input, getDelimiter());
		String sender = ST.nextToken();
		getClient().systemMessage("Ping request from " + sender); 
	}

}