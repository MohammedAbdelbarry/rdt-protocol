package edu.csed.networks.rdt.observer;

import edu.csed.networks.rdt.observer.event.AckEvent;

public interface ServerObservable extends Observable<AckEvent, AckObserver> {
}
