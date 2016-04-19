/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STUN;

import java.io.IOException;
import java.net.*;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.crypto.tls.*;
import org.json.simple.JSONObject;
import webrtc.server.WebRTC.ICEAuth;

/**
 *
 * @author gaelph
 */
public class StunServer extends Thread {

    protected JSONObject candidateDict;
    protected ICEAuth localIceAuth;
    protected ICEAuth remoteIceAuth;

    protected String type;

    protected String address;
    protected int port;

    protected int icePriority;

    public StunTransport transport;
    Object encryptedTransport;

    private static final String TCP_TYPE = "tcp";
    private static final String UDP_TYPE = "udp";

    public static StunServer createServerWithType(String type, String address, int port) {
        StunServer instance = new StunServer();
        synchronized (StunServer.class) {
            try {
                //TODO default rootPath
                instance = StunServer.class.newInstance();
                instance.type = type;
            }
            catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(StunServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return instance;
    }

    public void setLocalCredentials(ICEAuth auth) {
        this.localIceAuth = auth;
    }

    public void setRemoteCredentials(ICEAuth auth) {
        this.remoteIceAuth = auth;
    }

    public void setICEPriority(int priority) {
        this.icePriority = priority;
    }

    public int getLocalPort() {
        if (this.type.equalsIgnoreCase(TCP_TYPE)) {
            return this.port;
        }
        else {
            while (this.transport == null) {
                synchronized (this) {
                    try {
                        this.wait();
                    }
                    catch (InterruptedException ex) {
                        Logger.getLogger(StunServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            return this.transport.getLocalPort();
        }
    }

    private void startListener() {
        try {
            synchronized (this) {
                transport = this.type.equalsIgnoreCase(TCP_TYPE)
                            ? StunTCPTransport.withAddress(this.address, this.port, this)
                            : StunUDPTransport.withAddress(this.address, this.port, this);

                transport.setLocalCredentials(localIceAuth);
                transport.setRemoteCredentials(remoteIceAuth);
                transport.setICEPriority(icePriority);

                transport.start();

                this.notify();
            }
        }
        catch (SocketException | UnknownHostException ex) {
            Logger.getLogger(StunServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IOException ex) {
            Logger.getLogger(StunServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void gotBinding() {
        synchronized (this) {
            this.notify();
        }
    }

    public Object getEncryptedTransport() {
        synchronized (this) {
            try {
                wait();
            }
            catch (InterruptedException ex) {
                Logger.getLogger(StunServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            return this.encryptedTransport;
        }
    }

    @Override
    public void run() {
        startListener();

        synchronized (this) {
            try {
                this.wait();
            }
            catch (InterruptedException ex) {
                Logger.getLogger(StunServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            SRPTlsClient srpClient = new SRPTlsClient(this.localIceAuth.ufrag.getBytes(),
                                                      this.localIceAuth.pwd.getBytes());

            if (this.transport instanceof StunUDPTransport) {

                try {
                    UDPTransport uTransport = new UDPTransport(this.transport.getDatagramSocket(),
                                                               this.transport.getDatagramSocket().getReceiveBufferSize());

                    DTLSClientProtocol protocol = new DTLSClientProtocol(new SecureRandom());

                    this.encryptedTransport = protocol.connect(srpClient, uTransport);

                }
                catch (IOException ex) {
                    Logger.getLogger(StunUDPTransport.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else {

                try {
                    TlsClientProtocol tlsClient = new TlsClientProtocol(new SecureRandom());

                    tlsClient.connect(srpClient);

                    this.encryptedTransport = tlsClient;
                }
                catch (IOException ex) {
                    Logger.getLogger(StunServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            this.notify();
        }

        this.interrupt();
    }

}
