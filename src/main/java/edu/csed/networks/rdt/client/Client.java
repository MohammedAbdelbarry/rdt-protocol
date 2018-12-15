package edu.csed.networks.rdt.client;

import edu.csed.networks.rdt.packet.DataPacket;
import edu.csed.networks.rdt.protocol.RDTSocket;
import edu.csed.networks.rdt.protocol.strategy.SelectiveRepeatStrategy;
import org.apache.commons.lang3.Conversion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Client {
    private DatagramSocket socket;
    private String fileName;
    private FileOutputStream fileStream;
    private static final String DOWNLOADS_FOLDER = "client-downloads";

    public Client(InetAddress address, int serverPort, int clientPort, String fileName) throws SocketException {
        socket = new DatagramSocket(clientPort);
        socket.connect(address, serverPort);
        this.fileName = fileName;
        try {
            new File(DOWNLOADS_FOLDER).mkdir();
            this.fileStream = new FileOutputStream(DOWNLOADS_FOLDER + File.separator + fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        requestFile();
        RDTSocket rdtSocket = new RDTSocket(socket, socket.getInetAddress(), socket.getPort(), new SelectiveRepeatStrategy());
        long len = 0;
        long bytesRead = 0;
        try {
            byte[] bytes = rdtSocket.receive();
            System.out.println(bytes.length);
            len = Conversion.byteArrayToLong(bytes, 0, 0, 0, bytes.length);
        } catch (IOException e) {
            return;
        }
        while (bytesRead < len) {
            try {
                byte[] bytes = rdtSocket.receive();
                bytesRead += bytes.length;
                System.out.println(String.format("Read(%d bytes)", bytes.length));

                if (bytesRead > 150000000) {
                    System.out.println("Read(" + bytesRead + ", " + len + ")");
                }


                fileStream.write(bytes, 0, bytes.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        rdtSocket.close();
        socket.close();
        try {
            fileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestFile() {
        DataPacket dataPacket = new DataPacket((short) fileName.length(), 0, fileName.getBytes(), socket.getInetAddress(), socket.getPort());
        byte[] bytes = dataPacket.getBytes();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
