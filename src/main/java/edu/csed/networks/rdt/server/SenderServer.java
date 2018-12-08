package edu.csed.networks.rdt.server;

import edu.csed.networks.rdt.observer.AckObserver;
import edu.csed.networks.rdt.observer.TimeoutObserver;
import edu.csed.networks.rdt.observer.event.AckEvent;
import edu.csed.networks.rdt.observer.event.TimeoutEvent;
import edu.csed.networks.rdt.packet.DataPacket;
import edu.csed.networks.rdt.protocol.TimeoutTask;
import edu.csed.networks.rdt.protocol.strategy.TransmissionStrategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;

public class SenderServer implements AckObserver, TimeoutObserver, Runnable {
    private Map<Long, Timer> timers;
    private TransmissionStrategy strategy;
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private FileInputStream fileStream;
    private Thread workThread;
    private Queue<TimeoutEvent> eventQueue;
    private static final long TIMEOUT = 30000;


    private static final int CHUNK_SIZE = 4096;

    public SenderServer(DatagramSocket socket, InetAddress address, int port, String filePath) {
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.strategy = null;
        timers = new HashMap<>();
        eventQueue = new LinkedBlockingQueue<>();
        try {
            long fileSize = Files.size(new File(filePath).toPath());
            // TODO: calculate number of packets and create a new strategy.
            this.fileStream = new FileInputStream(filePath);
            workThread = new Thread(this);
            workThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void accept(AckEvent event) {
        if (event.getPacket().getPort() == port && Objects.equals(event.getPacket().getHost(), address)) {
            strategy.acceptAck(event.getPacket().getSeqNo());
            Timer timer = timers.get((long) event.getPacket().getSeqNo());
            timer.cancel();
            timers.remove((long) event.getPacket().getSeqNo());
        }
    }

    @Override
    public void accept(TimeoutEvent event) {
        if (!strategy.isAcked(event.getSeqNo())) {
            eventQueue.offer(event);
        }
    }

    private void sendTimedOut() throws IOException {
        while (!eventQueue.isEmpty()) {
            TimeoutEvent event = eventQueue.poll();
            if (event != null) {
                send(event.getSeqNo());
            }
        }
    }

    private void send(long seqNo) throws IOException {
        byte[] bytes = new byte[CHUNK_SIZE];
        int len = fileStream.read(bytes, (int) (seqNo * CHUNK_SIZE), CHUNK_SIZE);
        DataPacket packet = new DataPacket((short) len, (int) seqNo, bytes, address, port);
        byte[] msgBytes = packet.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, address, port);
        socket.send(datagramPacket);
        strategy.sentPacket(packet.getSeqNo());
        TimeoutTask timerTask = new TimeoutTask(seqNo);
        Timer timer = new Timer();
        timer.schedule(timerTask, TIMEOUT);
        timers.put(seqNo, timer);
    }

    @Override
    public void run() {

        while (!strategy.isDone()) {
            try {
                sendTimedOut();
                send(strategy.getNextSeqNo());
            } catch (IOException e) {
                e.printStackTrace();
            }
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
