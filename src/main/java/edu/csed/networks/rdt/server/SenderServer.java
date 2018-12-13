package edu.csed.networks.rdt.server;

import edu.csed.networks.rdt.observer.ServerObservable;
import edu.csed.networks.rdt.protocol.RDTSocket;
import edu.csed.networks.rdt.protocol.strategy.StopAndWaitStrategy;
import org.apache.commons.lang3.Conversion;

import java.awt.*;
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


    private static final int CHUNK_SIZE = 4096;

    public SenderServer(DatagramSocket socket, InetAddress address, int port, String filePath, ServerObservable observable) {
        // TODO: Create new Strategy from config.
        this.socket = new RDTSocket(socket, address, port, new StopAndWaitStrategy());
        observable.addListener(this.socket);
        try {
            System.out.println(String.format("File Path: %s", filePath));
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
        if (len < CHUNK_SIZE) {
            bytes[len] = 0x03;
            len++;
        }
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        workThread.stop();
        socket.close();
        try {
            fileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
