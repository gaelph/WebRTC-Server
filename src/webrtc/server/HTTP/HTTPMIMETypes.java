/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.HTTP;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author gaelph
 */
public class HTTPMIMETypes {

    public static enum TYPES {
        APP_XML,
        TEXT_XML,
        HTML,
        HTM,
        JSON,
        MULTIPART_URL,
        MULTIPART_DATA,
        MULTIPART_MIXED,
        JAVASCRIPT,
        CSS,
        TEXT_PLAIN
    }

    protected final Map<TYPES, String> map = new HashMap<>();

    protected final Map<String, TYPES> fileExtMap = new HashMap<>();

    protected HTTPMIMETypes() {
        map.put(TYPES.APP_XML, "application/xml");
        map.put(TYPES.TEXT_XML, "text/xml");

        map.put(TYPES.HTML, "text/html");
        map.put(TYPES.HTM, "text/htm");
        map.put(TYPES.CSS, "text/css");
        map.put(TYPES.JAVASCRIPT, "application/javascript");

        map.put(TYPES.JSON, "application/json");

        map.put(TYPES.MULTIPART_URL, "application/x-www-form-urlencoded");
        map.put(TYPES.MULTIPART_DATA, "multipart/form-data");
        map.put(TYPES.MULTIPART_MIXED, "multipart/mixed");

        map.put(TYPES.TEXT_PLAIN, "text/plain");

        //-----///
        fileExtMap.put("xml", TYPES.APP_XML);

        fileExtMap.put("html", TYPES.HTML);
        fileExtMap.put("htm", TYPES.HTM);
        fileExtMap.put("css", TYPES.CSS);
        fileExtMap.put("js", TYPES.JAVASCRIPT);

        fileExtMap.put("txt", TYPES.TEXT_PLAIN);
    }

    public static String get(TYPES type) {
        HTTPMIMETypes instance = new HTTPMIMETypes();
        return instance.map.getOrDefault(type, "text/plain");
    }

    public static String getMIMEForExtension(String ext) {
        HTTPMIMETypes instance = new HTTPMIMETypes();
        return instance.map.getOrDefault(instance.fileExtMap.get(ext), "text/plain");
    }

}
