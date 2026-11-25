package com.pvz.model.zombie;

import com.pvz.model.board.GameBoard;
import javafx.scene.paint.Color;

/**
 * Standard baseline Zombie unit.
 * Dimensions: 50.0 x 90.0 px
 * Max HP: 200
 * Speed: 17.0 px/s
 * Attack Damage: 25
 */
public class NormalZombie extends Zombie {
    public static final double WIDTH = 50.0;
    public static final double HEIGHT = 90.0;
    public static final int MAX_HP = 200;
    public static final double SPEED = 17.0;
    public static final int ATTACK_DAMAGE = 25;

    public NormalZombie(double x, double y) {
        super(x, y, WIDTH, HEIGHT, MAX_HP, SPEED, ATTACK_DAMAGE);
        this.renderColor = Color.web("#4A7C59"); // Zombie green
    }

    public NormalZombie(double x, double y, int lane, GameBoard board) {
        super(x, y, WIDTH, HEIGHT, MAX_HP, SPEED, ATTACK_DAMAGE, lane, board);
        this.renderColor = Color.web("#4A7C59");
    }
}
