package com.pvz.model.wave;

import com.pvz.model.board.GameBoard;
import com.pvz.model.zombie.Zombie;
import com.pvz.model.zombie.ZombieFactory;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Orchestrates wave-based zombie spawning and lifecycle management.
 * Dispatches wave entries, updates active entities, and prunes dead zombies safely.
 */
public class WaveManager {
    private final GameBoard board;
    private final List<Wave> waves;
    private final List<Zombie> activeZombies;

    private int currentWaveIndex;
    private double waveElapsedTime;
    private boolean hugeWaveFlag;

    public WaveManager(GameBoard board) {
        this(board, createDefaultLevelWaves());
    }

    public WaveManager(GameBoard board, List<Wave> waves) {
        this.board = board;
        this.waves = waves != null ? new ArrayList<>(waves) : new ArrayList<>();
        this.activeZombies = new ArrayList<>();
        this.currentWaveIndex = 0;
        this.waveElapsedTime = 0.0;
        this.hugeWaveFlag = false;
    }

    /**
     * Builds standard 3-wave campaign progression with increasing difficulty.
     * Wave 3 is a culminating Huge Wave containing Flag, Conehead, Buckethead, and Pole Vaulting zombies.
     */
    public static List<Wave> createDefaultLevelWaves() {
        List<Wave> defaultWaves = new ArrayList<>();

        // Wave 1: Gentle initial assault
        Wave wave1 = new Wave(false);
        wave1.addEntry("NORMAL", 1, 2.0);
        wave1.addEntry("NORMAL", 3, 5.0);
        wave1.addEntry("NORMAL", 2, 8.0);
        defaultWaves.add(wave1);

        // Wave 2: Escalating threat with armor and water zombies
        Wave wave2 = new Wave(false);
        wave2.addEntry("NORMAL", 0, 2.0);
        wave2.addEntry("CONEHEAD", 1, 4.0);
        wave2.addEntry("WATER", 2, 6.0);
        wave2.addEntry("NORMAL", 4, 8.0);
        defaultWaves.add(wave2);

        // Wave 3: Huge Wave finale
        Wave wave3 = new Wave(true);
        wave3.addEntry("FLAG", 2, 1.0);
        wave3.addEntry("CONEHEAD", 0, 1.5);
        wave3.addEntry("POLE_VAULTING", 1, 2.0);
        wave3.addEntry("WATER", 2, 2.5);
        wave3.addEntry("BUCKETHEAD", 3, 3.0);
        wave3.addEntry("NORMAL", 4, 3.5);
        defaultWaves.add(wave3);

        return defaultWaves;
    }

    /**
     * Advances the wave spawner and updates active zombies per frame.
     *
     * @param deltaTime Elapsed frame time in seconds.
     */
    public void update(double deltaTime) {
        if (currentWaveIndex < waves.size()) {
            Wave currentWave = waves.get(currentWaveIndex);
            hugeWaveFlag = currentWave.isHugeWave();
            waveElapsedTime += deltaTime;

            // 1. Spawn entries whose delay has elapsed
            for (WaveEntry entry : currentWave.getEntries()) {
                if (!entry.isSpawned() && waveElapsedTime >= entry.getSpawnDelay()) {
                    Zombie zombie = ZombieFactory.createZombie(entry.getZombieType(), entry.getLane(), board);
                    activeZombies.add(zombie);
                    entry.setSpawned(true);
                }
            }
        }

        // 2. Update active zombies
        for (Zombie zombie : activeZombies) {
            zombie.update(deltaTime);
        }

        // 3. Prune dead zombies safely using Iterator
        Iterator<Zombie> iterator = activeZombies.iterator();
        while (iterator.hasNext()) {
            Zombie zombie = iterator.next();
            if (zombie.isReadyToRemove()) {
                iterator.remove();
            }
        }

        // 4. Advance to next wave if current wave has finished spawning and all zombies are cleared
        if (currentWaveIndex < waves.size()) {
            Wave currentWave = waves.get(currentWaveIndex);
            if (currentWave.isAllSpawned() && activeZombies.isEmpty()) {
                currentWaveIndex++;
                waveElapsedTime = 0.0;
                hugeWaveFlag = (currentWaveIndex < waves.size() && waves.get(currentWaveIndex).isHugeWave());
            }
        }
    }

    /**
     * Renders all active zombies.
     *
     * @param gc The GraphicsContext canvas target.
     */
    public void render(GraphicsContext gc) {
        if (gc == null) {
            return;
        }
        for (Zombie zombie : activeZombies) {
            zombie.render(gc);
        }
    }

    // Getters for UI and game state integration
    public List<Zombie> getActiveZombies() {
        return Collections.unmodifiableList(activeZombies);
    }

    public int getCurrentWaveIndex() {
        return currentWaveIndex;
    }

    public int getTotalWaves() {
        return waves.size();
    }

    public boolean isHugeWaveApproaching() {
        if (currentWaveIndex < waves.size()) {
            return waves.get(currentWaveIndex).isHugeWave();
        }
        return hugeWaveFlag;
    }

    public boolean isAllWavesCleared() {
        return currentWaveIndex >= waves.size() && activeZombies.isEmpty();
    }

    public double getProgressRatio() {
        if (waves.isEmpty() || isAllWavesCleared()) {
            return 1.0;
        }
        return Math.min(1.0, Math.max(0.0, (double) currentWaveIndex / waves.size()));
    }

    public double getWaveElapsedTime() {
        return waveElapsedTime;
    }

    public GameBoard getBoard() {
        return board;
    }

    public List<Wave> getWaves() {
        return Collections.unmodifiableList(waves);
    }
}
