/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.HTTP;

import TCPServer.TCPServer;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import webrtc.server.Files.Files;
import webrtc.server.Files.RootFolderNotFoundException;

/**
 *
 * @author gaelph
 */
public class HTTPServer extends TCPServer {

    private String rootPath;
    private String subPath = "";

    private final Map<String, BiConsumer<HTTPRequest, HTTPResponse>> routeMap = new HashMap<>();
    private final Map<Pattern, List<String>> pathPatterns = new HashMap<>();

    private final Set<String> allowedPaths = new HashSet<>();
    private final Set<String> disallowedPaths = new HashSet<>();

    private final List<Consumer<HTTPRequest>> middlewareBefore = new ArrayList<>();
    private final List<Consumer<HTTPResponse>> middlewareAfter = new ArrayList<>();

    protected HTTPServer(String rootPath) {
        super();
        this.rootPath = rootPath;
    }

    public static TCPServer createServer() {
        HTTPServer instance;

        synchronized (HTTPServer.class) {
            //TODO default rootPath
            instance = new HTTPServer(null);

            ((HTTPServer) instance).init();
        }

        return instance;
    }

    @Override
    public void listen(Integer port) {
        this.port = port;
        this.init();
        new Thread(this, this.getClass().getSimpleName()).start();
    }

    public void on(String method, String path, BiConsumer<HTTPRequest, HTTPResponse> action) {
        //Parse path for :param patterns
        PatternTuple p = makePattern(path);
        this.pathPatterns.put(p.pattern, p.parameters);

        //the path with the /user/:uid form
        routeMap.put(method + " " + path, action);
    }

