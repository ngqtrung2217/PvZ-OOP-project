package com.pvz.model.zombie;

import com.pvz.model.board.GameBoard;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * BucketheadZombie wears a sturdy metal bucket providing 1100 armor HP.
 * Extremely durable frontline zombie.
 */
public class BucketheadZombie extends Zombie {
    public static final double WIDTH = 50.0;
    public static final double HEIGHT = 90.0;
    public static final int MAX_HP = 200;
    public static final int DEFAULT_ARMOR_HP = 1100;
    public static final double SPEED = 17.0;
    public static final int ATTACK_DAMAGE = 25;

    protected int armorHp;

    public BucketheadZombie(double x, double y) {
        super(x, y, WIDTH, HEIGHT, MAX_HP, SPEED, ATTACK_DAMAGE);
        this.armorHp = DEFAULT_ARMOR_HP;
        this.renderColor = Color.web("#4A7C59");
    }

    public BucketheadZombie(double x, double y, int lane, GameBoard board) {
        super(x, y, WIDTH, HEIGHT, MAX_HP, SPEED, ATTACK_DAMAGE, lane, board);
        this.armorHp = DEFAULT_ARMOR_HP;
        this.renderColor = Color.web("#4A7C59");
    }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0) {
            return;
        }

        if (armorHp > 0) {
            if (armorHp >= amount) {
                armorHp -= amount;
                return;
            } else {
                int excess = amount - armorHp;
                armorHp = 0;
                super.takeDamage(excess);
                return;
            }
        }

        super.takeDamage(amount);
    }

    @Override
    public void render(GraphicsContext gc) {
        super.render(gc);
        if (gc == null) {
            return;
        }

        // Render silver metal bucket on head if armor remains
        if (armorHp > 0) {
            double bucketWidth = 28.0;
            double bucketHeight = 22.0;
            double bucketX = x + (width - bucketWidth) / 2.0;
            double bucketY = y - 18.0;

            gc.setFill(Color.SILVER);
            gc.fillRect(bucketX, bucketY, bucketWidth, bucketHeight);
            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(1.5);
            gc.strokeRect(bucketX, bucketY, bucketWidth, bucketHeight);

            // Bucket handle / rim accent
            gc.setStroke(Color.GRAY);
            gc.strokeLine(bucketX - 2.0, bucketY + bucketHeight, bucketX + bucketWidth + 2.0, bucketY + bucketHeight);
        }
    }

    public int getArmorHp() {
        return armorHp;
    }

    public void setArmorHp(int armorHp) {
        this.armorHp = Math.max(0, armorHp);
    }
}
