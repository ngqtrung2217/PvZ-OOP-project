package com.pvz.model.wave;

import com.pvz.model.board.GameBoard;
import com.pvz.model.zombie.Zombie;
import com.pvz.model.zombie.ZombieFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Campaign spawn strategy that dispatches zombies according to a scripted list of {@link Wave}s.
 */
public class ScriptedSpawnPolicy implements SpawnPolicy {
    private final List<Wave> waves;
    private int currentWaveIndex;
    private double waveElapsedTime;
    private boolean hugeWaveFlag;

    public ScriptedSpawnPolicy(List<Wave> waves) {
        this.waves = waves != null ? new ArrayList<>(waves) : new ArrayList<>();
        this.currentWaveIndex = 0;
        this.waveElapsedTime = 0.0;
        this.hugeWaveFlag = false;
    }

    @Override
    public List<Zombie> update(double deltaTime, GameBoard board, List<Zombie> activeZombies, ZombieFactory factory) {
        List<Zombie> spawned = new ArrayList<>();
        if (currentWaveIndex < waves.size()) {
            Wave currentWave = waves.get(currentWaveIndex);
            hugeWaveFlag = currentWave.isHugeWave();
            waveElapsedTime += deltaTime;

            for (WaveEntry entry : currentWave.getEntries()) {
                if (!entry.isSpawned() && waveElapsedTime >= entry.getSpawnDelay()) {
                    Zombie zombie = factory.create(entry.getZombieType(), entry.getLane(), board);
                    spawned.add(zombie);
                    entry.setSpawned(true);
                }
            }
        }
        return spawned;
    }

    @Override
    public void afterPrune(List<Zombie> activeZombies) {
        if (currentWaveIndex < waves.size()) {
            Wave currentWave = waves.get(currentWaveIndex);
            if (currentWave.isAllSpawned() && activeZombies.isEmpty()) {
                currentWaveIndex++;
                waveElapsedTime = 0.0;
                hugeWaveFlag = currentWaveIndex < waves.size() && waves.get(currentWaveIndex).isHugeWave();
            }
        }
    }

    @Override
    public int getCurrentWaveIndex() {
        return currentWaveIndex;
    }

    @Override
    public int getTotalWaves() {
        return waves.size();
    }

    @Override
    public boolean isHugeWaveApproaching() {
        if (currentWaveIndex < waves.size()) {
            return waves.get(currentWaveIndex).isHugeWave();
        }
        return hugeWaveFlag;
    }

    @Override
    public boolean isAllWavesCleared(List<Zombie> activeZombies) {
        return currentWaveIndex >= waves.size() && activeZombies.isEmpty();
    }

    @Override
    public double getProgressRatio(List<Zombie> activeZombies) {
        if (waves.isEmpty() || isAllWavesCleared(activeZombies)) {
            return 1.0;
        }
        return Math.min(1.0, Math.max(0.0, (double) currentWaveIndex / waves.size()));
    }

    @Override
    public double getWaveElapsedTime() {
        return waveElapsedTime;
    }

    public List<Wave> getWaves() {
        return Collections.unmodifiableList(waves);
    }
}
