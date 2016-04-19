/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.HTTP;

import TCPServer.TCPConnection;
import TCPServer.TCPServer;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author gaelph
 */
public class HTTPConnection extends TCPConnection {

    protected final HTTPServer httpServer;

    protected HTTPConnection(TCPServer server, Socket socket) {
        super(server, socket);
        httpServer = null;
    }

    protected HTTPConnection(HTTPServer server, Socket socket) {
        super(server, socket);
        httpServer = server;
    }

    public static TCPConnection newInstance(HTTPServer server, Socket socket) {
        HTTPConnection conn = new HTTPConnection(server, socket);

        conn.start();

        return conn;
    }

    @Override
    public void init() {

        this.on(TCPConnection.Events.INCOMING, (Object event) -> {
            String dataAsString = new String((byte[]) ((TCPConnectionEvent) event).value.get());

            if (!isHTTP(dataAsString)) {
                running = false;
                return null;
            }
            else {
                try {
                    HTTPRequest request = new HTTPRequest(dataAsString);

                    new Thread(() -> {
                        httpServer.callBefore(request);

                        HTTPResponse response = new HTTPResponse(this);

                        httpServer.call(request, response);

                    }, request.method + " " + request.path).start();

                }
                catch (HTTPRequestParseException ex) {
                    Logger.getLogger(HTTPConnection.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }
        });

        this.on(TCPConnection.Events.AFTER_OUTGOING, (event) -> {
            try {
                this.close();
            }
            catch (IOException ex) {
                Logger.getLogger(HTTPConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;

        });
    }

    private boolean isHTTP(String data) {
        Pattern pattern = Pattern.compile("HTTP\\/1\\..");
        Matcher matcher = pattern.matcher(data);

        return matcher.find();
    }

}
