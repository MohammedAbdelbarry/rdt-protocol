package edu.csed.networks.rdt.observer.event;

public class TimeoutEvent {
    private long seqNo;

    public TimeoutEvent(long seqNo) {
        this.seqNo = seqNo;
    }

    public long getSeqNo() {
        return seqNo;
    }
}
