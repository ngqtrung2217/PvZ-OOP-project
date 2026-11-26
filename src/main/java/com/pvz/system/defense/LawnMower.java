package com.pvz.system.defense;

import com.pvz.model.base.GameObject;
import com.pvz.model.board.GameBoard;
import com.pvz.model.board.Lane;
import com.pvz.system.resource.ResourceManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Last-line defense lawn mower stationed at the left edge of each lane.
 * Activates automatically when a zombie collides with it, then rushes across
 * the lane clearing all zombies before becoming {@link MowerState#USED}.
 */
public class LawnMower extends GameObject {

    public enum MowerState {
        READY,
        ACTIVE,
        USED
    }

    public static final double DEFAULT_WIDTH = 64.0;
    public static final double DEFAULT_HEIGHT = 56.0;
    public static final double DEFAULT_SPEED = 380.0;
    public static final double DEFAULT_MAX_X = 1024.0;
    public static final String SPRITE_PATH = "/assets/sprites/ui/lawn_mower.png";

    private final int lane;
    private final Lane laneRef;
    private final double speed;
    private final double maxX;
    private MowerState state;

    public LawnMower(int lane, Lane laneRef) {
        this(
                lane,
                laneRef,
                GameBoard.START_X - DEFAULT_WIDTH,
                GameBoard.START_Y + lane * GameBoard.CELL_HEIGHT + (GameBoard.CELL_HEIGHT - DEFAULT_HEIGHT) / 2.0,
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT,
                DEFAULT_SPEED,
                DEFAULT_MAX_X
        );
    }

    public LawnMower(int lane, GameBoard board) {
        this(lane, board != null ? board.getLane(lane) : null);
    }

    public LawnMower(int lane, Lane laneRef, double x, double y, double width, double height,
                     double speed, double maxX) {
        super(x, y, width, height);
        this.lane = lane;
        this.laneRef = laneRef;
        this.speed = speed;
        this.maxX = maxX;
        this.state = MowerState.READY;
    }

    /**
     * Activates this lawn mower if it is currently {@link MowerState#READY},
     * syncing with the associated {@link Lane} reference if present.
     */
    public void activate() {
        if (state == MowerState.READY) {
            state = MowerState.ACTIVE;
            if (laneRef != null) {
                laneRef.triggerLawnMower();
            }
        }
    }

    @Override
    public void update(double deltaTime) {
        if (state != MowerState.ACTIVE || deltaTime <= 0.0) {
            return;
        }
        x += speed * deltaTime;
        if (x > maxX) {
            state = MowerState.USED;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (gc == null || state == MowerState.USED) {
            return;
        }

        Image sprite = ResourceManager.getInstance().getImage(SPRITE_PATH);
        if (sprite != null) {
            gc.drawImage(sprite, x, y, width, height);
            return;
        }

        // Fallback geometric rendering for LawnMower
        gc.setFill(Color.CRIMSON);
        gc.fillRoundRect(x + 8.0, y + 10.0, width - 12.0, height - 22.0, 8.0, 8.0);
        gc.setStroke(Color.DARKRED);
        gc.setLineWidth(2.0);
        gc.strokeRoundRect(x + 8.0, y + 10.0, width - 12.0, height - 22.0, 8.0, 8.0);

        // Handle bar
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(3.0);
        gc.strokeLine(x + 12.0, y + 14.0, x, y + 2.0);

        // Wheels
        gc.setFill(Color.web("#263238"));
        gc.fillOval(x + 10.0, y + height - 16.0, 14.0, 14.0);
        gc.fillOval(x + width - 22.0, y + height - 16.0, 14.0, 14.0);
    }

    public int getLane() {
        return lane;
    }

    public Lane getLaneRef() {
        return laneRef;
    }

    public double getSpeed() {
        return speed;
    }

    public double getMaxX() {
        return maxX;
    }

    public MowerState getState() {
        return state;
    }

    public boolean isReady() {
        return state == MowerState.READY;
    }

    public boolean isActive() {
        return state == MowerState.ACTIVE;
    }

    public boolean isUsed() {
        return state == MowerState.USED;
    }

    public boolean isReadyToRemove() {
        return state == MowerState.USED;
    }
}
