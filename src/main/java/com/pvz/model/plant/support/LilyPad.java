package com.pvz.model.plant.support;

import com.pvz.model.plant.Plant;
import com.pvz.model.plant.PlantSpec;
import com.pvz.model.plant.PlantType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Aquatic base platform placed on WaterTile (PlantLayer.BASE),
 * allowing one TOP-layer plant to be planted on top of it.
 */
public class LilyPad extends Plant {

    public LilyPad(int row, int col, double x, double y, double width, double height, PlantSpec spec) {
        super(PlantType.LILY_PAD, row, col, x, y, width, height, spec);
    }

    @Override
    public void update(double deltaTime) {
        // Passive aquatic support platform
    }

    @Override
    public void render(GraphicsContext gc) {
        // Flat floating pad near the bottom of the water cell so topPlant sits visibly above it
        gc.setFill(Color.web("#66BB6A"));
        gc.fillOval(x + width * 0.10, y + height * 0.62, width * 0.80, height * 0.28);

        gc.setStroke(Color.web("#2E7D32"));
        gc.setLineWidth(2.0);
        gc.strokeOval(x + width * 0.10, y + height * 0.62, width * 0.80, height * 0.28);
    }
}
