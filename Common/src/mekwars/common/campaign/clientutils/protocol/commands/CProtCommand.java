package common.campaign.clientutils.protocol.commands;

import common.campaign.clientutils.protocol.CConnector;
import common.campaign.clientutils.protocol.IClient;

/**
 * Abstract class for protocol Commands
 */

public abstract class CProtCommand implements IProtCommand
{
  String name = "";
  String prefix = "";
  String delimiter = "";
  IClient client;
  CConnector Connector;

  public void setName(String name) {
	  this.name = name;
  }
  
  public IClient getClient() {
	  return client;
  }
  
  public void setClient(IClient c) {
	  client = c;
  }
  
  public String getPrefix() {
	  return prefix;
  }
  
  public void setPrefix(String prefix) {
	  this.prefix = prefix;
  }
  
  public String getDelimiter() {
	  return delimiter;
  }
  
  public void setDelimiter(String delimiter) {
	  this.delimiter = delimiter;
  }
  
  public void setConnector (CConnector connector) {
	  Connector = connector;
  }
  
  public CConnector getConnector() {
	  return Connector;
  }
  
  public CProtCommand(IClient client)
  {
    this.client = client;
    Connector = client.getConnector();
    prefix = IClient.PROTOCOL_PREFIX;
    delimiter = IClient.PROTOCOL_DELIMITER;
  }

  public boolean check(String tname)
  {
    if (tname.startsWith(prefix)) {tname = tname.substring(prefix.length());}
    return(name.equals(tname));
  }

  // execute command
  public boolean execute(String input) {return true;}

  // echo command in GUI
  protected void echo(String input) {}

  // remove prefix and name/alias from input
  protected String decompose(String input)
  {
    if (input.startsWith(prefix)) {input = input.substring(prefix.length()).trim();}
    if (input.startsWith(name)) {input = input.substring(name.length()).trim();}
    return input;
  }

  public String getName() {return name;}

}