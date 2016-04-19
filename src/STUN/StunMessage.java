/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STUN;

import static Utils.Digests.Digests.SHA1;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import webrtc.server.Utils;

/**
 *
 * @author gaelph
 */
public class StunMessage {

    public static final short STUN_BINDING_REQUEST = 0x0001;
    public static final short STUN_BINDING_INDICATION = 0x0011;
    public static final short STUN_BINDING_RESPONSE = 0x0101;
    public static final short STUN_BINDING_ERROR_RESPONSE = 0x0111;

    public static final int MAGIC_COOKIE = 0x2112A442;

    private byte[] transactionID = new byte[12];

    public final short type;

    private final List<StunAttribute> attributes = new ArrayList<>();

    public StunMessage(short type) {
        this.type = type;

        this.newTransactionID();
    }

    public final void newTransactionID() {
        Random rand = new Random(Instant.now().toEpochMilli());
        rand.nextBytes(transactionID);

        this.setTransactionID(transactionID);
    }

    public final void setTransactionID(byte[] tid) {
        this.transactionID = tid;
    }

    public final byte[] getTransactionID() {
        return this.transactionID;
    }

    public final void addAttribute(StunAttribute attr) {
        attributes.add(attr);
    }

    public final void put(short attrType, String attrValue) {
        attributes.add(new StunAttribute(attrType, attrValue.getBytes()));
    }

    public final void put(short attrType, byte[] attrValue) {
        attributes.add(new StunAttribute(attrType, attrValue));
    }

    public final byte[] getBytes(short attrType) {

        byte[] retVal = null;

        for (StunAttribute attr : this.attributes) {
            if (attr.type == attrType) {
                retVal = attr.getContent().getBytes();
                break;
            }
        }

        return retVal;
    }

    public final String getString(short attrType) {
        return new String(getBytes(attrType));
    }

    public static StunMessage parse(byte[] data) {

        short type = (short) (data[1] | (short) ((data[0] << 8)));
        short length = (short) (data[3] | (short) ((data[2] << 8)));

        StunMessage message = new StunMessage(type);

        int mcookie = 0;
        for (int i = 4; i < 8; i++) {
            int shift = (7 - i) * 8;
            mcookie |= (int) ((data[i] << shift) & (0xFF << shift));
        }

        if (mcookie != MAGIC_COOKIE) {
            return null;
        }

        byte[] transactionID = new byte[12];
        System.arraycopy(data, 8, transactionID, 0, transactionID.length);

        message.setTransactionID(transactionID);

        int offset = (8 + 12);

        while (offset < length + 8) {
            byte[] attrs = Arrays.copyOfRange(data, offset, length + 8);
            StunAttribute A = StunAttribute.parse(attrs);
            message.addAttribute(A);

            offset += A.getLength() + 4;
        }

        return message;
    }

    public int size() {
        return attributes.stream()
                .map(attr -> (int) attr.toBytes().length)
                .reduce(0, (a, b) -> {
                    return a + b + 4;
                });
    }

    private byte[] prepBytes() {

        byte[] result = new byte[20];
        short t = (short) ((this.type & 0x3FFF));

        result[1] = (byte) (t & 0xFF);
        result[0] = (byte) ((t & 0xFF00) >> 8);

        for (int i = 4; i < 8; i++) {
            int shift = (7 - i) * 8;
            result[i] = (byte) ((MAGIC_COOKIE >> shift) & 0xFF);
        }

        System.arraycopy(this.transactionID, 0, result, 8, this.transactionID.length);

        byte[] attrBytes = null;
        for (StunAttribute attr : this.attributes) {
            if (attrBytes == null) {
                if (attr instanceof StunXORAddress) {
                    attrBytes = ((StunXORAddress) attr).toBytes();
                }
                else {
                    attrBytes = attr.toBytes();
                }
            }
            else {
                byte[] a;
                if (attr instanceof StunXORAddress) {
                    a = ((StunXORAddress) attr).toBytes();
                }
                else {
                    a = attr.toBytes();
                }
                attrBytes = Arrays.copyOf(attrBytes, attrBytes.length + a.length);
                System.arraycopy(a, 0, attrBytes, attrBytes.length - a.length, a.length);
            }
        }

        if (attrBytes != null) {
            result = Arrays.copyOf(result, result.length + attrBytes.length);
            System.arraycopy(attrBytes, 0, result, result.length - attrBytes.length, attrBytes.length);
        }

        result[2] = (byte) (((result.length - 20) >> 8) & 0xFF);
        result[3] = (byte) ((result.length - 20) & 0xFF);

        return result;
    }

