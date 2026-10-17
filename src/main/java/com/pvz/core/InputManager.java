package com.pvz.core;

import com.pvz.state.game.GameState;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;

/**
 * Attaches mouse and keyboard listeners to JavaFX Canvas and Scene,
 * forwarding events to the active GameState.
 */
public class InputManager {

    public void registerInputHandlers(Canvas canvas, Scene scene) {
        canvas.setOnMouseClicked(event -> {
            GameState currentState = GameManager.getInstance().getCurrentState();
            if (currentState != null) {
                currentState.handleMouseClick(event);
            }
        });

        scene.setOnKeyPressed(event -> {
            GameState currentState = GameManager.getInstance().getCurrentState();
            if (currentState != null) {
                currentState.handleKeyPressed(event);
            }
        });
    }
}
