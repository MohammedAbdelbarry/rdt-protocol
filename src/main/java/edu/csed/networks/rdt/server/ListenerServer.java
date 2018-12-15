package edu.csed.networks.rdt.server;

import edu.csed.networks.rdt.observer.AckObserver;
import edu.csed.networks.rdt.observer.ServerObservable;
import edu.csed.networks.rdt.observer.event.AckEvent;
import edu.csed.networks.rdt.packet.AckPacket;
import edu.csed.networks.rdt.packet.DataPacket;
import edu.csed.networks.rdt.packet.exceptions.PacketCorruptedException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ListenerServer implements Runnable, ServerObservable {

    private DatagramSocket socket;
    private Set<AckObserver> observers;

    private static final int MAX_LEN = 4096;

    public ListenerServer(DatagramSocket socket) {
        this.socket = socket;
        this.observers = new HashSet<>();
    }

    @Override
    public void broadcast(AckEvent event) {
        for (AckObserver observer : observers) {
            observer.accept(event);
        }
    }

    @Override
    public void send(AckEvent event, AckObserver observer) {
        observer.accept(event);
    }

    @Override
    public void addListener(AckObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeListener(AckObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void run() {
        while (!socket.isClosed()) {
            DatagramPacket packet = new DatagramPacket(new byte[MAX_LEN], MAX_LEN);

            try {
                socket.receive(packet);
                if (packet.getLength() == AckPacket.ACK_LEN) {
                    AckPacket ackPacket = null;
                    try {
                        ackPacket = AckPacket.valueOf(packet.getData(), packet.getLength(),
                                packet.getAddress(), packet.getPort());
                    } catch (PacketCorruptedException e) {
                        System.out.println("Received Corrupted Ack");
                        continue;
                    }
                    AckEvent event = new AckEvent(ackPacket);
                    broadcast(event);
                } else {// Data packet.
                    DataPacket dataPacket = null;
                    try {
                        dataPacket = DataPacket.valueOf(packet.getData(), packet.getAddress(), packet.getPort());
                        System.out.println(String.format("Connect(%s, %d)", packet.getAddress(), packet.getPort()));
                    } catch (PacketCorruptedException e) {
                        System.out.println("Received Corrupted Connection Request");
                        continue;
                    }
                    new SenderServer(socket, packet.getAddress(), packet.getPort(),
                            new String(dataPacket.getData(), 0, dataPacket.getLength()), this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Host {
        private InetAddress address;
        private int port;

        public Host(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Host host = (Host) o;
            return port == host.port &&
                    Objects.equals(address, host.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, port);
        }
    }
}
