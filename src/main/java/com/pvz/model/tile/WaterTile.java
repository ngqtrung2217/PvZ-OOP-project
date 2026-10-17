package com.pvz.model.tile;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Pool / water tile requiring aquatic support (LilyPad) before planting other plants.
 */
public class WaterTile extends Tile {

    public WaterTile(int row, int col, double x, double y, double width, double height) {
        // Requires a LilyPad before standard plants can be placed on water.
        super(row, col, x, y, width, height, TileType.WATER, false);
    }

    @Override
    public void update(double deltaTime) {
        // Can animate water ripples if needed
    }

    @Override
    public void render(GraphicsContext gc) {
        boolean isEven = (row + col) % 2 == 0;
        gc.setFill(isEven ? Color.web("#29B6F6") : Color.web("#039BE5"));
        gc.fillRect(x, y, width, height);

        gc.setStroke(Color.web("#0288D1"));
        gc.setLineWidth(1.0);
        gc.strokeRect(x, y, width, height);
    }
}
