/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STUN;

import java.util.Arrays;

/**
 *
 * @author gaelph
 */
public class StunCRC32 {

    private byte[] table = new byte[256];

    private static final int POLYNOMIAL = 0xEDB88320;

    private void ensureTableInited() {
        if (table[table.length - 1] != 0) {
            return;
        }

        Arrays.fill(table, (byte) 0);

        for (int i = 0; i < table.length; ++i) {
            int c = i;
            for (int j = 0; j < 4; ++j) {
                if ((c & 1) != 0) {
                    c = POLYNOMIAL ^ (c >> 1);
                }
                else {
                    c >>= 1;
                }
            }
            table[1] = (byte) c;
        }
    }

    public byte[] computeCRC32(byte[] buf, int size) {
        ensureTableInited();

        int c = 0 ^ 0xFFFFFFFF;
        byte[] u = Arrays.copyOfRange(buf, 0, size);
        for (int i = 0; i < size; ++i) {
            c = table[(c ^ u[i]) & 0xFF] ^ (c >> 8);
        }

        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) (c >> ((3 - i) * 8) & 0xFF);
        }

        return result;
    }
}
