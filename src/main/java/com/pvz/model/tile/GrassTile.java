package com.pvz.model.tile;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Normal lawn tile suitable for planting ground plants.
 */
public class GrassTile extends Tile {

    public GrassTile(int row, int col, double x, double y, double width, double height) {
        super(row, col, x, y, width, height, TileType.GRASS, true);
    }

    @Override
    public void update(double deltaTime) {
        // Static tile logic
    }

    @Override
    public void render(GraphicsContext gc) {
        // Alternating green checkerboard pattern
        boolean isEven = (row + col) % 2 == 0;
        gc.setFill(isEven ? Color.web("#4CAF50") : Color.web("#43A047"));
        gc.fillRect(x, y, width, height);

        // Subtle grid border
        gc.setStroke(Color.web("#388E3C"));
        gc.setLineWidth(1.0);
        gc.strokeRect(x, y, width, height);
    }
}
