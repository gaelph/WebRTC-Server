/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.HTTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.*;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author gaelph
 */
public class HTTPRequest {

    private static final Logger LOG = Logger.getLogger(HTTPRequest.class.getName());

    public String method;
    public String version;
    public int status;

    public String path;

    public Map<String, Object> params = new HashMap<>();
    public Map<String, String> header = new HashMap<>();
    public Set<HTTPCookie> cookies = new HashSet<>();

    public String stringContent;
    public Document xmlContent;
    public JSONObject jsonContent;
    public byte[] byteContent;

    public Map<String, Object> formContent = new HashMap<>();

    public HTTPRequest(String data) throws HTTPRequestParseException {
        String[] lines = data.split("\r\n");

        parseStatusLine(lines[0]);

        int i = 1;
        String line;

        while ((line = lines[i++]).length() > 1) {
            String[] keyValues = line.split(":");

            header.put(keyValues[0].trim().toLowerCase(), keyValues[1].trim());

            if (header.containsKey("cookie") && cookies.isEmpty()) {
                parseCookies(header.get("cookie"));
            }

            if (i == lines.length) {
                break;
            }
        }

        if (header.containsKey("content-length")) {
            stringContent = data.substring(data.indexOf("\r\n\r\n"));

//            while (i < lines.length) {
//                stringContent += lines[i++] + "\r\n";
//            }
        }

        if (header.containsKey("content-type")) {
            String[] elements = ((String) header.get("content-type")).split(";");
            switch (elements[0]) {
                case "application/xml": {
                    xmlContent = xmlFromString(stringContent);
                }
                break;

                case "application/json": {
                    try {
                        jsonContent = jsonFromString(stringContent);
                    }
                    catch (ParseException ex) {
                        Logger.getLogger(HTTPRequest.class.getName()).log(Level.SEVERE, null, ex);
                        throw new HTTPRequestParseException();
                    }
                }
                break;

                case "application/x-www-form-urlencoded":
                    formContent = mapFromUrlEncodedString(stringContent);
                    break;

                //TODO: Implement file upload
                case "multipart/form-data":
                case "multipart/mixed":
                    String[] bs = elements[1].split("=");
                    String boundary = bs[1];

                    formContent = mapFromMultipart(stringContent, boundary);

                    break;

                default:
                    break;
            }
        }

    }

    private static Document xmlFromString(String xmlString) {
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();

            is.setCharacterStream(new StringReader(xmlString));
            return dBuilder.parse(is);

        }
        catch (ParserConfigurationException | SAXException | IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }

        return null;
    }

    private static JSONObject jsonFromString(String jsonString) throws ParseException {
        return (JSONObject) new JSONParser().parse(jsonString);
    }

    private static HashMap<String, Object> mapFromUrlEncodedString(String urlString) {
        HashMap<String, Object> result = new HashMap<>();

        String[] pairs = urlString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");

            try {
                result.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
            }
            catch (UnsupportedEncodingException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }

        return result;
    }

    //TODO: implement proper file / form-data
    private static HashMap<String, Object> mapFromMultipart(String data, String boundary) {
        HashMap<String, Object> result = new HashMap<>();

        StringReader reader = new StringReader(data);
        BufferedReader br = new BufferedReader(reader);
        String line;

        {
            try {
                String currentName = "";
                String currentValue = "";
                boolean appendContent = false;
                boolean skipTillNextBoundary = true;

                while ((line = br.readLine()) != null) {

                    if (line.equals("--" + boundary + "--")) {
                        return result;
                    }

                    if (line.equals("--" + boundary)) {
                        if (!currentName.equals("")) {
                            result.put(currentName, currentValue);
                        }
                        currentName = "";
                        currentValue = "";
                        appendContent = false;
                        skipTillNextBoundary = false;
                        continue;
                    }

                    if (skipTillNextBoundary) {
                        continue;
                    }

                    if (line.contains("Content-Disposition") || line.contains("content-disposition")) {
                        String[] firstSplit = line.split(":");
                        String[] components = firstSplit[1].trim().split(";");

                        for (String component : components) {
                            String[] keyValue = component.split("=");
                            if (keyValue[0].equals("name")) {
                                currentName = keyValue[1].trim().replaceAll("\"", "");
                            }
                        }

                        continue;
                    }

                    if (line.contains("Content-Type") || line.contains("content-type")) {
                        String[] firstSplit = line.split(":");
                        String[] components = firstSplit[1].trim().split(";");

                        for (String component : components) {
                            component = component.trim();
                            if (component.contains("multipart")) {
                                String[] elements = component.split(";");
                                String[] bs = elements[1].trim().split("=");
                                String newBoundary = bs[1];

                                HashMap<String, Object> submap = mapFromMultipart(data, newBoundary);
                                result.put(currentName, submap);
                                skipTillNextBoundary = true;
                            }
                        }

                        continue;
                    }

                    if (line.length() < 2) {
                        appendContent = true;
                        continue;
                    }

                    if (appendContent) {
                        currentValue += line + "\r\n";
                    }

                }

            }
            catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }

        return result;
    }

    private void parseStatusLine(String statusLine) {
        String[] fields = statusLine.split(" ");
        String[] uriElements = fields[1].split("\\?");
        method = fields[0];
        path = uriElements[0];
        version = fields[2];

        if (uriElements.length > 1) {
            parseParams(uriElements[1]);
        }
    }

    private void parseParams(String paramString) {
        String[] uriParams = paramString.split("&");

        for (String param : uriParams) {
            String[] keyValues = param.split("=");

            if (keyValues.length > 1) {
                try {
                    params.put(URLDecoder.decode(keyValues[0], "UTF-8"), URLDecoder.decode(keyValues[1], "UTF-8"));
                }
                catch (UnsupportedEncodingException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }

            }
            else {
                try {
                    params.put(URLDecoder.decode(keyValues[0], "UTF-8"), "");
                }
                catch (UnsupportedEncodingException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    private void parseCookies(String cookieString) {
        String[] cs = cookieString.split(";");
        for (String cookie : cs) {
            String[] keyValues = cookie.split("=");
            HTTPCookie c = new HTTPCookie(keyValues[0], keyValues[1]);
            this.cookies.add(c);
        }
    }

    @Override
    public String toString() {
        return method + " " + path;
    }
}
