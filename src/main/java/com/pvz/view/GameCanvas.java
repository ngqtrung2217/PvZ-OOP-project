package com.pvz.view;

import javafx.scene.canvas.Canvas;

/**
 * Primary JavaFX Canvas used for drawing game board, entities, projectiles, and HUD.
 */
public class GameCanvas extends Canvas {
    public static final double DEFAULT_WIDTH = 1024.0;
    public static final double DEFAULT_HEIGHT = 768.0;

    public GameCanvas() {
        super(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public GameCanvas(double width, double height) {
        super(width, height);
    }
}
