/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TCPServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gaelph
 */
public abstract class TCPConnection extends Thread {

    public final TCPServer server;

    protected final Socket socket;
    private int localPort;

    private InputStream inputStream;
    private DataOutputStream outputStream;

    protected boolean running = true;

    private Map<String, Function<Object, Optional<? super Object>>> callbacks = new HashMap<>();

    private static final Logger LOG = Logger.getLogger(TCPConnection.class.getName());

    protected TCPConnection(Socket socket) {
        this.socket = socket;
        this.server = null;
        this.setName(socket.toString());
        try {
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            this.inputStream = socket.getInputStream();

        }
        catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    protected TCPConnection(String address, int port, int localPort) throws IOException {
        this.socket = new Socket(InetAddress.getByName(address), port, null, localPort);
        this.server = null;
        this.setName(socket.toString());
        try {
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            this.inputStream = socket.getInputStream();

        }
        catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    protected TCPConnection(TCPServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
        this.setName(socket.toString());
        try {
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            this.inputStream = socket.getInputStream();
        }
        catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    public final int getLocalPort() {
        return this.socket.getLocalPort();
    }

    public final Socket getSocket() {
        return this.socket;
    }

    public final void setLocalPort(int port) throws IOException {
        this.socket.bind(new InetSocketAddress(port));
    }

    public abstract void init();

    public void on(String event, Function<Object, Optional<? super Object>> callback) {
        callbacks.put(event, callback);
    }

    public Optional<Object> call(String event) {
        if (callbacks.containsKey(event)) {
            return callbacks.get(event).apply(new TCPConnectionEvent(this, Optional.empty()));
        }
        else {
            return Optional.empty();
        }
    }

    public Optional<Object> call(String event, Object data) {
        if (callbacks.containsKey(event)) {
            return callbacks.get(event).apply(new TCPConnectionEvent(this, Optional.of(data)));
        }
        else {
            return Optional.empty();
        }
    }

    @Override
    public void run() {
        this.init();
        call(Events.CONNECTION);

        while (running) {

            try {
                int readBytes;
                byte[] buffer = new byte[this.socket.getReceiveBufferSize()];
                int totalReadBytes = 0;
                byte[] aggregBuffer = new byte[0];

                while ((readBytes = inputStream.read(buffer)) != -1 && running) {
                    byte[] newBuffer = new byte[totalReadBytes + readBytes];
                    System.arraycopy(buffer, 0, newBuffer, totalReadBytes, readBytes);
                    System.arraycopy(aggregBuffer, 0, newBuffer, 0, totalReadBytes);
                    totalReadBytes += readBytes;
                    aggregBuffer = newBuffer;

                    if (readBytes < this.socket.getReceiveBufferSize()) {
                        break;
                    }
                }

                if (!running) {
                    break;
                }

                if (totalReadBytes != 0) {
                    call(Events.INCOMING, aggregBuffer);
                }
            }
            catch (IOException e) {
                //LOG.log(Level.SEVERE, "Error reading socket : {0}", e.getMessage());
                //e.printStackTrace();

                if (running) {
                    LOG.log(Level.SEVERE, "Socket closed abnormally : {0}", e.getMessage());
                    try {
                        close();
                    }
                    catch (IOException ioe) {
//                        ioe.printStackTrace();
                    }
                }
            }
        }

        terminate();
    }

    public void write(byte[] data) throws IOException {
        try {
            call(Events.BEFORE_OUTGOING, data);

            this.outputStream.write(data, 0, data.length);
            this.outputStream.flush();

            call(Events.AFTER_OUTGOING, data);
        }
        catch (Exception e) {
            LOG.log(Level.SEVERE, "Error writing to socket : {0}", e.getMessage());
            try {
                close();
                terminate();
            }
            catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            throw new IOException("Socket Closed");
        }
    }

    public void close() throws IOException {
        running = false;
        inputStream.close();
        outputStream.close();
        socket.close();
    }

    protected void terminate() {
        call(Events.DISCONNECTION);
        interrupt();
    }

    public class TCPConnectionEvent {

        public TCPConnection connection;
        public Optional<Object> value;

        public TCPConnectionEvent(TCPConnection connection, Optional<Object> value) {
            this.connection = connection;
            this.value = value;
        }
    }

    public class Events {

        public static final String CONNECTION = "connection";
        public static final String INCOMING = "incoming";
        public static final String BEFORE_OUTGOING = "before_outgoing";
        public static final String AFTER_OUTGOING = "after_outgoing";
        public static final String DISCONNECTION = "disconnectio";
    }
}
