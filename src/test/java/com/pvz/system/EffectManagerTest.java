package com.pvz.system;

import com.pvz.model.zombie.NormalZombie;
import com.pvz.system.effect.EffectManager;
import com.pvz.system.effect.ParticleEffect;
import com.pvz.system.event.BombExplosionEvent;
import com.pvz.system.event.BulletHitEvent;
import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.ZombieDeathEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless unit tests for {@link EffectManager} and {@link ParticleEffect}.
 */
class EffectManagerTest {

    private EffectManager effectManager;
    private CombatEventBus eventBus;

    @BeforeEach
    void setUp() {
        effectManager = new EffectManager();
        eventBus = CombatEventBus.getInstance();
        eventBus.clear();
    }

    @AfterEach
    void tearDown() {
        effectManager.unregisterListeners(eventBus);
        effectManager.clear();
        eventBus.clear();
    }

    @Test
    @DisplayName("spawnEffect adds effects and update(deltaTime) removes them once finished")
    void testSpawnAndAutoExpire() {
        ParticleEffect shortEffect = new ParticleEffect(100, 150, 0.20, 20, ParticleEffect.EffectType.PEA_SPLAT);
        ParticleEffect longEffect = new ParticleEffect(200, 250, 0.60, 50, ParticleEffect.EffectType.EXPLOSION);

        effectManager.spawnEffect(null);
        effectManager.spawnEffect(shortEffect);
        effectManager.spawnEffect(longEffect);
        assertEquals(2, effectManager.getActiveEffectCount());

        effectManager.update(0.25);
        assertTrue(shortEffect.isFinished());
        assertFalse(longEffect.isFinished());
        assertEquals(1, effectManager.getActiveEffectCount());

        effectManager.update(0.40);
        assertTrue(longEffect.isFinished());
        assertEquals(0, effectManager.getActiveEffectCount());
    }

    @Test
    @DisplayName("render(null) and clear() operate safely without exceptions")
    void testRenderNullAndClear() {
        effectManager.spawnExplosion(300, 200, 100);
        effectManager.spawnHitEffect(350, 220, true);
        assertEquals(2, effectManager.getActiveEffectCount());

        assertDoesNotThrow(() -> effectManager.render(null));

        effectManager.clear();
        assertEquals(0, effectManager.getActiveEffectCount());
    }

    @Test
    @DisplayName("CombatEventBus events automatically spawn corresponding particle effects")
    void testCombatEventBusSpawnsEffects() {
        effectManager.registerListeners(eventBus);

        eventBus.publish(new BulletHitEvent(null, null, 20, false, 400.0, 200.0));
        assertEquals(1, effectManager.getActiveEffectCount());

        eventBus.publish(new BulletHitEvent(null, null, 20, true, 420.0, 200.0));
        assertEquals(2, effectManager.getActiveEffectCount());

        eventBus.publish(new BombExplosionEvent(500.0, 300.0, 120.0, 1800));
        assertEquals(3, effectManager.getActiveEffectCount());

        eventBus.publish(new ZombieDeathEvent(new NormalZombie(550.0, 300.0, 2, null)));
        assertEquals(4, effectManager.getActiveEffectCount());

        effectManager.unregisterListeners(eventBus);
        effectManager.clear();

        eventBus.publish(new BombExplosionEvent(500.0, 300.0, 120.0, 1800));
        assertEquals(0, effectManager.getActiveEffectCount(),
                "Unregistered EffectManager should not spawn new effects");
    }
}
