/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UDPServer;

import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gaelph
 */
public abstract class UDPConnection extends Thread {

    protected final DatagramSocket socket;

    private final InetAddress address;
    private final int port;
    private int localPort;

    private static final int MAX_RECURSIONS = 15;
    private int recursions = 0;

    private boolean running = true;

    private final StringBuilder stringBuffer = new StringBuilder();
    private byte[] buffer;

    private final Map<EVENTS, Function<byte[], byte[]>> callbacks = new HashMap<>();

    public static enum EVENTS {
        CONNECTED,
        INCOMING,
        CLOSED,
        ERROR
    }

    public UDPConnection(DatagramSocket socket) {
        this.socket = socket;
        this.address = socket.getInetAddress();
        this.port = socket.getPort();
        this.localPort = socket.getLocalPort();
    }

    public UDPConnection(String address, int port) throws SocketException, UnknownHostException {
        this.socket = new DatagramSocket(null);
        this.address = InetAddress.getByName(address);
        this.port = port;
    }

    public UDPConnection(String address, int port, int localPort) throws SocketException, UnknownHostException {
        this.socket = new DatagramSocket(localPort);
        this.address = InetAddress.getByName(address);
        this.port = port;
        this.localPort = localPort;
    }

    public final int getLocalPort() {
        return this.localPort;
    }

    public DatagramSocket getDatagramSocket() {
        return this.socket;
    }

    public final void setLocalPort(int port) {
        try {
            this.socket.bind(new InetSocketAddress(port));
            this.localPort = port;
        }
        catch (SocketException ex) {
            if (this.recursions++ < UDPConnection.MAX_RECURSIONS) {
                Random rand = new Random(Instant.now().toEpochMilli());
                port = 1025 + rand.nextInt(65535 - 1025);
                this.setLocalPort(port);
            }
        }
    }

    public final void connect() {
        this.socket.connect(this.address, this.port);
    }

    public final void on(EVENTS event, Function<byte[], byte[]> action) {
        this.callbacks.put(event, action);
    }

    public final void call(EVENTS event, byte[] data) {
        if (this.callbacks.containsKey(event)) {
            this.callbacks.get(event).apply(data);
        }
    }

    public abstract void init();

    @Override
    public final void run() {
        this.init();
        try {

            this.call(EVENTS.CONNECTED, null);

            this.buffer = new byte[this.socket.getReceiveBufferSize()];

            DatagramPacket packet = new DatagramPacket(this.buffer, this.socket.getReceiveBufferSize());

            while (running) {
                this.connect();
                this.socket.receive(packet);

                this.call(EVENTS.INCOMING, packet.getData());
            }
        }
        catch (SocketException ex) {
            Logger.getLogger(UDPConnection.class.getName()).log(Level.SEVERE, null, ex);
            this.call(EVENTS.ERROR, null);
        }
        catch (IOException ex) {
            Logger.getLogger(UDPConnection.class.getName()).log(Level.SEVERE, null, ex);
            this.call(EVENTS.ERROR, null);
        }
    }

    public void write(byte[] data) throws IOException {
        this.socket.send(new DatagramPacket(data, data.length, this.address, this.port));
        this.socket.disconnect();
    }

    public void close() {
        this.running = false;

        this.socket.close();

        this.call(EVENTS.CLOSED, null);
    }
}
