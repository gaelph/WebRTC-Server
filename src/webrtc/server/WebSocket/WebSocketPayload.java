/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webrtc.server.WebSocket;

import java.io.UnsupportedEncodingException;
import java.util.logging.*;
import java.util.Random;

/**
 *
 * @author gaelph
 */
public class WebSocketPayload {

    public boolean isFinal;

    public static final byte CONTINUATION_OPCODE = 0x0;
    public static final byte TEXT_OPCODE = 0x1;
    public static final byte BINARY_OPCODE = 0x2;
    public static final byte CONNECTION_CLOSED_OPCODE = 0x8;
    public static final byte PING_OPCODE = 0x9;
    public static final byte PONG_OPCODE = 0xA;

    public boolean isMasked;

    public byte opcode;

    public long length = 0;

    public byte[] maskKey = new byte[4];
    public byte[] content;

    private static final int SMALL_SIZE = 125;
    private static final int MEDIUM_SIZE = 126;
    private static final int MEDIUM_SIZE_MAX = 0xFFFF;
    private static final int LARGE_SIZE = 127;

    private static final int MASK_OFFSET_SMALL = 2;
    private static final int MASK_OFFSET_MEDIUM = 4;
    private static final int MASK_OFFSET_LARGE = 10;

    private static final int PAYLOAD_OFFSET_SMALL = 6;
    private static final int PAYLOAD_OFFSET_MEDIUM = 10;
    private static final int PAYLOAD_OFFSET_LARGE = 14;

    private static final int FINAL_BIT_MASK = 0x80;
    private static final int FINAL_BIT_OFFSET = 7;

    private static final int OPCODE_MASK = 0xF;

    private static final int MASK_BIT_MASK = 0x80;
    private static final int MASK_BIT_OFFSET = 7;

    private static final int SMALL_SIZE_MASK = 0x7F;

    private static final int FINAL_BYTE = 0;
    private static final int OPCODE_BYTE = 0;
    private static final int MASK_BIT_BYTE = 1;
    private static final int SMALL_SIZE_BYTE = 1;
    private static final int MEDIUM_SIZE_BYTE = 2;
    private static final int LARGE_SIZE_BYTE = 4;

    private static final int MASK_KEY_SIZE = 4;

    public WebSocketPayload() {
        isFinal = true;
        isMasked = false;
        opcode = TEXT_OPCODE;
    }

