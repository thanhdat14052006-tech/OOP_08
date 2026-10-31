package lab.events;

@FunctionalInterface
public interface EventHandler<T> {
    void handle(Object sender, T e);
}
