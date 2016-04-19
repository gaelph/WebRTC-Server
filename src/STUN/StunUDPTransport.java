/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STUN;

import UDPServer.UDPConnection;
import Utils.bytes.Bytes;
import java.io.IOException;
import java.net.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import webrtc.server.Utils;

import Utils.stringprep.StringPrep;
import java.util.Arrays;

import webrtc.server.WebRTC.ICEAuth;

/**
 *
 * @author gaelph
 */
public class StunUDPTransport extends UDPConnection implements StunTransport {

    private static final Range PORT_RANGE = new Range(5000, 1000);

    protected ICEAuth localAuth;
    protected ICEAuth remoteAuth;

    protected String address;
    protected int port;

    protected int icePriority;

    protected StunServer stunServer;

    private StunBinding binding;

    public StunUDPTransport(DatagramSocket socket) {
        super(socket);
    }

    public StunUDPTransport(String address, int port) throws SocketException, UnknownHostException {
        super(address, port);

        int localPort = Utils.randomIntWithin(StunUDPTransport.PORT_RANGE.location,
                                              StunUDPTransport.PORT_RANGE.location + StunUDPTransport.PORT_RANGE.length);

        this.setLocalPort(localPort);
    }

    public static StunUDPTransport withAddress(String address, int port, StunServer server) throws SocketException,
                                                                                                   UnknownHostException {

        StunUDPTransport conn = new StunUDPTransport(address, port);
        conn.address = address;
        conn.port = port;
        conn.stunServer = server;

        conn.setName("STUN over UDP" + conn.address + " " + conn.port);

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
    public DatagramSocket getDatagramSocket() {
        return this.socket;
    }

    @Override
    public void init() {
        this.on(UDPConnection.EVENTS.INCOMING, data -> {
            Bytes d = new Bytes(data);
            int size = d.read16(2);
            size += 20;

            d.trim(size);

            try {
                if (!StunMessageTest.validateMessage(d, StringPrep.prepAsQueryString(this.localAuth.pwd))) {
                    Logger.getLogger(StunUDPTransport.class.getName()).log(Level.INFO,
                                                                           "Received an erroneous message on {0}",
                                                                           this.getName()
                    );
                    return null;
                }
            }
            catch (StringPrep.StringPrepError ex) {
                Logger.getLogger(StunUDPTransport.class.getName()).log(Level.SEVERE, null, ex);
            }

            StunMessage message = StunMessage.parse(data);

            if (message.type == StunMessage.STUN_BINDING_RESPONSE) {

                this.stunServer.gotBinding();
                return null;
            }
            else if (message.type == StunMessage.STUN_BINDING_REQUEST) {

                try {
                    StunMessage success = binding.responseWithAttributes(message, Arrays.asList(
                                                                         new StunXORAddress(address,
                                                                                            port)));
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

            return null;

        });
    }

    @Override
    public Socket getSocket() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static class Range {

        public int location = 0;
        public int length = 0;

        public Range(int location, int length) {
            this.location = location;
            this.length = length;
        }
    }

}
