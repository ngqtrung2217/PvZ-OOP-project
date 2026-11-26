package com.pvz.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pvz.model.zombie.Zombie;
import com.pvz.model.zombie.ZombieFactory;
import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.ZombieDeathEvent;
import com.pvz.system.event.ZombieDeathEvent.DeathCause;
import com.pvz.system.score.RunStats;
import com.pvz.system.score.ScoreTracker;

/**
 * Unit tests for ScoreTracker: it counts deaths from the event bus only while attached.
 */
class ScoreTrackerTest {
    private CombatEventBus bus;
    private RunStats stats;
    private ScoreTracker tracker;

    @BeforeEach
    void setUp() {
        bus = CombatEventBus.getInstance();
        bus.clear();
        stats = new RunStats();
        tracker = new ScoreTracker(stats);
    }

    @AfterEach
    void tearDown() {
        bus.clear();
    }

    private static Zombie zombie(String type) {
        return ZombieFactory.createZombie(type, 0, null);
    }

    @Test
    @DisplayName("Zombies from the factory know their type")
    void testZombieType() {
        assertEquals("BUCKETHEAD", zombie("bucketHead").getType());
    }

    @Test
    @DisplayName("Deaths are counted by type and cause while attached")
    void testCountsWhileAttached() {
        tracker.attach(bus);
        tracker.attach(bus); // a second attach must not double-count

        bus.publish(new ZombieDeathEvent(zombie("NORMAL"), DeathCause.KILLED));
        bus.publish(new ZombieDeathEvent(zombie("CONEHEAD"), DeathCause.MOWED));
        bus.publish(new ZombieDeathEvent(zombie("NORMAL"), DeathCause.DROWNED));

        assertEquals(2, stats.getKillCount());
        assertEquals(1, stats.getKills().get("NORMAL"));
        assertEquals(1, stats.getKills().get("CONEHEAD"));
        assertEquals(1, stats.getDrownedCount());
    }

    @Test
    @DisplayName("Nothing is counted after detaching, as during a pause")
    void testDetach() {
        tracker.attach(bus);
        tracker.detach(bus);

        bus.publish(new ZombieDeathEvent(zombie("NORMAL")));

        assertEquals(0, stats.getKillCount());
    }
}
