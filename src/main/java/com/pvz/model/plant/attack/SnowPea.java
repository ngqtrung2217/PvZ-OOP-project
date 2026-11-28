package com.pvz.model.plant.attack;

import com.pvz.model.plant.Plant;
import com.pvz.model.plant.PlantSpec;
import com.pvz.model.plant.PlantType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Frost ranged attacker that fires chilling peas (50% slow effect).
 */
public class SnowPea extends Plant {
    public static final double SLOW_FACTOR = 0.5;

    private double attackTimer = 0.0;
    private int shotsFired = 0;

    private BooleanSupplier zombieInLaneSupplier = () -> false;
    private Consumer<SnowPea> onShoot;

    public SnowPea(int row, int col, double x, double y, double width, double height, PlantSpec spec) {
        super(PlantType.SNOW_PEA, row, col, x, y, width, height, spec);
    }

    public void setZombieInLaneSupplier(BooleanSupplier zombieInLaneSupplier) {
        this.zombieInLaneSupplier = zombieInLaneSupplier != null ? zombieInLaneSupplier : () -> false;
    }

    public void setOnShoot(Consumer<SnowPea> onShoot) {
        this.onShoot = onShoot;
    }

    public int getShotsFired() {
        return shotsFired;
    }

    public double getSlowFactor() {
        return SLOW_FACTOR;
    }

    public double getProjectileSpawnX() {
        return x + width * 0.8;
    }

    public double getProjectileSpawnY() {
        return y + height * 0.35;
    }

    @Override
    public void update(double deltaTime) {
        if (!isAlive() || deltaTime <= 0 || attackSpeed <= 0) {
            return;
        }
        if (!zombieInLaneSupplier.getAsBoolean()) {
            attackTimer = Math.min(attackTimer + deltaTime, attackSpeed);
            return;
        }
        attackTimer += deltaTime;
        while (attackTimer >= attackSpeed) {
            attackTimer -= attackSpeed;
            shotsFired++;
            if (onShoot != null) {
                onShoot.accept(this);
            }
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        // Stem
        gc.setFill(Color.web("#0277BD"));
        gc.fillRect(x + width * 0.44, y + height * 0.48, width * 0.12, height * 0.36);

        // Ice head
        gc.setFill(Color.web("#4FC3F7"));
        gc.fillOval(x + width * 0.22, y + height * 0.18, width * 0.48, height * 0.38);

        // Ice snout
        gc.setFill(Color.web("#29B6F6"));
        gc.fillOval(x + width * 0.58, y + height * 0.26, width * 0.24, height * 0.20);

        // Eye
        gc.setFill(Color.web("#01579B"));
        gc.fillOval(x + width * 0.48, y + height * 0.27, width * 0.08, height * 0.08);
    }
}
