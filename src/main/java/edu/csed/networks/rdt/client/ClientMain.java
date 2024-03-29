package edu.csed.networks.rdt.client;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) throws FileNotFoundException, UnknownHostException, SocketException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Invalid number of arguments passed to client.");
        }

        try {
            URL logFileStream = ClientMain.class.getResource("log4j2-client.properties");
            Configuration conf = PropertiesConfigurationFactory.getInstance().getConfiguration(null,
                    "client-conf", logFileStream.toURI());
            Configurator.initialize(conf);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        File file = new File(args[0]);
        Scanner scanner = new Scanner(file);
        InetAddress address = InetAddress.getByName(scanner.next());
        int serverPort = scanner.nextInt();
        int clientPort = scanner.nextInt();
        String fileName = scanner.next();
        int recWindow = scanner.nextInt();
        scanner.close();
        Client client = new Client(address, serverPort, clientPort, fileName, recWindow);
        client.start();
    }
}