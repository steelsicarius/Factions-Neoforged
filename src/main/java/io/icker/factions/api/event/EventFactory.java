package io.icker.factions.api.event;

import java.lang.reflect.Array;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Minimal drop-in replacement for Fabric's {@code EventFactory.createArrayBacked}.
 *
 * <p>The invoker is rebuilt from the current listener list on every registration, so
 * calling the invoker never iterates over a collection — same array-backed semantics as
 * Fabric's implementation.
 */
public final class EventFactory {
    private EventFactory() {}

    public static <T> Event<T> createArrayBacked(
            Class<? super T> type, Function<T[], T> invokerFactory) {
        return new Event<T>() {
            private final List<T> listeners = new CopyOnWriteArrayList<>();
            private volatile T invoker = invokerFactory.apply(emptyArray(type));

            @Override
            public T invoker() {
                return invoker;
            }

            @Override
            public void register(T listener) {
                if (listener == null) {
                    throw new NullPointerException("Listener cannot be null");
                }
                listeners.add(listener);
                invoker = invokerFactory.apply(listeners.toArray(emptyArray(type)));
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] emptyArray(Class<? super T> type) {
        return (T[]) Array.newInstance(type, 0);
    }
}
