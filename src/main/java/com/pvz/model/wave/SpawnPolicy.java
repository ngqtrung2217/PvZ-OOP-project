package com.pvz.model.wave;

import com.pvz.model.board.GameBoard;
import com.pvz.model.zombie.Zombie;
import com.pvz.model.zombie.ZombieFactory;

import java.util.List;

/**
 * Strategy interface defining how zombies are spawned over time.
 */
public interface SpawnPolicy {
    /**
     * Advances the spawn strategy by {@code deltaTime} and returns any newly spawned zombies.
     *
     * @param deltaTime     Elapsed time since last frame in seconds.
     * @param board         The GameBoard (may be null in isolated tests).
     * @param activeZombies Current active zombies on the board.
     * @param factory       ZombieFactory (with injected Random) for instantiating zombies.
     * @return List of zombies spawned during this update (never null).
     */
    List<Zombie> update(double deltaTime, GameBoard board, List<Zombie> activeZombies, ZombieFactory factory);

    /**
     * Hook called after active zombies are updated and dead ones are pruned.
     * Allows scripted policies to advance to the next wave on the exact frame the wave clears.
     */
    default void afterPrune(List<Zombie> activeZombies) {}

    int getCurrentWaveIndex();

    int getTotalWaves();

    boolean isHugeWaveApproaching();

    boolean isAllWavesCleared(List<Zombie> activeZombies);

    double getProgressRatio(List<Zombie> activeZombies);

    double getWaveElapsedTime();
}
