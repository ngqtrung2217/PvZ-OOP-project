package com.pvz.model.board;

import com.pvz.model.tile.GrassTile;
import com.pvz.model.tile.Tile;
import javafx.scene.canvas.GraphicsContext;

/**
 * Manages the 5x9 tile grid of the game lawn.
 * Handles the coordinate transformation between pixel positions and grid indices.
 * <p>
 * Final because the constructor seeds the grid through this class's own coordinate
 * helpers; subclassing could observe an uninitialised board.
 */
public final class GameBoard {
    public static final int ROWS = 5;
    public static final int COLS = 9;

    // Grid layout constants for 1024x768 display window
    public static final double START_X = 220.0;
    public static final double START_Y = 100.0;
    public static final double CELL_WIDTH = 80.0;
    public static final double CELL_HEIGHT = 100.0;

    private final Tile[][] tiles = new Tile[ROWS][COLS];
    private final Lane[] lanes = new Lane[ROWS];

    public GameBoard() {
        initDefaultLawn();
    }

    /**
     * Fills every cell with a standard {@link GrassTile} and creates one lane per row.
     */
    private void initDefaultLawn() {
        for (int r = 0; r < ROWS; r++) {
            lanes[r] = new Lane(r);
            for (int c = 0; c < COLS; c++) {
                double x = getCellOriginX(c);
                double y = getCellOriginY(r);
                tiles[r][c] = new GrassTile(r, c, x, y, CELL_WIDTH, CELL_HEIGHT);
            }
        }
    }

    /**
     * Converts a screen X pixel coordinate into a column index (0 to 8).
     * @param mouseX X position on canvas.
     * @return Column index, or -1 if out of grid bounds.
     */
    public int getColAt(double mouseX) {
        if (mouseX < START_X || mouseX >= START_X + COLS * CELL_WIDTH) {
            return -1;
        }
        return (int) ((mouseX - START_X) / CELL_WIDTH);
    }

    /**
     * Converts a screen Y pixel coordinate into a row index (0 to 4).
     * @param mouseY Y position on canvas.
     * @return Row index, or -1 if out of grid bounds.
     */
    public int getRowAt(double mouseY) {
        if (mouseY < START_Y || mouseY >= START_Y + ROWS * CELL_HEIGHT) {
            return -1;
        }
        return (int) ((mouseY - START_Y) / CELL_HEIGHT);
    }

    /**
     * Computes the X coordinate of the center of a specified column.
     * Ideal for centering planted units.
     */
    public double getCellCenterX(int col) {
        return START_X + col * CELL_WIDTH + (CELL_WIDTH / 2.0);
    }

    /**
     * Computes the Y coordinate of the center of a specified row.
     */
    public double getCellCenterY(int row) {
        return START_Y + row * CELL_HEIGHT + (CELL_HEIGHT / 2.0);
    }

    /**
     * Computes the top-left X coordinate of a column.
     */
    public double getCellOriginX(int col) {
        return START_X + col * CELL_WIDTH;
    }

    /**
     * Computes the top-left Y coordinate of a row.
     */
    public double getCellOriginY(int row) {
        return START_Y + row * CELL_HEIGHT;
    }

    /**
     * Validates if row and column coordinates are within grid bounds.
     */
    public boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    public Tile getTile(int row, int col) {
        if (!isValidCoordinate(row, col)) {
            return null;
        }
        return tiles[row][col];
    }

    public void setTile(int row, int col, Tile tile) {
        if (isValidCoordinate(row, col)) {
            tiles[row][col] = tile;
        }
    }

    public Lane getLane(int row) {
        if (row >= 0 && row < ROWS) {
            return lanes[row];
        }
        return null;
    }

    /**
     * Updates all grid tiles.
     */
    public void update(double deltaTime) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (tiles[r][c] != null) {
                    tiles[r][c].update(deltaTime);
                }
            }
        }
    }

    /**
     * Renders all grid tiles onto the canvas.
     */
    public void render(GraphicsContext gc) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (tiles[r][c] != null) {
                    tiles[r][c].render(gc);
                }
            }
        }
    }
}
