package com.pvz.core;

import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.GrassTile;
import com.pvz.model.tile.Tile;
import com.pvz.model.tile.TileType;
import com.pvz.model.tile.WaterTile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameBoard coordinate math and tile operations.
 */
public class GameBoardTest {
    private GameBoard board;

    @BeforeEach
    void setUp() {
        board = new GameBoard();
    }

    @Test
    @DisplayName("Initial board should contain 5x9 GrassTiles")
    void testInitialBoardSetup() {
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                Tile tile = board.getTile(r, c);
                assertNotNull(tile, "Tile at (" + r + ", " + c + ") should not be null");
                assertEquals(TileType.GRASS, tile.getType());
                assertTrue(tile.canPlant());
                assertEquals(r, tile.getRow());
                assertEquals(c, tile.getCol());
            }
        }
    }

    @Test
    @DisplayName("Pixel to Column conversion within bounds")
    void testGetColAtValid() {
        // First column [START_X, START_X + CELL_WIDTH) = [220, 300)
        assertEquals(0, board.getColAt(220.0));
        assertEquals(0, board.getColAt(260.0));
        assertEquals(0, board.getColAt(299.9));

        // Column 4 (middle column)
        double col4X = GameBoard.START_X + 4 * GameBoard.CELL_WIDTH + 10.0;
        assertEquals(4, board.getColAt(col4X));

        // Last column (col 8)
        double col8X = GameBoard.START_X + 8 * GameBoard.CELL_WIDTH + 10.0;
        assertEquals(8, board.getColAt(col8X));
    }

    @Test
    @DisplayName("Pixel to Column conversion out of bounds")
    void testGetColAtOutOfBounds() {
        // Left of grid
        assertEquals(-1, board.getColAt(GameBoard.START_X - 1.0));
        assertEquals(-1, board.getColAt(0.0));

        // Right of grid
        double rightLimit = GameBoard.START_X + GameBoard.COLS * GameBoard.CELL_WIDTH;
        assertEquals(-1, board.getColAt(rightLimit));
        assertEquals(-1, board.getColAt(rightLimit + 50.0));
    }

    @Test
    @DisplayName("Pixel to Row conversion within bounds")
    void testGetRowAtValid() {
        // Row 0 [START_Y, START_Y + CELL_HEIGHT) = [100, 200)
        assertEquals(0, board.getRowAt(100.0));
        assertEquals(0, board.getRowAt(150.0));
        assertEquals(0, board.getRowAt(199.9));

        // Row 2 (middle row)
        double row2Y = GameBoard.START_Y + 2 * GameBoard.CELL_HEIGHT + 20.0;
        assertEquals(2, board.getRowAt(row2Y));

        // Row 4 (bottom row)
        double row4Y = GameBoard.START_Y + 4 * GameBoard.CELL_HEIGHT + 20.0;
        assertEquals(4, board.getRowAt(row4Y));
    }

    @Test
    @DisplayName("Pixel to Row conversion out of bounds")
    void testGetRowAtOutOfBounds() {
        // Above grid
        assertEquals(-1, board.getRowAt(GameBoard.START_Y - 1.0));
        assertEquals(-1, board.getRowAt(0.0));

        // Below grid
        double bottomLimit = GameBoard.START_Y + GameBoard.ROWS * GameBoard.CELL_HEIGHT;
        assertEquals(-1, board.getRowAt(bottomLimit));
        assertEquals(-1, board.getRowAt(bottomLimit + 100.0));
    }

    @Test
    @DisplayName("Cell center coordinates math check")
    void testGetCellCenterCoordinates() {
        // Center of Col 0, Row 0
        double expectedCenterX = GameBoard.START_X + GameBoard.CELL_WIDTH / 2.0;
        double expectedCenterY = GameBoard.START_Y + GameBoard.CELL_HEIGHT / 2.0;

        assertEquals(expectedCenterX, board.getCellCenterX(0), 0.001);
        assertEquals(expectedCenterY, board.getCellCenterY(0), 0.001);
    }

    @Test
    @DisplayName("Setting custom Tile (e.g. WaterTile) should update cell")
    void testSetTile() {
        WaterTile water = new WaterTile(2, 3, board.getCellOriginX(3), board.getCellOriginY(2),
                GameBoard.CELL_WIDTH, GameBoard.CELL_HEIGHT);
        board.setTile(2, 3, water);

        Tile retrieved = board.getTile(2, 3);
        assertNotNull(retrieved);
        assertEquals(TileType.WATER, retrieved.getType());
        assertFalse(retrieved.canPlant());
    }
}
