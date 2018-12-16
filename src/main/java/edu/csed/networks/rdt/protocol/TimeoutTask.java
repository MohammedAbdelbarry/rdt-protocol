package edu.csed.networks.rdt.protocol;

import edu.csed.networks.rdt.observer.TimeoutObserver;
import edu.csed.networks.rdt.observer.TimerObservable;
import edu.csed.networks.rdt.observer.event.TimeoutEvent;
import edu.csed.networks.rdt.protocol.strategy.TransmissionStrategy;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TimeoutTask extends TimerTask implements TimerObservable {
    private Set<TimeoutObserver> observers;
    private TransmissionStrategy strategy;
    private ConcurrentMap<Long, SequenceTime> startTimes;
    private boolean stop;

    public TimeoutTask(TransmissionStrategy strategy) {
        observers = new HashSet<>();
        this.strategy = strategy;
        this.startTimes = new ConcurrentHashMap<>();
        stop = false;
    }

    public void addTimer(long seqNo, long timeOut) {
        startTimes.put(seqNo, new SequenceTime(System.currentTimeMillis(), timeOut));
    }

    public void removeTimer(long seqNo) {
        startTimes.remove(seqNo);
    }

    public Long getStartTime(long seqNo) {
        if (startTimes.get(seqNo) == null) {
            return null;
        }
        return startTimes.get(seqNo).getStartTime();
    }

    @Override
    public synchronized void run() {
        while (!stop || !startTimes.isEmpty()) {
            for (Map.Entry<Long, SequenceTime> seqTime : startTimes.entrySet()) {
                if (System.currentTimeMillis() - seqTime.getValue().getStartTime() >= seqTime.getValue().getTimeOut()) {
                    if (seqTime.getKey() < strategy.getWindowBase()) {
                        startTimes.remove(seqTime.getKey());
                    } else if (strategy.canTimeOut(seqTime.getKey())) {
                        startTimes.remove(seqTime.getKey());
                        broadcast(new TimeoutEvent(seqTime.getKey()));
                    }
                }
            }

            try {
                wait(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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

    public void stop() {
        stop = true;
    }

    private static class SequenceTime {
        private long startTime;
        private long timeOut;

        public SequenceTime(long startTime, long timeOut) {
            this.startTime = startTime;
            this.timeOut = timeOut;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getTimeOut() {
            return timeOut;
        }
    }
}
