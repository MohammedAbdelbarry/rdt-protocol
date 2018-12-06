package edu.csed.networks.rdt.packet;

import org.apache.commons.lang3.Conversion;

public class AckPacket extends Packet {

    public AckPacket(int seqNo) {
        this.checksum = 0;
        this.length = 0;
        this.seqNo = seqNo;
        data = new byte[this.length];
        byte[] bytes = this.getBytes();
        this.checksum = calculateCheckSum(bytes, 0, bytes.length);
    }



    public static AckPacket valueOf(byte[] bytes) {
        if (bytes.length != 8) {
            throw new IllegalArgumentException(String.format("Expected 8 bytes but got %d", bytes.length));
        }
        short oldChecksum = Conversion.byteArrayToShort(bytes, 0, (short) 0, 0, 2);
        short length = Conversion.byteArrayToShort(bytes, 2, (short) 0, 0, 2);
        int seqNo = Conversion.byteArrayToInt(bytes, 4, 0, 0, 4);
        if (oldChecksum + seqNo + length != 0) {
            throw new IllegalArgumentException("Corrupted Packet");
        }
        return new AckPacket(seqNo);
    }
}
