package com.pvz.model.wave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a wave of zombie spawns.
 * Tracks all spawn entries and whether this wave is flagged as a Huge Wave.
 */
public class Wave {
    private final List<WaveEntry> entries;
    private boolean isHugeWave;

    public Wave() {
        this(new ArrayList<>(), false);
    }

    public Wave(boolean isHugeWave) {
        this(new ArrayList<>(), isHugeWave);
    }

    public Wave(List<WaveEntry> entries, boolean isHugeWave) {
        this.entries = entries != null ? entries : new ArrayList<>();
        this.isHugeWave = isHugeWave;
    }

    public void addEntry(WaveEntry entry) {
        if (entry != null) {
            entries.add(entry);
        }
    }

    public void addEntry(String zombieType, int lane, double spawnDelay) {
        entries.add(new WaveEntry(zombieType, lane, spawnDelay));
    }

    public List<WaveEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public boolean isHugeWave() {
        return isHugeWave;
    }

    public void setHugeWave(boolean hugeWave) {
        isHugeWave = hugeWave;
    }

    /**
     * Checks if all zombie entries within this wave have been spawned.
     */
    public boolean isAllSpawned() {
        for (WaveEntry entry : entries) {
            if (!entry.isSpawned()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resets all entries so this wave can be replayed if needed.
     */
    public void reset() {
        for (WaveEntry entry : entries) {
            entry.reset();
        }
    }
}
