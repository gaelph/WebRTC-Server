/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 *
 * @author gaelph
 */
public class Utils {

    public static String makeRandomString(int length) {
        byte[] randomBytes = new byte[length];

        new Random(new Date().getTime()).nextBytes(randomBytes);

        return Base64.getEncoder().encodeToString(randomBytes);
    }

    public static byte[] bytesFromLong(long in) {
        byte[] result = new byte[Long.SIZE];

        for (int i = 0; i < Long.SIZE; i++) {
            int shift = (Long.SIZE - 1 - i) * 8;
            result[i] = (byte) ((in >> shift) & 0xFF);
        }

        return result;
    }

    public static byte[] bytesFromLong32(long in) {
        byte[] result = new byte[4];

        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            result[i] = (byte) ((in >> shift) & 0xFF);
        }

        return result;
    }

    public static Enumeration<InetAddress> getLocalAddresses() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            NetworkInterface current = interfaces.nextElement();

            if (!current.isUp() || current.isLoopback() || current.isVirtual()) {
                continue;
            }

            Enumeration<InetAddress> addresses = current.getInetAddresses();
            return addresses;
        }

        throw new SocketException("No address found");
    }

    public static InetAddress getLocalIP4Address() throws SocketException {
        Enumeration<InetAddress> addresses = getLocalAddresses();

        while (addresses.hasMoreElements()) {
            InetAddress current_addr = addresses.nextElement();
            if (current_addr.isLoopbackAddress()) {
                continue;
            }

            if (current_addr instanceof Inet4Address) {
                return current_addr;
            }
        }

        throw new SocketException("No IP4 address found");
    }

    public static InetAddress getLocalIP6Address() throws SocketException {
        Enumeration<InetAddress> addresses = getLocalAddresses();

        while (addresses.hasMoreElements()) {
            InetAddress current_addr = addresses.nextElement();
            if (current_addr.isLoopbackAddress()) {
                continue;
            }

            if (current_addr instanceof Inet6Address) {
                return current_addr;
            }
        }

        throw new SocketException("No IP6 address found");
    }

    public static int randomIntWithin(int min, int max) {
        Random rand = new Random(Instant.now().toEpochMilli());

        return rand.nextInt((max - min) + 1) + min;
    }

    public static byte[] xorEnDecrypt(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }

        return result;
    }
}
