/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TCPServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gaelph
 */
public abstract class TCPServer implements Runnable {

    protected Integer port;

    private boolean running = true;

    private ServerSocket serverSocket;

    private final Map<EVENTS, Consumer<TCPEvent>> callbacks;

    private static final Logger LOG = Logger.getLogger(TCPServer.class.getName());

    public static enum EVENTS {
        START,
        CONNECTION,
        STOP,
        ERROR
    }

    /**
     *
     */
    protected TCPServer() {
        this.callbacks = new HashMap<>();
    }

    public static TCPServer createServer() {
        TCPServer instance = null;

        synchronized (TCPServer.class) {
            try {
                //TODO default rootPath
                instance = TCPServer.class.newInstance();

                ((TCPServer) instance).init();
            }
            catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(TCPServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return instance;
    }

    public void listen(Integer port) {
        this.setPort(port);
        new Thread(this, this.getClass().getName()).start();
    }

    protected abstract void init();

    protected void setPort(Integer port) {
        this.port = port;
    }

    public Integer getPort() {
        return this.port;
    }

    public void on(EVENTS event, Consumer<TCPEvent> callback) {
        callbacks.put(event, callback);
    }

    private void call(EVENTS event) {
        if (callbacks.containsKey(event)) {
            callbacks.get(event).accept(new TCPEvent(this, Optional.empty()));
        }
    }

    private void call(EVENTS event, Object data) {
        if (callbacks.containsKey(event)) {
            callbacks.get(event).accept(new TCPEvent(this, Optional.of(data)));
        }
    }

    @Override
    public void run() {
        call(EVENTS.START);

        try {
            this.serverSocket = new ServerSocket(port);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        LOG.log(Level.INFO, "Server started");

        while (running) {
            try {
                Socket socket = serverSocket.accept();

                call(EVENTS.CONNECTION, socket);

            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        running = false;

        call(EVENTS.STOP);
    }

    public class TCPEvent {

        public TCPServer server;
        public Optional<Object> value;

        public TCPEvent(TCPServer server, Optional<Object> value) {
            this.server = server;
            this.value = value;
        }
    }
}
