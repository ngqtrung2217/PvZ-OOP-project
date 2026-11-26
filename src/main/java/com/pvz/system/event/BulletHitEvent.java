package com.pvz.system.event;

import com.pvz.model.base.GameObject;
import com.pvz.model.zombie.Zombie;

/**
 * Event published when a projectile collides with a zombie target.
 */
public final class BulletHitEvent extends GameEvent {
    private final GameObject projectile;
    private final Zombie target;
    private final int damage;
    private final boolean slowEffect;
    private final double x;
    private final double y;

    public BulletHitEvent(GameObject projectile, Zombie target, int damage,
                          boolean slowEffect, double x, double y) {
        super();
        this.projectile = projectile;
        this.target = target;
        this.damage = damage;
        this.slowEffect = slowEffect;
        this.x = x;
        this.y = y;
    }

    public BulletHitEvent(GameObject projectile, Zombie target, int damage, boolean slowEffect) {
        this(
                projectile,
                target,
                damage,
                slowEffect,
                resolveX(projectile, target),
                resolveY(projectile, target)
        );
    }

    private static double resolveX(GameObject projectile, Zombie target) {
        if (projectile != null) {
            return projectile.getX();
        }
        return target != null ? target.getX() : 0.0;
    }

    private static double resolveY(GameObject projectile, Zombie target) {
        if (projectile != null) {
            return projectile.getY();
        }
        return target != null ? target.getY() : 0.0;
    }

    public GameObject getProjectile() {
        return projectile;
    }

    public Zombie getTarget() {
        return target;
    }

    public int getDamage() {
        return damage;
    }

    public boolean hasSlowEffect() {
        return slowEffect;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
