package edu.csed.networks.rdt.protocol.strategy;

import java.util.*;

public class SelectiveRepeatStrategy extends TransmissionStrategy {
    protected Set<Long> unackedPackets;

    public SelectiveRepeatStrategy() {
        unackedPackets = new HashSet<>();
        windowBase = 0;
        windowSize = 1;
    }

    @Override
    public boolean isAcked(long seqNo) {
        return seqNo < windowBase || !unackedPackets.contains(seqNo);
    }

    @Override
    public void acceptAck(long seqNo) {
        unackedPackets.remove(seqNo);
        windowSize++;
        if (seqNo == windowBase) {
            windowBase++;
        }
    }

    @Override
    public void sentPacket(long seqNo) {
        unackedPackets.add(seqNo);
    }

    @Override
    public Collection<Long> packetTimedOut(long seqNo) {
        windowSize = Math.max(windowSize / 2, 1);
        return new ArrayList<>(Collections.singletonList(seqNo));
    }
}
