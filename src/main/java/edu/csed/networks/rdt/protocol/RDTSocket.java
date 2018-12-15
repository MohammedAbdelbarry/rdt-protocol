package edu.csed.networks.rdt.protocol;

import edu.csed.networks.rdt.observer.AckObserver;
import edu.csed.networks.rdt.observer.TimeoutObserver;
import edu.csed.networks.rdt.observer.event.AckEvent;
import edu.csed.networks.rdt.observer.event.TimeoutEvent;
import edu.csed.networks.rdt.packet.AckPacket;
import edu.csed.networks.rdt.packet.DataPacket;
import edu.csed.networks.rdt.packet.Packet;
import edu.csed.networks.rdt.protocol.strategy.TransmissionStrategy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RDTSocket implements TimeoutObserver, AckObserver {
    private InetAddress address;
    private int port;
    private DatagramSocket socket;
    private ConcurrentMap<Long, Timer> timers;
    private TransmissionStrategy strategy;
    private static final long TIMEOUT = 30;
    private static final int CHUNK_SIZE = 1024;
    private int seqNo;
    private double plp;
    private Random rng;
    private ConcurrentMap<Long, Packet> senderWindow;
    private static final int RWND = 100;
    private TreeMap<Long, Packet> receiverWindow;
    private ReentrantLock lock;
    private Condition sleepCondVar;



    public RDTSocket(DatagramSocket socket, InetAddress address, int port, TransmissionStrategy strategy) {
        this.address = address;
        this.socket = socket;
        this.port = port;
        this.strategy = strategy;
        seqNo = 0;
        plp = 0.01;
        long seed = 1; // TODO: GET SEED FROM CONFIG
        rng = new Random(seed);
        timers = new ConcurrentHashMap<>();
        senderWindow = new ConcurrentHashMap<>();
        receiverWindow = new TreeMap<>();
        lock = new ReentrantLock();
        sleepCondVar = lock.newCondition();
    }

    public void send(byte[] bytes, int offset, int len) throws IOException {
        if (len <= 0) {
            return;
        }
        if (len <= CHUNK_SIZE) {
            byte[] copiedBytes = new byte[len];
            System.arraycopy(bytes, offset, copiedBytes, 0, len);
            DataPacket packet = new DataPacket((short) len, seqNo, copiedBytes, address, port);
            send(packet);
            seqNo++;
        } else {
            send(bytes, offset, CHUNK_SIZE);
            send(bytes, offset + CHUNK_SIZE, len - CHUNK_SIZE);
        }
    }

    private void send(Packet packet) throws IOException {
        if (packet == null) {
            return;
        }
        System.out.println(String.format("SeqNo=%d, WindowBase=%d, WindowSize=%d", packet.getSeqNo(), strategy.getWindowBase(), strategy.getWindowSize() * CHUNK_SIZE));
        lock.lock();
        if (packet.getSeqNo() < strategy.getWindowBase()) {
            lock.unlock();
            return;
        }
        while (packet.getSeqNo() >= strategy.getWindowBase() + strategy.getWindowSize()) {
            // Sender should block until the msg is in the senderWindow.
            try {
                sleepCondVar.await();
            } catch (InterruptedException ignored) {

            }
        }
        senderWindow.put((long) packet.getSeqNo(), packet);
        byte[] msgBytes = packet.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, address, port);
        if (rng.nextDouble() > plp) {
            socket.send(datagramPacket);
        }
        System.out.println(String.format("Sent(%d)", packet.getSeqNo()));
        strategy.sentPacket(packet.getSeqNo());
        TimeoutTask timerTask = new TimeoutTask(packet.getSeqNo());
        timerTask.addListener(this);
        Timer timer = new Timer();
        timer.schedule(timerTask, TIMEOUT);
        timers.put((long) packet.getSeqNo(), timer);
        lock.unlock();
    }

    private boolean noGaps(Map<Long, ?> window, long seqNo) {
        for (long i = seqNo; i < seqNo + window.size(); i++) {
            if (!window.containsKey(i)) {
                return false;
            }
        }
        return true;
    }

    private void sendAck(DataPacket dataPacket) throws IOException {
        AckPacket ackPacket = new AckPacket(dataPacket.getSeqNo(), dataPacket.getHost(), dataPacket.getPort());
        byte[] ackBytes = ackPacket.getBytes();
        DatagramPacket ackDatagramPacket = new DatagramPacket(ackBytes, ackBytes.length,
                ackPacket.getHost(), ackPacket.getPort());
        socket.send(ackDatagramPacket);
    }

    public byte[] receive() throws IOException {
        while (receiverWindow.size() < RWND) {
            byte[] buffer = new byte[CHUNK_SIZE + Packet.HEADERS_LENGTH];
            DatagramPacket packet = new DatagramPacket(buffer, CHUNK_SIZE + AckPacket.ACK_LEN);
            socket.receive(packet);
            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
            DataPacket dataPacket = DataPacket.valueOf(data, packet.getAddress(), packet.getPort());
            if (dataPacket.getSeqNo() >= seqNo + RWND) {
                System.out.println(String.format("Dropped(%d, Window(%d, %d, %d))", dataPacket.getSeqNo(),
                        seqNo, seqNo + RWND - 1, receiverWindow.size()));
                continue;
            } else if (dataPacket.getSeqNo() < seqNo) {
                sendAck(dataPacket);
                continue;
            }
            System.out.println(String.format("Received(%d)", dataPacket.getSeqNo()));
            sendAck(dataPacket);
            receiverWindow.put((long) dataPacket.getSeqNo(), dataPacket);
            if (noGaps(receiverWindow, seqNo)) {
                break;
            }
        }
        seqNo += receiverWindow.size();
        int size = 0;
        for (Packet packet : receiverWindow.values()) {
            size += packet.getLength();
        }
        byte[] bytes = new byte[size];
        int idx = 0;
        for (Packet packet : receiverWindow.values()) {
            System.arraycopy(packet.getData(), 0, bytes, idx, packet.getData().length);
            idx += packet.getData().length;
        }
        receiverWindow.clear();
        return bytes;
    }

    @Override
    public void accept(AckEvent event) {
        if (event.getPacket().getPort() == port && Objects.equals(event.getPacket().getHost(), address)) {
            lock.lock();
            if (event.getPacket().getSeqNo() < strategy.getWindowBase()
                    || event.getPacket().getSeqNo() >= strategy.getWindowBase() + strategy.getWindowSize()) {
                lock.unlock();
                return;
            }
            System.out.println(String.format("Ack(%d)", event.getPacket().getSeqNo()));
            long oldWindowBase = strategy.getWindowBase();
            strategy.acceptAck(event.getPacket().getSeqNo());
            if (strategy.getWindowBase() > oldWindowBase) {
                sleepCondVar.signalAll();
                senderWindow.remove((long) event.getPacket().getSeqNo());
            }
            Timer timer = timers.get((long) event.getPacket().getSeqNo());
            if (timer != null) {
                timer.cancel();
                timers.remove((long) event.getPacket().getSeqNo());
            }
            lock.unlock();
        }
    }

    @Override
    public void accept(TimeoutEvent event) {
        lock.lock();
        if (event.getSeqNo() < strategy.getWindowBase()
                || event.getSeqNo() >= strategy.getWindowBase() + strategy.getWindowSize()) {
            lock.unlock();
            return;
        }
        if (!strategy.isAcked(event.getSeqNo())) {
            System.out.println(String.format("Packet(%d) Timed Out", event.getSeqNo()));
            int oldSize = strategy.getWindowSize();
            Collection<Long> packetsToSend = strategy.packetTimedOut(event.getSeqNo());
            int newSize = strategy.getWindowSize();
            for (long i = strategy.getWindowBase() + newSize; i < strategy.getWindowBase() + oldSize; i++) {
                if (timers.containsKey(i)) {
                    timers.get(i).cancel();
                    timers.remove(i);
                }
            }
            lock.unlock();
            for (long packetSeqNo : packetsToSend) {
                if (!strategy.isAcked(packetSeqNo)) {
                    try {
                        System.out.println(String.format("Will Try to Resend(%d)", packetSeqNo));
                        send(senderWindow.get(packetSeqNo));
                        System.out.println(String.format("Resent(%d)", packetSeqNo));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public synchronized void close() {
        while (!timers.isEmpty()) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
