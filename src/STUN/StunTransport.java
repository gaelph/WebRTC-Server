/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STUN;

import java.net.DatagramSocket;
import java.net.Socket;
import webrtc.server.WebRTC.ICEAuth;

/**
 *
 * @author gaelph
 */
public interface StunTransport {

    public void setLocalCredentials(ICEAuth auth);

    public void setRemoteCredentials(ICEAuth auth);

    public void setICEPriority(int priority);

    public int getLocalPort();

    public Socket getSocket();

    public DatagramSocket getDatagramSocket();

    public void start();

    public void interrupt();
}