    public void call(HTTPRequest request, HTTPResponse response) {
        //match path

        if (routeMap.containsKey(request.method + " " + request.path)) {
            routeMap.get(request.method + " " + request.path).accept(request, response);
        }
        else if ((request.method.equals("GET") || request.method.equals("HEAD"))
                 && request.status != HTTPStatusCodes.FORBIDDEN) {
            try {
                response.sendFile(request.path);
            }
            catch (Exception ex) {
                Logger.getLogger(HTTPServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            String allowedMethods = "";

            Iterator itr = this.routeMap.keySet().iterator();
            while (itr.hasNext()) {
                String key = (String) itr.next();
                String method = key.substring(0, key.indexOf(' '));
                allowedMethods += method;

                if (method.equals("GET")) {
                    allowedMethods += ", HEAD";
                }

                allowedMethods += itr.hasNext() ? ", " : "";
            }

            response.status = HTTPStatusCodes.METHOD_NOT_ALLOWED;
            response.header.put("Allow", allowedMethods);

            try {
                response.send();
            }
            catch (Exception ex) {
                Logger.getLogger(HTTPServer.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    @Override
    protected void init() {

        on(TCPServer.EVENTS.CONNECTION, this::createNewConnection);

        useBefore(this::usePath);
        useBefore(this::routeParams);
        useBefore(this::filter);

        disallow(".ht*");

    }

    private void createNewConnection(TCPEvent event) {
        HTTPConnection.newInstance(this, (Socket) event.value.get());
    }

    private void routeParams(HTTPRequest request) {
        // colon pattern
        this.pathPatterns.entrySet()
                .stream()
                .filter((entry) -> {
                    Pattern pattern = entry.getKey();
                    List<String> parameters = entry.getValue();

                    if (parameters.isEmpty()) {
                        return false;
                    }

                    Matcher matcher = pattern.matcher(request.path);

                    //Find parameters according to the path pattern and adds them to the request's params Map
                    if (matcher.find()) {
                        return matcher.groupCount() == parameters.size();
                    }

                    return false;
                })
                .forEach((entry) -> {
                    Pattern pattern = entry.getKey();
                    List<String> parameters = entry.getValue();

                    Matcher matcher = pattern.matcher(request.path);

                    matcher.find();

                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        String group = matcher.group(i);
                        request.params.put(parameters.get(i - 1), group);
                        request.path = request.path.replaceFirst(group, ":" + parameters.get(i - 1));
                    }

                });

        // URI params shouldn't this be in the request class ?
        if (request.path.contains("?")) {
            String paramsString = request.path.split("\\?")[1];
            request.path = request.path.split("\\?")[0];

            List<String> paramsPairs = Arrays.asList(paramsString.split("\\&"));

            paramsPairs
                    .forEach(pair -> {
                        try {
                            request.params.put(URLDecoder.decode(pair.split("=")[0], "UTF-8"),
                                               URLDecoder.decode(pair.split("=")[1], "UTF-8"));
                        }
                        catch (UnsupportedEncodingException ex) {
                            Logger.getLogger(HTTPServer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
        }

        //Post form
        if (request.method.equals("POST") || request.method.equals("PUT")) {
            if (request.header.keySet().stream()
                    .anyMatch(key -> key.equalsIgnoreCase("content-type")
                                     && request.header.get(key).equalsIgnoreCase("application/x-www-form-urlencoded")
                    )) {

                Arrays.asList(request.stringContent.split("&"))
                        .forEach(pair -> {
                            try {
                                request.params.put(URLDecoder.decode(pair.split("=")[0], "UTF-8"),
                                                   URLDecoder.decode(pair.split("=")[1], "UTF-8"));
                            }
                            catch (UnsupportedEncodingException ex) {
                                Logger.getLogger(HTTPServer.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });
            }
            //Post multi-part
            else if (request.header.keySet().stream()
                    .anyMatch(key -> key.equalsIgnoreCase("content-type")
                                     && request.header.get(key).contains("multipart/form-data")
                    )) {
                String boundary = request.header.get("content-type").split(";")[0].trim();
                request.params.putAll(parseMultipart(request.stringContent, boundary));

            }

        }
    }

    private Map<String, Object> parseMultipart(String data, String boundary) {
        Map<String, Object> result = new HashMap<>();

        Arrays.asList(data.split("--" + boundary))
                .forEach(part -> {
                    String name;

                    String value = part.substring(part.indexOf("\r\n\r\n"));

                    Pattern p = Pattern.compile(
                            "([nN]ame=\\\"(?<name>[^\\\\s ]+)\\\")?([bB]oundary=(?<boundary>[^\\\\s ]+))?([fF]ilename=\\\"(?<filename>[^\\\\s ]+)\\\")?(: ?(?<type>form-data|file))?");
                    Matcher m = p.matcher(part);

                    if (m.find()) {
                        name = m.group("name");

                        if (m.group("boundary") != null) {
                            result.putAll(parseMultipart(value, m.group(1)));
                        }
                        else if (m.group("filename") == null) {
                            if (name != null) {
                                result.put(name, value);
                            }
                        }
                        else {
                            if (name == null) {
                                name = m.group("filename");
                            }
                            String filename = m.group("filename");
                            FormFile file = new FormFile();
                            file.name = filename;

                            m = Pattern.compile("[cC]ontent-[tT]ype:(.)\r\n").matcher(part);
                            if (m.find()) {
                                file.MIMEType = m.group(1);
                            }

                            file.setBytes(value);

                            result.put(name, file);
                        }

                    }
                });

        return result;
    }

    private void filter(HTTPRequest request) {
        String[] pathElements = request.path.split("/");
        if (pathElements.length == 0) {
            pathElements = new String[1];
            pathElements[0] = request.path;
        }
        String fileOrDirectory = pathElements[pathElements.length - 1];

        if (fileOrDirectory.contains("\\?")) {
            fileOrDirectory = fileOrDirectory.split("\\?")[0];
        }

        try {
            fileOrDirectory = URLDecoder.decode(fileOrDirectory, "UTF-8");
        }
        catch (UnsupportedEncodingException ex) {
            Logger.getLogger(HTTPServer.class.getName()).log(Level.SEVERE, null, ex);
        }

        final String fOrD = fileOrDirectory;

        this.disallowedPaths
                .forEach((disallowedPath) -> {
                    Pattern p = Pattern.compile(disallowedPath);

                    if (p.matcher(fOrD).find()) {
                        request.status = HTTPStatusCodes.FORBIDDEN;
                    }
                });

    }

    public void usePath(HTTPRequest request) {
        if (!this.subPath.equals("")) {
            request.path = this.subPath + request.path;
        }
    }

    //White list folder and files with wildcards
    public void allow(String path) {
        path = path.replace(".", "\\.");
        path = path.replace("*", ".");
        allowedPaths.add(path);
    }

    //Blacklists folders and files with wildcards
    public void disallow(String path) {
        path = path.replace(".", "\\.");
        path = path.replace("*", ".");
        disallowedPaths.add("^" + path);

    }

    //Set the server's root path
    public void setRootPath(String path) {
        try {
            this.rootPath = path;
            Files.getInstance().setRoot(rootPath);
        }
        catch (RootFolderNotFoundException ex) {
            Logger.getLogger(HTTPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getRootPath() {
        return this.rootPath;
    }

    //Use a folder from root path (eg. /public/ or /www/
    public void use(String path) {
        this.subPath = path;
    }

    //Middleware to use before processing the request
    public void useBefore(Consumer<HTTPRequest> beforeAction) {
        this.middlewareBefore.add(beforeAction);
    }

    //Middleware to use after request has been processed, before sending the response
    public void useAfter(Consumer<HTTPResponse> afterAction) {
        this.middlewareAfter.add(afterAction);
    }

    public void callBefore(HTTPRequest req) {

        this.middlewareBefore.forEach(action -> {
            action.accept(req);
        });
    }

    public void callAfter(HTTPResponse res) {

        this.middlewareAfter.forEach(action -> {
            action.accept(res);
        });
    }

    private PatternTuple makePattern(String path) {
        List<String> parameters = new ArrayList<>();
        //First finds all occurences of ':parameter'
        Pattern pat = Pattern.compile(":([\\w]+)");
        Matcher matcher = pat.matcher(path);

        //Stores those parameters for matching
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                parameters.add(matcher.group(i));
            }
        }
        //Replace the ':parameter' pattern by url encoded values
        String patternString = path.replaceAll("(:[\\w]+)", "([\\\\w%]+)");
        //use the whole string as pattern
        return new PatternTuple(Pattern.compile("^" + patternString + "$"), parameters);

    }

    private class PatternTuple {

        public Pattern pattern;
        public List<String> parameters = new ArrayList<>();

        public PatternTuple(Pattern pattern, List<String> parameters) {
            this.pattern = pattern;
            this.parameters = parameters;
        }
    }

    public class FormFile {

        public String name;
        public String MIMEType;
        private byte[] bytes;

        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        public void setBytes(String value) {
            this.bytes = value.getBytes();
        }

        public byte[] getBytes() {
            return this.bytes;
        }
    }
}
