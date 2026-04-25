package mekwars.common.campaign.clientutils.protocol;

public interface IClient {
    /**
     * The delimiter.  A tab character.
     */
    String DELIMITER = "\t";

    String PROTOCOL_DELIMITER = "\t"; // delimiter for protocol commands
    String PROTOCOL_PREFIX = "/"; // prefix for protocol commands

    /**
     * if you understand this you are a 1.1-compliant client. Following DEFLATED + DELIMITER is the number of bytes in
     * the undeflated text. This will be a maximum of 29999, so you don't have to buffer more than that.
     * com.carnageblender.chat.net gives an example implemenation.
     */
    String DEFLATED = "/deflated";

    // called when there's a system message to show
    void systemMessage(String message);

    // called when there's an error message to show
    void errorMessage(String message);

    // called when there's server input to process
    void processIncoming(String incoming);

    // called when connection is lost
    void connectionLost();

    // called when connection is established
    void connectionEstablished();

    CConnector getConnector();

    void startHost(boolean dedicated, boolean deploy, boolean loadSavedGame);

    boolean isDedicated();

    void setUsername(String name);

    void doParseDataInput(String input);

    void parseDedDataInput(String input);

    void setLastPing(long lastPing);
}
