package com.pvz.model.wave;

import com.pvz.model.board.GameBoard;
import com.pvz.model.zombie.Zombie;
import com.pvz.model.zombie.ZombieFactory;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Orchestrates wave-based zombie spawning and lifecycle management via a {@link SpawnPolicy}.
 * Dispatches spawns from the configured strategy, updates active entities, and prunes dead zombies safely.
 */
public class WaveManager {
    private final GameBoard board;
    private final SpawnPolicy spawnPolicy;
    private final ZombieFactory zombieFactory;
    private final List<Zombie> activeZombies;

    public WaveManager(GameBoard board) {
        this(board, new ScriptedSpawnPolicy(createDefaultLevelWaves()), new Random());
    }

    public WaveManager(GameBoard board, List<Wave> waves) {
        this(board, new ScriptedSpawnPolicy(waves), new Random());
    }

    public WaveManager(GameBoard board, List<Wave> waves, Random random) {
        this(board, new ScriptedSpawnPolicy(waves), random);
    }

    public WaveManager(GameBoard board, SpawnPolicy spawnPolicy) {
        this(board, spawnPolicy, new Random());
    }

    public WaveManager(GameBoard board, SpawnPolicy spawnPolicy, Random random) {
        this.board = board;
        this.spawnPolicy = spawnPolicy != null
                ? spawnPolicy
                : new ScriptedSpawnPolicy(createDefaultLevelWaves());
        this.zombieFactory = new ZombieFactory(random != null ? random : new Random());
        this.activeZombies = new ArrayList<>();
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
     * Advances the spawn policy and updates active zombies per frame.
     *
     * @param deltaTime Elapsed frame time in seconds.
     */
    public void update(double deltaTime) {
        // 1. Delegate spawning to the injected SpawnPolicy
        List<Zombie> newlySpawned = spawnPolicy.update(deltaTime, board, activeZombies, zombieFactory);
        if (newlySpawned != null && !newlySpawned.isEmpty()) {
            activeZombies.addAll(newlySpawned);
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

        // 4. Notify policy after pruning so scripted waves can advance immediately
        spawnPolicy.afterPrune(activeZombies);
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
        return spawnPolicy.getCurrentWaveIndex();
    }

    public int getTotalWaves() {
        return spawnPolicy.getTotalWaves();
    }

    public boolean isHugeWaveApproaching() {
        return spawnPolicy.isHugeWaveApproaching();
    }

    public boolean isAllWavesCleared() {
        return spawnPolicy.isAllWavesCleared(activeZombies);
    }

    public double getProgressRatio() {
        return spawnPolicy.getProgressRatio(activeZombies);
    }

    public double getWaveElapsedTime() {
        return spawnPolicy.getWaveElapsedTime();
    }

    public GameBoard getBoard() {
        return board;
    }

    public SpawnPolicy getSpawnPolicy() {
        return spawnPolicy;
    }

    public List<Wave> getWaves() {
        if (spawnPolicy instanceof ScriptedSpawnPolicy scripted) {
            return scripted.getWaves();
        }
        return Collections.emptyList();
    }
}
