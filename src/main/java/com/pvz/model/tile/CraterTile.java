package com.pvz.model.tile;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Crater tile left by explosions or environmental hazards; prevents planting.
 */
public class CraterTile extends Tile {

    public CraterTile(int row, int col, double x, double y, double width, double height) {
        super(row, col, x, y, width, height, TileType.CRATER, false);
    }

    @Override
    public void update(double deltaTime) {
        // Can track regeneration cooldown if crater recovers over time
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(Color.web("#5D4037"));
        gc.fillRect(x, y, width, height);

        // Crater outline / inner hole
        gc.setFill(Color.web("#3E2723"));
        gc.fillOval(x + width * 0.15, y + height * 0.2, width * 0.7, height * 0.6);

        gc.setStroke(Color.web("#271C19"));
        gc.setLineWidth(1.0);
        gc.strokeRect(x, y, width, height);
    }
}
