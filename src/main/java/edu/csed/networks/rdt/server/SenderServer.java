package edu.csed.networks.rdt.server;

import edu.csed.networks.rdt.observer.ServerObservable;
import edu.csed.networks.rdt.protocol.RDTSocket;
import edu.csed.networks.rdt.protocol.strategy.SelectiveRepeatStrategy;
import org.apache.commons.lang3.Conversion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;

public class SenderServer implements Runnable {
    private RDTSocket socket;
    private FileInputStream fileStream;
    private Thread workThread;
    private long fileSize;
    private ServerObservable observable;
    private static final Logger LOGGER = LogManager.getLogger(SenderServer.class);


    private static final int CHUNK_SIZE = 4096 * 64;

    public SenderServer(DatagramSocket socket, InetAddress address, int port, String filePath,
                                int maxCwnd, int seed, double plp, double pcp, ServerObservable observable) {
        LOGGER.debug(String.format("Sender(%s, %d) Started", address, port));
        this.socket = new RDTSocket(socket, address, port, new SelectiveRepeatStrategy(maxCwnd), 1, seed, plp, pcp);
        this.observable = observable;
        observable.addListener(this.socket);
        try {
            LOGGER.debug(String.format("File Path: %s", filePath));
            fileSize = Files.size(new File(filePath).toPath());
            byte[] sizeBytes = new byte[Long.BYTES];
            Conversion.longToByteArray(fileSize, 0, sizeBytes, 0, sizeBytes.length);
            this.socket.send(sizeBytes, 0, sizeBytes.length);
            this.fileStream = new FileInputStream(filePath);
            workThread = new Thread(this);
            workThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void send() throws IOException {
        byte[] bytes = new byte[CHUNK_SIZE];
        int len = fileStream.read(bytes, 0, CHUNK_SIZE);
        socket.send(bytes, 0, len);
    }

    @Override
    public void run() {

        int i = 0;
        while (i < fileSize) {
            try {
                send();
            } catch (IOException e) {
                e.printStackTrace();
            }
            i += CHUNK_SIZE;
        }
        try {
            fileStream.close();
            socket.close();
            observable.removeListener(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        workThread.stop();
        socket.close();
        observable.removeListener(socket);
        try {
            fileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
