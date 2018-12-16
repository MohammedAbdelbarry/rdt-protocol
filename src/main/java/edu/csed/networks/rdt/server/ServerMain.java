package edu.csed.networks.rdt.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Scanner;

public class ServerMain {

    public static void main(String[] args) throws FileNotFoundException, SocketException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Invalid number of arguments passed to server.");
        }
        File file = new File(args[0]);
        Scanner scanner = new Scanner(file);
        int serverPort = scanner.nextInt();
        int maxCwnd = scanner.nextInt();
        int seed = scanner.nextInt();
        double plp = scanner.nextDouble();
        double pcp = scanner.nextDouble();
        scanner.close();
        ListenerServer listener = new ListenerServer(new DatagramSocket(serverPort), maxCwnd, seed, plp, pcp);
        listener.run();
    }
}
