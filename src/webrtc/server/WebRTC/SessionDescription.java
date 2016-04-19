/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.WebRTC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gaelph
 */
public class SessionDescription {

    public static final String EOL = "\r\n";

    public static final int O_NAME = 0;
    public static final int O_NUM = 1;
    public static final int O_VER = 2;
    public static final int O_NET = 3;
    public static final int O_IPTYPE = 4;
    public static final int O_ADDR = 5;

    private int parsingMedia = -1;

    public int version;
    public String[] origin;
    public String session;
    public int[] timestamp;
    public Map<String, String> attrs = new HashMap<>();
    public List<Media> medias = new ArrayList<>();

    public SessionDescription(String Sdp) {
        StringReader sr = new StringReader(Sdp);
        BufferedReader br = new BufferedReader(sr);

        String line;
        try {
            while ((line = br.readLine()) != null) {
                String key = line.split("=")[0];
                String value = line.substring(line.indexOf("=") + 1);
                String[] v;

                switch (key) {
                    case "v":
                        version = Integer.parseInt(value);
                        break;

                    case "o":
                        origin = value.split(" ");
                        break;

                    case "s":
                        session = value;
                        break;

                    case "t":
                        String[] ts = value.split(" ");
                        timestamp = new int[ts.length];
                        for (int i = 0; i < ts.length; i++) {
                            timestamp[i] = Integer.parseInt(ts[i]);
                        }

                    case "a":
                        int colon = value.indexOf(":");
                        String k;
                        String val;
                        if (colon > 0) {
                            k = value.substring(0, colon);
                            val = value.substring(colon + 1);
                        }
                        else {
                            k = value;
                            val = "";
                        }
                        if (parsingMedia < 0) {

                            attrs.put(k, val);
                        }
                        else {

                            int cID;

                            switch (k) {
                                case "ice-ufrag":
                                    medias.get(parsingMedia).iceAuth.ufrag = val;
                                    break;

                                case "ice-pwd":
                                    medias.get(parsingMedia).iceAuth.pwd = val;
                                    break;

                                case "fingerprint":
                                    String type = val.split(" ")[0];
                                    String fpatterned = val.split(" ")[1].trim();
                                    String[] b = fpatterned.split(":");
                                    byte[] bytes = new byte[b.length];

                                    for (int i = 0; i < b.length; i++) {
                                        bytes[i] = Byte.parseByte(b[i]);
                                    }

                                    medias.get(parsingMedia).fingerprint.put(type, bytes);
                                    break;

                                case "sendrecv":
                                case "sendonly":
                                case "recvonly":
                                    medias.get(parsingMedia).direction = k;
                                    break;

                                case "rtpmap":
                                    v = val.split(" ");
                                    int codecID = Integer.parseInt(v[0]);
                                    medias.get(parsingMedia).codecs.stream()
                                            .filter(codec -> codec.id == codecID)
                                            .forEach(codec -> {
                                                codec.name = v[1].split("/")[0];
                                                codec.bitrate = Integer.parseInt(v[1].split("/")[1]);
                                            });
                                    break;

                                case "fmtp":
                                    v = val.split(" ");
                                    cID = Integer.parseInt(v[0]);
                                    medias.get(parsingMedia).codecs.stream()
                                            .filter(codec -> codec.id == cID)
                                            .forEach(codec -> {
                                                codec.fmtp.add(v[1]);
                                            });
                                    break;

                                case "rtcp-fb":
                                    v = val.split(" ");
                                    cID = Integer.parseInt(v[0]);
                                    medias.get(parsingMedia).codecs.stream()
                                            .filter(codec -> codec.id == cID)
                                            .forEach(codec -> {
                                                codec.params.add(v[1]);
                                            });
                                    break;

                                case "ssrc":
                                    v = val.split(" ");
                                    int ssrcId = Integer.parseInt(v[0]);

                                    if (!medias.get(parsingMedia).ssrc.containsKey(ssrcId)) {
                                        medias.get(parsingMedia).newSSRC(ssrcId);
                                    }

                                    Media.SSRC ssrc = medias.get(parsingMedia).ssrc.get(ssrcId);

                                    for (int i = 1; i < v.length; i++) {
                                        if (v[i].contains(":")) {
                                            String[] pair = v[i].split(":");
                                            ssrc.attributes.put(pair[0], pair[1]);
                                        }
                                        else {
                                            ssrc.attributes.put(v[i], "");
                                        }
                                    }
                                    break;

                                default:
                                    break;
                            }
                        }
                        break;

                    case "m":
                        parsingMedia++;
                        Media media = new Media();
                        v = value.split(" ");
                        media.type = v[0];
                        media.port = Integer.parseInt(v[1]);
                        media.protocols = Arrays.asList(v[2].split("/"));

                        String[] codecs = new String[v.length - 3];
                        System.arraycopy(v, 3, codecs, 0, v.length - 3);

                        Arrays.asList(codecs)
                                .forEach(codec -> media.addCodec(Integer.parseInt(codec)));

                        medias.add(media);
                        break;

                    case "c":
                        if (parsingMedia >= 0) {
                            v = value.split(" ");
                            medias.get(parsingMedia).cAddress = v[2];
                        }

                }
            }
        }
        catch (IOException ex) {
            Logger.getLogger(SessionDescription.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ICEAuth authForMediaIndex(int index) {
        return this.medias.get(index).iceAuth;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("v=").append(version).append(EOL);
        sb.append("o=").append(String.join(" ", origin)).append(EOL);
        sb.append("s=");

        if (!session.equals("")) {
            sb.append(session);
        }
        else {
            sb.append("-");
        }
        sb.append(EOL);

        sb.append("t=").append(timestamp[0]).append(" ").append(timestamp[1]).append(EOL);

        attrs.forEach((key, value) -> {
            sb.append("a=").append(key).append(":").append(value).append(EOL);
        });

        medias.forEach((Media media) -> {
            sb.append("m=");
            sb.append(String.join(" ", Arrays.asList(media.type, String.valueOf(media.port), String.join("/", media.protocols))));
            sb.append(" ");
            media.codecs.forEach(codec -> {
                sb.append(codec.id).append(" ");
            });
            sb.append(EOL);

            sb.append("c=").append("IN IP4").append(media.cAddress).append(EOL);

            sb.append("a=").append("ice-ufrag:").append(media.iceAuth.ufrag).append(EOL);
            sb.append("a=").append("ice-pwd:").append(media.iceAuth.pwd).append(EOL);
            sb.append("a=").append(media.direction).append(EOL);

            String fpType = media.fingerprint.keySet().stream().findFirst().get();
            sb.append("a=").append("fingerprint:").append(fpType).append(" ");

            byte[] fp = media.fingerprint.get(fpType);
            for (int i = 0; i < fp.length; i++) {
                sb.append(Byte.toString(fp[i]));
                if (i < fp.length - 1) {
                    sb.append(":");
                }
            }
            sb.append(EOL);

            media.codecs.forEach(codec -> {
                sb.append("a=");
                sb.append("rtpmap:").append(String.valueOf(codec.id)).append(" ");
                sb.append(codec.name).append("/");
                sb.append(String.valueOf(codec.bitrate));
                sb.append(EOL);

                codec.params.forEach(p -> {
                    sb.append("a=");
                    if (media.type.equals(Media.AUDIO)) {
                        sb.append("fmtp:").append(String.valueOf(codec.id)).append(" ").append(p);
                    }
                    else {
                        sb.append("rtcp-fb:").append(String.valueOf(codec.id)).append(" ").append(p);
                    }
                    sb.append(EOL);
                });
            });

            media.ssrc.keySet().forEach(key -> {
                sb.append("a=");
                sb.append("ssrc:");
                sb.append(String.valueOf(key)).append(" ");

                Media.SSRC ssrc = media.ssrc.get(key);
                ssrc.attributes.forEach((k, value) -> sb.append(k).append(":").append(value).append(" "));

                sb.append(EOL);
            });

        });

        return sb.toString();
    }

    public class Media {

        public static final String AUDIO = "audio";
        public static final String VIDEO = "video";

        public static final String SENDRECV = "sendrecv";
        public static final String SENDONLY = "sendonly";
        public static final String RECVONLY = "recvonly";

        public String type;
        public int port;
        public String cAddress;
        public List<String> protocols;
        public List<Codec> codecs = new ArrayList<>();

        public ICEAuth iceAuth;
        public Map<String, byte[]> fingerprint = new HashMap<>();

        public String direction;

        public Map<Integer, SSRC> ssrc = new HashMap<>();

        public void addCodec(Integer codec) {
            Codec c = new Codec();
            c.id = codec;
            codecs.add(c);
        }

        public void newSSRC(int ssrcId) {
            SSRC nSSRC = new SSRC();
            this.ssrc.put(ssrcId, nSSRC);
        }

        public class Codec {

            int id;
            String name;
            int bitrate;
            int sampling;
            List<String> fmtp = new ArrayList<>();
            List<String> params = new ArrayList<>();

        }

        public class SSRC {

            Map<String, String> attributes = new HashMap<>();
        }
    }
}
