package edu.csed.networks.rdt.protocol;

import edu.csed.networks.rdt.observer.AckObserver;
import edu.csed.networks.rdt.observer.TimeoutObserver;
import edu.csed.networks.rdt.observer.event.AckEvent;
import edu.csed.networks.rdt.observer.event.TimeoutEvent;
import edu.csed.networks.rdt.packet.DataPacket;
import edu.csed.networks.rdt.packet.Packet;
import edu.csed.networks.rdt.protocol.strategy.TransmissionStrategy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class RDTSocket implements TimeoutObserver, AckObserver {
    private InetAddress address;
    private int port;
    private DatagramSocket socket;
    private Map<Long, Timer> timers;
    private TransmissionStrategy strategy;
    private Queue<TimeoutEvent> eventQueue;
    private static final long TIMEOUT = 30000;
    private static final int CHUNK_SIZE = 1024;
    private int seqNo;
    private Semaphore semaphore;
    private Map<Long, Packet> senderWindow;
    private TreeMap<Long, Packet> receiverWindow;



    public RDTSocket(DatagramSocket socket, InetAddress address, int port, TransmissionStrategy strategy) {
        this.address = address;
        this.socket = socket;
        this.port = port;
        this.strategy = strategy;
        seqNo = 0;
        semaphore = new Semaphore(0);
        timers = new HashMap<>();
        eventQueue = new LinkedBlockingQueue<>();
        senderWindow = new HashMap<>();
        receiverWindow = new TreeMap<>();
    }

    public void send(byte[] bytes, int offset, int len) throws IOException {
        if (len <= 0) {
            return;
        }
        if (len <= CHUNK_SIZE) {
            byte[] copiedBytes = new byte[len];
            System.arraycopy(bytes, offset, copiedBytes, 0, len);
            DataPacket packet = new DataPacket((short) len, seqNo, copiedBytes, address, port);
            seqNo += len;
            send(packet);
        } else {
            send(bytes, offset, CHUNK_SIZE);
            send(bytes, offset + CHUNK_SIZE, len - CHUNK_SIZE);
        }
    }

    private void send(Packet packet) throws IOException {
        while (packet.getSeqNo() >= strategy.getWindowBase() + strategy.getWindowSize()) {
            // Sender should block until the msg is in the senderWindow.
            try {
                semaphore.acquire();
            } catch (InterruptedException ignored) {

            }
        }
        senderWindow.put((long) packet.getSeqNo(), packet);
        byte[] msgBytes = packet.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, address, port);
        socket.send(datagramPacket);
        strategy.sentPacket(packet.getSeqNo());
        TimeoutTask timerTask = new TimeoutTask(packet.getSeqNo());
        timerTask.addListener(this);
        Timer timer = new Timer();
        timer.schedule(timerTask, TIMEOUT);
        timers.put((long) packet.getSeqNo(), timer);
    }

    public byte[] receive(int len) {
        return new byte[0];
    }

    private Packet receive(Packet packet) {
        return null;
    }

    @Override
    public void accept(AckEvent event) {
        if (event.getPacket().getPort() == port && Objects.equals(event.getPacket().getHost(), address)) {
            long oldWindowBase = strategy.getWindowBase();
            strategy.acceptAck(event.getPacket().getSeqNo());
            if (strategy.getWindowBase() > oldWindowBase) {
                semaphore.release();
            }
            Timer timer = timers.get((long) event.getPacket().getSeqNo());
            timer.cancel();
            timers.remove((long) event.getPacket().getSeqNo());
            senderWindow.remove((long) event.getPacket().getSeqNo());
        }
    }

    @Override
    public void accept(TimeoutEvent event) {
        if (!strategy.isAcked(event.getSeqNo())) {
            try {
                send(senderWindow.get(event.getSeqNo()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
