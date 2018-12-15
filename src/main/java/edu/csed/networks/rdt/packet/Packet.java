package edu.csed.networks.rdt.packet;

import org.apache.commons.lang3.Conversion;

import java.net.InetAddress;

public abstract class Packet {
    protected short checksum;
    protected short length;
    protected long seqNo;
    protected byte[] data;

    public static final int HEADERS_LENGTH = Short.BYTES + Short.BYTES + Long.BYTES;

    protected InetAddress host;

    protected int port;

    public InetAddress getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public short getChecksum() {
        return checksum;
    }

    public short getLength() {
        return length;
    }

    public long getSeqNo() {
        return seqNo;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getBytes() {
        byte[] bytes = new byte[HEADERS_LENGTH + length];
        int ptr = 0;
        Conversion.shortToByteArray(checksum, 0, bytes, 0, Short.BYTES);
        ptr += Short.BYTES;
        Conversion.shortToByteArray(length, 0, bytes, ptr, Short.BYTES);
        ptr += Short.BYTES;
        Conversion.longToByteArray(seqNo, 0, bytes, ptr, Long.BYTES);
        ptr += Long.BYTES;
        System.arraycopy(data, 0, bytes, ptr, length);
        return bytes;
    }

    public static short calculateCheckSum(byte[] data, int start, int end) {
        int sum = 0;
        int second;
        for(int i = start; i < end; i += 2) {
            second = i + 1 >= end ? 0 : data[i + 1];
            int num = ((((int)data[i]) & 0xff) << Byte.SIZE) + (second & 0xff);
            sum += num;
            if (sum >= (1 << 16))
                sum += 1;
            sum &= 0xFFFF;
        }
        sum = ~sum;
        sum &= 0xFFFF;
        return (short) sum;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "length=" + length +
                ", seqNo=" + seqNo +
                ", host=" + host +
                ", port=" + port +
                '}';
    }
}
