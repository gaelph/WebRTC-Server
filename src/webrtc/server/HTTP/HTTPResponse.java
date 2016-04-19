/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.HTTP;

import TCPServer.TCPConnection;
import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.format.*;

import java.util.Map;
import java.util.HashMap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import java.util.logging.Level;
import java.util.logging.Logger;

import webrtc.server.Files.Files;
import webrtc.server.Files.NoRootFolderException;

/**
 * HTTP Response Class
 *
 * @author gaelph
 * @version 0.1 TODO: Implement default Responses (eg: 304, 404, 500 ...)
 */
public class HTTPResponse {

    private static final String EOL = "\r\n";

    /**
     * the HTTP Version
     */
    public String version;

    /**
     * HTTP StatusCode
     */
    public int status;

    /**
     * A Map of header fields
     */
    public Map<String, String> header = new HashMap<>();

    /**
     * A Map of Cookies to set TODO: Could be a Set
     */
    public Set<HTTPCookie> cookies = new HashSet<>();

    /**
     * The content to send
     */
    private String pathToContent;
    private String content;

    private TCPConnection connection;

    public HTTPResponse() {
        this.version = "HTTP/1.1";

        header.put("Cache-Control", "max-age=3600");
        header.put("Connection", "close");

        header.put("Server", "GaelphServer/0.1 (Java)");
    }

    /**
     *
     * @param content Content to send (HTML...)
     *
     */
    public HTTPResponse(String content) {
        this();
        this.status = this.setContent(content);
    }

    public HTTPResponse(TCPConnection connection) {
        this();
        this.setConnection(connection);
    }

    public void send() throws Exception {
        this.connection.write(this.toString().getBytes());
    }

    public void sendFile(String path) throws Exception {
        this.status = this.setContent(path);
        this.connection.write(this.toString().getBytes());
    }

    private void setConnection(TCPConnection connection) {
        this.connection = connection;
    }

    private static String makeEtag(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(data.getBytes());
            String etag = "";
            for (byte b : md5) {
                etag += String.format("%02x", b);
            }

            return etag;
        }
        catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(HTTPResponse.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public final void setBody(String body) {
        this.content = body;
    }

    public final int setContent(String relativePathToContent) {
        if (relativePathToContent != null) {
            try {
                this.pathToContent = relativePathToContent;

                File file = Files.getInstance().getFile(relativePathToContent);

                content = Files.getInstance().getContent(file);

                header.put("Content-Length", String.valueOf(content.length()));
                header.put("Last-Modified", Files.getInstance().getLastModified(file));
                header.put("ETag", makeEtag(content));

                String path = file.getPath();
                String fileName = file.getName();
                String absPath = file.getAbsolutePath();
                String canPath = file.getCanonicalPath();
                String[] nameElements = fileName.split("\\.");
                String extension = nameElements[nameElements.length - 1];

                setContentType(HTTPMIMETypes.getMIMEForExtension(extension) + "; charset=utf-8");

                return HTTPStatusCodes.OK;
            }
            catch (NoRootFolderException ex) {
                return HTTPStatusCodes.INTERNAL_SERVER_ERROR;
            }
            catch (IOException ex) {
                return HTTPStatusCodes.NOT_FOUND;
            }
        }
        else {
            this.pathToContent = null;
            content = null;
            return 0;
        }
    }

    public final String getContent() {
        return this.content;
    }

    public void setContentType(String type) {
        header.put("Content-Type", type);
    }

    public void setDate(ZonedDateTime date) {
        header.put("Date", date.format(DateTimeFormatter.RFC_1123_DATE_TIME));
    }

    public void setDate(Date date) {
        date.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        header.put("Date", sdf.format(date.getTime()));
    }

    /**
     *
     * @return A String representation of the HTTPRequest ready to be sent over
     */
    @Override
    public String toString() {
        String result = "";

        result += makeStatusLine() + EOL;

        result += makeHeaderString();

        result += EOL;

        if (status >= 200 && status < 300) {
            if (content != null) {
                result += content;
            }
        }

        return result;
    }

    private String makeStatusLine() {
        return version + " " + String.valueOf(status) + " " + HTTPStatusCodes.getString(status);
    }

    private String makeHeaderString() {
        StringBuilder sb = new StringBuilder();

        setDate(new Date());

        if (status < 200 && status >= 300) {
            header.remove("Content-Length");
        }

        header.keySet().stream().forEach((key) -> {
            sb.append(key).append(": ").append(header.get(key)).append(EOL);
        });

        cookies.stream().forEach((cookie) -> {
            sb.append("Set-Cookie: ").append(cookie.toString()).append(EOL);
        });

        return sb.toString();
    }
}
