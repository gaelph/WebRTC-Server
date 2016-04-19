/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.WebSocket;

import TCPServer.TCPConnection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.Socket;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.*;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.function.Consumer;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import webrtc.server.HTTP.*;

/**
 *
 * @author gaelph
 */
public class WebSocketConnection extends TCPConnection {

    private String buffer = "";

    /**
     * A unique session ID Actually the value send as Sec-WebSocket-Key in the handshake header
     */
    public String sid = "";
    public String uid = "";
    /**
     * the request sent for the handshake
     */
    public HTTPRequest request;

    private final Map<String, Consumer<String>> actions = new HashMap<>();

    private final CopyOnWriteArrayList<WebSocketRoom> joinedRooms = new CopyOnWriteArrayList<>();

    private static final Logger LOG = Logger.getLogger(WebSocketConnection.class.getName());

    public WebSocketConnection(WebSocketServer server, Socket socket) {
        super(server, socket);
    }

    /**
     * @param string the string to get SHA1 digest from
     *
     * @return the SHA1 digest as an array of bytes
     */
    private static byte[] stringToSHA1(String string) {
        byte[] result = null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            try {
                result = digest.digest(string.getBytes("UTF-8"));
            }
            catch (UnsupportedEncodingException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Initialisation function
     * <p>
     * Subclasses should override this but must call super.init()
     */
    @Override
    public void init() {

        this.on(TCPConnection.Events.INCOMING, event -> {
            if (isHTTP(new String((byte[]) ((TCPConnection.TCPConnectionEvent) event).value.get()))) {
                try {
                    request = new HTTPRequest(new String((byte[]) ((TCPConnection.TCPConnectionEvent) event).value.get()));
                }
                catch (HTTPRequestParseException ex) {
                    Logger.getLogger(WebSocketConnection.class.getName()).log(Level.SEVERE, null, ex);
                }

                return onHandshake();

            }
            else {
                byte[] data = (byte[]) ((TCPConnection.TCPConnectionEvent) event).value.get();
                int size = WebSocketPayload.getPacketSize(data);
                int offset = WebSocketPayload.getOffsetForSize(size);
                int remainingBytes = data.length - size - offset;

                byte[] validData;
                int packetOffset = 0;

                //We may have more than one websocket payload to deal with...
                do {
                    validData = Arrays.copyOfRange(data, packetOffset, packetOffset + size + offset);

                    if (validData.length > 8) {
                        onPacket(WebSocketPayload.withRawBytes(validData));
                    }

                    packetOffset += size + offset;

                    if (packetOffset < data.length) {
                        byte[] temp = new byte[8];
                        System.arraycopy(data, packetOffset, temp, 0, temp.length);

                        size = WebSocketPayload.getPacketSize(temp);
                        offset = WebSocketPayload.getOffsetForSize(size);
                        remainingBytes -= (size + offset);
                    }
                    else {
                        break;
                    }

                } while (remainingBytes > 0);

                return null;

            }
        });

    }

    private Optional<Object> onHandshake() {
        String[] pathElements = request.path.split("/");
        String rid;

        if (pathElements.length == 0) {
            rid = "defaultRoom";
        }
        else {
            rid = pathElements[pathElements.length - 1];
        }

        if (((WebSocketServer) this.server).getRooms().stream().filter(room -> room.rid.equals(rid))
                .findFirst().isPresent()) {

            if (request.header.containsKey("upgrade")) {
                if (request.header.get("upgrade").equalsIgnoreCase("websocket")) {
                    //Prepares the answer
                    HTTPResponse response = new HTTPResponse(this);
                    response.status = HTTPStatusCodes.SWITCHING_PROTOCOLS;
                    response.header.put("Connection", "Upgrade");
                    response.header.put("Upgrade", "websocket");

                    this.sid = request.header.get("sec-websocket-key").trim();
                    byte[] SHA1 = stringToSHA1(sid + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11");
                    byte[] accept = Base64.getEncoder().encode(SHA1);
                    response.header.put("Sec-WebSocket-Accept", new String(accept));

                    try {
                        response.send();
                    }
                    catch (Exception ex) {
                        Logger.getLogger(WebSocketConnection.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    ((WebSocketServer) this.server).call("connect", this);

                    WebSocketRoom userRoom = new WebSocketRoom(this.sid,
                                                               WebSocketRoom.TYPES.USER);
                    ((WebSocketServer) this.server).addRoom(userRoom);
                    userRoom.join(this);

                    ((WebSocketServer) this.server).getRooms().stream()
                            .filter(room -> room.rid.equals(rid))
                            .forEach(room -> room.join(this));

                    return null;
                }
            }

            try {
                close();
            }
            catch (IOException ex) {
                Logger.getLogger(WebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    private Optional<Object> onPacket(WebSocketPayload payload) {
        switch (payload.opcode) {
            case WebSocketPayload.CONTINUATION_OPCODE://Append to a buffer
                buffer += new String(payload.content);
                if (payload.isFinal) {
                    process(payload);
                    buffer = "";
                }
                break;

            case WebSocketPayload.CONNECTION_CLOSED_OPCODE:
                call("disconnect", null);

                this.joinedRooms.forEach(room -> room.leave(this));

                 {
                    try {
                        close();
                    }
                    catch (IOException ex) {
                        Logger.getLogger(WebSocketConnection.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;

            case WebSocketPayload.PING_OPCODE:
                payload.opcode = WebSocketPayload.PONG_OPCODE;
                 {
                    try {
                        write(payload.toBytes());
                    }
                    catch (Exception ex) {
                        try {
                            Logger.getLogger(WebSocketConnection.class.getName()).log(Level.SEVERE, null, ex);
                            this.getJoinedRooms().forEach(room -> room.leave(this));
                            this.close();
                            this.terminate();
                        }
                        catch (IOException ex1) {
                            Logger.getLogger(WebSocketConnection.class.getName()).log(Level.SEVERE, null, ex1);
                        }
                    }
                }
                break;

            case WebSocketPayload.PONG_OPCODE://Do anything ???
                break;

            case WebSocketPayload.TEXT_OPCODE:
            case WebSocketPayload.BINARY_OPCODE:
            default:
                process(payload);
                break;
        }

        return null;
    }

    private void process(WebSocketPayload payload) {
        String label;
        String data;
        String dataString = new String(payload.content);
        Integer index = dataString.indexOf("##");

        label = (index > 0) ? dataString.substring(0, index) : "message";

        data = dataString.substring(index + 2);

        call(label, data);
    }

    /**
     * Tests if a request is an HTTP request
     *
     * @param data
     *
     * @return
     */
    private boolean isHTTP(String data) {
        Pattern pattern = Pattern.compile("HTTP\\/1\\..");
        Matcher matcher = pattern.matcher(data);

        return matcher.find();
    }

    /**
     * Adds a callback to the callbacks list
     *
     * @param event the label associated to the callback
     * @param action the action to perform, a functional Consumer which takes a String as argument
     */
    public void on(String event, Consumer<String> action) {
        actions.put(event, action);
    }

    /**
     * Calls a callback associated with an event
     *
     * @param event the label associated with the action
     * @param data the data to supply to the Consumer action
     */
    public void call(String event, String data) {
        if (actions.containsKey(event)) {
            actions.get(event).accept(data);
        }
    }

    /**
     * Targets a room designated by its rid
     *
     * @param rid room identifier
     *
     * @return
     */
    public Optional<WebSocketRoom> to(String rid) {
        for (WebSocketRoom room : joinedRooms) {
            if (room.rid.equals(rid)) {
                return Optional.of(room);
            }
        }

        return Optional.empty();
    }

    /**
     * Joins a room Creates a room it doesn't exist
     *
     * @param rid room identifier
     */
    public void join(String rid) {
        if (((WebSocketServer) this.server).getRooms().stream()
                .anyMatch(room -> room.rid.equals(rid))) {
            join(rid);
        }
        else {
            WebSocketRoom room = new WebSocketRoom(makeRandomString(24), WebSocketRoom.TYPES.VOLATILE);
            join(room);
        }

    }

    /**
     * Joins a room
     *
     * @param room
     */
    public void join(WebSocketRoom room) {
        joinedRooms.add(room);
    }

    /**
     * Leaves a room If the room isn't a permanent one, it is dropped when empty
     *
     * @param rid room identifier
     */
    public void leave(String rid) {
        ((WebSocketServer) this.server).getRooms().stream()
                .filter((room) -> (room.rid.equals(rid)))
                .forEach((room) -> {
                    leave(room);
                });
    }

    /**
     * Leaves a room If the room isn't a permanent one, it is dropped when empty
     *
     * @param room
     */
    public void leave(WebSocketRoom room) {
        joinedRooms.remove(room);
        if (room.isEmpty() && room.type != WebSocketRoom.TYPES.PERMANENT) {
            ((WebSocketServer) server).dropRoom(room);
        }
    }

    public CopyOnWriteArrayList<WebSocketRoom> getJoinedRooms() {
        return this.joinedRooms;
    }

    /**
     * Sends a message to the connection
     *
     * @param label
     * @param message
     */
    public void emit(String label, String message) {

        try {
            if (message.length() > this.socket.getSendBufferSize()) {
                int offset, length, remaining;
                offset = 0;
                length = this.socket.getSendBufferSize();
                remaining = message.length();

                message = label + "##" + message;
                int padding = WebSocketPayload.getOffsetForSize(length);

                do {
                    WebSocketPayload payload = new WebSocketPayload(message.substring(offset, length - padding));
                    offset += length - padding;
                    remaining -= offset;

                    payload.opcode = offset == 0 ? WebSocketPayload.TEXT_OPCODE : WebSocketPayload.CONTINUATION_OPCODE;
                    payload.isFinal = remaining <= 0;

                    this.write(payload.toBytes());

                } while (remaining > 0);
            }
            else {
                WebSocketPayload payload = new WebSocketPayload(label + "##" + message);
                this.write(payload.toBytes());
            }
        }
        catch (Exception ex) {
            Logger.getLogger(WebSocketConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String makeRandomString(int length) {
        byte[] randomBytes = new byte[length];

        new Random(new Date().getTime()).nextBytes(randomBytes);

        return Base64.getEncoder().encodeToString(randomBytes);
    }
}
