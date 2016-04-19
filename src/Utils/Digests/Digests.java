/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utils.Digests;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import static webrtc.server.Utils.bytesFromLong32;

/**
 *
 * @author gaelph
 */
public class Digests {

    /**
     * @param string the string to get SHA1 digest from
     *
     * @return the SHA1 digest as an array of bytes
     */
    public static byte[] stringToSHA1(String string) {
        byte[] result = null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            try {
                result = digest.digest(string.getBytes("UTF-8"));
            }
            catch (UnsupportedEncodingException ex) {
                Logger.getLogger("stringToSHA1").log(Level.SEVERE, null, ex);
            }

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] SHA1(byte[] data) {
        byte[] result = null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            result = digest.digest(data);

        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] crc32(byte[] in) {
        CRC32 coder = new CRC32();
        coder.update(in);
        return bytesFromLong32(coder.getValue());
    }
}
