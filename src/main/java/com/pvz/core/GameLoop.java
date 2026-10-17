package com.pvz.core;

import javafx.animation.AnimationTimer;

/**
 * Game loop implementation backed by JavaFX AnimationTimer (~60 FPS).
 * Computes hardware-independent delta-time between consecutive frames.
 */
public class GameLoop extends AnimationTimer {
    private static final double MAX_DELTA_TIME = 0.1; // Cap to prevent simulation jumps
    private final GameEngine engine;
    private long lastNanoTime = 0;

    public GameLoop(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void handle(long now) {
        if (lastNanoTime == 0) {
            lastNanoTime = now;
            return;
        }

        // Convert elapsed nanoseconds to seconds
        double deltaTime = (now - lastNanoTime) / 1_000_000_000.0;
        lastNanoTime = now;

        // Clamp delta time in case of system hitch or window focus loss
        if (deltaTime > MAX_DELTA_TIME) {
            deltaTime = MAX_DELTA_TIME;
        }

        // Step 1: Update game logic
        engine.update(deltaTime);

        // Step 2: Render graphics
        engine.render();
    }

    @Override
    public void start() {
        lastNanoTime = 0;
        super.start();
    }

    @Override
    public void stop() {
        lastNanoTime = 0;
        super.stop();
    }
}
