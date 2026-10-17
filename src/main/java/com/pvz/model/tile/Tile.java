package com.pvz.model.tile;

import com.pvz.model.base.GameObject;

/**
 * Abstract class representing a single cell on the 5x9 lawn grid.
 * <p>
 * Each subclass fixes its own {@link #canPlant} rule, so a tile's plantability is
 * read-only once constructed; use {@link GameBoard#setTile} to swap terrain instead.
 */
public abstract class Tile extends GameObject {
    protected final int row;
    protected final int col;
    protected final TileType type;
    protected final boolean canPlant;

    public Tile(int row, int col, double x, double y, double width, double height,
                TileType type, boolean canPlant) {
        super(x, y, width, height);
        this.row = row;
        this.col = col;
        this.type = type;
        this.canPlant = canPlant;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public TileType getType() {
        return type;
    }

    public boolean canPlant() {
        return canPlant;
    }
}
