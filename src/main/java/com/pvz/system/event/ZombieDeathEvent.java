package com.pvz.system.event;

import com.pvz.model.zombie.Zombie;

/**
 * Event published when a zombie is defeated, drowned, or mowed down.
 */
public final class ZombieDeathEvent extends GameEvent {

    public enum DeathCause {
        KILLED,
        DROWNED,
        MOWED
    }

    private final Zombie zombie;
    private final double x;
    private final double y;
    private final int lane;
    private final DeathCause cause;

    public ZombieDeathEvent(Zombie zombie, double x, double y, int lane, DeathCause cause) {
        super();
        this.zombie = zombie;
        this.x = x;
        this.y = y;
        this.lane = lane;
        this.cause = cause != null ? cause : DeathCause.KILLED;
    }

    public ZombieDeathEvent(Zombie zombie, double x, double y, int lane) {
        this(zombie, x, y, lane, DeathCause.KILLED);
    }

    public ZombieDeathEvent(Zombie zombie, DeathCause cause) {
        this(
                zombie,
                zombie != null ? zombie.getX() : 0.0,
                zombie != null ? zombie.getY() : 0.0,
                zombie != null ? zombie.getLane() : -1,
                cause
        );
    }

    public ZombieDeathEvent(Zombie zombie) {
        this(zombie, DeathCause.KILLED);
    }

    public Zombie getZombie() {
        return zombie;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getLane() {
        return lane;
    }

    public DeathCause getCause() {
        return cause;
    }
}
