package edu.csed.networks.rdt.observer;

import edu.csed.networks.rdt.observer.event.AckEvent;

public interface AckObserver {

    public void accept(AckEvent event);
}
