/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.WebSocket;

import TCPServer.TCPServer;

import java.util.concurrent.CopyOnWriteArrayList;

import java.net.*;

import java.util.HashMap;
import java.util.HashSet;

import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.function.Consumer;

import java.util.logging.*;

/**
 *
 * @author gaelph
 */
public class WebSocketServer extends TCPServer {

    private final CopyOnWriteArrayList<WebSocketRoom> rooms = new CopyOnWriteArrayList<>();

    private final Map<String, Consumer<WebSocketConnection>> wsCallbacks = new HashMap<>();

    private final Set<Consumer<WebSocketConnection>> beforeCallbacks = new HashSet<>();

    private final static Logger LOG = Logger.getLogger(WebSocketServer.class.getName());

    /**
     *
     */
    protected WebSocketServer() {
        super();
    }

    /**
     * Creates a new WebSocket server instance
     *
     * @return
     */
    public static WebSocketServer createServer() {
        WebSocketServer instance;

        synchronized (WebSocketServer.class) {
            instance = new WebSocketServer();

            ((WebSocketServer) instance).init();
        }

        return instance;
    }

    /**
     * Initialisation function
     * <p>
     * Subclasses should override this to add mandatory callbacks, but must call super.init()
     */
    @Override
    protected void init() {

        on(TCPServer.EVENTS.CONNECTION, this::createNewConnection);
    }

    /**
     * Add a Callback to the callbacks list
     *
     * @param wsEvent the label to which the callback responds
     * @param action the callback, this is a functional Consumer which takes a WebSocketConnection as an argument
     */
    public void on(String wsEvent, Consumer<WebSocketConnection> action) {
        wsCallbacks.put(wsEvent, action);
    }

    /**
     * Calls the callback associated with the wsEvent label
     *
     * @param wsEvent the label associated with the wanted callback
     * @param req the parameter to pass to the callback
     */
    protected void call(String wsEvent, WebSocketConnection req) {
        if (wsCallbacks.containsKey(wsEvent)) {
            wsCallbacks.get(wsEvent).accept(req);
        }
    }

    protected void createNewConnection(TCPEvent event) {
        WebSocketConnection con = new WebSocketConnection(this, (Socket) event.value.get());
        con.start();
    }

    /**
     * Adds a middleware action
     * <p>
     * the action is performed after the HTTP handshake before the 'connect' event is thrown
     *
     * @param action the action to perform
     */
    public void useBefore(Consumer<WebSocketConnection> action) {
        beforeCallbacks.add(action);
    }

    /**
     * Calls the middleware action
     *
     * @param arg
     */
    protected void callBefore(WebSocketConnection arg) {
        beforeCallbacks.forEach((Consumer<WebSocketConnection> action) -> action.accept(arg));
    }

    /**
     * Adds a room to the rooms list
     *
     * @param room
     */
    public void addRoom(WebSocketRoom room) {
        rooms.add(room);
    }

    /**
     * Creates and add a new room
     *
     * @param rid
     * @param type
     */
    public void newRoom(String rid, WebSocketRoom.TYPES type) {
        addRoom(new WebSocketRoom(rid, type));
    }

    /**
     * Removes a room from the rooms list
     *
     * @param room
     */
    public void dropRoom(WebSocketRoom room) {
        rooms.remove(room);
    }

    /**
     * Returns the rooms list
     *
     * @return a List of WebSocketRoom objects
     */
    public List<WebSocketRoom> getRooms() {
        return rooms;
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    public void finalize() throws Throwable {
        close();
        LOG.log(Level.INFO, "Server Closed");

        super.finalize();
    }

}
