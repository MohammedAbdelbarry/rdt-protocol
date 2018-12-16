package edu.csed.networks.rdt.protocol.strategy;

import java.util.Collection;
import java.util.List;

public abstract class TransmissionStrategy {
    protected long windowBase;
    protected int windowSize;
    protected List<Integer> cwndHistory;
    protected int maxCwnd;

    public synchronized long getWindowBase() {
        return windowBase;
    }

    public synchronized int getWindowSize() {
        return windowSize;
    }

    public List<Integer> getCwndHistory() {
        return cwndHistory;
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

    public abstract boolean canTimeOut(long seqNo);
}
