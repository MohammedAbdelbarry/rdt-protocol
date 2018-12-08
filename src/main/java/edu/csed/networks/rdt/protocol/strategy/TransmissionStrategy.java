package edu.csed.networks.rdt.protocol.strategy;

public abstract class TransmissionStrategy {
    protected long windowBase;
    protected int windowSize;

    public abstract boolean isAcked(long seqNo);

    public long getWindowBase() {
        return windowBase;
    }

    public int getWindowSize() {
        return windowSize;
    }

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

    public abstract void packetTimedOut(long seqNo);
}
