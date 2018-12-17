package edu.csed.networks.rdt.protocol.strategy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class SelectiveRepeatStrategy extends TransmissionStrategy {
    protected Set<Long> unackedPackets;
    protected Set<Long> ackedPackets;
    private static final Logger LOGGER = LogManager.getLogger(SelectiveRepeatStrategy.class);

    public SelectiveRepeatStrategy(int maxCwnd) {
        unackedPackets = new HashSet<>();
        ackedPackets = new HashSet<>();
        cwndHistory = new ArrayList<>();
        windowBase = 0;
        windowSize = 1;
        this.maxCwnd = maxCwnd;
        cwndHistory.add(windowSize);
    }

    @Override
    public synchronized boolean isAcked(long seqNo) {
        return ackedPackets.contains(seqNo);
    }

    @Override
    public synchronized void acceptAck(long seqNo) {
        unackedPackets.remove(seqNo);
        ackedPackets.add(seqNo);
        if (seqNo == windowBase) {
            windowBase = unackedPackets.stream().min(Comparator.comparingLong(x -> x))
                    .orElse(ackedPackets.stream().max(Comparator.comparingLong(x -> x)).orElse(windowBase) + 1);
            LOGGER.trace(unackedPackets);
            LOGGER.trace(ackedPackets.stream().max(Comparator.comparingLong(x -> x)));
            LOGGER.debug(String.format("new Window-Base(%d)", windowBase));
        }
        if (windowSize < maxCwnd) {
            windowSize++;
            cwndHistory.add(windowSize);
        }
    }

    @Override
    public synchronized void sentPacket(long seqNo) {
        if (!isAcked(seqNo)) {
            unackedPackets.add(seqNo);
        }
    }

    @Override
    public synchronized Collection<Long> packetTimedOut(long seqNo) {
        Collection<Long> packets = new TreeSet<>();
        packets.add(seqNo);
        int newWindowSize = Math.max(windowSize / 2, 1);
        cwndHistory.add(newWindowSize);
        for (long i = windowBase + newWindowSize; i < windowBase + windowSize; i++) {
            packets.add(i);
        }
        windowSize = newWindowSize;
        return packets;
    }

    @Override
    public boolean canTimeOut(long seqNo) {
        return true;
    }
}
