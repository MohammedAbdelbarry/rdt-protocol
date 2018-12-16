package edu.csed.networks.rdt.protocol.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class GoBackNStrategy extends TransmissionStrategy {
    protected Set<Long> unackedPackets;
    protected Set<Long> ackedPackets;

    public GoBackNStrategy(int maxCwnd) {
        unackedPackets = new HashSet<>();
        ackedPackets = new HashSet<>();
        cwndHistory = new ArrayList<>();
        windowBase = 0;
        windowSize = 1;
        this.maxCwnd = maxCwnd;
        cwndHistory.add(windowSize);
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
        if (windowSize < maxCwnd) {
            windowSize++;
            cwndHistory.add(windowSize);
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
        Collection<Long> packets = new TreeSet<>();
        int newWindowSize = Math.max(windowSize / 2, 1);
        cwndHistory.add(newWindowSize);
        for (long i = seqNo; i < windowBase + windowSize; i++) {
            packets.add(i);
        }
        windowSize = newWindowSize;
        return packets;
    }

    @Override
    public boolean canTimeOut(long seqNo) {
        return seqNo == windowBase;
    }
}
