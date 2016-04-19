/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.WebRTC;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.crypto.tls.DTLSTransport;
import org.bouncycastle.crypto.tls.TlsClientProtocol;

/**
 *
 * @author gaelph
 */
public class MediaStream implements Runnable {

    private boolean running = true;
    private final IMediaStream stream;

    private byte[] buffer;

    public MediaStream(DTLSTransport udp) {
        this.stream = new UDPMediaStream(udp);
        try {

            this.buffer = new byte[udp.getReceiveLimit()];
        }
        catch (IOException ex) {
            Logger.getLogger(MediaStream.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public MediaStream(TlsClientProtocol tcp) {
        this.stream = new TCPMediaStream(tcp);
        this.buffer = new byte[tcp.getAvailableInputBytes()];
    }

    public byte[] read() {
        synchronized (this) {
            try {
                this.wait();
            }
            catch (InterruptedException ex) {
                Logger.getLogger(MediaStream.class.getName()).log(Level.SEVERE, null, ex);
            }

            return this.buffer;
        }
    }

    @Override
    public void run() {

        while (running) {
            try {
                this.stream.read(this.buffer);
            }
            catch (IOException ex) {
                Logger.getLogger(MediaStream.class.getName()).log(Level.SEVERE, null, ex);
            }

            synchronized (this) {
                this.notify();
            }
        }
    }

    public void close() {
        running = false;
    }

    private interface IMediaStream {

        public void read(byte[] buffer) throws IOException;

        public void close();

    }

    private class UDPMediaStream implements IMediaStream {

        private final DTLSTransport transport;

        UDPMediaStream(DTLSTransport transport) {
            this.transport = transport;
        }

        @Override
        public void read(byte[] buffer) throws IOException {
            this.transport.receive(buffer, 0, buffer.length, 0);
        }

        @Override
        public void close() {
            try {
                this.transport.close();
            }
            catch (IOException ex) {
                Logger.getLogger(MediaStream.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class TCPMediaStream implements IMediaStream {

        private final TlsClientProtocol transport;

        TCPMediaStream(TlsClientProtocol transport) {
            this.transport = transport;
        }

        @Override
        public void read(byte[] buffer) throws IOException {
            this.transport.getInputStream().read(buffer);
        }

        @Override
        public void close() {
            try {
                this.transport.close();
            }
            catch (IOException ex) {
                Logger.getLogger(MediaStream.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

}
