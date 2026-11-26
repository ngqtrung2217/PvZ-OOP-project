package com.pvz.model.plant.attack;

import com.pvz.model.plant.Plant;
import com.pvz.model.plant.PlantSpec;
import com.pvz.model.plant.PlantType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Basic ranged attacker that fires a pea whenever a zombie is detected in its lane.
 */
public class Peashooter extends Plant {
    private double attackTimer = 0.0;
    private int shotsFired = 0;

    private BooleanSupplier zombieInLaneSupplier = () -> false;
    private Consumer<Peashooter> onShoot;

    public Peashooter(int row, int col, double x, double y, double width, double height, PlantSpec spec) {
        super(PlantType.PEASHOOTER, row, col, x, y, width, height, spec);
    }

    public void setZombieInLaneSupplier(BooleanSupplier zombieInLaneSupplier) {
        this.zombieInLaneSupplier = zombieInLaneSupplier != null ? zombieInLaneSupplier : () -> false;
    }

    public void setOnShoot(Consumer<Peashooter> onShoot) {
        this.onShoot = onShoot;
    }

    public int getShotsFired() {
        return shotsFired;
    }

    /**
     * X coordinate of the pea muzzle for projectile spawning.
     */
    public double getProjectileSpawnX() {
        return x + width * 0.8;
    }

    /**
     * Y coordinate of the pea muzzle for projectile spawning.
     */
    public double getProjectileSpawnY() {
        return y + height * 0.35;
    }

    @Override
    public void update(double deltaTime) {
        if (!isAlive() || deltaTime <= 0 || attackSpeed <= 0) {
            return;
        }
        if (!zombieInLaneSupplier.getAsBoolean()) {
            // Keep ready to fire immediately once a zombie enters the lane.
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
        // Temporary Canvas fallback until TV4's ResourceManager sprites are wired in
        gc.setFill(Color.web("#2E7D32"));
        gc.fillRect(x + width * 0.44, y + height * 0.48, width * 0.12, height * 0.36);

        // Head
        gc.setFill(Color.web("#7CB342"));
        gc.fillOval(x + width * 0.22, y + height * 0.18, width * 0.48, height * 0.38);

        // Snout
        gc.setFill(Color.web("#558B2F"));
        gc.fillOval(x + width * 0.58, y + height * 0.26, width * 0.24, height * 0.20);

        // Eye
        gc.setFill(Color.BLACK);
        gc.fillOval(x + width * 0.48, y + height * 0.27, width * 0.08, height * 0.08);
    }
}
