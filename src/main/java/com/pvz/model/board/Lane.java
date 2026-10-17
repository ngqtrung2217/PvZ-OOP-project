package com.pvz.model.board;

/**
 * Represents a single horizontal row on the board (Lane).
 * Tracks lane-specific defense mechanisms like LawnMowers.
 */
public class Lane {
    private final int rowIndex;
    private boolean lawnMowerReady;

    public Lane(int rowIndex) {
        this.rowIndex = rowIndex;
        this.lawnMowerReady = true;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public boolean isLawnMowerReady() {
        return lawnMowerReady;
    }

    public void triggerLawnMower() {
        this.lawnMowerReady = false;
    }

    public void resetLawnMower() {
        this.lawnMowerReady = true;
    }
}
