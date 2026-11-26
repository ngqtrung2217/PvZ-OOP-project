package com.pvz.system.event;

/**
 * Event published when an area-of-effect explosion (such as a 3x3 bomb) detonates.
 */
public final class BombExplosionEvent extends GameEvent {
    private final double x;
    private final double y;
    private final double radius;
    private final int damage;

    public BombExplosionEvent(double x, double y, double radius, int damage) {
        super();
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.damage = damage;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getRadius() {
        return radius;
    }

    public int getDamage() {
        return damage;
    }
}
