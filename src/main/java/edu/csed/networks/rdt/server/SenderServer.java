package edu.csed.networks.rdt.server;

import edu.csed.networks.rdt.protocol.RDTSocket;
import edu.csed.networks.rdt.protocol.strategy.TransmissionStrategy;

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

    public SenderServer(DatagramSocket socket, InetAddress address, int port, String filePath, TransmissionStrategy strategy) {
        this.socket = new RDTSocket(socket, address, port, strategy);
        try {
            fileSize = Files.size(new File(filePath).toPath());
            // TODO: calculate number of packets and create a new strategy.
            this.fileStream = new FileInputStream(filePath);
            workThread = new Thread(this);
            workThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void send(long chunkNum) throws IOException {
        byte[] bytes = new byte[CHUNK_SIZE];
        int len = fileStream.read(bytes, (int) (chunkNum * CHUNK_SIZE), CHUNK_SIZE);
        socket.send(bytes, 0, len);
    }

    @Override
    public void run() {
        int i = 0;
        while (i < fileSize) {
            try {
                send(i);
            } catch (IOException e) {
                e.printStackTrace();
            }
            i += CHUNK_SIZE;
        }
        try {
            fileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        workThread.stop();
        try {
            fileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
