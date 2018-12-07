package edu.csed.networks.rdt.server;

import edu.csed.networks.rdt.observer.AckObserver;
import edu.csed.networks.rdt.observer.TimeoutObserver;
import edu.csed.networks.rdt.observer.event.AckEvent;
import edu.csed.networks.rdt.observer.event.TimeoutEvent;

public class SenderServer implements AckObserver, TimeoutObserver {
    @Override
    public void accept(AckEvent event) {

    }

    @Override
    public void accept(TimeoutEvent event) {

    }
}
