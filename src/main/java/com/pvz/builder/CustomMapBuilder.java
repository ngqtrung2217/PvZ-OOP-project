package com.pvz.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.pvz.core.GameManager;
import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.TileType;

/**
 * Default {@link MapBuilder}. Starts from an all-grass board with the standard sun balance.
 */
public class CustomMapBuilder implements MapBuilder {
    private final TileType[][] tiles = new TileType[GameBoard.ROWS][GameBoard.COLS];
    private final List<PrePlanted> prePlanted = new ArrayList<>();
    private final Set<String> validPlantIds;
    private int startingSun = GameManager.STARTING_SUN;

    public CustomMapBuilder(Set<String> validPlantIds) {
        this.validPlantIds = Set.copyOf(validPlantIds);
        for (int r = 0; r < GameBoard.ROWS; r++) {
            Arrays.fill(tiles[r], TileType.GRASS);
        }
    }

    @Override
    public MapBuilder setTile(int row, int col, TileType type) {
        requireValidCell(row, col);
        if (type == null) {
            throw new IllegalArgumentException("Tile type cannot be null");
        }
        tiles[row][col] = type;
        return this;
    }

    @Override
    public MapBuilder setStartingSun(int sun) {
        if (sun < 0) {
            throw new IllegalArgumentException("Starting sun cannot be negative");
        }
        this.startingSun = sun;
        return this;
    }

    @Override
    public MapBuilder addPrePlanted(int row, int col, String plantId) {
        requireValidCell(row, col);
        PrePlanted plant = new PrePlanted(row, col, plantId);
        if (!validPlantIds.contains(plantId)) {
            throw new IllegalArgumentException("Unknown plant ID: " + plantId);
        }
        prePlanted.add(plant);
        return this;
    }

    @Override
    public GameMap build() {
        validatePlantCapacity();
        // TODO(An Nguyen): check allowedTiles / isSupport when plants_spec.json exists
        return new GameMap(tiles, startingSun, prePlanted);
    }

    /**
     * Checked when building because terrain may change after planting.
     */
    private void validatePlantCapacity() {
        int[][] counts = new int[GameBoard.ROWS][GameBoard.COLS];
        for (PrePlanted plant : prePlanted) {
            int row = plant.getRow();
            int col = plant.getCol();
            counts[row][col]++;
            if (counts[row][col] > plantCapacity(tiles[row][col])) {
                throw new IllegalStateException("Too many plants on " + tiles[row][col]
                        + " cell (" + row + ", " + col + ")");
            }
        }
    }

    /** Water holds two: a supporting plant and the plant standing on it. */
    private static int plantCapacity(TileType type) {
        return switch (type) {
            case GRASS -> 1;
            case WATER -> 2;
            case CRATER -> 0;
        };
    }

    private static void requireValidCell(int row, int col) {
        if (row < 0 || row >= GameBoard.ROWS || col < 0 || col >= GameBoard.COLS) {
            throw new IllegalArgumentException("Cell out of bounds: (" + row + ", " + col + ")");
        }
    }
}