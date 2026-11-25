package com.pvz.system.event;

/**
 * Functional observer interface for receiving combat events of type {@code T}.
 *
 * @param <T> The concrete {@link GameEvent} subtype handled by this listener.
 */
@FunctionalInterface
public interface CombatListener<T extends GameEvent> {
    /**
     * Invoked when an event of type {@code T} is published on {@link CombatEventBus}.
     *
     * @param event The published event instance.
     */
    void onEvent(T event);
}
