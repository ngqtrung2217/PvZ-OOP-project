package com.pvz.model.zombie;

import com.pvz.model.board.GameBoard;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * FlagZombie moves faster than standard zombies and signals an incoming wave.
 * When rendered, displays a red flag held overhead.
 */
public class FlagZombie extends Zombie {
    public static final double WIDTH = 50.0;
    public static final double HEIGHT = 90.0;
    public static final int MAX_HP = 200;
    public static final double SPEED = 22.0;
    public static final int ATTACK_DAMAGE = 25;

    public FlagZombie(double x, double y) {
        super(x, y, WIDTH, HEIGHT, MAX_HP, SPEED, ATTACK_DAMAGE);
        this.renderColor = Color.web("#5A8C69");
    }

    public FlagZombie(double x, double y, int lane, GameBoard board) {
        super(x, y, WIDTH, HEIGHT, MAX_HP, SPEED, ATTACK_DAMAGE, lane, board);
        this.renderColor = Color.web("#5A8C69");
    }

    @Override
    public void render(GraphicsContext gc) {
        super.render(gc);
        if (gc == null) {
            return;
        }

        // Draw flag pole
        gc.setStroke(Color.SADDLEBROWN);
        gc.setLineWidth(2.5);
        double poleX = x + width - 10.0;
        double poleTopY = y - 28.0;
        double poleBottomY = y + 15.0;
        gc.strokeLine(poleX, poleBottomY, poleX, poleTopY);

        // Draw red flag cloth
        gc.setFill(Color.RED);
        gc.fillPolygon(
                new double[]{poleX, poleX + 18.0, poleX},
                new double[]{poleTopY, poleTopY + 8.0, poleTopY + 16.0},
                3
        );
        gc.setStroke(Color.DARKRED);
        gc.setLineWidth(1.0);
        gc.strokePolygon(
                new double[]{poleX, poleX + 18.0, poleX},
                new double[]{poleTopY, poleTopY + 8.0, poleTopY + 16.0},
                3
        );
    }
}
