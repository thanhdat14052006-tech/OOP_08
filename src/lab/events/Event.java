package lab.events;

public interface Event<T> {
    void add(EventHandler<T> handler);

    void remove(EventHandler<T> handler);

    int handlerCount();
}
