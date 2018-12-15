package edu.csed.networks.rdt.packet;

import edu.csed.networks.rdt.packet.exceptions.PacketCorruptedException;
import org.apache.commons.lang3.Conversion;

import java.net.InetAddress;

public class DataPacket extends Packet {
    public DataPacket(Short length, long seqNo, byte[] data, InetAddress host, int port) {
        this.checksum = 0;
        this.length = length;
        this.seqNo = seqNo;
        this.data = data;
        byte[] bytes = this.getBytes();
        this.checksum = calculateCheckSum(bytes, 0, bytes.length);
        this.host = host;
        this.port = port;
    }

    public static DataPacket valueOf(byte[] bytes, InetAddress host, int port) throws PacketCorruptedException {
        int ptr = 0;
        short oldChecksum = Conversion.byteArrayToShort(bytes, 0, (short) 0, 0, Short.BYTES);
        ptr += Short.BYTES;
        short length = Conversion.byteArrayToShort(bytes, ptr, (short) 0, 0, Short.BYTES);
        ptr += Short.BYTES;
        long seqNo = Conversion.byteArrayToLong(bytes, ptr, 0, 0, Long.BYTES);
        ptr += Long.BYTES;
        byte[] data = new byte[length];
//        System.out.println(oldChecksum);
//        System.out.println(length);
//        System.out.println(seqNo);
//        System.out.println(calculateCheckSum(bytes, 2, bytes.length));
//        System.out.println(bytes.length - length);
        //        System.out.println(new String(bytes, 8, (int) length));
        System.arraycopy(bytes, ptr, data, 0, length);
        if (oldChecksum != calculateCheckSum(bytes, Short.BYTES, bytes.length)) {
            throw new PacketCorruptedException("Corrupted Packet");
        }
        return new DataPacket(length, seqNo, data, host, port);
    }
}
