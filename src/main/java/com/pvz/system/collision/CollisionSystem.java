package com.pvz.system.collision;

import com.pvz.model.base.Entity;
import com.pvz.model.projectile.Projectile;
import com.pvz.model.zombie.Zombie;
import com.pvz.model.zombie.state.SwimmingState;
import com.pvz.model.zombie.state.WalkingState;
import com.pvz.model.zombie.state.ZombieState;
import com.pvz.system.defense.LawnMower;
import com.pvz.system.event.BulletHitEvent;
import com.pvz.system.event.CombatEventBus;
import com.pvz.system.event.ZombieDeathEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Lane-based AABB collision detection and combat resolution system.
 * <p>
 * Scans only entities sharing the same lane to maintain performance even during
 * high-density endless waves. Publishes combat events through {@link CombatEventBus}.
 */
public class CollisionSystem {

    private final CombatEventBus eventBus;

    public CollisionSystem() {
        this(CombatEventBus.getInstance());
    }

    public CollisionSystem(CombatEventBus eventBus) {
        this.eventBus = eventBus != null ? eventBus : CombatEventBus.getInstance();
    }

    /**
     * Checks projectile-to-zombie collisions restricted to matching lanes.
     * On impact, applies projectile effects, deactivates the projectile, publishes
     * {@link BulletHitEvent}, and publishes {@link ZombieDeathEvent} (KILLED) once
     * if the zombie dies from the hit.
     */
    public void checkProjectileZombieCollisions(List<Projectile> projectiles, List<Zombie> zombies) {
        if (projectiles == null || projectiles.isEmpty() || zombies == null || zombies.isEmpty()) {
            return;
        }

        Map<Integer, List<Zombie>> livingZombiesByLane = groupLivingZombiesByLane(zombies);
        if (livingZombiesByLane.isEmpty()) {
            return;
        }

        List<Projectile> projectileSnapshot = new ArrayList<>(projectiles);
        for (Projectile projectile : projectileSnapshot) {
            if (projectile == null || !projectile.isActive()) {
                continue;
            }

            List<Zombie> laneZombies = livingZombiesByLane.get(projectile.getLane());
            if (laneZombies == null || laneZombies.isEmpty()) {
                continue;
            }

            for (Zombie zombie : laneZombies) {
                if (zombie == null || !zombie.isAlive()) {
                    continue;
                }
                if (Hitbox.intersects(projectile, zombie)) {
                    boolean wasAlive = zombie.isAlive();
                    projectile.onHit(zombie);
                    projectile.deactivate();

                    eventBus.publish(new BulletHitEvent(
                            projectile,
                            zombie,
                            projectile.getDamage(),
                            projectile.hasSlowEffect(),
                            projectile.getX(),
                            projectile.getY()
                    ));

                    if (wasAlive && !zombie.isAlive()) {
                        eventBus.publish(new ZombieDeathEvent(zombie, ZombieDeathEvent.DeathCause.KILLED));
                    }
                    break;
                }
            }
        }
    }

    /**
     * Checks collisions between moving zombies ({@link WalkingState} or {@link SwimmingState})
     * and lane-indexed obstacle/plant entities.
     */
    public void checkZombieEntityCollisions(
            List<Zombie> zombies,
            Map<Integer, ? extends List<? extends Entity>> entitiesByLane,
            Predicate<Entity> tallObstaclePredicate) {
        if (zombies == null || zombies.isEmpty() || entitiesByLane == null || entitiesByLane.isEmpty()) {
            return;
        }

        List<Zombie> zombieSnapshot = new ArrayList<>(zombies);
        for (Zombie zombie : zombieSnapshot) {
            if (!isMovingZombie(zombie)) {
                continue;
            }

            List<? extends Entity> laneEntities = entitiesByLane.get(zombie.getLane());
            if (laneEntities == null || laneEntities.isEmpty()) {
                continue;
            }

            for (Entity target : laneEntities) {
                if (target == null || !target.isAlive()) {
                    continue;
                }
                if (Hitbox.intersects(zombie, target)) {
                    boolean isTall = tallObstaclePredicate != null && tallObstaclePredicate.test(target);
                    zombie.onCollideWithTarget(target, isTall);
                    break;
                }
            }
        }
    }

    /**
     * Convenience overload assuming no obstacles are tall.
     */
    public void checkZombieEntityCollisions(
            List<Zombie> zombies,
            Map<Integer, ? extends List<? extends Entity>> entitiesByLane) {
        checkZombieEntityCollisions(zombies, entitiesByLane, null);
    }

    /**
     * Convenience method for checking collisions between zombies and targets within a single lane.
     */
    public void checkZombieEntityCollisionsInLane(
            List<Zombie> zombies,
            List<? extends Entity> targets,
            int lane,
            boolean isTallObstacle) {
        if (zombies == null || zombies.isEmpty() || targets == null || targets.isEmpty()) {
            return;
        }
        Map<Integer, List<? extends Entity>> map = Map.of(lane, targets);
        checkZombieEntityCollisions(zombies, map, entity -> isTallObstacle);
    }

    /**
     * Checks collisions between {@link LawnMower}s and {@link Zombie}s sharing the same lane.
     * <p>
     * When a living zombie intersects a {@code READY} mower, the mower activates immediately.
     * Any living zombie intersecting an {@code ACTIVE} mower is instantly eliminated
     * (bypassing armor via {@link Integer#MAX_VALUE} damage) and triggers a single
     * {@link ZombieDeathEvent} with {@link ZombieDeathEvent.DeathCause#MOWED}.
     */
    public void checkLawnMowerZombieCollisions(List<LawnMower> mowers, List<Zombie> zombies) {
        if (mowers == null || mowers.isEmpty() || zombies == null || zombies.isEmpty()) {
            return;
        }

        Map<Integer, List<Zombie>> livingZombiesByLane = groupLivingZombiesByLane(zombies);
        if (livingZombiesByLane.isEmpty()) {
            return;
        }

        List<LawnMower> mowerSnapshot = new ArrayList<>(mowers);
        for (LawnMower mower : mowerSnapshot) {
            if (mower == null || mower.isUsed()) {
                continue;
            }

            List<Zombie> laneZombies = livingZombiesByLane.get(mower.getLane());
            if (laneZombies == null || laneZombies.isEmpty()) {
                continue;
            }

            for (Zombie zombie : laneZombies) {
                if (zombie == null || !zombie.isAlive()) {
                    continue;
                }
                if (Hitbox.intersects(mower, zombie)) {
                    if (mower.isReady()) {
                        mower.activate();
                    }
                    if (mower.isActive()) {
                        boolean wasAlive = zombie.isAlive();
                        zombie.takeDamage(Integer.MAX_VALUE);
                        if (wasAlive && !zombie.isAlive()) {
                            eventBus.publish(new ZombieDeathEvent(zombie, ZombieDeathEvent.DeathCause.MOWED));
                        }
                    }
                }
            }
        }
    }

    private Map<Integer, List<Zombie>> groupLivingZombiesByLane(List<Zombie> zombies) {
        Map<Integer, List<Zombie>> byLane = new HashMap<>();
        for (Zombie zombie : zombies) {
            if (zombie != null && zombie.isAlive()) {
                byLane.computeIfAbsent(zombie.getLane(), key -> new ArrayList<>()).add(zombie);
            }
        }
        return byLane;
    }

    private boolean isMovingZombie(Zombie zombie) {
        if (zombie == null || !zombie.isAlive()) {
            return false;
        }
        ZombieState state = zombie.getCurrentState();
        return state instanceof WalkingState || state instanceof SwimmingState;
    }
}
