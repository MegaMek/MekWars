package mekwars.common.interfaces;

/**
 * Minimal abstraction over "the server" as seen by code that needs to push a message to a single connected client
 * without depending on the concrete server implementation (which lives outside this module, e.g. in
 * MekWarsServer/MekWarsDedicated).
 * <p>
 * Implementations are responsible for locating the connection associated with {@code username} and delivering
 * {@code message} to it.
 */
public interface IServer {

    /**
     * Sends a message to a specific, already-connected client.
     *
     * @param message  the text to deliver to the client
     * @param username the login name identifying which client connection to send to
     */
    void clientSend(String message, String username);
}
