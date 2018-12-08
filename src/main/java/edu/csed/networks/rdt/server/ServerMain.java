package edu.csed.networks.rdt.server;

import java.net.DatagramSocket;
import java.net.SocketException;

public class ServerMain {

    public static void main(String[] args) {
        int port = 8081;
        try {
            ListenerServer listener = new ListenerServer(new DatagramSocket(port));
            listener.run();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
