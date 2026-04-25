package common.campaign.clientutils.protocol.commands;

public interface IProtCommand
{
  // check if this is proper command
  public boolean check(String name);
  // invoked when command is executed
  public boolean execute(String command);
  public String getName();
}