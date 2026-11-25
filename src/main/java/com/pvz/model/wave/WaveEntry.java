package com.pvz.model.wave;

/**
 * Represents a single zombie spawn instruction within a Wave.
 * Defines the zombie type, target lane, and delay (in seconds) from wave start.
 */
public class WaveEntry {
    private String zombieType;
    private int lane;
    private double spawnDelay;
    private boolean spawned;

    public WaveEntry(String zombieType, int lane, double spawnDelay) {
        this.zombieType = zombieType;
        this.lane = lane;
        this.spawnDelay = spawnDelay;
        this.spawned = false;
    }

    public String getZombieType() {
        return zombieType;
    }

    public void setZombieType(String zombieType) {
        this.zombieType = zombieType;
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public double getSpawnDelay() {
        return spawnDelay;
    }

    public void setSpawnDelay(double spawnDelay) {
        this.spawnDelay = spawnDelay;
    }

    public boolean isSpawned() {
        return spawned;
    }

    public void setSpawned(boolean spawned) {
        this.spawned = spawned;
    }

    public void reset() {
        this.spawned = false;
    }
}
