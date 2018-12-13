package edu.csed.networks.rdt.protocol.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    public Collection<Long> packetTimedOut(long seqNo) {
        return new ArrayList<>(Collections.singletonList(seqNo));
    }
}
