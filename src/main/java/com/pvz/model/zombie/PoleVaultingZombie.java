package com.pvz.model.zombie;

import com.pvz.model.base.Entity;
import com.pvz.model.board.GameBoard;
import com.pvz.model.zombie.state.VaultingState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * PoleVaultingZombie runs swiftly with a pole (speed 31.0 px/s, HP 500).
 * Upon colliding with its first non-tall obstacle, it vaults over it into the next cell.
 * Afterwards, its speed drops to normal walking speed (17.0 px/s).
 */
public class PoleVaultingZombie extends Zombie {
    public static final double WIDTH = 50.0;
    public static final double HEIGHT = 90.0;
    public static final int MAX_HP = 500;
    public static final double INITIAL_SPEED = 31.0;
    public static final double NORMAL_SPEED = 17.0;
    public static final int ATTACK_DAMAGE = 25;

    protected double normalSpeed = NORMAL_SPEED;

    public PoleVaultingZombie(double x, double y) {
        super(x, y, WIDTH, HEIGHT, MAX_HP, INITIAL_SPEED, ATTACK_DAMAGE);
        this.normalSpeed = NORMAL_SPEED;
        this.renderColor = Color.web("#3D6647");
    }

    public PoleVaultingZombie(double x, double y, int lane, GameBoard board) {
        super(x, y, WIDTH, HEIGHT, MAX_HP, INITIAL_SPEED, ATTACK_DAMAGE, lane, board);
        this.normalSpeed = NORMAL_SPEED;
        this.renderColor = Color.web("#3D6647");
    }

    @Override
    public void onCollideWithTarget(Entity target, boolean isTallObstacle) {
        if (target == null || !target.isAlive()) {
            return;
        }

        // Vault over first non-tall obstacle
        if (!hasVaulted && !isTallObstacle) {
            changeState(new VaultingState());
        } else {
            // Already vaulted or blocked by a tall wall like Tall-nut: eat target normally
            super.onCollideWithTarget(target, isTallObstacle);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        super.render(gc);
        if (gc == null) {
            return;
        }

        // Render vaulting pole when not yet used
        if (!hasVaulted) {
            gc.setStroke(Color.PERU);
            gc.setLineWidth(3.5);
            // Draw pole held diagonally in front
            gc.strokeLine(x - 12.0, y + height - 5.0, x + width + 8.0, y - 10.0);
        }
    }

    public double getNormalSpeed() {
        return normalSpeed;
    }

    public void setNormalSpeed(double normalSpeed) {
        this.normalSpeed = normalSpeed;
    }
}
