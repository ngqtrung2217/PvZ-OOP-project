package com.pvz.builder;

import java.util.List;

import com.pvz.model.wave.Wave;

/**
 * Immutable description of a wave. A {@link Wave} changes while it is played, so a map
 * stores this instead and creates new waves for each session.
 */
public final class WaveConfig {
    private final List<SpawnConfig> spawns;
    private final boolean hugeWave;

    public WaveConfig(List<SpawnConfig> spawns, boolean hugeWave) {
        if (spawns == null || spawns.isEmpty()) {
            throw new IllegalArgumentException("Wave must spawn at least one zombie");
        }
        this.spawns = List.copyOf(spawns);
        this.hugeWave = hugeWave;
    }

    /** Creates a new wave with nothing spawned yet. */
    Wave createWave() {
        Wave wave = new Wave(hugeWave);
        for (SpawnConfig spawn : spawns) {
            wave.addEntry(spawn.getZombieType(), spawn.getLane(), spawn.getSpawnDelay());
        }
        return wave;
    }

    // Getters
    public List<SpawnConfig> getSpawns() {
        return spawns;
    }

    public boolean isHugeWave() {
        return hugeWave;
    }
}