    public byte[] toBytes() {
        byte[] message = prepBytes();

        short size = (short) (message.length - 20 + 24);

        message[2] = (byte) ((size >> 8) & 0xFF);
        message[3] = (byte) (size & 0xFF);

        Logger.getLogger(StunMessage.class.getName()).log(Level.INFO, "Size for Message Integrity : {0}", size);

        byte[] messageIntegrity = Arrays.copyOf(SHA1(message), 20);
        messageIntegrity = new StunAttribute(StunAttribute.MESSAGE_INTEGRITY, messageIntegrity).toBytes();

        message = Arrays.copyOf(message, message.length + messageIntegrity.length);
        System.arraycopy(messageIntegrity, 0, message, message.length - messageIntegrity.length, messageIntegrity.length);
        message = appendFingerprint(message);

        return message;
    }

    public byte[] toBytes(String pwd) {
        byte[] message = prepBytes();

        short size = (short) (message.length - 20 + 24);

        message[2] = (byte) ((size >> 8) & 0xFF);
        message[3] = (byte) (size & 0xFF);

        byte[] messageIntegrity = Arrays.copyOf(
                StunMessageTest.computeHMAC(pwd.getBytes(),
                                            pwd.getBytes().length,
                                            message,
                                            message.length), 20);
        messageIntegrity = new StunAttribute(StunAttribute.MESSAGE_INTEGRITY, messageIntegrity).toBytes();

        message = Arrays.copyOf(message, message.length + messageIntegrity.length);
        System.arraycopy(messageIntegrity, 0, message, message.length - messageIntegrity.length, messageIntegrity.length);
        message = appendFingerprint(message);

        return message;
    }

    private byte[] appendFingerprint(byte[] data) {
        long lKey = 0x5354554e;

        data = Arrays.copyOf(data, data.length + 8);
        int fpAttrOffset = data.length - 8;

        data[2] = (byte) (((data.length - 20) >> 8) & 0xFF);
        data[3] = (byte) ((data.length - 20) & 0xFF);

        data[fpAttrOffset] = (byte) ((StunAttribute.FINGERPRINT >> 8) & 0xFF);
        data[fpAttrOffset + 1] = (byte) (StunAttribute.FINGERPRINT & 0xFF);
        data[fpAttrOffset + 2] = (byte) 0;
        data[fpAttrOffset + 3] = (byte) 4;

        Arrays.fill(data, data.length - 4, data.length - 1, (byte) 0x00);

        CRC32 crc = new CRC32();
        crc.reset();
        crc.update(data, 0, data.length - 8);
        long v = crc.getValue() ^ lKey;
        byte[] fingerprint = new byte[4];
        for (int i = 0; i < 4; i++) {
            int shift = (3 - i) * 8;
            fingerprint[i] = (byte) ((v >> shift) & 0xFF);
        }

        System.arraycopy(fingerprint, 0, data, data.length - fingerprint.length, fingerprint.length);

        int boundSize = data.length;

        if ((data.length % 4) > 0) {
            boundSize = data.length + (4 - data.length % 4);
        }

        data = Arrays.copyOf(data, boundSize);

        return data;
    }
}
