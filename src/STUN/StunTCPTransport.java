/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STUN;

import TCPServer.TCPConnection;
import Utils.bytes.Bytes;
import Utils.stringprep.StringPrep;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import webrtc.server.Utils;
import webrtc.server.WebRTC.ICEAuth;

/**
 *
 * @author gaelph
 */
public class StunTCPTransport extends TCPConnection implements StunTransport {

    private static final Range PORT_RANGE = new Range(5000, 1000);

    protected ICEAuth localAuth;
    protected ICEAuth remoteAuth;

    protected String address;
    protected int port;

    private StunServer stunServer;
    private StunBinding binding;

    protected int icePriority;

    private static final Logger LOG = Logger.getLogger(StunTCPTransport.class.getName());

    protected StunTCPTransport(Socket socket) {
        super(socket);
    }

    public static StunTCPTransport withAddress(String address, int port, StunServer server) throws SocketException,
                                                                                                   UnknownHostException,
                                                                                                   IOException {

        port = Utils.randomIntWithin(4000, 6000);

        ServerSocket socketServer = new ServerSocket(port);

        StunTCPTransport conn = new StunTCPTransport(socketServer.accept());
        conn.stunServer = server;

        conn.setName("STUN over TCP " + address + " " + conn.socket.getLocalPort());

        conn.address = address;
        conn.port = port;

        return conn;
    }

    @Override
    public void setRemoteCredentials(ICEAuth auth) {
        this.remoteAuth = auth;
    }

    @Override
    public void setLocalCredentials(ICEAuth auth) {
        this.localAuth = auth;
    }

    @Override
    public void setICEPriority(int priority) {
        this.icePriority = priority;
    }

    @Override
    public void init() {
        this.on(TCPConnection.Events.INCOMING, (Object event) -> {
            try {
                byte[] data = (byte[]) ((TCPConnectionEvent) event).value.get();
                data = Arrays.copyOfRange(data, 2, data.length);

                Bytes d = new Bytes(data);

                int size = d.read16(2);
                size += 20;

                d.trim(size);

                if (!StunMessageTest.validateMessage(d, StringPrep.prepAsQueryString(this.localAuth.pwd))) {
                    Logger.getLogger(StunUDPTransport.class.getName()).log(Level.INFO,
                                                                           "Received an erroneous message on ",
                                                                           this.getName()
                    );
                }

                StunMessage message = StunMessage.parse((byte[]) data);

                if (message.type == StunMessage.STUN_BINDING_RESPONSE) {

                    this.stunServer.gotBinding();
                    return null;
                }
                else if (message.type == StunMessage.STUN_BINDING_REQUEST) {

                    try {
                        StunMessage success = binding.responseWithAttributes(message, Arrays.asList(
                                                                             new StunXORAddress(this.address,
                                                                                                this.port)));

                        this.write(success.toBytes(StringPrep.prepAsQueryString(this.remoteAuth.pwd)));
                    }
                    catch (IOException | StringPrep.StringPrepError ex) {
                        Logger.getLogger(StunUDPTransport.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    try {
                        StunMessage request = binding.ICEControlledRequest();
                        request.addAttribute(new StunAttribute(StunAttribute.PRIORITY, this.icePriority));
                        this.write(request.toBytes(StringPrep.prepAsQueryString(this.remoteAuth.pwd)));

                    }
                    catch (IOException | StringPrep.StringPrepError ex) {
                        Logger.getLogger(StunUDPTransport.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            catch (StringPrep.StringPrepError ex) {
                Logger.getLogger(StunTCPTransport.class.getName()).log(Level.SEVERE, null, ex);
            }

            return null;
        });
    }

    @Override
    public DatagramSocket getDatagramSocket() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static class Range {

        public int position;
        public int length;

        public Range(int position, int length) {
            this.position = position;
            this.length = length;
        }
    }
}
