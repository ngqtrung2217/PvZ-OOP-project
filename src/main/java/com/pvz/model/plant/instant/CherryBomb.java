package com.pvz.model.plant.instant;

import com.pvz.model.plant.Plant;
import com.pvz.model.plant.PlantSpec;
import com.pvz.model.plant.PlantType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.function.Consumer;

/**
 * One-time instant plant that detonates in a 3x3 grid radius after its fuse delay (1.2s),
 * hitting all zombies in range including swimming zombies.
 */
public class CherryBomb extends Plant {
    private double fuseTimer = 0.0;
    private boolean exploded = false;
    private Consumer<CherryBomb> onExplode;

    public CherryBomb(int row, int col, double x, double y, double width, double height, PlantSpec spec) {
        super(PlantType.CHERRY_BOMB, row, col, x, y, width, height, spec);
    }

    public void setOnExplode(Consumer<CherryBomb> onExplode) {
        this.onExplode = onExplode;
    }

    public boolean hasExploded() {
        return exploded;
    }

    public double getFuseTimer() {
        return fuseTimer;
    }

    /**
     * Checks whether a grid cell (targetRow, targetCol) falls inside the 3x3 blast zone.
     */
    public boolean isInExplosionRange(int targetRow, int targetCol) {
        return Math.abs(targetRow - row) <= 1 && Math.abs(targetCol - col) <= 1;
    }

    @Override
    public void update(double deltaTime) {
        if (exploded || !isAlive() || deltaTime <= 0) {
            return;
        }
        fuseTimer += deltaTime;
        if (fuseTimer >= attackSpeed) {
            exploded = true;
            this.hp = 0;
            if (onExplode != null) {
                onExplode.accept(this);
            }
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        // Stems
        gc.setStroke(Color.web("#2E7D32"));
        gc.setLineWidth(3.0);
        gc.strokeLine(x + width * 0.50, y + height * 0.16, x + width * 0.34, y + height * 0.50);
        gc.strokeLine(x + width * 0.50, y + height * 0.16, x + width * 0.66, y + height * 0.52);

        // Two red cherries
        gc.setFill(Color.web("#D32F2F"));
        gc.fillOval(x + width * 0.16, y + height * 0.44, width * 0.34, height * 0.32);
        gc.fillOval(x + width * 0.48, y + height * 0.46, width * 0.34, height * 0.32);

        // Fuse warning ring
        if (attackSpeed > 0 && !exploded) {
            double progress = Math.min(1.0, fuseTimer / attackSpeed);
            gc.setStroke(Color.web("#FF5252", 0.5 + 0.5 * progress));
            gc.setLineWidth(2.0);
            gc.strokeOval(x + width * 0.10, y + height * 0.10, width * 0.80, height * 0.80);
        }
    }
}
