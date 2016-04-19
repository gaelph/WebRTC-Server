/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.HTTP;

import java.util.Map;
import java.util.HashMap;

/**
 * A Class to easily find status codes and their corresponding string
 * @author gaelph
 * @version 0.1
 */
public class HTTPStatusCodes {

    private static HTTPStatusCodes instance = null;
    private final Map<Integer, String> codes = new HashMap<>();
    
    public static final int CONTINUE = 100;
    public static final int SWITCHING_PROTOCOLS = 101;
    
    public static final int OK = 200;
    public static final int CREATED = 201;
    //...
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    //...
    public static final int INTERNAL_SERVER_ERROR = 500;

    protected HTTPStatusCodes() {
        //
        codes.put(CONTINUE, "Continue");
        codes.put(SWITCHING_PROTOCOLS, "Switching Protocols");
        
        //
        codes.put(OK, "OK");
        codes.put(CREATED, "Created");
        codes.put(202, "Accepted");
        codes.put(203, "Non-Authoritative Information");
        codes.put(204, "No Content");
        codes.put(205, "Reset Content");
        codes.put(206, "Partial Content");
        
        //Redirection
        codes.put(300, "Multiple Choices");
        codes.put(301, "Moved Permanently");
        codes.put(302, "Found");
        codes.put(303, "See Other");
        codes.put(304, "Not Modified");
        codes.put(305, "Use Proxy");
        codes.put(307, "Temporary Redirect");
        
        //Request Errors
        codes.put(BAD_REQUEST, "Bad Request");
        codes.put(UNAUTHORIZED, "Unauthorized");
        codes.put(402, "Payment Required");
        codes.put(FORBIDDEN, "Forbidden");
        codes.put(NOT_FOUND, "Not Found");
        codes.put(METHOD_NOT_ALLOWED, "Method Not Allowed");
        codes.put(406, "Not Acceptable");
        codes.put(407, "Proxy Authentication Required");
        codes.put(408, "Request Timeout");
        codes.put(409, "Conflict");
        codes.put(410, "Gone");
        codes.put(411, "Length Required");
        codes.put(412, "Precondition Failed");
        codes.put(413, "Request Entity Too Large");
        codes.put(414, "Request-URI Too Long");
        codes.put(415, "Unsupported Media Type");
        codes.put(416, "Requested Range Not Satisfiable");
        codes.put(417, "Expectation Failed");
        
        //Server Errors
        codes.put(INTERNAL_SERVER_ERROR, "Internal Server Error");
        codes.put(501, "Not Implemented");
        codes.put(502, "Bad Gateway");
        codes.put(503, "Service Unavailable");
        codes.put(504, "Gateway Timeout");
        codes.put(505, "HTTP Version Not Supported");
        
    }

    /**
     *
     * @param code  the status code for which we want the corresponding string
     * @return      the string matching the code
     */
    public static String getString(int code) {
        if (instance == null) {
            synchronized (HTTPStatusCodes.class) {
                instance = new HTTPStatusCodes();
            }
        }

        if (instance.codes.containsKey(code)) {
            return instance.codes.get(code);
        }

        return "";
    }
}
