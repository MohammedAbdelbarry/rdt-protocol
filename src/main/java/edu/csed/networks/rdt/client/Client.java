package edu.csed.networks.rdt.client;

import edu.csed.networks.rdt.packet.DataPacket;
import edu.csed.networks.rdt.protocol.RDTSocket;
import edu.csed.networks.rdt.protocol.strategy.StopAndWaitStrategy;
import org.apache.commons.lang3.Conversion;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {
    private DatagramSocket socket;
    private String fileName;
    private FileOutputStream fileStream;

    public Client(InetAddress address, int serverPort, int clientPort, String fileName) throws SocketException {
        socket = new DatagramSocket(clientPort);
        socket.connect(address, serverPort);
        this.fileName = fileName;
        try {
            this.fileStream = new FileOutputStream(fileName + "2");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        requestFile();
        RDTSocket rdtSocket = new RDTSocket(socket, socket.getInetAddress(), socket.getPort(), new StopAndWaitStrategy());
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

    public static void main(String[] args) throws UnknownHostException, SocketException {
        InetAddress address = InetAddress.getByName("localhost");
        int serverPort = 8081;
        int clientPort = 8082;
        String fileName = "0d1b53eee747122bcb65744248ff4afc8920.png";
        new Client(address, serverPort, clientPort, fileName).start();
    }
}
