package com.pvz.system.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central Publish-Subscribe event bus implementing the Observer Pattern.
 * Decouples combat logic from audio, visual effects, and entity reactions.
 */
public final class CombatEventBus {
    private static final CombatEventBus INSTANCE = new CombatEventBus();

    private final Map<Class<? extends GameEvent>, List<CombatListener<?>>> listeners = new HashMap<>();

    private CombatEventBus() {
    }

    public static CombatEventBus getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a listener for a specific event type.
     * Duplicate registrations for the same event type and listener are ignored.
     */
    public <T extends GameEvent> void subscribe(Class<T> eventType, CombatListener<T> listener) {
        if (eventType == null || listener == null) {
            return;
        }
        List<CombatListener<?>> eventListeners =
                listeners.computeIfAbsent(eventType, key -> new ArrayList<>());
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    /**
     * Unregisters a previously registered listener for a specific event type.
     */
    public <T extends GameEvent> void unsubscribe(Class<T> eventType, CombatListener<T> listener) {
        if (eventType == null || listener == null) {
            return;
        }
        List<CombatListener<?>> eventListeners = listeners.get(eventType);
        if (eventListeners == null) {
            return;
        }
        eventListeners.remove(listener);
        if (eventListeners.isEmpty()) {
            listeners.remove(eventType);
        }
    }

    /**
     * Dispatches an event to all listeners subscribed to its exact runtime type.
     * Iterates over a defensive copy so listeners may safely subscribe or unsubscribe
     * during notification without causing {@link java.util.ConcurrentModificationException}.
     */
    @SuppressWarnings("unchecked")
    public <T extends GameEvent> void publish(T event) {
        if (event == null) {
            return;
        }
        List<CombatListener<?>> registered = listeners.get(event.getClass());
        if (registered == null || registered.isEmpty()) {
            return;
        }
        List<CombatListener<?>> snapshot = new ArrayList<>(registered);
        for (CombatListener<?> rawListener : snapshot) {
            ((CombatListener<T>) rawListener).onEvent(event);
        }
    }

    /**
     * Removes all registered listeners across all event types.
     */
    public void clear() {
        listeners.clear();
    }

    /**
     * Returns the number of currently registered listeners for the given event type.
     */
    public int getListenerCount(Class<? extends GameEvent> eventType) {
        if (eventType == null) {
            return 0;
        }
        List<CombatListener<?>> registered = listeners.get(eventType);
        return registered == null ? 0 : registered.size();
    }
}
