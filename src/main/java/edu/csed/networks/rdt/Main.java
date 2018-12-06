package edu.csed.networks.rdt;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Main {
    private static final int PORT = 8081;
    private static DatagramSocket clientSocket;
    private static DatagramSocket serverSocket;

    static {
        try {
            clientSocket = new DatagramSocket();
            serverSocket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private static void send(String msg) throws IOException {
        InetAddress address = InetAddress.getByName("localhost");
        byte[] bytes = msg.getBytes();
        DatagramPacket sentPacket = new DatagramPacket(bytes, bytes.length, address, PORT);
        clientSocket.send(sentPacket);
    }

    public static void main(String[] args) throws IOException {
        byte[] buffer = new byte[4096];
        new Thread(() -> {
            try {
                send("Hello, World!");
                byte[] clientBuf = new byte[4096];
                DatagramPacket packet = new DatagramPacket(clientBuf, clientBuf.length);
                clientSocket.receive(packet);
                String received = new String(
                        packet.getData(), 0, packet.getLength());
                System.out.println(received);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        serverSocket.receive(receivedPacket);
        InetAddress clientAddr = receivedPacket.getAddress();
        int clientPort = receivedPacket.getPort();
        System.out.println(new String(receivedPacket.getData(), 0, receivedPacket.getLength()));
        String confirmation = "CONFIRMED!";
        byte[] bytes = confirmation.getBytes();
        DatagramPacket confirmationPacket = new DatagramPacket(bytes, bytes.length, clientAddr, clientPort);
        serverSocket.send(confirmationPacket);
    }
}
