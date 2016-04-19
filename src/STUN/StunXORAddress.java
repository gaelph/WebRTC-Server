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
public class StunXORAddress extends StunAttribute {

    protected int address;
    protected int port;

    public static final byte IP4_FAMILY = 1;
    public static final byte IP6_FAMILY = 2;

    public static short MAGIC_COOKIE_MSB = 0x2112;

    private static final int HEADER_LENGTH = 4;
    private static final int FAMILY_BYTE = 1;
    private static final int PORT_OFFSET = 2;
    private static final int ADDRESS_OFFSET = 4;

    private static final int PORT_LENGTH = 2;
    private static final int IP4_LENGTH = 4;
    private static final int IP6_LENGTH = 16;

    protected StunXORAddress(short type, byte[] data) {
        super(type, data);
    }

    public StunXORAddress(String ip, int port) {
        super(StunAttribute.X_OR_MAPPED_ADDRES);

        this.port = port;
        this.address = ipStringToInt(ip);
    }

    public StunXORAddress(StunAttribute attr) {
        super(attr.type, attr.getContent());
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return this.port;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getAddress() {
        return this.address;
    }

    public static StunXORAddress parse(byte[] data) {
        StunXORAddress result = new StunXORAddress(StunAttribute.parse(data));
        Bytes d = result.getContent();

        int family = data[FAMILY_BYTE];

        if (family == IP4_FAMILY) {
            short port = d.read16(PORT_OFFSET);

            int address = d.read32(ADDRESS_OFFSET);

            result.setPort(port);
            result.setAddress(address);
        }

        return result;
    }

    @Override
    public byte[] toBytes() {
        short xoredPort = (short) ((((short) this.port) & 0xFFFF) ^ MAGIC_COOKIE_MSB);
        int xoredAddress = this.address ^ StunMessage.MAGIC_COOKIE;

        Bytes content = new Bytes(new byte[2 + PORT_LENGTH + IP4_LENGTH]);

        content.set(IP4_FAMILY, FAMILY_BYTE);
        content.set16(xoredPort, PORT_OFFSET);
        content.setX(xoredAddress, ADDRESS_OFFSET, IP4_LENGTH);

        this.setContent(content);

        return super.toBytes();
    }

    private static int ipStringToInt(String ip) {
        String[] ipFrags = ip.trim().split("\\.");
        if (ipFrags.length != 4) {
            return 0;
        }

        int result = 0;

        for (int i = 0; i < ipFrags.length; i++) {
            byte bFrag = (byte) Integer.parseInt(ipFrags[i]);
            int shift = ((ipFrags.length - 1) - i) * 8;

            result |= (bFrag & 0xFF) << shift;
        }

        return result;
    }

}
