package com.pvz.system;

import com.pvz.model.zombie.NormalZombie;
import com.pvz.model.zombie.Zombie;
import com.pvz.system.event.BombExplosionEvent;
import com.pvz.system.event.BulletHitEvent;
import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.CombatListener;
import com.pvz.system.event.PlantEatenEvent;
import com.pvz.system.event.SunCollectedEvent;
import com.pvz.system.event.ZombieDeathEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CombatEventBus} and concrete {@link com.pvz.system.event.GameEvent} types.
 */
class CombatEventBusTest {

    private CombatEventBus bus;

    @BeforeEach
    void setUp() {
        bus = CombatEventBus.getInstance();
        bus.clear();
    }

    @AfterEach
    void tearDown() {
        bus.clear();
    }

    @Test
    @DisplayName("Singleton returns the same CombatEventBus instance")
    void testSingletonInstance() {
        assertSame(bus, CombatEventBus.getInstance());
    }

    @Test
    @DisplayName("Subscribed listener receives published event of matching type with payload intact")
    void testSubscribeAndPublishMatchingEvent() {
        Zombie zombie = new NormalZombie(500.0, 200.0, 1, null);
        AtomicReference<BulletHitEvent> received = new AtomicReference<>();

        bus.subscribe(BulletHitEvent.class, received::set);
        BulletHitEvent published = new BulletHitEvent(null, zombie, 20, true, 510.0, 220.0);
        bus.publish(published);

        assertSame(published, received.get());
        assertSame(zombie, received.get().getTarget());
        assertEquals(20, received.get().getDamage());
        assertTrue(received.get().hasSlowEffect());
        assertEquals(510.0, received.get().getX());
        assertEquals(220.0, received.get().getY());
        assertTrue(received.get().getTimestamp() > 0);
    }

    @Test
    @DisplayName("Listener does not receive events of a different type")
    void testDoesNotReceiveOtherEventTypes() {
        AtomicInteger deathCount = new AtomicInteger(0);
        AtomicInteger sunTotal = new AtomicInteger(0);

        bus.subscribe(ZombieDeathEvent.class, e -> deathCount.incrementAndGet());
        bus.subscribe(SunCollectedEvent.class, e -> sunTotal.addAndGet(e.getAmount()));

        bus.publish(new SunCollectedEvent(25, 300.0, 150.0));
        bus.publish(new BombExplosionEvent(400.0, 250.0, 120.0, 1800));
        bus.publish(new PlantEatenEvent(2, 4));

        assertEquals(0, deathCount.get(), "ZombieDeathEvent listener must ignore unrelated events");
        assertEquals(25, sunTotal.get());
    }

    @Test
    @DisplayName("Unsubscribed listener stops receiving subsequent events")
    void testUnsubscribe() {
        AtomicInteger count = new AtomicInteger(0);
        CombatListener<SunCollectedEvent> listener = e -> count.incrementAndGet();

        bus.subscribe(SunCollectedEvent.class, listener);
        bus.publish(new SunCollectedEvent(25, 100.0, 100.0));
        assertEquals(1, count.get());

        bus.unsubscribe(SunCollectedEvent.class, listener);
        bus.publish(new SunCollectedEvent(25, 100.0, 100.0));
        assertEquals(1, count.get(), "Unsubscribed listener should not be notified");
        assertEquals(0, bus.getListenerCount(SunCollectedEvent.class));
    }

    @Test
    @DisplayName("clear() removes all listeners across all event types")
    void testClearRemovesAllListeners() {
        AtomicInteger calls = new AtomicInteger(0);
        bus.subscribe(ZombieDeathEvent.class, e -> calls.incrementAndGet());
        bus.subscribe(BombExplosionEvent.class, e -> calls.incrementAndGet());
        bus.subscribe(PlantEatenEvent.class, e -> calls.incrementAndGet());

        bus.clear();

        bus.publish(new ZombieDeathEvent(new NormalZombie(300.0, 100.0, 0, null)));
        bus.publish(new BombExplosionEvent(300.0, 100.0, 100.0, 1800));
        bus.publish(new PlantEatenEvent(0, 1));

        assertEquals(0, calls.get());
        assertEquals(0, bus.getListenerCount(ZombieDeathEvent.class));
        assertEquals(0, bus.getListenerCount(BombExplosionEvent.class));
        assertEquals(0, bus.getListenerCount(PlantEatenEvent.class));
    }

    @Test
    @DisplayName("Unsubscribing inside a callback does not throw ConcurrentModificationException")
    void testSelfUnsubscribeDuringPublishIsSafe() {
        List<String> order = new ArrayList<>();
        AtomicReference<CombatListener<BombExplosionEvent>> selfRemovingRef = new AtomicReference<>();

        CombatListener<BombExplosionEvent> first = event -> {
            order.add("first");
            bus.unsubscribe(BombExplosionEvent.class, selfRemovingRef.get());
        };
        selfRemovingRef.set(first);

        CombatListener<BombExplosionEvent> second = event -> order.add("second");

        bus.subscribe(BombExplosionEvent.class, first);
        bus.subscribe(BombExplosionEvent.class, second);

        assertDoesNotThrow(() -> bus.publish(new BombExplosionEvent(250.0, 250.0, 120.0, 1800)));
        assertEquals(List.of("first", "second"), order);

        // Second publish should only invoke the remaining listener
        order.clear();
        bus.publish(new BombExplosionEvent(250.0, 250.0, 120.0, 1800));
        assertEquals(List.of("second"), order);
    }

    @Test
    @DisplayName("Duplicate subscriptions and null arguments are handled safely")
    void testDuplicateSubscriptionAndNullSafety() {
        AtomicInteger calls = new AtomicInteger(0);
        CombatListener<PlantEatenEvent> listener = e -> calls.incrementAndGet();

        bus.subscribe(PlantEatenEvent.class, listener);
        bus.subscribe(PlantEatenEvent.class, listener);
        bus.subscribe(null, listener);
        bus.subscribe(PlantEatenEvent.class, null);

        assertDoesNotThrow(() -> bus.publish(null));
        bus.publish(new PlantEatenEvent(1, 2));

        assertEquals(1, calls.get(), "Duplicate listener registration should only fire once");
    }

    @Test
    @DisplayName("ZombieDeathEvent records DeathCause (KILLED, DROWNED, MOWED) and defaults null to KILLED")
    void testZombieDeathEventCauses() {
        Zombie zombie = new NormalZombie(450.0, 200.0, 2, null);
        List<ZombieDeathEvent.DeathCause> causes = new ArrayList<>();
        bus.subscribe(ZombieDeathEvent.class, e -> causes.add(e.getCause()));

        bus.publish(new ZombieDeathEvent(zombie));
        bus.publish(new ZombieDeathEvent(zombie, ZombieDeathEvent.DeathCause.DROWNED));
        bus.publish(new ZombieDeathEvent(zombie, 450.0, 200.0, 2, ZombieDeathEvent.DeathCause.MOWED));
        bus.publish(new ZombieDeathEvent(zombie, (ZombieDeathEvent.DeathCause) null));

        assertEquals(List.of(
                ZombieDeathEvent.DeathCause.KILLED,
                ZombieDeathEvent.DeathCause.DROWNED,
                ZombieDeathEvent.DeathCause.MOWED,
                ZombieDeathEvent.DeathCause.KILLED
        ), causes);
    }
}
