package edu.csed.networks.rdt.protocol;

import edu.csed.networks.rdt.observer.AckObserver;
import edu.csed.networks.rdt.observer.TimeoutObserver;
import edu.csed.networks.rdt.observer.event.AckEvent;
import edu.csed.networks.rdt.observer.event.TimeoutEvent;
import edu.csed.networks.rdt.packet.AckPacket;
import edu.csed.networks.rdt.packet.DataPacket;
import edu.csed.networks.rdt.packet.Packet;
import edu.csed.networks.rdt.packet.exceptions.PacketCorruptedException;
import edu.csed.networks.rdt.protocol.strategy.TransmissionStrategy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class RDTSocket implements TimeoutObserver, AckObserver {
    private InetAddress address;
    private int port;
    private DatagramSocket socket;
    private ConcurrentMap<Long, TimerFuture> timers;
    private TransmissionStrategy strategy;
    private static final long TIMEOUT = 100;
    private static final int CHUNK_SIZE = 16 * 1024;
    private static final double ALPHA = 0.125;
    private static final double BETA = 0.25;
    private double eRTT;
    private double devRTT;
    private long timeOut;
    private int recSeqNo;
    private int sendSeqNo;
    private double plp;
    private double pcp;
    private Random rng;
    private ConcurrentMap<Long, Packet> senderWindow;
    private int rwnd;
    private TreeMap<Long, Packet> receiverWindow;
    private ReentrantLock lock;
    private Condition sleepCondVar;
    private ScheduledThreadPoolExecutor executor;
    private Thread cleanerThread;
    private PriorityBlockingQueue<Long> timedOutPackets;

    public RDTSocket(DatagramSocket socket, InetAddress address, int port, TransmissionStrategy strategy, int rwnd) {
        this.address = address;
        this.socket = socket;
        this.port = port;
        this.strategy = strategy;
        this.rwnd = rwnd;
        recSeqNo = 0;
        sendSeqNo = 0;
        plp = 0.05;
        pcp = 0.0;
        long seed = 1; // TODO: GET SEED FROM CONFIG
        rng = new Random(seed);
        timers = new ConcurrentHashMap<>();
        senderWindow = new ConcurrentHashMap<>();
        receiverWindow = new TreeMap<>();
        lock = new ReentrantLock();
        sleepCondVar = lock.newCondition();
        int cores = Runtime.getRuntime().availableProcessors();
        executor = new ScheduledThreadPoolExecutor(cores);
        timeOut = TIMEOUT;
        eRTT = TIMEOUT;
        devRTT = 0;
        timedOutPackets = new PriorityBlockingQueue<>();
        cleanerThread = new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                while (true) {
                    long seqNo = -1;
                    try {
                        System.out.println("Try-Take");
                        seqNo = timedOutPackets.take();
                        System.out.println(String.format("Take(%d)", seqNo));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (seqNo == Long.MAX_VALUE) {
                        System.out.println("Cleaner is Done");
                        break;
                    }
                    System.out.println(String.format("Packet(%d) is not in sender window?%s", seqNo, senderWindow.get(seqNo) == null));
                    if (seqNo != -1 && seqNo >= strategy.getWindowBase() && senderWindow.get(seqNo) != null) {
                        try {
                            Packet packet = senderWindow.get(seqNo);
//                        System.out.println(String.format("Trying to Resend(%d)", seqNo));
                            if (!trySend(packet)) {
                                timedOutPackets.add(seqNo);
                                try {
                                    wait(30);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                System.out.println(String.format("Can't Resend(%d, Window(%d, %d))", seqNo,
                                        strategy.getWindowBase(), strategy.getWindowBase() + strategy.getWindowSize()));
                            } else {
                                System.out.println(String.format("Resent(%d)", seqNo));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        cleanerThread.start();
    }

    public void send(byte[] bytes, int offset, int len) throws IOException {
        if (len <= 0) {
            return;
        }
        if (len <= CHUNK_SIZE) {
            byte[] copiedBytes = new byte[len];
            System.arraycopy(bytes, offset, copiedBytes, 0, len);
            DataPacket packet = new DataPacket((short) len, sendSeqNo, copiedBytes, address, port);
            send(packet);
            lock.lock();
            sendSeqNo++;
            lock.unlock();
        } else {
            send(bytes, offset, CHUNK_SIZE);
            send(bytes, offset + CHUNK_SIZE, len - CHUNK_SIZE);
        }
    }

    private boolean trySend(Packet packet) throws IOException {
        if (packet == null) {
            return true;
        }
        return send(packet, false);
    }

    private boolean send(Packet packet, boolean sleep) throws IOException {
        if (packet == null) {
            return false;
        }
        if (packet.getSeqNo() < strategy.getWindowBase()) {
            return true;
        }
        lock.lock();
        System.out.println(String.format("SeqNo=%d, WindowBase=%d, WindowSize=%d, PacketLength=%d", packet.getSeqNo(), strategy.getWindowBase(), strategy.getWindowSize(), packet.getLength()));
        while (packet.getSeqNo() >= strategy.getWindowBase() + strategy.getWindowSize()) {
            // Sender should block until the msg is in the senderWindow.
            if (!sleep) {
                lock.unlock();
                return false;
            }
            try {
                sleepCondVar.await();
            } catch (InterruptedException ignored) {

            }
        }
        senderWindow.put(packet.getSeqNo(), packet);
        // Corrupt Packet.
        if (rng.nextDouble() <= pcp) {
            packet = new DataPacket(packet.getLength(), packet.getSeqNo(),
                    packet.getData(), packet.getHost(), packet.getPort());
            packet.corrupt();
        }
        byte[] msgBytes = packet.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, address, port);
        // Drop Packet.
        if (rng.nextDouble() > plp) {
            try {
                socket.send(datagramPacket);
            } catch (IOException e) {
                lock.unlock();
                throw e;
            }
            System.out.println(String.format("Sent(%d)", packet.getSeqNo()));
        } else {
            System.out.println(String.format("Dropped(%d)", packet.getSeqNo()));
        }
        strategy.sentPacket(packet.getSeqNo());
        TimeoutTask timerTask = new TimeoutTask(packet.getSeqNo());
        timerTask.addListener(this);
        if (timers.containsKey(packet.getSeqNo())) {
            timers.get(packet.getSeqNo()).getFuture().cancel(false);
        }
        timers.put(packet.getSeqNo(), new TimerFuture(System.currentTimeMillis(),
                executor.schedule(timerTask, timeOut, TimeUnit.MILLISECONDS)));
        lock.unlock();
        return true;
    }

    private void send(Packet packet) throws IOException {
        send(packet, true);
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
        while (receiverWindow.size() < rwnd) {
            byte[] buffer = new byte[CHUNK_SIZE + Packet.HEADERS_LENGTH];
            DatagramPacket packet = new DatagramPacket(buffer, CHUNK_SIZE + Packet.HEADERS_LENGTH);
            socket.receive(packet);
            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
            DataPacket dataPacket;
            try {
                dataPacket = DataPacket.valueOf(data, packet.getAddress(), packet.getPort());
            } catch (PacketCorruptedException e) {
                System.out.println("Received Corrupted Packet");
                continue;
            }
            if (dataPacket.getSeqNo() >= recSeqNo + rwnd) {
                System.out.println(String.format("Dropped(%d, Window(%d, %d, %d))", dataPacket.getSeqNo(),
                        recSeqNo, recSeqNo + rwnd - 1, receiverWindow.size()));
                continue;
            } else if (dataPacket.getSeqNo() < recSeqNo || receiverWindow.containsKey(dataPacket.getSeqNo())) {
                sendAck(dataPacket);
                continue;
            }
            System.out.println(String.format("Received(%d, %d bytes)", dataPacket.getSeqNo(), dataPacket.getLength()));
            sendAck(dataPacket);
            receiverWindow.put(dataPacket.getSeqNo(), dataPacket);
            if (noGaps(receiverWindow, recSeqNo)) {
                break;
            }
        }
        recSeqNo += receiverWindow.size();
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
            if (event.getPacket().getSeqNo() < strategy.getWindowBase()) {
                return;
            }
            lock.lock();
            System.out.println(String.format("Ack(%d)", event.getPacket().getSeqNo()));
            long oldWindowBase = strategy.getWindowBase();
            strategy.acceptAck(event.getPacket().getSeqNo());
            long newBase = strategy.getWindowBase();
            if (newBase > oldWindowBase) {
                Set<Long> seqNumbers = senderWindow.keySet();
                for (Long seqNum : seqNumbers) {
                    if (seqNum < newBase) {
                        senderWindow.remove(seqNum);
                    }
                }
                sleepCondVar.signalAll();
            }
            TimerFuture timerFuture = timers.get(event.getPacket().getSeqNo());
            if (timerFuture != null) {
                timerFuture.getFuture().cancel(false);
                long RTT = System.currentTimeMillis() - timerFuture.getStartTime();
                eRTT = (1 - ALPHA) * eRTT + ALPHA * RTT;
                devRTT = (1 - BETA) * devRTT + BETA * Math.abs(RTT - eRTT);
                timeOut = (long) Math.ceil(eRTT + 4 * devRTT);
                System.out.println("TimeOUT is " + timeOut);
                timers.remove(event.getPacket().getSeqNo());
            }
            lock.unlock();
        }
    }

    @Override
    public void accept(TimeoutEvent event) {
        if (event.getSeqNo() < strategy.getWindowBase()
                || event.getSeqNo() >= strategy.getWindowBase() + strategy.getWindowSize()) {
            return;
        }
        lock.lock();
        if (!strategy.isAcked(event.getSeqNo())) {
            System.out.println(String.format("Time-Out(%d)", event.getSeqNo()));
            int oldSize = strategy.getWindowSize();
            Collection<Long> packetsToSend = strategy.packetTimedOut(event.getSeqNo());
            int newSize = strategy.getWindowSize();
            for (long i = strategy.getWindowBase() + newSize; i < strategy.getWindowBase() + oldSize; i++) {
                if (timers.containsKey(i)) {
                    timers.get(i).getFuture().cancel(false);
                    timers.remove(i);
                }
            }
            System.out.println(String.format("New-Window(%d, %d)", strategy.getWindowBase(), strategy.getWindowSize()));
            for (long packetSeqNo : packetsToSend) {
                if (!strategy.isAcked(packetSeqNo)) {
                    if (!timedOutPackets.contains(packetSeqNo)) {
                        System.out.println(String.format("Try-Queue(%d)", packetSeqNo));
                        timedOutPackets.add(packetSeqNo);
                        System.out.println(String.format("Queued(%d)", packetSeqNo));
                    }
                }
            }
        }
        lock.unlock();
    }

    private void printCwndHistory() {
        String history = strategy.getCwndHistory().stream().map(Objects::toString).collect(Collectors.joining("\n"));
        String filePath = String.format("cwnd-history-%.2f.txt", plp);
        try {
            Files.write(Paths.get(filePath), history.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void close() {
        while (strategy.getWindowBase() < sendSeqNo) {
            try {
                wait(TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        timedOutPackets.add(Long.MAX_VALUE);
        executor.shutdown();
        timers.clear();
        printCwndHistory();
        System.out.println("Socket Closed");
    }

    private static class TimerFuture {
        private long startTime;
        private ScheduledFuture<?> future;

        TimerFuture(long startTime, ScheduledFuture<?> future) {
            this.startTime = startTime;
            this.future = future;
        }

        long getStartTime() {
            return startTime;
        }

        ScheduledFuture<?> getFuture() {
            return future;
        }
    }
}
