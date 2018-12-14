package edu.csed.networks.rdt.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) throws FileNotFoundException, UnknownHostException, SocketException {
        if (args.length < 2) {
            throw new IllegalArgumentException("Invalid number of arguments passed to client.");
        }
        File file = new File(args[1]);
        Scanner scanner = new Scanner(file);
        InetAddress address = InetAddress.getByName(scanner.next());
        int serverPort = scanner.nextInt();
        int clientPort = scanner.nextInt();
        String fileName = scanner.next();
        int recWindow = scanner.nextInt();
        scanner.close();
        Client client = new Client(address, serverPort, clientPort, fileName);
        client.start();
    }
}