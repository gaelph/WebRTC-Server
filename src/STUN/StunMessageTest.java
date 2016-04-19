/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STUN;

import Utils.bytes.Bytes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 *
 * @author gaelph
 */
public class StunMessageTest {

    private static final int BYTE_SIZE = 1;
    private static final int SHORT_SIZE = 2;
    private static final int INT_SIZE = 4;
    private static final int LONG_SIZE = 8;
    private static final int TRANSACTION_ID_SIZE = 12;
    private static final int HEADER_SIZE = 20;
    private static final int MESSAGE_INTEGRITY_SIZE = 20;

    private static final int COOKIE_OFFSET = 2 * SHORT_SIZE;
    private static final int TRANSACTION_ID_OFFSET = COOKIE_OFFSET + INT_SIZE;

    private static final int MAGIC_COOKIE = 0x2112A442;

    private static final Logger LOG = Logger.getLogger("StunMessageTest");

    public static boolean validateMessage(Bytes buffer, String pwd) {

        short type = buffer.read16(0);
        if ((type & 0x8000) != 0) {
            LOG.log(Level.INFO, "Wrong type : {0}", type);
            return false;
        }

        short size = buffer.read16(SHORT_SIZE);
        if (size == 0) {
            LOG.log(Level.INFO, "Wrong size : {0}", size);
            return false;
        }

        int cookie = buffer.read32(COOKIE_OFFSET);
        if (cookie != StunMessage.MAGIC_COOKIE) {
            LOG.log(Level.INFO, "Wrong cookie : {0}", String.format("%x", cookie));
            return false;
        }

        int attrsPos = HEADER_SIZE;
        int remaining = buffer.size() - HEADER_SIZE;
        boolean hasMessageIntegrity = false;
        boolean hasFingerprint = false;

        do {
            int aType = buffer.read16(attrsPos);
            int aSize = buffer.read16(attrsPos + SHORT_SIZE);

            if ((aSize % 4) > 0) {
                aSize += (4 - (aSize % 4));
            }

            if (aType == StunAttribute.MESSAGE_INTEGRITY) {
                hasMessageIntegrity = true;
                if (!validateMessageIntegrity(attrsPos, buffer, size, pwd)) {
                    LOG.log(Level.INFO, "Wrong Message Integrity");
                    return false;
                }
            }
            else if (aType == StunAttribute.FINGERPRINT) {
                hasFingerprint = true;
                if (!validateFingerprint(attrsPos, buffer, size)) {
                    LOG.log(Level.INFO, "Wrong Fingerprint");
                    return false;
                }
            }

            remaining -= 4 + aSize;
            attrsPos += 4 + aSize;
        } while (remaining > 0);

        if (!hasMessageIntegrity) {
            LOG.log(Level.INFO, "Missing Message Integrity");
            return false;
        }

        if (!hasFingerprint) {
            LOG.log(Level.INFO, "Missing fingerprint");
            return false;
        }

        return true;
    }

    public static boolean validateMessageIntegrity(int miAttrOffset, Bytes buf, int size, String pwd) {
        if ((buf.size() % 4) > 0) {
            return false;
        }

        Bytes temp = buf.copy();

        int aSize = temp.read16(miAttrOffset + SHORT_SIZE);
        Bytes mi = buf.copyOfRange(miAttrOffset + 4, miAttrOffset + 4 + MESSAGE_INTEGRITY_SIZE);

        //There are attributes after the message integrity
        if ((miAttrOffset + 4 + aSize) < temp.size()) {
            int extraOffset = buf.size() - miAttrOffset - 4 - aSize;
            int newAdjustedSize = buf.size() - extraOffset - HEADER_SIZE;

            temp.set16((short) newAdjustedSize, SHORT_SIZE);
        }

        byte[] hmac = computeHMAC(pwd.getBytes(), pwd.getBytes().length, temp.getBytes(), miAttrOffset);

        if (hmac.length != MESSAGE_INTEGRITY_SIZE) {
            return false;
        }

        return Arrays.equals(hmac, mi.getBytes());
    }

    public static boolean validateFingerprint(int fpAttrOffset, Bytes buf, int size) {
        long lKey = 0x5354554E;

        Bytes fpValue = buf.copyOfRange(buf.size() - INT_SIZE, buf.size());

        CRC32 crc = new CRC32();
        crc.reset();
        crc.update(buf.getBytes(), 0, buf.size() - 8);
        long v = crc.getValue() ^ lKey;
        Bytes test = new Bytes(4);
        test.set32((int) v, 0);

        return fpValue.equals(test);
    }

    public static byte[] computeHMAC(byte[] key, int keySize, byte[] input, int inputSize) {
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            int block_size = 64;

            byte[] newKey;

            if (keySize > block_size) {
                sha1Digest.update(key);
                newKey = sha1Digest.digest();
                newKey = Arrays.copyOf(newKey, block_size);
            }
            else {
                newKey = Arrays.copyOf(key, block_size);
            }

            byte[] o_pad = new byte[block_size];
            byte[] i_pad = new byte[block_size];

            for (int i = 0; i < block_size; i++) {
                o_pad[i] = (byte) (0x5c ^ newKey[i]);
                i_pad[i] = (byte) (0x36 ^ newKey[i]);
            }

            byte[] inner;
            sha1Digest.update(i_pad);
            sha1Digest.update(input, 0, inputSize);
            inner = sha1Digest.digest();

            sha1Digest.update(o_pad);
            sha1Digest.update(inner);
            return sha1Digest.digest();
        }
        catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(StunMessageTest.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
