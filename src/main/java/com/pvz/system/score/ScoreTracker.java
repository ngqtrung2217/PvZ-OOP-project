package com.pvz.system.score;

import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.CombatListener;
import com.pvz.system.event.ZombieDeathEvent;

/**
 * Observer that counts zombie deaths into {@link RunStats}. It only listens to the event
 * bus and never calls the combat code directly (README rule 2).
 */
public final class ScoreTracker implements CombatListener<ZombieDeathEvent> {
    private final RunStats stats;

    public ScoreTracker(RunStats stats) {
        if (stats == null) {
            throw new IllegalArgumentException("Stats cannot be null");
        }
        this.stats = stats;
    }

    /** Starts listening. Calling it again is harmless: the bus ignores duplicates. */
    public void attach(CombatEventBus bus) {
        bus.subscribe(ZombieDeathEvent.class, this);
    }

    public void detach(CombatEventBus bus) {
        bus.unsubscribe(ZombieDeathEvent.class, this);
    }

    @Override
    public void onEvent(ZombieDeathEvent event) {
        String type = event.getZombie() != null ? event.getZombie().getType() : "UNKNOWN";
        stats.recordDeath(type, event.getCause());
    }

    public RunStats getStats() {
        return stats;
    }
}
