package com.pvz.system.event;

/**
 * Immutable base class for all combat and gameplay events dispatched through
 * {@link CombatEventBus}.
 */
public abstract class GameEvent {
    private final long timestamp;

    protected GameEvent() {
        this(System.currentTimeMillis());
    }

    protected GameEvent(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the epoch time in milliseconds when this event was created.
     */
    public long getTimestamp() {
        return timestamp;
    }
}
