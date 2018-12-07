package edu.csed.networks.rdt.observer;

public interface Observable<T, E> {
    public void broadcast(T event);
    public void send(T event, E observer);
    public void addListener(E observer);
    public void removeListener(E observer);
}
