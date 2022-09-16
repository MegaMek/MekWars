/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - Torren (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

/*
 * Derived from NFCChat, a GPL chat client/server. 
 * Original code can be found @ http://nfcchat.sourceforge.net
 * Our thanks to the original authors.
 */ 
/**
 * 
 * @author Torren (Jason Tighe) 11.5.05 
 * 
 */
package server.MWChatServer;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import common.util.MWLogger;
import server.MWChatServer.commands.ICommandProcessorRemote;
import server.MWChatServer.commands.ICommands;
import server.MWChatServer.commands.UnknownCommand;

/**
 * When messages come in on the socket from the client, they have to be processed.  This class
 * does that, by invoking the proper ICommandProcessorRemote.  Instance of ICommandProcessorRemote
 * are kept in a registry (HashMap), using the constants in ICommands as keys.
 *
 * @see com.lyrisoft.chat.ICommands
 */
public class CommandProcessorRemote implements ICommands{
    private static HashMap<String,ICommandProcessorRemote> _processors;
    private static HashSet<String> _idleTimeImmune;
    private static UnknownCommand unknownCommandProcessor = new UnknownCommand();

    public static void init(Properties p) {
        if (_processors != null && _idleTimeImmune != null) {
            MWLogger.infoLog("CommandProcessorRemote: Warning: init() called a second time");
        }
        
        _processors = new HashMap<String,ICommandProcessorRemote>();
        _idleTimeImmune = new HashSet<String>();
        
        for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            int idx = name.indexOf(".");
            if (idx < 1) {
                MWLogger.infoLog("CommandProcessorRemote: unknown property: " + name);
                continue;
            }
            String command = name.substring(0, idx);
            if (name.endsWith(".class")) {
                String className = p.getProperty(name);
                MWLogger.infoLog("CommandProcessorRemote: initting the " + command + " command processor");
                try {
                    ICommandProcessorRemote cp = 
                            (ICommandProcessorRemote)Class.forName(className).newInstance();
                    
                    String help = p.getProperty(command + ".help");
                    cp.setHelp(help);

                    String usage = p.getProperty(command + ".usage");
                    cp.setUsage(usage);
                    
                    int access = 0;
                    try {
                        String s = p.getProperty(command + ".access");
                        access = Integer.parseInt(s);
                    }
                    catch (Exception ex) {}
                    cp.setAccessRequired(access);
                    
                    CommandProcessorRemote.extendCommandSet("/" + command, cp);
                }
                catch (Exception ex) {
                    MWLogger.errLog("Unable to install the " + command + " command");
                    MWLogger.errLog(ex);
                    MWLogger.errLog("Continuing despite error(s)");
                }
            } else if (name.endsWith(".idleImmune")) {
                _idleTimeImmune.add("/" + command);
            }
        }
    }

    /**
     * Used to extend the core set of commands that the server can process.  Note: If a
     * custom reply is to be sent back, there should be a corresponding extension on the
     * client side.
     *
     * @param command the first argument of the command
     * @param processor an ICommandProcessorRemote instance that will handle the new command.
     * @see com.lyrisoft.chat.server.local.CommandProcessorLocal#extendCommandSet
     */
    public static void extendCommandSet(String command, ICommandProcessorRemote processor) {
        _processors.put(command, processor);
    }

    public static void setIdleTimeImmune(String command) {
        _idleTimeImmune.add(command);
    }

    /**
     * Process a raw message from the client
     * <ul>
     * <li>The message is decomposed into a string array of arguments
     * <li>Use the first argument to look up an ICommandProcessorRemote instance in the registry
     * <li>If found, check the access level
     * <li>Invoke process() on the ICommandProcessorRemote instance
     * </ul>
     *
     * If an ICommandProcessorRemote was not found, then use the UnknownCommandProcessor()<br>
     * If the access level on the client is less than the required access, then send a
     * generalError() to the client.
     *
     * @see com.lyrisoft.chat.server.remote.command
     * @see com.lyrisoft.chat.server.remote.MWChatClient#generalError
     */
    public static void process(String input, MWChatClient client) {
		
    	//No point in comparing something to MAX_VALUE. Can never be greater.
		/*
		 * if (input.length() > Integer.MAX_VALUE) {
		 *     client.generalError(Translator.getMessage("command_too_long"));
		 *     return;
		 * }
		 */

        String[] args = decompose(input);
        if (args.length == 0)
            return;

        ICommandProcessorRemote processor = _processors.get(args[0]);
        if (processor == null) {
            unknownCommandProcessor.process(client, args);
        } else if (client.getAccessLevel() < processor.accessRequired()) {
        	client.generalError(Translator.getMessage("access_denied"));
        	client.generalError("Access denied");
        } else {
        	processor.process(client, args);
        }
    }

    /**
     * Decompose a raw message into an array of String, splitting on
     * the DELIMITER defined in ICommands.
     * @see com.lyrisoft.chat.ICommands
     */
    public static String[] decompose(String input) {
        StringTokenizer st = new StringTokenizer(input, DELIMITER);
        ArrayList<String> list = new ArrayList<String>(5);
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        String[] args = new String[list.size()];
        return list.toArray(args);
    }

    /**
     * Get the map that contains the command processors.  This method exists
     * so the Help command processor can do its work
     */
    public static Map<String,ICommandProcessorRemote> getCommandProcessors() {
        return _processors;
    }
}
