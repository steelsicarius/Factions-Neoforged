package io.icker.factions.api.event;

/**
 * Minimal drop-in replacement for Fabric's {@code net.fabricmc.fabric.api.event.Event}.
 *
 * <p>Keeps the exact same usage pattern as the original Fabric mod ({@link #invoker()} /
 * {@link #register(Object)}), so the event definitions in {@code io.icker.factions.api.events}
 * only needed their imports swapped.
 */
public interface Event<T> {
    /** Returns the composite invoker that calls all registered listeners in order. */
    T invoker();

    /** Registers a listener. No-op if already registered (mirrors Fabric's behaviour loosely). */
    void register(T listener);
}
