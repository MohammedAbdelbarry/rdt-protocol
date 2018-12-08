package edu.csed.networks.rdt.packet;

import org.apache.commons.lang3.Conversion;

import java.net.InetAddress;

public class AckPacket extends Packet {

    public static final int ACK_LEN = 8;

    public AckPacket(int seqNo, InetAddress host, int port) {
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
        if (packetLength != 8) {
            throw new IllegalArgumentException(String.format("Expected 8 bytes but got %d", packetLength));
        }
        short oldChecksum = Conversion.byteArrayToShort(bytes, 0, (short) 0, 0, 2);
        short length = Conversion.byteArrayToShort(bytes, 2, (short) 0, 0, 2);
        int seqNo = Conversion.byteArrayToInt(bytes, 4, 0, 0, 4);
        if (oldChecksum + seqNo + length != 0) {
            throw new IllegalArgumentException("Corrupted Packet");
        }
        return new AckPacket(seqNo, host, port);
    }

    public static AckPacket valueOf(byte[] bytes, InetAddress host, int port) {
        return valueOf(bytes, bytes.length, host, port);
    }
}
