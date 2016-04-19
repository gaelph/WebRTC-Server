/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Utils.bytes;

import java.util.Iterator;
import org.bouncycastle.util.Arrays;

/**
 *
 * @author gaelph
 */
public class Bytes implements Iterable {

    private byte[] bytes;

    public Bytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public Bytes(int size) {
        this.bytes = new byte[size];
    }

    public int size() {
        return bytes.length;
    }

    public byte byteAtIndex(int index) {
        return this.bytes[index];
    }

    public int indexOf(byte b) {

        for (int i = 0; i < this.bytes.length; i++) {
            if (this.bytes[i] == b) {
                return i;
            }
        }

        return -1;
    }

    public Bytes copy() {
        return new Bytes(Arrays.copyOf(bytes, bytes.length));
    }

    public Bytes copyOfRange(int from, int to) {
        return new Bytes(Arrays.copyOfRange(bytes, from, to));
    }

    public void append(byte[] data) {
        this.bytes = Arrays.concatenate(this.bytes, data);
    }

    public void append(Bytes data) {
        byte[] d = data.bytes;
        this.append(d);
    }

    public void prepend(byte[] data) {
        this.bytes = Arrays.concatenate(data, this.bytes);
    }

    public void prepend(Bytes data) {
        byte[] d = data.bytes;
        this.prepend(d);
    }

    public void splice(int from, int to) {
        byte[] first = Arrays.copyOf(bytes, from);
        byte[] second = Arrays.copyOfRange(bytes, to, this.bytes.length);

        this.bytes = Arrays.concatenate(first, second);
    }

    public void trim(int length) {
        this.bytes = Arrays.copyOf(bytes, length);
    }

    public byte read(int offset) {
        return this.byteAtIndex(offset);
    }

    public short read16(int offset) {
        return (short) readX(offset, 2);
    }

    public int read32(int offset) {
        return (int) readX(offset, 4);
    }

    public long read64(int offset) {
        return readX(offset, 8);
    }

    public long readX(int offset, int num) {
        long res = 0;
        for (int i = 0; i < num; i++) {
            int shift = ((num - 1) - i) * 8;
            res |= (this.bytes[offset + i] & 0xFF) << shift;
        }

        return res;
    }

    public void set(byte val, int offset) {
        this.bytes[offset] = val;
    }

    public void set16(short val, int offset) {
        setX((short) val, offset, 2);
    }

    public void set32(int val, int offset) {
        setX((int) val, offset, 4);
    }

    public void set64(long val, int offset) {
        setX(val, offset, 8);
    }

    public void setX(long val, int offset, int num) {
        for (int i = 0; i < num; i++) {
            int shift = ((num - 1) - i) * 8;
            this.bytes[offset + i] = (byte) ((val >> shift) & 0xFF);
        }
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    @Override
    public boolean equals(Object b) {
        if (b instanceof Bytes) {
            return Arrays.areEqual(bytes, ((Bytes) b).getBytes());
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public Iterator iterator() {
        return new BytesIterator(this);
    }

    private class BytesIterator implements Iterator {

        private final byte[] bytes;
        private int current;

        public BytesIterator(Bytes b) {
            this.bytes = b.bytes;
            current = 0;

        }

        @Override
        public boolean hasNext() {
            return current < this.bytes.length - 1;
        }

        @Override
        public Object next() {
            byte res = this.bytes[current];

            current++;

            return res;
        }

    }
}
