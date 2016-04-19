/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.WebRTC;

import Utils.stringprep.StringPrep;
import java.time.Instant;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gaelph
 */
public class ICEAuth {

    public String ufrag;
    public String pwd;

    private static final int UFRAG_LENGTH = 16;
    private static final int PWD_LENGTH = 32;

    public ICEAuth() {
        String chars = "/+01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rand = new Random(Instant.now().toEpochMilli());
        char[] newUfrag = new char[UFRAG_LENGTH];
        char[] newPwd = new char[PWD_LENGTH];

        for (int i = 0; i < newUfrag.length; i++) {
            newUfrag[i] = chars.charAt(rand.nextInt(chars.length()));
        }

        for (int i = 0; i < newPwd.length; i++) {
            newPwd[i] = chars.charAt(rand.nextInt(chars.length()));
        }

        try {
            this.ufrag = Utils.stringprep.StringPrep.prepAsQueryString(new String(newUfrag));
            this.pwd = Utils.stringprep.StringPrep.prepAsQueryString(new String(newPwd));
        }
        catch (StringPrep.StringPrepError ex) {
            Logger.getLogger(ICEAuth.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ICEAuth(String ufrag, String pwd) {
        this.ufrag = ufrag;
        this.pwd = pwd;
    }
}
