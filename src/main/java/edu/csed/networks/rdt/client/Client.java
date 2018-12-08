package edu.csed.networks.rdt.client;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Client {
    private DatagramSocket socket;

    public Client(InetAddress address, int serverPort, int clientPort, String fileName, int recWindow) throws SocketException {
        socket = new DatagramSocket(clientPort);
        socket.connect(address, serverPort);
    }

    public static void main(String[] args) {

    }
}
