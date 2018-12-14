package edu.csed.networks.rdt.protocol.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class SelectiveRepeatStrategy extends TransmissionStrategy {
    protected Set<Long> unackedPackets;
    protected Set<Long> ackedPackets;

    public SelectiveRepeatStrategy() {
        unackedPackets = new HashSet<>();
        ackedPackets = new HashSet<>();
        windowBase = 0;
        windowSize = 1;
    }

    @Override
    public boolean isAcked(long seqNo) {
        return ackedPackets.contains(seqNo);
    }

    @Override
    public void acceptAck(long seqNo) {
        unackedPackets.remove(seqNo);
        ackedPackets.add(seqNo);
        if (seqNo == windowBase) {
            windowBase = unackedPackets.stream().min(Comparator.comparingLong(x -> x))
                    .orElse(ackedPackets.stream().max(Comparator.comparingLong(x -> x)).orElse(windowBase) + 1);
            System.out.println(unackedPackets);
            System.out.println(ackedPackets.stream().max(Comparator.comparingLong(x -> x)));
            System.out.println(String.format("new Window-Base(%d)", windowBase));
        }
        if (windowSize < 150) {
            windowSize++;
        }
    }

    @Override
    public void sentPacket(long seqNo) {
        if (!isAcked(seqNo)) {
            unackedPackets.add(seqNo);
        }
    }

    @Override
    public Collection<Long> packetTimedOut(long seqNo) {
        Collection<Long> packets = new ArrayList<>();
        packets.add(seqNo);
        int newWindowSize = Math.max(windowSize / 2, 1);
        for (long i = windowBase + newWindowSize; i < windowBase + windowSize; i++) {
            packets.add(i);
        }
        windowSize = newWindowSize;
        return packets;
    }
}
