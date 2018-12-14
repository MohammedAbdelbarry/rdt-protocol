package edu.csed.networks.rdt.packet;

import org.apache.commons.lang3.Conversion;

import java.net.InetAddress;

public class DataPacket extends Packet {
    public DataPacket(short length, int seqNo, byte[] data, InetAddress host, int port) {
        this.checksum = 0;
        this.length = length;
        this.seqNo = seqNo;
        this.data = data;
        byte[] bytes = this.getBytes();
        this.checksum = calculateCheckSum(bytes, 0, bytes.length);
        this.host = host;
        this.port = port;
    }

    public static DataPacket valueOf(byte[] bytes, InetAddress host, int port) {
        short oldChecksum = Conversion.byteArrayToShort(bytes, 0, (short) 0, 0, 2);
        short length = Conversion.byteArrayToShort(bytes, 2, (short) 0, 0, 2);
        int seqNo = Conversion.byteArrayToInt(bytes, 4, 0, 0, 4);
        byte[] data = new byte[length];
//        System.out.println(oldChecksum);
//        System.out.println(length);
//        System.out.println(seqNo);
//        System.out.println(calculateCheckSum(bytes, 2, bytes.length));
//        System.out.println(bytes.length - length);
        //        System.out.println(new String(bytes, 8, (int) length));
        System.arraycopy(bytes, 8, data, 0, length);
        if (oldChecksum != calculateCheckSum(bytes, 2, bytes.length)) {
            throw new IllegalArgumentException("Corrupted Packet");
        }
        return new DataPacket(length, seqNo, data, host, port);
    }
}
