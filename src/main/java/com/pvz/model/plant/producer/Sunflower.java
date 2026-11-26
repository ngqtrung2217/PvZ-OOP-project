package com.pvz.model.plant.producer;

import com.pvz.model.economy.Sun;
import com.pvz.model.plant.Plant;
import com.pvz.model.plant.PlantSpec;
import com.pvz.model.plant.PlantType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Resource-producing plant that periodically generates a collectible Sun.
 * Uses only deltaTime (pause-safe) and optional injected Random (testable).
 */
public class Sunflower extends Plant {
    private double productionTimer = 0.0;
    private Random random;
    private Consumer<Sun> onSunProduced;
    private final List<Sun> producedSuns = new ArrayList<>();

    public Sunflower(int row, int col, double x, double y, double width, double height, PlantSpec spec) {
        super(PlantType.SUNFLOWER, row, col, x, y, width, height, spec);
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public void setOnSunProduced(Consumer<Sun> onSunProduced) {
        this.onSunProduced = onSunProduced;
    }

    public double getProductionTimer() {
        return productionTimer;
    }

    /**
     * Drains any Suns generated since the last drain call.
     * Useful for polling-based boards and unit tests.
     */
    public List<Sun> drainProducedSuns() {
        List<Sun> copy = new ArrayList<>(producedSuns);
        producedSuns.clear();
        return copy;
    }

    @Override
    public void update(double deltaTime) {
        if (!isAlive() || deltaTime <= 0 || attackSpeed <= 0) {
            return;
        }
        productionTimer += deltaTime;
        while (productionTimer >= attackSpeed) {
            productionTimer -= attackSpeed;
            double offsetX = random != null ? (random.nextDouble() - 0.5) * 24.0 : 0.0;
            double sunX = x + (width - Sun.DEFAULT_SIZE) / 2.0 + offsetX;
            double sunY = y + height * 0.25;
            Sun sun = Sun.fromSunflower(sunX, sunY);
            producedSuns.add(sun);
            if (onSunProduced != null) {
                onSunProduced.accept(sun);
            }
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        // Stem
        gc.setFill(Color.web("#388E3C"));
        gc.fillRect(x + width * 0.45, y + height * 0.50, width * 0.10, height * 0.34);

        // Petals
        gc.setFill(Color.web("#FDD835"));
        gc.fillOval(x + width * 0.16, y + height * 0.12, width * 0.68, height * 0.54);

        // Center disk
        gc.setFill(Color.web("#6D4C41"));
        gc.fillOval(x + width * 0.30, y + height * 0.23, width * 0.40, height * 0.32);
    }
}
