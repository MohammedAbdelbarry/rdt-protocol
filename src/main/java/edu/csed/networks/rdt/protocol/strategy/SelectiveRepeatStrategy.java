package edu.csed.networks.rdt.protocol.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class SelectiveRepeatStrategy extends TransmissionStrategy {
    protected Set<Long> unackedPackets;
    protected long nextUnAcked;

    public SelectiveRepeatStrategy() {
        unackedPackets = new HashSet<>();
        windowBase = 0;
        windowSize = 1;
        nextUnAcked = 0;
    }

    @Override
    public boolean isAcked(long seqNo) {
        return seqNo < windowBase || !unackedPackets.contains(seqNo);
    }

    @Override
    public void acceptAck(long seqNo) {
        unackedPackets.remove(seqNo);
        if (seqNo == windowBase) {
            windowBase = unackedPackets.stream().min(Comparator.comparingLong(x -> x)).orElse(windowBase + windowSize);
            System.out.println(String.format("new Window-Base(%d)", windowBase));
        }
        windowSize++;
    }

    @Override
    public void sentPacket(long seqNo) {
        unackedPackets.add(seqNo);
        nextUnAcked = Math.max(nextUnAcked, seqNo + 1);
    }

    @Override
    public Collection<Long> packetTimedOut(long seqNo) {
        Collection<Long> packets = new ArrayList<>();
        packets.add(seqNo);
        int newWindowSize = Math.max(windowSize / 2, 1);
        for (long i = windowBase + newWindowSize; i < windowBase + windowSize; i++) {
            packets.add(i);
        }
        return packets;
    }
}
