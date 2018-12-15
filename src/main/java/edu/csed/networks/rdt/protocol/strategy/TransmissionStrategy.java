package edu.csed.networks.rdt.protocol.strategy;

import java.util.Collection;

public abstract class TransmissionStrategy {
    protected long windowBase;
    protected int windowSize;

    public long getWindowBase() {
        return windowBase;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public abstract boolean isAcked(long seqNo);

    /**
     * TCP Protocol received ack from client.
     * @param seqNo
     */
    public abstract void acceptAck(long seqNo);

    /**
     * TCP Protocol received packet to send from server and set it.
     * @param seqNo
     */
    public abstract void sentPacket(long seqNo);

    public abstract Collection<Long> packetTimedOut(long seqNo);
}
