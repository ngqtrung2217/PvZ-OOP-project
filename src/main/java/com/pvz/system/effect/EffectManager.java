package com.pvz.system.effect;

import com.pvz.system.event.BombExplosionEvent;
import com.pvz.system.event.BulletHitEvent;
import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.CombatListener;
import com.pvz.system.event.ZombieDeathEvent;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages the lifecycle and rendering of all active {@link ParticleEffect}s.
 * Can subscribe to {@link CombatEventBus} to automatically spawn VFX in response
 * to combat events.
 */
public class EffectManager {
    private static final double DEFAULT_EXPLOSION_DURATION = 0.50;
    private static final double DEFAULT_HIT_DURATION = 0.25;
    private static final double DEFAULT_SLOW_DURATION = 0.35;
    private static final double DEFAULT_DEATH_DURATION = 0.45;

    private final List<ParticleEffect> activeEffects = new ArrayList<>();

    private final CombatListener<BulletHitEvent> bulletHitListener =
            event -> spawnHitEffect(event.getX(), event.getY(), event.hasSlowEffect());

    private final CombatListener<BombExplosionEvent> bombExplosionListener =
            event -> spawnExplosion(event.getX(), event.getY(), event.getRadius());

    private final CombatListener<ZombieDeathEvent> zombieDeathListener =
            event -> spawnZombieDeath(event.getX(), event.getY());

    /**
     * Adds a custom {@link ParticleEffect} to the active effect list.
     */
    public void spawnEffect(ParticleEffect effect) {
        if (effect != null) {
            activeEffects.add(effect);
        }
    }

    /**
     * Spawns an area-of-effect explosion particle effect.
     */
    public void spawnExplosion(double x, double y, double radius) {
        spawnEffect(new ParticleEffect(
                x, y, DEFAULT_EXPLOSION_DURATION, radius, ParticleEffect.EffectType.EXPLOSION
        ));
    }

    /**
     * Spawns a projectile impact effect (standard pea splat or frost slow).
     */
    public void spawnHitEffect(double x, double y, boolean isSlow) {
        if (isSlow) {
            spawnEffect(new ParticleEffect(
                    x, y, DEFAULT_SLOW_DURATION, 24.0, ParticleEffect.EffectType.ICE_SLOW
            ));
        } else {
            spawnEffect(new ParticleEffect(
                    x, y, DEFAULT_HIT_DURATION, 16.0, ParticleEffect.EffectType.PEA_SPLAT
            ));
        }
    }

    /**
     * Spawns a defeat puff effect when a zombie dies.
     */
    public void spawnZombieDeath(double x, double y) {
        spawnEffect(new ParticleEffect(
                x, y, DEFAULT_DEATH_DURATION, 30.0, ParticleEffect.EffectType.ZOMBIE_DEATH
        ));
    }

    /**
     * Updates all active effects and safely removes finished ones using an {@link Iterator}.
     */
    public void update(double deltaTime) {
        Iterator<ParticleEffect> iterator = activeEffects.iterator();
        while (iterator.hasNext()) {
            ParticleEffect effect = iterator.next();
            effect.update(deltaTime);
            if (effect.isFinished()) {
                iterator.remove();
            }
        }
    }

    /**
     * Renders all active effects onto the canvas. Safely no-ops if {@code gc} is {@code null}.
     */
    public void render(GraphicsContext gc) {
        if (gc == null) {
            return;
        }
        for (ParticleEffect effect : activeEffects) {
            effect.render(gc);
        }
    }

    /**
     * Clears all active effects.
     */
    public void clear() {
        activeEffects.clear();
    }

    public int getActiveEffectCount() {
        return activeEffects.size();
    }

    /**
     * Subscribes this manager to combat events on the provided bus.
     */
    public void registerListeners(CombatEventBus bus) {
        if (bus == null) {
            return;
        }
        bus.subscribe(BulletHitEvent.class, bulletHitListener);
        bus.subscribe(BombExplosionEvent.class, bombExplosionListener);
        bus.subscribe(ZombieDeathEvent.class, zombieDeathListener);
    }

    /**
     * Unsubscribes this manager from combat events on the provided bus.
     */
    public void unregisterListeners(CombatEventBus bus) {
        if (bus == null) {
            return;
        }
        bus.unsubscribe(BulletHitEvent.class, bulletHitListener);
        bus.unsubscribe(BombExplosionEvent.class, bombExplosionListener);
        bus.unsubscribe(ZombieDeathEvent.class, zombieDeathListener);
    }
}
