package com.pvz.builder;

/**
 * One zombie to spawn in a wave.
 * <p>
 * Immutable, unlike the {@code WaveEntry} made from it, which tracks whether it has spawned.
 * Lane bounds and whether the zombie type exists are checked by the map builder.
 */
public final class SpawnConfig {
    private final String zombieType;
    private final int lane; // lane: 0-4
    private final double spawnDelay; // seconds from wave start

    public SpawnConfig(String zombieType, int lane, double spawnDelay) {
        if (zombieType == null || zombieType.isBlank()) {
            throw new IllegalArgumentException("Zombie type cannot be blank");
        }
        // A NaN or infinite delay would never elapse, so the wave would never end
        if (!Double.isFinite(spawnDelay) || spawnDelay < 0) {
            throw new IllegalArgumentException("Spawn delay must be finite and non-negative");
        }
        this.zombieType = zombieType;
        this.lane = lane;
        this.spawnDelay = spawnDelay;
    }

    // Getters
    public String getZombieType() {
        return zombieType;
    }

    public int getLane() {
        return lane;
    }

    public double getSpawnDelay() {
        return spawnDelay;
    }
}
