/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.WebSocket;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import webrtc.server.WebRTCServer;

/**
 *
 * @author gaelph
 */
public class WebSocketRoom {

    /**
     * Room identifier
     */
    public String rid;
    private String name;

    /**
     * This room type
     * <p>
     * @see webrtc.server.WebSocket.WebSocketRoom.TYPES
     */
    public final TYPES type;

    public final CopyOnWriteArrayList<WebSocketConnection> participants = new CopyOnWriteArrayList<>();

    /**
     * Room types
     * <p>
     * <ul>
     * <li><b>USER : </b>A room created when a user connected. Its rid is the user's sid.</li>
     * <li><b>VOLATILE : </b>A user created room. It should contain its creator</li>
     * <li><b>PERMANENT : </b>A room stored in a database. It should be loaded at launch time and can be empty.</li>
     * </ul>
     */
    public static enum TYPES {
        USER,
        VOLATILE,
        PERMANENT
    }

    /**
     * Constructor
     *
     * @param rid A unique room identifier
     * @param type Room type
     */
    public WebSocketRoom(String rid, TYPES type) {
        this.rid = rid;
        this.name = rid;
        this.type = type;
    }

    /**
     * Constructor Named room
     *
     * @param rid A unique room identifier
     * @param name A name
     * @param type Room type
     */
    public WebSocketRoom(String rid, String name, TYPES type) {
        this.rid = rid;
        this.name = name;
        this.type = type;
    }

    /**
     * Returns the room's name
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the room's name
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Joins the room
     *
     * @param joiner
     */
    public void join(WebSocketConnection joiner) {
        this.participants.forEach(conn -> {
            if (conn.uid.equals(joiner.uid)) {
                this.leave(conn);

                try {
                    conn.close();
                }
                catch (IOException ex) {
                    Logger.getLogger(WebRTCServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        this.participants.add(joiner);
        joiner.join(this);
        joiner.call("joined", rid);
    }

    /**
     * Leaves a room
     *
     * @param leaver
     */
    public void leave(WebSocketConnection leaver) {
        this.participants.remove(leaver);
        leaver.call("left", rid);
        leaver.leave(this);

    }

    /**
     * Returns true if the room has no participants
     *
     * @return
     */
    public boolean isEmpty() {
        return this.participants.isEmpty();
    }

    /**
     * Returns a JSON String representation of the room
     *
     * @return
     */
    public String toJSONString() {
        JSONObject jRoom = new JSONObject();
        JSONArray jParticipants = new JSONArray();

        jRoom.put("rid", rid);
        jRoom.put("name", name);

        participants.forEach(participant -> {
            jParticipants.add(participant.sid);
        });

        jRoom.put("participants", jParticipants);

        return jRoom.toJSONString();
    }

    /**
     * Sends a message to every member of the room
     *
     * @param label
     * @param message
     */
    public void emit(String label, String message) {
        this.participants.forEach(conn -> {
            try {
//                WebSocketPayload payload = new WebSocketPayload((label + "##" + message));
//                conn.write(payload.toBytes());
                conn.emit(label, message);
            }
            catch (Exception ex) {
                Logger.getLogger(WebSocketRoom.class.getName()).log(Level.INFO, "{0} had left", conn.sid);
                this.leave(conn);
            }
        });
    }

}
