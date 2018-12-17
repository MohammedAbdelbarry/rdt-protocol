package edu.csed.networks.rdt.server;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

public class ServerMain {

    public static void main(String[] args) throws FileNotFoundException, SocketException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Invalid number of arguments passed to server.");
        }

        try {
            URL logFileStream = ServerMain.class.getResource("log4j2-server.properties");
            Configuration conf = PropertiesConfigurationFactory.getInstance().getConfiguration(null,
                    "server-conf", logFileStream.toURI());
            Configurator.initialize(conf);
        } catch (URISyntaxException e) {
            e.printStackTrace();
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