    public WebSocketPayload(String message) {
        isFinal = true;
        isMasked = false;
        opcode = TEXT_OPCODE;
        try {
            this.setContent(message.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WebSocketPayload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static byte[] xorEnDecrypt(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }

        return result;
    }

    public byte[] toBytes() {
        int size = 0;
        int maskKeyOffset;
        int payloadOffset;

        if (length < SMALL_SIZE) {
            maskKeyOffset = MASK_OFFSET_SMALL;
            payloadOffset = (isMasked ? MASK_OFFSET_SMALL + MASK_KEY_SIZE : MASK_OFFSET_SMALL);

        }
        else if (size < MEDIUM_SIZE_MAX) {
            maskKeyOffset = MASK_OFFSET_MEDIUM;
            payloadOffset = (isMasked ? MASK_OFFSET_SMALL + MASK_KEY_SIZE + 2 : MASK_OFFSET_SMALL);

        }
        else {
            maskKeyOffset = MASK_OFFSET_LARGE;
            payloadOffset = (isMasked ? MASK_OFFSET_SMALL + MASK_KEY_SIZE : MASK_OFFSET_SMALL);
        }

        size = (int) length + maskKeyOffset;

        byte[] result = new byte[size];

        if (isFinal) {
            result[FINAL_BYTE] |= FINAL_BIT_MASK;
        }

        result[OPCODE_BYTE] |= opcode;

        if (isMasked) {
            result[MASK_BIT_BYTE] |= MASK_BIT_MASK;
            Random rand = new Random();
            rand.nextBytes(maskKey);
            for (int i = maskKeyOffset; i < maskKeyOffset + MASK_KEY_SIZE; i++) {
                result[i] = maskKey[i - maskKeyOffset];
            }
        }

        if (length < SMALL_SIZE) {
            result[SMALL_SIZE_BYTE] |= (byte) length & SMALL_SIZE_MASK;
        }
        else if (length < MEDIUM_SIZE_MAX) {
            result[SMALL_SIZE_BYTE] |= (byte) MEDIUM_SIZE;
            result[MEDIUM_SIZE_BYTE] = (byte) ((length & 0xFF00) >> 8);
            result[MEDIUM_SIZE_BYTE + 1] = (byte) ((length & 0xFF));
        }
        else {
            result[SMALL_SIZE_BYTE] |= (byte) LARGE_SIZE;
            for (int i = 0; i < 8; i++) {
                int index = i + LARGE_SIZE_BYTE;
                int offset = (8 - i) * 8;
                result[index] = (byte) ((length & (0xFF << offset)) >> offset);
            }
        }

        byte[] encryptedContent = xorEnDecrypt(content, maskKey);

        System.arraycopy(encryptedContent, 0, result, maskKeyOffset, (int) length);

        return result;
    }

    public final void setContent(byte[] content) {
        length = content.length;
        this.content = content;
    }

    public static int getPacketSize(byte[] data) {
        int maybeLength = data[SMALL_SIZE_BYTE] & SMALL_SIZE_MASK;
        int size = 0;
        int payloadOffset = 0;
        if (maybeLength <= SMALL_SIZE) {
            size = maybeLength;
        }
        else if (maybeLength == MEDIUM_SIZE) {
            size = data[MEDIUM_SIZE_BYTE] << 8;
            size |= (data[MEDIUM_SIZE_BYTE + 1] & 0xFF);
            size &= MEDIUM_SIZE_MAX;
        }
        else if (maybeLength == LARGE_SIZE) {
            for (int i = 0; i < 8; i++) {
                size |= data[LARGE_SIZE_BYTE + i] << (8 * (8 - i));
            }
        }

        return size;
    }

    public static int getOffsetForSize(int size) {
        int offset;

        if (size <= SMALL_SIZE) {
            offset = MASK_OFFSET_SMALL;
        }
        else if (size <= MEDIUM_SIZE_MAX) {
            offset = MASK_OFFSET_MEDIUM;
        }
        else {
            offset = MASK_OFFSET_LARGE;
        }

        return offset + MASK_KEY_SIZE;
    }

    public static WebSocketPayload withRawBytes(byte[] data) {
        WebSocketPayload result = new WebSocketPayload();
        result.length = getPacketSize(data);
        int payloadOffset = getOffsetForSize((int) result.length);

        //opcode = (byte) (data[0] >> 4 & 0x07);
        //isFinal = ((data[0] & 0x01) == 1);
        result.opcode = (byte) (data[OPCODE_BYTE] & OPCODE_MASK);
        result.isFinal = (byte) (data[FINAL_BYTE] & FINAL_BIT_MASK >> FINAL_BIT_OFFSET) == 1;

        result.isMasked = (((data[MASK_BIT_BYTE] & MASK_BIT_MASK) >> MASK_BIT_OFFSET) == 1);

        System.arraycopy(data, payloadOffset - MASK_KEY_SIZE, result.maskKey, 0, MASK_KEY_SIZE);

        byte[] encryptedContent = new byte[(int) result.length];

        if (data.length - payloadOffset != result.length) {
            System.out.println("Size mismatch : reported = " + result.length + " actual = " + (data.length - payloadOffset));
        }
        System.arraycopy(data, payloadOffset, encryptedContent, 0, (int) result.length);
        result.content = xorEnDecrypt(encryptedContent, result.maskKey);

        return result;
    }
}
