package edu.csed.networks.rdt.protocol.strategy;

public class StopAndWaitStrategy extends SelectiveRepeatStrategy {
    public StopAndWaitStrategy() {
        super();
    }

    @Override
    public void acceptAck(long seqNo) {
        unackedPackets.remove(seqNo);
        if (seqNo == windowBase) {
            windowBase++;
        }
    }

    @Override
    public void packetTimedOut(long seqNo) {

    }
}
