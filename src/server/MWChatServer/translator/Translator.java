/*
 * Copyright (c) 2000 Lyrisoft Solutions, Inc.
 * Used by permission
 */
package server.MWChatServer.translator;

import java.util.Properties;

/**
 * This class "translates" things by using a specially formatted
 * varibles that can be substituted.  A properties file, which is
 * really just key/value pairs, might look something like this:
 *
 * <pre>
 * user.not.found=The user, {0}, was not found.
 * invited={0} has invited you to {1}
 * </pre>
 *
 * {0} through {n} can be used inside of any message string to allow
 * variable substition.
 * <p>
 *
 * Given the properties file above, this call:
 * <pre>getMessage("invited", "jack", "jack's room");</pre>
 *
 * Would return <pre>Jack has invited you to jack's room</pre>
 * 
 * The "translation" part is actually a manual step, in that somebody
 * has to actually create a translated version of the properties
 * file(s).
 */
public class Translator {
    private static Properties _messages;

    /**
     * Construct a Translator that will used the specified Properties.
     */
    public Translator(Properties p) {
        if (_messages == null) {
            _messages = p;
        }
    }

    public Translator() {
        this(new Properties());
    }

    /**
     * Get a message without any variable substition.
     */
    public String getMessage(String key) {
        String s = _messages.getProperty(key);
        if (s == null) {
            throw new RuntimeException("No such key: " + key);
        }
        return s;
    }

    /**
     * Get a message and substitute {0} with arg1
     */
    public String getMessage(String key, String arg1) {
        String[] args = { arg1 };
        return getMessage(key, args);
    }

    /**
     * Get a message and substitute {0} and {1} with arg1 and arg2
     */
    public String getMessage(String key, String arg1, String arg2) {
        String[] args = { arg1, arg2 };
        return getMessage(key, args);
    }

    /**
     * Get a message and substitute {0}, {1}, and {2} with arg1, arg2,
     * and arg3.
     */
    public String getMessage(String key, String arg1, String arg2, String arg3) {
        String[] args = { arg1, arg2, arg3 };
        return getMessage(key, args);
    }

    /**
     * The most general case of substituting variables: {0} thru {n}
     * are substibuted with args[0] thru args[n].
     */
    public String getMessage(String key, String[] args) {
        String s = getMessage(key);
        StringBuilder sb = new StringBuilder();
        char[] raw = s.toCharArray();
        for (int i=0; i < raw.length; i++) {
            char c = raw[i];
//            System.err.println(i + ": " + c);
            if (c == '{') {
                int j=i;
                for (; j < raw.length; j++) {
                    if (raw[j] == '}') {
                        break;
                    }
                }
                int idx = extractInt(raw, i, j);
                sb.append(args[idx]);
                i = j;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Extract an int from in between { and } characters.
     */
    int extractInt(char[] buf, int start, int end) {
        start++;
        return Integer.parseInt(new String(buf, start, end-start));
    }
}
