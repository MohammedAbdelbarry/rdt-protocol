package edu.csed.networks.rdt.server;

import edu.csed.networks.rdt.observer.AckObserver;
import edu.csed.networks.rdt.observer.ServerObservable;
import edu.csed.networks.rdt.observer.event.AckEvent;

public class ListenerServer implements ServerObservable {
    @Override
    public void broadcast(AckEvent event) {

    }

    @Override
    public void send(AckEvent event, AckObserver observer) {

    }

    @Override
    public void addListener(AckObserver observer) {

    }

    @Override
    public void removeListener(AckObserver observer) {

    }
}
