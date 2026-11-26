package com.pvz.builder;

import java.util.ArrayList;
import java.util.List;

import com.pvz.model.board.GameBoard;
import com.pvz.model.tile.CraterTile;
import com.pvz.model.tile.GrassTile;
import com.pvz.model.tile.Tile;
import com.pvz.model.tile.TileType;
import com.pvz.model.tile.WaterTile;
import com.pvz.model.wave.Wave;

/**
 * Immutable map configuration: name, terrain, starting sun, allowed plants, number of
 * turns and scripted waves. Describes how a session starts and is never changed by
 * playing it.
 */
public final class GameMap {
    private final String name;
    private final TileType[][] tiles;
    private final int startingSun;
    private final List<String> allowedPlants;
    private final Integer maxTurns; // null = endless
    private final List<WaveConfig> waves;

    // Package-private: other packages must create maps through a MapBuilder
    GameMap(String name, TileType[][] tiles, int startingSun, List<String> allowedPlants,
            Integer maxTurns, List<WaveConfig> waves) {
        this.name = name;
        this.tiles = new TileType[tiles.length][];

        for (int r = 0; r < tiles.length; r++) {
            this.tiles[r] = tiles[r].clone();
        }

        this.startingSun = startingSun;
        this.allowedPlants = List.copyOf(allowedPlants);
        this.maxTurns = maxTurns;
        this.waves = List.copyOf(waves);
    }

    public String getName() {
        return name;
    }

    /** Plants offered in the seed bar, in order. The list cannot be modified. */
    public List<String> getAllowedPlants() {
        return allowedPlants;
    }

    /** @return Number of turns to survive, or null for endless. */
    public Integer getMaxTurns() {
        return maxTurns;
    }

    public boolean isEndless() {
        return maxTurns == null;
    }

    /**
     * Returns the terrain of a cell.
     * 
     * @return Tile type, or null if out of grid bounds.
     */
    public TileType getTileType(int row, int col) {
        if (row < 0 || row >= GameBoard.ROWS || col < 0 || col >= GameBoard.COLS) {
            return null;
        }
        return tiles[row][col];
    }

    public int getStartingSun() {
        return startingSun;
    }

    public List<WaveConfig> getWaves() {
        return waves;
    }

    /**
     * Creates this map's waves with nothing spawned yet. Each call returns independent
     * waves.
     */
    public List<Wave> createWaves() {
        List<Wave> created = new ArrayList<>();
        for (WaveConfig wave : waves) {
            created.add(wave.createWave());
        }
        return created;
    }

    /**
     * Creates a new board with this map's terrain. Each call returns an independent
     * board.
     */
    public GameBoard createBoard() {
        GameBoard board = new GameBoard();
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                board.setTile(r, c, createTile(board, r, c));
            }
        }
        return board;
    }

    private Tile createTile(GameBoard board, int row, int col) {
        double x = board.getCellOriginX(col);
        double y = board.getCellOriginY(row);
        double w = GameBoard.CELL_WIDTH;
        double h = GameBoard.CELL_HEIGHT;

        return switch (tiles[row][col]) {
            case GRASS -> new GrassTile(row, col, x, y, w, h);
            case WATER -> new WaterTile(row, col, x, y, w, h);
            case CRATER -> new CraterTile(row, col, x, y, w, h);
        };
    }
}
