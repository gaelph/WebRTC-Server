/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STUN;

import Utils.bytes.Bytes;
import java.util.Arrays;

/**
 *
 * @author gaelph
 */
public class StunAttribute {

    public static short MAPPED_ADDRESS = 0x0001;
    public static short USERNAME = 0x0006;
    public static short MESSAGE_INTEGRITY = 0x0008;
    public static short ERROR_CODE = 0x0009;
    public static short UNKNOWN = 0x000A;
    public static short REALM = 0x0014;
    public static short NONCE = 0x0015;
    public static short X_OR_MAPPED_ADDRES = 0x0020;

    public static short PRIORITY = 0x0024;
    public static short USE_CANDIDATE = 0x0025;

    public static short SOFTWARE = (short) 0x8022;
    public static short ALTERNATE_SERVER = (short) 0x8023;
    public static short FINGERPRINT = (short) 0x8028;
    public static short ICE_CONTROLLED = (short) 0x8029;
    public static short ICE_CONTROLLING = (short) 0x802A;

    private static int TYPE_INDEX = 0;
    private static int SIZE_INDEX = 2;

    private static int HEADER_SIZE = 4;

    public final short type;
    private Bytes content = null;

    public StunAttribute(short type, byte[] data) {
        this.type = type;

        this.setContent(new Bytes(data));
    }

    public StunAttribute(short type, Bytes data) {
        this.type = type;

        this.setContent(data);
    }

    public StunAttribute(short type, int data) {
        this.type = type;

        Bytes b = new Bytes(new byte[4]);

        b.set32(data, 0);

        this.setContent(b);
    }

    public StunAttribute(short type) {
        this.type = type;
    }

    public static StunAttribute parse(byte[] data) {
        Bytes b = new Bytes(data);
        short type = b.read16(TYPE_INDEX);
        short length = b.read16(SIZE_INDEX);
        byte[] content = Arrays.copyOfRange(data, HEADER_SIZE, length + HEADER_SIZE);

        return new StunAttribute(type, content);
    }

    public byte[] toBytes() {
        Bytes result = new Bytes(HEADER_SIZE);

        result.set16(this.type, TYPE_INDEX);

        if (this.content != null) {
            result.set16((short) this.content.size(), SIZE_INDEX);

            result.append(this.content);
        }

        return result.getBytes();
    }

    public final short getLength() {
        if (this.content != null) {
            int boundSize = this.content.size();

            if ((this.content.size() % 4) > 0) {
                boundSize = this.content.size() + (4 - this.content.size() % 4);
            }

            return (short) boundSize;
        }
        else {
            return (short) 0;
        }
    }

    public final void setContent(Bytes data) {
        if (data == null || data.size() == 0) {
            this.content = null;
            return;
        }

        this.content = data;
    }

    public final void setContent(String data) {
        this.setContent(new Bytes(data.getBytes()));
    }

    public final Bytes getContent() {
        if (this.content == null) {
            return new Bytes(new byte[1]);
        }
        return this.content;
    }
}
