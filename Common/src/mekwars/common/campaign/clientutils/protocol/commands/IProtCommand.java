package mekwars.common.campaign.clientutils.protocol.commands;

public interface IProtCommand {
    // check if this is proper command
    boolean check(String name);

    // invoked when command is executed
    boolean execute(String command);

    String getName();
}
