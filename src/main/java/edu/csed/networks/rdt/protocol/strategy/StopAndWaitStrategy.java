package edu.csed.networks.rdt.protocol.strategy;

public class StopAndWaitStrategy extends SelectiveRepeatStrategy {
    public StopAndWaitStrategy() {
        super();
    }

    @Override
    public void acceptAck(long seqNo, long nextSeqNo) {
        unackedPackets.add(seqNo);
        if (seqNo == windowBase) {
            windowBase = nextSeqNo;
        }
    }

    @Override
    public void packetTimedOut(long seqNo) {

    }
}
