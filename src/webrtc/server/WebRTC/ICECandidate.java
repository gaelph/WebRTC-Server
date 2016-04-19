/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.WebRTC;

import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 *
 * @author gaelph
 */
public class ICECandidate {

    public String foundation;
    public int id;
    public String transport;
    public int priority;
    public String address;
    public Integer port;

    public String Mid;
    public int lineIndex;

    public Map<String, String> extensions = new HashMap<>();

    private static final int FOUNDATION_INDEX = 0;
    private static final int ID_INDEX = 1;
    private static final int TRANSPORT_INDEX = 2;
    private static final int PRIORITY_INDEX = 3;
    private static final int ADDRESS_INDEX = 4;
    private static final int PORT_INDEX = 5;
    private static final int EXT_INDEX = 6;

    public static final String UDP = "udp";
    public static final String TCP = "tcp";

    public static final String EXT_TYP_KEY = "typ";
    public static final String EXT_TCPTYP_KEY = "tcptype";
    public static final String EXT_GEN_KEY = "generation";
    public static final String EXT_UFRAG_KEY = "ufrag";

    private static final String SP = " ";

    public static ICECandidate parse(JSONObject candidateDict) {
        String candidateString = (String) candidateDict.get("candidate");
        ICECandidate candidate = new ICECandidate();

        candidate.Mid = (String) candidateDict.get("sdpMid");
        candidate.lineIndex = Integer.parseInt((String) candidateDict.get("sdpMLineIndex"));

        candidateString = candidateString.split(":")[1];

        String[] elements = candidateString.split(" ");

        candidate.foundation = elements[FOUNDATION_INDEX];
        candidate.id = Integer.parseInt(elements[ID_INDEX]);
        candidate.transport = elements[TRANSPORT_INDEX];
        candidate.priority = Integer.parseInt(elements[PRIORITY_INDEX]);
        candidate.address = elements[ADDRESS_INDEX];
        candidate.port = Integer.parseInt(elements[PORT_INDEX]);

        for (int i = EXT_INDEX; i < elements.length; i += 2) {
            candidate.extensions.put(elements[i], elements[i + 1]);
        }

        return candidate;
    }

    public int priority() {
        String type = this.extensions.get(EXT_TYP_KEY);
        int type_preference;
        this.priority = 0;

        switch (type) {
            case "host":
                type_preference = 126;
                break;

            case "srflx":
                type_preference = 116;
                break;

            case "prlx":
                type_preference = 100;
                break;

            case "relay":
            default:
                type_preference = 0;
        }

        this.priority += type_preference << 24;
        this.priority += 65535 << 8;

        this.priority += this.id;

        return this.priority;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("candidate:");

        sb.append(foundation).append(SP);
        sb.append(String.valueOf(id)).append(SP);
        sb.append(transport).append(SP);
        sb.append(String.valueOf(priority)).append(SP);
        sb.append(address).append(SP);
        sb.append(String.valueOf(port)).append(SP);

        sb.append(EXT_TYP_KEY).append(SP);
        sb.append(extensions.get(EXT_TYP_KEY)).append(SP);

        extensions.keySet().stream()
                .filter(key -> !key.equals(EXT_TYP_KEY))
                .forEach(key -> sb.append(key).append(SP).append(extensions.get(key)).append(SP)
                );

        return sb.toString().trim();
    }

}
