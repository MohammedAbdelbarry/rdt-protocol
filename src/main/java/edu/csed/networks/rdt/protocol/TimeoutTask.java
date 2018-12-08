package edu.csed.networks.rdt.protocol;

import edu.csed.networks.rdt.observer.TimeoutObserver;
import edu.csed.networks.rdt.observer.TimerObservable;
import edu.csed.networks.rdt.observer.event.TimeoutEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

public class TimeoutTask extends TimerTask implements TimerObservable {
    private long seqNo;
    private Set<TimeoutObserver> observers;

    public TimeoutTask(long seqNo) {
        this.seqNo = seqNo;
        observers = new HashSet<>();
    }

    @Override
    public void run() {
        broadcast(new TimeoutEvent(seqNo));
    }

    @Override
    public void broadcast(TimeoutEvent event) {
        for (TimeoutObserver observer : observers) {
            observer.accept(event);
        }
    }

    @Override
    public void send(TimeoutEvent event, TimeoutObserver observer) {
        observer.accept(event);
    }

    @Override
    public void addListener(TimeoutObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeListener(TimeoutObserver observer) {
        observers.remove(observer);
    }
}
