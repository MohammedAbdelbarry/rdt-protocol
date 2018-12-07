package edu.csed.networks.rdt.observer;

import edu.csed.networks.rdt.observer.event.TimeoutEvent;

public interface TimeoutObserver {

    public void accept(TimeoutEvent event);
}
