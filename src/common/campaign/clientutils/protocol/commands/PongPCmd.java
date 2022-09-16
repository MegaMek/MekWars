package common.campaign.clientutils.protocol.commands;

import java.util.StringTokenizer;

import common.campaign.clientutils.protocol.IClient;

/**
 * Pong command
 */

public class PongPCmd extends CProtCommand {

	public PongPCmd(IClient mwclient) {
		super(mwclient);
		name = "pong";
	}

	// execute command
	@Override
	public boolean execute(String input) {

		StringTokenizer ST = new StringTokenizer(input, delimiter);
		if (check(ST.nextToken()) && ST.hasMoreTokens()) {
			input = decompose(input);
			echo(input);
			return true;
		}

		//else
		return false; 
	}

	// echo command in GUI
	@Override
	protected void echo(String input) {
		StringTokenizer ST = new StringTokenizer(input, delimiter);
		String sender = ST.nextToken();
		if (sender.equals("server")) {return;}
		float time = (float)(System.currentTimeMillis() - Long.parseLong(ST.nextToken())) / 1000;
		client.systemMessage("Ping reply from " + sender + ": " + time + " s");
	}

}