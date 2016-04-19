/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STUN;

import java.time.Instant;
import java.util.List;
import java.util.Random;

/**
 *
 * @author gaelph
 */
public class StunBinding {

    private final String destUFrag;
    private final String destPwd;
    private final String srcUFrag;
    private final String srcPwd;

    StunBinding(String destUFrag, String destPwd, String srcUFrag, String srcPwd) {
        this.destUFrag = destUFrag;
        this.destPwd = destPwd;
        this.srcUFrag = srcUFrag;
        this.srcPwd = srcPwd;
    }

    public StunMessage responseWithAttributes(StunMessage message, List<StunAttribute> attributes) {
        StunMessage success = new StunMessage(StunMessage.STUN_BINDING_RESPONSE);
        success.setTransactionID(message.getTransactionID());

        attributes.forEach(attribute -> success.addAttribute(attribute));

        return success;
    }

    public StunMessage request() {
        StunMessage request = new StunMessage(StunMessage.STUN_BINDING_REQUEST);
        request.newTransactionID();
        request.addAttribute(new StunAttribute(StunAttribute.USERNAME, (this.srcUFrag + ":" + this.destUFrag).getBytes()));
        return request;
    }

    public StunMessage ICEControllingRequest() {
        StunMessage request = request();

        byte[] tiebreaker = new byte[8];
        new Random(Instant.now().toEpochMilli()).nextBytes(tiebreaker);
        request.addAttribute(new StunAttribute(StunAttribute.ICE_CONTROLLING, tiebreaker));

        return request;
    }

    public StunMessage ICEControlledRequest() {
        StunMessage request = request();

        byte[] tiebreaker = new byte[8];
        new Random(Instant.now().toEpochMilli()).nextBytes(tiebreaker);
        request.addAttribute(new StunAttribute(StunAttribute.ICE_CONTROLLED, tiebreaker));

        return request;
    }

}
