package com.pvz.model.economy;

import com.pvz.model.board.GameBoard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Periodically spawns Sun from the sky onto random positions within the 5x9 lawn.
 * Accepts an injected Random for deterministic unit testing and uses deltaTime only.
 */
public class SunSpawner {
    public static final double DEFAULT_SPAWN_INTERVAL = 10.0;

    private final double spawnInterval;
    private final Random random;
    private double timer = 0.0;

    public SunSpawner(double spawnInterval, Random random) {
        if (spawnInterval <= 0) {
            throw new IllegalArgumentException("spawnInterval must be positive");
        }
        this.spawnInterval = spawnInterval;
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    public double getSpawnInterval() {
        return spawnInterval;
    }

    public double getTimer() {
        return timer;
    }

    public void reset() {
        this.timer = 0.0;
    }

    /**
     * Advances the spawn timer by deltaTime (only call inside PlayingState)
     * and returns any Suns spawned during this step.
     */
    public List<Sun> update(double deltaTime) {
        List<Sun> spawned = new ArrayList<>();
        if (deltaTime <= 0) {
            return spawned;
        }
        timer += deltaTime;
        while (timer >= spawnInterval) {
            timer -= spawnInterval;
            spawned.add(spawnRandomSkySun());
        }
        return spawned;
    }

    private Sun spawnRandomSkySun() {
        double boardWidth = GameBoard.COLS * GameBoard.CELL_WIDTH;
        double boardHeight = GameBoard.ROWS * GameBoard.CELL_HEIGHT;

        double maxOffsetX = Math.max(0.0, boardWidth - Sun.DEFAULT_SIZE);
        double maxOffsetY = Math.max(0.0, boardHeight - Sun.DEFAULT_SIZE);

        double x = GameBoard.START_X + random.nextDouble() * maxOffsetX;
        double targetY = GameBoard.START_Y + random.nextDouble() * maxOffsetY;
        double startY = GameBoard.START_Y - Sun.DEFAULT_SIZE;

        return Sun.fromSky(x, startY, targetY);
    }
}
