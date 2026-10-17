package com.pvz.core;

import com.pvz.state.game.GameState;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

/**
 * Main coordinator managing frame updates, rendering cycles, and the canvas.
 * <p>
 * Final because it hands itself to {@link GameLoop} during construction: allowing
 * subclasses would let an overriding method read half-initialised state.
 */
public final class GameEngine {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final GameLoop gameLoop;

    public GameEngine(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.gameLoop = new GameLoop(this);
    }

    /**
     * Starts the 60 FPS animation timer.
     */
    public void start() {
        gameLoop.start();
    }

    /**
     * Stops the animation timer.
     */
    public void stop() {
        gameLoop.stop();
    }

    /**
     * Updates game logic for the current state.
     * @param deltaTime Elapsed time in seconds.
     */
    public void update(double deltaTime) {
        GameState currentState = GameManager.getInstance().getCurrentState();
        if (currentState != null) {
            currentState.update(deltaTime);
        }
    }

    /**
     * Clears canvas and renders the current state.
     */
    public void render() {
        GameState currentState = GameManager.getInstance().getCurrentState();
        if (currentState != null) {
            currentState.render(gc);
        }
    }

    public Canvas getCanvas() {
        return canvas;
    }
}
