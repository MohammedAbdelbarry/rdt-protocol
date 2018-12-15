package edu.csed.networks.rdt.packet;

import org.apache.commons.lang3.Conversion;

import java.net.InetAddress;

public class AckPacket extends Packet {

    public static final int ACK_LEN = HEADERS_LENGTH;

    public AckPacket(long seqNo, InetAddress host, int port) {
        this.checksum = 0;
        this.length = 0;
        this.seqNo = seqNo;
        data = new byte[this.length];
        byte[] bytes = this.getBytes();
        this.host = host;
        this.port = port;
        this.checksum = calculateCheckSum(bytes, 0, bytes.length);
    }



    public static AckPacket valueOf(byte[] bytes, int packetLength, InetAddress host, int port) {
        if (packetLength != ACK_LEN) {
            throw new IllegalArgumentException(String.format("Expected 8 bytes but got %d", packetLength));
        }
        int ptr = 0;
        short oldChecksum = Conversion.byteArrayToShort(bytes, 0, (short) 0, 0, Short.BYTES);
        ptr += Short.BYTES;
        short length = Conversion.byteArrayToShort(bytes, ptr, (short) 0, 0, Short.BYTES);
        ptr += Short.BYTES;
        long seqNo = Conversion.byteArrayToLong(bytes, ptr, 0, 0, Long.BYTES);
        if (oldChecksum != calculateCheckSum(bytes, 2, ACK_LEN)) {
            throw new IllegalArgumentException("Corrupted Packet");
        }
        return new AckPacket(seqNo, host, port);
    }

    public static AckPacket valueOf(byte[] bytes, InetAddress host, int port) {
        return valueOf(bytes, bytes.length, host, port);
    }
}
