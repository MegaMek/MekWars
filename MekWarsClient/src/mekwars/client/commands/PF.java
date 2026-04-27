package mekwars.client.commands;

public class PF extends Command {
    /**
     * @input should be one of the following: SDF|<PlayerFlags.export()>  to set all default flags
     *       AF|name|id|value  to add a flag to both defaults and personal DF|name  to delete a flag from both defaults
     *       and personal SF|name|value  to set a personal flag SSDF|name|value  to set a new value for a default flag
     * @see client.cmd.Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        String action = st.nextToken();
        if (action.equalsIgnoreCase("SDF")) {
            // Set Default Flags
            client.getPlayer().getDefaultPlayerFlags().loadDefaults(st.nextToken());
            client.getPlayer().getDefaultPlayerFlags().save();
        } else if (action.equalsIgnoreCase("AF")) {
            // Add a Flag
            // Should be a string with this format:  name|id|value
            String name = st.nextToken();
            int id = Integer.parseInt(st.nextToken());
            boolean value = Boolean.parseBoolean(st.nextToken());
            client.getPlayer().getFlags().addFlag(name, id, value);
            client.getPlayer().getDefaultPlayerFlags().addFlag(name, id, value);
        } else if (action.equalsIgnoreCase("DF")) {
            // Delete a Flag
            // Should be a string with this format:  name
            String name = st.nextToken();
            client.getPlayer().getFlags().clearFlag(name);
            client.getPlayer().getDefaultPlayerFlags().clearFlag(name);

        } else if (action.equalsIgnoreCase("SF")) {
            // Set Flag
            // Should be a string with this format:  name|value
            String name = st.nextToken();
            boolean value = Boolean.parseBoolean(st.nextToken());
            client.getPlayer().getFlags().setFlag(name, value);
        } else if (action.equalsIgnoreCase("SSDF")) {
            // Set Single Flag - used to change a single Default Flag
            // without changing status of the personal flag
            // Should be a string with this format: name|value
            String name = st.nextToken();
            boolean value = Boolean.parseBoolean(st.nextToken());
            client.getPlayer().getDefaultPlayerFlags().setFlag(name, value);
        }
        // As this is the last command sent on login, and since players' hangars aren't
        // being sorted when first logging in, it seems an appropriate time to send a
        // sortHangar command
        client.getPlayer().sortHangar();
    }

    public PF(client.MWClient mwclient) {
        super(mwclient);
    }
}
