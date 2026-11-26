package com.pvz.model.zombie;

import com.pvz.model.board.GameBoard;
import com.pvz.model.zombie.state.DeadState;
import com.pvz.model.zombie.state.EatingState;
import com.pvz.model.zombie.state.SwimmingState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * WaterZombie (Ducky Tube Zombie) navigates both land and water pools.
 * Moves at normal speed on grass (17.0 px/s) and accelerates when swimming (22.0 px/s).
 */
public class WaterZombie extends Zombie {
    public static final double WIDTH = 50.0;
    public static final double HEIGHT = 90.0;
    public static final int MAX_HP = 200;
    public static final double LAND_SPEED = 17.0;
    public static final double DEFAULT_SWIM_SPEED = 22.0;
    public static final int ATTACK_DAMAGE = 25;

    public WaterZombie(double x, double y) {
        super(x, y, WIDTH, HEIGHT, MAX_HP, LAND_SPEED, ATTACK_DAMAGE);
        this.swimSpeed = DEFAULT_SWIM_SPEED;
        this.waterZombie = true;
        this.canSwim = true;
        this.renderColor = Color.web("#2E5E4E");
    }

    public WaterZombie(double x, double y, int lane, GameBoard board) {
        super(x, y, WIDTH, HEIGHT, MAX_HP, LAND_SPEED, ATTACK_DAMAGE, lane, board);
        this.swimSpeed = DEFAULT_SWIM_SPEED;
        this.waterZombie = true;
        this.canSwim = true;
        this.renderColor = Color.web("#2E5E4E");
        // If spawned directly into a water tile, enter SwimmingState immediately
        if (isOnWater()) {
            changeState(new SwimmingState());
        }
    }

    @Override
    public boolean canSwim() {
        return true;
    }

    @Override
    public double getSwimSpeed() {
        return swimSpeed;
    }

    @Override
    public void update(double deltaTime) {
        super.update(deltaTime);
        // Ensure automatic transition to swimming when entering water
        if (isOnWater() && !(currentState instanceof SwimmingState)
                && !(currentState instanceof EatingState)
                && !(currentState instanceof DeadState)) {
            changeState(new SwimmingState());
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        super.render(gc);
        if (gc == null) {
            return;
        }

        // Render ducky inner tube around waist
        double tubeY = y + height * 0.65;
        double tubeHeight = 16.0;
        double tubeWidth = width + 14.0;
        double tubeX = x - 7.0;

        gc.setFill(Color.GOLD);
        gc.fillRoundRect(tubeX, tubeY, tubeWidth, tubeHeight, 10, 10);
        gc.setStroke(Color.DARKORANGE);
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(tubeX, tubeY, tubeWidth, tubeHeight, 10, 10);

        // Cute duck beak/head on front of tube
        gc.setFill(Color.ORANGE);
        gc.fillOval(tubeX - 6.0, tubeY + 2.0, 8.0, 10.0);
    }
}
