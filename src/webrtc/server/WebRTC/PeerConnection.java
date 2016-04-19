/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.WebRTC;

import STUN.StunServer;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.crypto.tls.DTLSTransport;
import org.bouncycastle.crypto.tls.TlsClientProtocol;
import org.json.simple.JSONObject;
import webrtc.server.Utils;
import webrtc.server.WebRTCServer;

/**
 *
 * @author gaelph
 */
public class PeerConnection {

    private MediaStream stream;

    private final Set<ICECandidate> candidates = new HashSet<>();

    private SessionDescription localDescription;
    private SessionDescription remoteDescription;

    private final Map<String, Consumer<Object>> callbacks = new HashMap<>();

    public void on(String label, Consumer<Object> action) {
        this.callbacks.put(label, action);
    }

    private void call(String label, Object arg) {
        if (this.callbacks.containsKey(label)) {
            this.callbacks.get(label).accept(arg);
        }
    }

    public void addCandidate(JSONObject candidate) {
        ICECandidate c = ICECandidate.parse(candidate);
        this.candidates.add(c);

        String address;
        try {
            address = Utils.getLocalIP4Address().getHostAddress();
            StunServer server = StunServer.createServerWithType(c.transport, c.address, c.port);

            ICEAuth remoteIceAuth = this.remoteDescription().authForMediaIndex(c.lineIndex - 1);
            ICEAuth localIceAuth = this.localDescription().authForMediaIndex(c.lineIndex - 1);

            server.setRemoteCredentials(remoteIceAuth);
            server.setLocalCredentials(localIceAuth);

            server.start();

            c.address = address;
            c.port = server.getLocalPort();

            candidate.put("candidate", c.toString());

            this.call("icecandidate", candidate.toJSONString());

            Object encryptedTransport = server.getEncryptedTransport();

            if (encryptedTransport instanceof DTLSTransport) {
                this.addStream(new MediaStream((DTLSTransport) encryptedTransport));
            }
            else {
                this.addStream(new MediaStream((TlsClientProtocol) encryptedTransport));
            }
        }
        catch (SocketException ex) {
            Logger.getLogger(WebRTCServer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void setLocalDescription(JSONObject dict) {
        String sdp = (String) dict.get("sdp");
        this.localDescription = new SessionDescription(sdp);
    }

    public void setRemoteDescription(JSONObject dict) {
        String sdp = (String) dict.get("sdp");
        this.remoteDescription = new SessionDescription(sdp);
    }

    public JSONObject createAnswer() {
        String desc = this.remoteDescription.toString();
        SessionDescription answerDesc = this.remoteDescription();
        answerDesc.attrs.put("setup", "active");

        answerDesc.medias.forEach(media -> {
            media.iceAuth = new ICEAuth();
        });

        JSONObject result = new JSONObject();
        result.put("sdp", desc);
        result.put("type", "answer");

        return result;
    }

    public void addStream(MediaStream stream) {
        this.stream = stream;
        new Thread(stream).start();
    }

    public SessionDescription localDescription() {
        return this.localDescription;
    }

    public SessionDescription remoteDescription() {
        return this.remoteDescription;
    }
}
