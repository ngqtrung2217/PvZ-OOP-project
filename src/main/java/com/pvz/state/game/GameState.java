package com.pvz.state.game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * Interface representing a distinct phase in the game lifecycle (State Pattern).
 */
public interface GameState {

    /**
     * Called when this state becomes active.
     */
    void enter();

    /**
     * Updates logic for this state per frame.
     * @param deltaTime Elapsed time in seconds.
     */
    void update(double deltaTime);

    /**
     * Renders visuals for this state onto the Canvas.
     * @param gc Canvas GraphicsContext.
     */
    void render(GraphicsContext gc);

    /**
     * Handles mouse click input events.
     */
    void handleMouseClick(MouseEvent event);

    /**
     * Handles key pressed input events.
     */
    void handleKeyPressed(KeyEvent event);

    /** Mouse button went down. Optional: most screens only need clicks. */
    default void handleMousePressed(MouseEvent event) {
    }

    /** Mouse moved with a button held, for painting by dragging. Optional. */
    default void handleMouseDragged(MouseEvent event) {
    }

    /** Mouse button was released. Optional. */
    default void handleMouseReleased(MouseEvent event) {
    }

    /** A character was typed, for text input such as a map name. Optional. */
    default void handleKeyTyped(KeyEvent event) {
    }

    /**
     * Called when exiting this state.
     */
    void exit();
}
