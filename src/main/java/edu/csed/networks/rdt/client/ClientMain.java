package edu.csed.networks.rdt.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        if (args.length < 2) {
            // TODO: report error of invalid number of arguments
        }
        InetAddress address = null;
        int serverPort = 0, clientPort = 0, recWindow = 0;
        String fileName = null;
        File file = new File(args[1]);
        try {
            Scanner scanner = new Scanner(file);
            try {
                address = InetAddress.getByName(scanner.next());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            serverPort = scanner.nextInt();
            clientPort = scanner.nextInt();
            fileName = scanner.next();
            recWindow = scanner.nextInt();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            Client client = new Client(address, serverPort, clientPort, fileName, recWindow);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}