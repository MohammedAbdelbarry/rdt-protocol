package edu.csed.networks.rdt.observer.event;

import edu.csed.networks.rdt.packet.AckPacket;

public class AckEvent {
    private AckPacket packet;

    public AckEvent(AckPacket packet) {
        this.packet = packet;
    }

    public AckPacket getPacket() {
        return packet;
    }
}
