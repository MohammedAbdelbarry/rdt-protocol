package edu.csed.networks.rdt.packet;

import org.apache.commons.lang3.Conversion;

public class DataPacket extends Packet {
    public DataPacket(short length, int seqNo, byte[] data) {
        this.checksum = 0;
        this.length = length;
        this.seqNo = seqNo;
        this.data = data;
        byte[] bytes = this.getBytes();
        this.checksum = calculateCheckSum(bytes, 0, bytes.length);
    }

    public static DataPacket valueOf(byte[] bytes) {
        short oldChecksum = Conversion.byteArrayToShort(bytes, 0, (short) 0, 0, 2);
        short length = Conversion.byteArrayToShort(bytes, 2, (short) 0, 0, 2);
        int seqNo = Conversion.byteArrayToInt(bytes, 4, 0, 0, 4);
        byte[] data = new byte[length];
        System.arraycopy(bytes, 8, data, 0, length);
        if (oldChecksum + seqNo + length + calculateCheckSum(data, 0, length) != 0) {
            throw new IllegalArgumentException("Corrupted Packet");
        }
        return new DataPacket(length, seqNo, data);
    }
}
