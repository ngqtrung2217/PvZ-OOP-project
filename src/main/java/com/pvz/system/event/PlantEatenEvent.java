package com.pvz.system.event;

import com.pvz.model.base.Entity;

/**
 * Event published when a plant entity on the board is completely eaten by a zombie.
 */
public final class PlantEatenEvent extends GameEvent {
    private final Entity plant;
    private final int row;
    private final int col;
    private final double x;
    private final double y;

    public PlantEatenEvent(Entity plant, int row, int col, double x, double y) {
        super();
        this.plant = plant;
        this.row = row;
        this.col = col;
        this.x = x;
        this.y = y;
    }

    public PlantEatenEvent(Entity plant, int row, int col) {
        this(
                plant,
                row,
                col,
                plant != null ? plant.getX() : 0.0,
                plant != null ? plant.getY() : 0.0
        );
    }

    public PlantEatenEvent(int row, int col) {
        this(null, row, col, 0.0, 0.0);
    }

    public Entity getPlant() {
        return plant;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
