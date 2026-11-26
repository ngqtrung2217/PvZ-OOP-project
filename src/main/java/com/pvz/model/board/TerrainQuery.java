package com.pvz.model.board;

import com.pvz.model.tile.TileType;

/**
 * Lets a zombie ask what terrain lies under it without depending on the whole
 * {@link GameBoard}. Tests can pass a small fake instead of a real board.
 */
public interface TerrainQuery {
    /**
     * @param lane board row, 0 at the top
     * @param x    screen x in pixels
     * @return the terrain at that point, or null outside the grid
     */
    TileType getTileType(int lane, double x);
}
