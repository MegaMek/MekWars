package common.campaign.clientutils.protocol;

public interface IClient
{
    /**
     * The delimiter.  A tab character.
     */
    public static final String DELIMITER = "\t";
    
    public static final String PROTOCOL_DELIMITER = "\t"; // delimiter for protocol commands
    public static final String PROTOCOL_PREFIX = "/"; // prefix for protocol commands

    /** if you understand this you are a 1.1-compliant client.
     *  Following DEFLATED + DELIMITER is the number of bytes in the undeflated text.
     *  This will be a maximum of 29999, so you don't have to buffer more than that.
     *  com.carnageblender.chat.net gives an example implemenation.
     */
    public static final String DEFLATED = "/deflated";

    // called when there's a system message to show
    public void systemMessage(String message);

    // called when there's an error message to show
    public void errorMessage(String message);

    // called when there's server input to process
    public void processIncoming(String incoming);

    // called when connection is lost
    public void connectionLost();

    // called when connection is established
    public void connectionEstablished();
    
    public CConnector getConnector();

	public void startHost(boolean dedicated, boolean deploy, boolean loadSavedGame);

	public boolean isDedicated();

	public void setUsername(String name);

	public void doParseDataInput(String input);

	public void parseDedDataInput(String input);

	public void setLastPing(long lastPing);
}