// Nguyen Thanh Dat - 202419040
package lab.events;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public final class EventPublisher<T> {
    private final CopyOnWriteArrayList<EventHandler<T>> handlers = new CopyOnWriteArrayList<>();
    private final Event<T> view = new View();
    private BiConsumer<EventHandler<T>, Throwable> errorReporter =
            (h, ex) -> System.err.println("[EventPublisher] Handler lỗi: " + ex);

    public Event<T> event() {
        return view;
    }

    public void setErrorReporter(BiConsumer<EventHandler<T>, Throwable> reporter) {
        this.errorReporter = Objects.requireNonNull(reporter);
    }

    public void raise(Object sender, T args) {
        for (EventHandler<T> h : handlers) {
            try {
                h.handle(sender, args);
            } catch (RuntimeException ex) {
                errorReporter.accept(h, ex);
            }
        }
    }

    private final class View implements Event<T> {
        @Override
        public void add(EventHandler<T> handler) {
            Objects.requireNonNull(handler, "handler");
            handlers.addIfAbsent(handler);
        }

        @Override
        public void remove(EventHandler<T> handler) {
            handlers.remove(handler);
        }

        @Override
        public int handlerCount() {
            return handlers.size();
        }
    }
}
