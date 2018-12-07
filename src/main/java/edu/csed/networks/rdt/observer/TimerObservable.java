package edu.csed.networks.rdt.observer;

import edu.csed.networks.rdt.observer.event.TimeoutEvent;

public interface TimerObservable extends Observable<TimeoutEvent, TimeoutObserver> {
}
