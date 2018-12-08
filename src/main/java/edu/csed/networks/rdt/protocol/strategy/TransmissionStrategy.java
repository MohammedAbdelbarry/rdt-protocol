package edu.csed.networks.rdt.protocol.strategy;

public abstract class TransmissionStrategy {
    protected long nextSeqNo;
    protected long windowBase;
    protected int windowSize;
    protected long firstUnAckedSeqNo;
    protected long initSeqNo;
    protected int numPackets;

    public abstract boolean isDone();

    public abstract boolean isAcked(long seqNo);

    public long getNextSeqNo() {
        return nextSeqNo;
    }

    public long getWindowBase() {
        return windowBase;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public long getFirstUnAckedSeqNo() {
        return firstUnAckedSeqNo;
    }

    public long getInitSeqNo() {
        return initSeqNo;
    }

    public int getNumPackets() {
        return numPackets;
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

    /**
     * TCP Protocol received packet from client.
     * @param seqNo
     */
    public abstract void receivedPacket(long seqNo);
}
