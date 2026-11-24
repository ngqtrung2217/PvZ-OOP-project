package com.pvz.model.zombie;

import com.pvz.model.board.GameBoard;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * ConeheadZombie wears a traffic cone granting 370 additional armor HP.
 * Damage is absorbed by the armor before affecting base health.
 */
public class ConeheadZombie extends Zombie {
    public static final double WIDTH = 50.0;
    public static final double HEIGHT = 90.0;
    public static final int MAX_HP = 200;
    public static final int DEFAULT_ARMOR_HP = 370;
    public static final double SPEED = 17.0;
    public static final int ATTACK_DAMAGE = 25;

    protected int armorHp;

    public ConeheadZombie(double x, double y) {
        super(x, y, WIDTH, HEIGHT, MAX_HP, SPEED, ATTACK_DAMAGE);
        this.armorHp = DEFAULT_ARMOR_HP;
        this.renderColor = Color.web("#4A7C59");
    }

    public ConeheadZombie(double x, double y, int lane, GameBoard board) {
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

        // Render orange cone on head if armor remains
        if (armorHp > 0) {
            double coneBaseY = y;
            double conePeakY = y - 24.0;
            double coneCenterX = x + width / 2.0;

            gc.setFill(Color.ORANGE);
            gc.fillPolygon(
                    new double[]{coneCenterX - 14.0, coneCenterX, coneCenterX + 14.0},
                    new double[]{coneBaseY, conePeakY, coneBaseY},
                    3
            );
            gc.setStroke(Color.DARKORANGE);
            gc.setLineWidth(1.5);
            gc.strokePolygon(
                    new double[]{coneCenterX - 14.0, coneCenterX, coneCenterX + 14.0},
                    new double[]{coneBaseY, conePeakY, coneBaseY},
                    3
            );
        }
    }

    public int getArmorHp() {
        return armorHp;
    }

    public void setArmorHp(int armorHp) {
        this.armorHp = Math.max(0, armorHp);
    }
}
